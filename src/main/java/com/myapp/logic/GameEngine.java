package com.myapp.logic;

import com.myapp.model.Agent;
import com.myapp.model.GameState;
import com.myapp.model.Message;
import com.myapp.model.Round;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class GameEngine {

    private final AiService aiService;

    public final Map<String, GameState> games = new HashMap<>();

    public GameEngine(AiService aiService){

        this.aiService = aiService;
    }


    public List<Agent> createAgents(List<Integer> impostorIds){

        List<Agent> agents = new ArrayList<>();

        for(int impostorId: impostorIds) agents.add(new Agent(impostorId, true));
        for(int i = 1; i < 9; i++) if(!impostorIds.contains(i))
            agents.add(new Agent(i, false));

        return agents;
    }

    public Round playRound(GameState game, List<Message> impostorMessages, String question){

        List<Agent> ais = game.currAis();

        if(impostorMessages.size() != game.numCurrImpostors())
            throw new RuntimeException("Number of impostor answers and impostors don't match");

        try {

            List<Message> messages = aiService.getMessages(question, ais);
            messages.addAll(impostorMessages);

            messages.sort((a,b) -> a.agentId() - b.agentId());

            Round round = new Round(question, messages, game.roundNum()+1);
            game.rounds.add(round);

            return round;
            }

        catch (IOException e){

            throw new RuntimeException("Failed to get AI answers", e);
        }
    }

    public void eliminateAgent(GameState game, int id){

        Agent agent = game.getAgent(id);

        if(!agent.isALive()) throw new RuntimeException("Agent was already eliminated");

        agent.eliminate();

    }

    //Use only after game is confirmed to be over
    public boolean impostorsWon(GameState game){

        // win conditions
        if(game.numCurrImpostors() == 0) return false;
        else if (game.numCurrAis() <= 4 - game.totalImpostors()) return true;
        else throw new RuntimeException("Game was not over yet");

    }
}
