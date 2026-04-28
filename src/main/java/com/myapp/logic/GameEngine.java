package com.myapp.logic;

import com.myapp.model.Agent;
import com.myapp.model.GameState;
import com.myapp.model.Message;
import com.myapp.model.Round;
import org.springframework.http.HttpStatus;
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

    private final Map<String, GameState> games = new ConcurrentHashMap<>();


    public GameEngine(AiService aiService) {

        this.aiService = aiService;
    }



    public String createGame(int numImpostors) {

        List<Agent> agents = createAgents(numImpostors);

        String gameId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        games.put(gameId, new GameState(agents));

        return gameId;

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

    public String assignImpostor(GameState game){

        synchronized (game){

            if(game.getPhase() != GameState.Phase.WAITING_FOR_START)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot assign impostor after the game started");

            List<Integer> availableIds = game.availableImpostorIds();

            if (availableIds.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All impostor roles are already assigned");

            int impostorId = availableIds.get(
                 new Random().nextInt(availableIds.size()));

            String impostorToken = UUID.randomUUID()
                    .toString()
                    .replace("-", "");

            game.setImpostorToken(impostorToken, impostorId);
            return impostorToken;
        }

    }

    public String assignGuesser(GameState game) {

        synchronized (game){

            if(game.getPhase() != GameState.Phase.WAITING_FOR_START)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot assign guesser after the game started");

            if (game.getGuesserToken() != null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Guesser role was already assigned");

            String guesserToken = UUID.randomUUID()
                    .toString()
                    .replace("-", "");

            game.setGuesserToken(guesserToken);
            return guesserToken;
        }
    }

    public void startGame(GameState game){

        synchronized (game){

            if(game.getPhase() != GameState.Phase.WAITING_FOR_START)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Game has already started");

            if(!game.canStart())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Not all players have joined yet");

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
                    game.setPhase(GameState.Phase.WAITING_FOR_ROUND);

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
    }

    public void addAiMessages(GameState game){

        try{
            List<Message> aiMessages = aiService.getMessages(game.getCurrQuestion(), game.currAis());
            game.addAiMessages(aiMessages);

            if (game.numPendingMessages() == game.numCurrAgents())
                game.setPhase(GameState.Phase.WAITING_FOR_ROUND);

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

            if (game.getPhase() != GameState.Phase.WAITING_FOR_ROUND)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play round in current phase");


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
                    game.setPhase(GameState.Phase.WAITING_FOR_ROUND);
                else
                    game.setPhase(GameState.Phase.OVER);

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent does not exist");
            }
        }
    }
}

