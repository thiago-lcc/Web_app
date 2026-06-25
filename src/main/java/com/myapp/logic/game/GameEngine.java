package com.myapp.logic.game;

import com.myapp.logic.game.ai.AiService;
import com.myapp.logic.room.RoomGateway;
import com.myapp.model.game.*;
import com.myapp.model.game.GameState.Phase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerErrorException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

@Service
public class GameEngine {

    public record GetGameResponse(String currQuestion, List<Round> rounds, Phase gamePhase, String whoWon){}
    public record QuestionEventMsg(String type, String question){}
    public record PlayRoundEventMsg(String type, Round round){}
    public record EliminateEventMsg(String type, int eliminatedId){}
    public record GameOverEventMsg(String type, String winner, int lastElimination) {}

    private final ApplicationEventPublisher eventPublisher;
    private final AiService aiService;
    private final GameStorage gameStorage;
    private final RoomGateway roomGateway;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public GameEngine(AiService aiService, ApplicationEventPublisher eventPublisher, GameStorage gameStorage, RoomGateway roomGateway) {

        this.aiService = aiService;
        this.eventPublisher = eventPublisher;
        this.gameStorage = gameStorage;
        this.roomGateway = roomGateway;
    }

    private void setPhase(String gameId, Phase phase){

        GameState game = gameStorage.getGame(gameId);

        game.setPhase(phase);
        Instant newPhaseInstant = Instant.now();

        switch (phase){

            case WAITING_FOR_QUESTION -> {
                scheduler.schedule(() -> {
                    if(game.phaseStartedAt().equals(newPhaseInstant))
                        handleQuestionTimeout(gameId);
                    },
                        15 + 1, SECONDS);
            }

            case WAITING_FOR_ELIMINATION -> {
                scheduler.schedule(() -> {
                    if(game.phaseStartedAt().equals(newPhaseInstant))
                        handleEliminationTimeout(gameId);},
                        10 + 1, SECONDS);
            }

            case WAITING_FOR_ANSWERS -> {
                scheduler.schedule(() -> {
                    if(game.phaseStartedAt().equals(newPhaseInstant))
                        handleAnswerTimeout(gameId);},
                        20 + 1, SECONDS);
            }
        }

    }

    public void startGame(String gameId){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            setPhase(gameId, Phase.WAITING_FOR_QUESTION);
        }
    }

    private void handleEliminationBroadcast(String gameId, int eliminatedId){

        GameState game = gameStorage.getGame(gameId);

        if (!game.isOver()){
            setPhase(gameId, Phase.WAITING_FOR_QUESTION);
            eventPublisher.publishEvent(new GameEvent<>(
                    gameId,
                    new EliminateEventMsg("PLAYER_ELIMINATED", eliminatedId)
            ));
        }

        else{
            setPhase(gameId, Phase.OVER);
            eventPublisher.publishEvent(new GameEvent<>(
                    gameId,
                    new GameOverEventMsg("GAME_OVER" ,game.whoWon(), eliminatedId)));
        }
    }

    private void handleRoundBroadcast(String gameId, Round round){

        eventPublisher.publishEvent(new GameEvent<>(
                gameId,
                new PlayRoundEventMsg("ROUND", round)));
    }

    private void handleQuestionBroadcast(String gameId){

        GameState game = gameStorage.getGame(gameId);

        eventPublisher.publishEvent(new GameEvent<>(
                gameId,
                new QuestionEventMsg("QUESTION", game.getCurrQuestion())));
    }

    private void handleEliminationTimeout(String gameId){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            List<Agent> agents = game.currAgents();

            Agent agent = agents.get(
                    new Random().nextInt(agents.size()));

            agent.eliminate();

            handleEliminationBroadcast(gameId, agent.getId());
        }
    }

    private void handleAnswerTimeout(String gameId){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            List<Agent> impostors = game.currImpostors();
            List<Integer> pendingAMessagesIds = game.getPendingMessages()
                    .stream().map(Message::agentId).toList();

            for(Agent impostor: impostors){

                if(!pendingAMessagesIds.contains(impostor.getId()))
                    game.addImpostorMessage(new Message(impostor.getId(), "..."));
            }

            handleRoundBroadcast(gameId, playRound(gameId));
        }
    }

    private void handleQuestionTimeout(String gameId){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            game.setCurrQuestion("...");
            setPhase(gameId, Phase.WAITING_FOR_ANSWERS);
            handleQuestionBroadcast(gameId);
        }
    }

    public void addImpostorMessage(String gameId, String answer, String token){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game) {

            if (game.getPhase() != Phase.WAITING_FOR_ANSWERS)
                throw new IllegalStateException("Cannot accept answers at current phase");

            int id = game.getIdByToken(token);

            if (!game.getAgent(id).isALive())
                    throw new IllegalStateException("Cannot accept answers from agents that were eliminated");

            if (game.alreadyAnswered(id))
                throw new IllegalStateException("Impostor already answered this round");

            Message message = new Message(id, answer);
            game.addImpostorMessage(message);

            if (game.numPendingMessages() == game.numCurrAgents())
                handleRoundBroadcast(gameId, playRound(gameId));

        }
    }

    private void addAiMessages(String gameId){

        GameState game = gameStorage.getGame(gameId);

        try{
            handleQuestionBroadcast(gameId);

            List<Message> aiMessages = aiService.getMessages(game.getCurrQuestion(), game.currAis());


            synchronized (game){

                game.addAiMessages(aiMessages);
                if (game.numPendingMessages() == game.numCurrAgents())
                    eventPublisher.publishEvent(new GameEvent<>(
                            gameId,
                            new PlayRoundEventMsg("ROUND", playRound(gameId))
                    ));
            }

        }
        catch (Exception e){

            synchronized (game){

                setPhase(gameId, Phase.WAITING_FOR_QUESTION);
                game.clearPendingMessages();
                game.discardCurrQuestion();
            }

            throw new ServerErrorException("Failed to get AI answers", e);
        }
    }

    public void addNewQuestion(String gameId, String question, String token){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            if(game.getPhase() != Phase.WAITING_FOR_QUESTION)
                throw new IllegalStateException("Cannot accept questions at current phase");

            if(!token.equals(game.getGuesserToken()))
                throw new IllegalArgumentException("Guesser token does not match");

            game.setCurrQuestion(question);

            setPhase(gameId, Phase.WAITING_FOR_ANSWERS);
        }

        addAiMessages(gameId);
    }

    private Round playRound(String gameId) {

        GameState game = gameStorage.getGame(gameId);

        synchronized (game) {

            List<Message> messages = new ArrayList<>(game.getPendingMessages());

            messages.sort(Comparator.comparingInt(Message::agentId));

            Round round = new Round(game.getCurrQuestion(), messages, game.roundNum() + 1);
            game.addRound(round);

            setPhase(gameId, Phase.WAITING_FOR_ELIMINATION);

            game.clearPendingMessages();
            game.discardCurrQuestion();

            return round;

            }
        }

    public void eliminateAgent(String gameId, int id, String token) {

        GameState game = gameStorage.getGame(gameId);

        synchronized (game) {

            if (game.getPhase() != Phase.WAITING_FOR_ELIMINATION)
                throw new IllegalStateException("Cannot eliminate agent in current phase");

            if (!token.equals(game.getGuesserToken()))
                throw new IllegalArgumentException("Guesser token does not match");

            Agent agent = game.getAgent(id);

            if (!agent.isALive())
                throw new IllegalStateException("Agent was already eliminated");

            agent.eliminate();

            handleEliminationBroadcast(gameId, id);

        }
    }

    public GetGameResponse getGameResponse(String gameId){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            return new GetGameResponse(
                game.getCurrQuestion(),
                game.getRounds(),
                game.getPhase(),
                game.whoWon()
            );
        }
    }

    public String createRematch(String gameId, String ownerToken, String nickname){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            if(!game.getOwnerToken().equals(ownerToken))
                throw new IllegalStateException("Owner token does not match");

            if(!game.isOver())
                throw new IllegalStateException("Game is not over yet");

            if(game.getRematchId() != null)
                throw new IllegalStateException("Rematch already created");

            String rematchId = roomGateway.createRoom(nickname, ownerToken, game.getName());
            game.setRematchId(rematchId);

            return rematchId;
        }

    }

    public String joinRematch(String gameId, String playerToken, String nickname){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            if(!game.isOver())
                throw new IllegalStateException("Game is not over yet");

            String rematchId = game.getRematchId();
            if(rematchId == null)
                throw new IllegalStateException("Rematch was not created yet");

            roomGateway.joinRoom(rematchId, playerToken, nickname);

            return rematchId;
        }
    }

}

