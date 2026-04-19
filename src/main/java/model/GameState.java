package model;

import java.util.ArrayList;
import java.util.List;

public class GameState{

    private final List<Agent> agents;
    public List<Round> rounds = new ArrayList<>();

    private boolean gameOver = false;
    private final int totalImpostors;


    public GameState(List<Agent> agents){

        this.agents = agents;
        this.totalImpostors = numCurrImpostors();
    }


    public int roundNum(){

       return rounds.size();
    }

    public List<Agent> currImpostors(){

        return agents.stream()
                .filter(a -> a.isALive() && a.isImpostor())
                .toList();
    }

    public List<Agent> currAis(){

        return agents.stream()
                .filter(a -> a.isALive() && !a.isImpostor())
                .toList();
    }

    public int numCurrImpostors(){

        return currImpostors().size();
    }

    public int totalImpostors(){

        return totalImpostors;
    }

    public Agent getAgent(int id){

        for(Agent agent: agents) if(agent.getId() == id) return agent;

        throw new IllegalArgumentException("Id: " + id + " not found.");
    }

    public void endGame(){

        this.gameOver = true;
    }

    public boolean isOver(){

        return gameOver;
    }
}
