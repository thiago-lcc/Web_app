package com.myapp.logic.game;

import com.myapp.logic.game.ai.AiService;
import com.myapp.model.game.Agent;
import com.myapp.model.game.GameState;
import com.myapp.model.game.Message;
import com.myapp.model.game.Round;
import com.myapp.model.room.Player;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameEngine {

    private final AiService aiService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, GameState> games = new ConcurrentHashMap<>();


    public GameEngine(AiService aiService, SimpMessagingTemplate messagingTemplate) {

        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
    }



    public void createGame(int numImpostors, String gameId) {

        List<Agent> agents = createAgents(numImpostors);

        games.put(gameId, new GameState(agents));
    }

    public GameState getGame(String gameId) {

        GameState game = games.get(gameId);
        if (game == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");

        return game;
    }

    //runs every minute
    @Scheduled(fixedRate = 60000)
    public void deleteFinishedGames(){

        //removes game if it's over for more than 10 minutes
        games.entrySet().removeIf(entry ->
                entry.getValue().getPhase() == GameState.Phase.OVER &&
                entry.getValue().finishedAt().isBefore(Instant.now().minus(Duration.ofMinutes(10))));
    }

    private List<Integer> createImpostorIds(int numImpostors) {

        return new Random()
                //Ids must be in the interval [1,8]
                .ints(1, 9)
                .distinct()
                .limit(numImpostors)
                .boxed()
                .toList();
    }

    public void assignImpostor(GameState game, String impostorToken){

            List<Integer> availableIds = game.availableImpostorIds();

            int impostorId = availableIds.get(
                 new Random().nextInt(availableIds.size()));

            game.setImpostorToken(impostorToken, impostorId);

    }

    public void assignPlayers(GameState game, Map<String, Player> playerMap) {

        synchronized (game){

            playerMap.forEach((token, player) -> {

                if(player.isGuesser())
                    game.setGuesserToken(token);

                else {
                    assignImpostor(game, token);
                }

            });

            game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
        }
    }

    private List<Agent> createAgents(int numImpostors) {

        List<Integer> impostorIds = createImpostorIds(numImpostors);

        List<Agent> agents = new ArrayList<>();

        for (int impostorId : impostorIds) agents.add(new Agent(impostorId, true));
        for (int i = 1; i < 9; i++)
            if (!impostorIds.contains(i))
                agents.add(new Agent(i, false));

        return agents;
    }

    public void addImpostorMessage(GameState game, String answer, String token){

        synchronized (game) {

            if (game.getPhase() != GameState.Phase.WAITING_FOR_ANSWERS)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot accept answers at current phase");

            try {
                int id = game.getIdByToken(token);

                if (!game.getAgent(id).isALive())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cannot accept answers from agents that were eliminated");

                if (game.alreadyAnswered(id))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Impostor already answered this round");

                Message message = new Message(id, answer);
                game.addImpostorMessage(message);

                if (game.numPendingMessages() == game.numCurrAgents())
                    messagingTemplate.convertAndSend(playRound(game));

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
    }

    public void addAiMessages(GameState game){

        try{
            List<Message> aiMessages = aiService.getMessages(game.getCurrQuestion(), game.currAis());

            synchronized (game){

                game.addAiMessages(aiMessages);
                if (game.numPendingMessages() == game.numCurrAgents())
                    messagingTemplate.convertAndSend(playRound(game));
            }

        }
        catch (Exception e){

            synchronized (game){

                game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
                game.clearPendingMessages();
                game.discardCurrQuestion();
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get AI answers", e);
        }
    }

    public void addNewQuestion(GameState game, String question, String token){

        synchronized (game){

            if(game.getPhase() != GameState.Phase.WAITING_FOR_QUESTION)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot accept questions at current phase");
            if(!token.equals(game.getGuesserToken()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Guesser token does not match");

            game.setCurrQuestion(question);

            game.setPhase(GameState.Phase.WAITING_FOR_ANSWERS);
        }

        addAiMessages(game);
    }

    public Round playRound(GameState game) {

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

    public void eliminateAgent(GameState game, int id, String token) {

        synchronized (game) {

            if (game.getPhase() != GameState.Phase.WAITING_FOR_ELIMINATION)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot eliminate agent in current phase");

            if (!token.equals(game.getGuesserToken()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Guesser token does not match");

            try {
                Agent agent = game.getAgent(id);

                if (!agent.isALive())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent was already eliminated");

                agent.eliminate();

                if (!game.isOver())
                    game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
                else
                    game.setPhase(GameState.Phase.OVER);

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent does not exist");
            }
        }
    }
}

