package com.myapp.logic;

import com.myapp.model.Agent;
import com.myapp.model.GameState;
import com.myapp.model.Message;
import com.myapp.model.Round;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Service
public class GameEngine {

    private final AiService aiService;

    public final Map<String, GameState> games = new HashMap<>();

    public GameEngine(AiService aiService){

        this.aiService = aiService;
    }


    public GameState getGame(String gameId){

        GameState game = games.get(gameId);
        if(game == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");

        return game;
    }

    public List<Agent> createAgents(List<Integer> impostorIds){

        Set<Integer> set = new HashSet<>(impostorIds);

        if (set.size() != impostorIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate IDs");
        }

        for(int impostorId: impostorIds)
            if(impostorId > 8 || impostorId < 1)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "ID: " + impostorId +" is out of permitted range [1,8]");


        List<Agent> agents = new ArrayList<>();

        for(int impostorId: impostorIds) agents.add(new Agent(impostorId, true));
        for(int i = 1; i < 9; i++) if(!impostorIds.contains(i))
            agents.add(new Agent(i, false));

        return agents;
    }

    public Round playRound(GameState game, List<Message> impostorMessages, String question){

        if(game.getPhase() != GameState.Phase.WAITING_FOR_ROUND)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot play round in current phase");

        List<Agent> ais = game.currAis();

        if(impostorMessages.size() != game.numCurrImpostors())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Number of impostor answers and impostors don't match"
            );

        try {

            List<Message> messages = aiService.getMessages(question, ais);
            messages.addAll(impostorMessages);

            messages.sort((a,b) -> a.agentId() - b.agentId());

            Round round = new Round(question, messages, game.roundNum()+1);
            game.rounds.add(round);
            game.setPhase(GameState.Phase.WAITING_FOR_ELIMINATION);

            return round;
            }

        catch (IOException e){

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get AI answers",
                    e
            );
        }
    }

    public void eliminateAgent(GameState game, int id){

        if(game.getPhase() != GameState.Phase.WAITING_FOR_ELIMINATION)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot eliminate agent in current phase");

        try {
            Agent agent = game.getAgent(id);

            if(!agent.isALive())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent was already eliminated");

            agent.eliminate();

            if(!game.isOver()) game.setPhase(GameState.Phase.WAITING_FOR_ROUND);
            else game.setPhase(GameState.Phase.OVER);

        }
        catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agent does not exist");
        }

    }

    //Use only after game is confirmed to be over
    public boolean impostorsWon(GameState game){

        // win conditions
        if(game.numCurrImpostors() == 0) return false;
        else if (game.numCurrAis() <= 4 - game.totalImpostors()) return true;
        else throw new RuntimeException("Game was not over yet");

    }
}
