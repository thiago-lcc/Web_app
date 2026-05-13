package com.myapp.logic.game;

import com.myapp.logic.game.ai.AiService;
import com.myapp.model.game.Agent;
import com.myapp.model.game.GameState;
import com.myapp.model.game.Message;
import com.myapp.model.game.Round;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerErrorException;

import java.util.*;

@Service
public class GameEngine {

    public record GetGameResponse(String currQuestion, List<Round> rounds, GameState.Phase gamePhase, String whoWon){}
    public record QuestionEventMsg(String question){}
    public record EliminateResponseMsg(int eliminatedId){}
    public record GameOverEventMsg(String winner, int lastElimination) {}

    private final AiService aiService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameStorage gameStorage;

    public GameEngine(AiService aiService, SimpMessagingTemplate messagingTemplate, GameStorage gameStorage) {

        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
        this.gameStorage = gameStorage;
    }

    public void addImpostorMessage(String gameId, String answer, String token){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game) {

            if (game.getPhase() != GameState.Phase.WAITING_FOR_ANSWERS)
                throw new IllegalStateException("Cannot accept answers at current phase");

            int id = game.getIdByToken(token);

            if (!game.getAgent(id).isALive())
                    throw new IllegalStateException("Cannot accept answers from agents that were eliminated");

            if (game.alreadyAnswered(id))
                throw new IllegalStateException("Impostor already answered this round");

            Message message = new Message(id, answer);
            game.addImpostorMessage(message);

            if (game.numPendingMessages() == game.numCurrAgents())
                messagingTemplate.convertAndSend("/topic/game/" + gameId, playRound(game));

        }
    }

    private void addAiMessages(String gameId){

        GameState game = gameStorage.getGame(gameId);

        try{
            messagingTemplate.convertAndSend("/topic/game/" + gameId,new QuestionEventMsg(game.getCurrQuestion()));
            List<Message> aiMessages = aiService.getMessages(game.getCurrQuestion(), game.currAis());


            synchronized (game){

                game.addAiMessages(aiMessages);
                if (game.numPendingMessages() == game.numCurrAgents())
                    messagingTemplate.convertAndSend("/topic/game/" + gameId, playRound(game));
            }

        }
        catch (Exception e){

            synchronized (game){

                game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
                game.clearPendingMessages();
                game.discardCurrQuestion();
            }

            throw new ServerErrorException("Failed to get AI answers", e);
        }
    }

    public void addNewQuestion(String gameId, String question, String token){

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            if(game.getPhase() != GameState.Phase.WAITING_FOR_QUESTION)
                throw new IllegalStateException("Cannot accept questions at current phase");

            if(!token.equals(game.getGuesserToken()))
                throw new IllegalArgumentException("Guesser token does not match");

            game.setCurrQuestion(question);

            game.setPhase(GameState.Phase.WAITING_FOR_ANSWERS);
        }

        addAiMessages(gameId);
    }

    private Round playRound(GameState game) {

        synchronized (game) {

            List<Message> messages = new ArrayList<>(game.getPendingMessages());

            messages.sort(Comparator.comparingInt(Message::agentId));

            Round round = new Round(game.getCurrQuestion(), messages, game.roundNum() + 1);
            game.addRound(round);
            game.setPhase(GameState.Phase.WAITING_FOR_ELIMINATION);

            game.clearPendingMessages();
            game.discardCurrQuestion();

            return round;

            }
        }

    public void eliminateAgent(String gameId, int id, String token) {

        GameState game = gameStorage.getGame(gameId);

        synchronized (game) {

            if (game.getPhase() != GameState.Phase.WAITING_FOR_ELIMINATION)
                throw new IllegalStateException("Cannot eliminate agent in current phase");

            if (!token.equals(game.getGuesserToken()))
                throw new IllegalArgumentException("Guesser token does not match");

            Agent agent = game.getAgent(id);

            if (!agent.isALive())
                throw new IllegalStateException("Agent was already eliminated");

            agent.eliminate();

            if (!game.isOver()){
                game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
                messagingTemplate.convertAndSend("/topic/game/" + gameId,
                        new EliminateResponseMsg(id));
            }

            else{
                game.setPhase(GameState.Phase.OVER);
                messagingTemplate.convertAndSend("/topic/game/" + gameId,
                        new GameOverEventMsg(game.whoWon(), id));
            }

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
}

