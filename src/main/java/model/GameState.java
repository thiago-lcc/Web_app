package model;

import java.util.List;

public class GameState{

    public List<Agent> agents;
    public List<Round> rounds;

    private boolean gameOver = false;
    private final int totalImpostors;
    

    public GameState(List<Agent> agents){

        this.agents = agents;

        int num = 0;
        for(Agent agent: agents)if(agent.isImpostor())num++;
        this.totalImpostors = num;
    }


    public int roundNum(){

        return this.rounds.size();
    }
    
    public int currAgents(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive()) count++;

        return count;
    }

    public int currImpostors(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive() && agent.isImpostor()) count++;

        return count;
    }

    public int totalImpostors(){

        return this.totalImpostors;
    }

    public Agent getAgent(int id){

        for(Agent agent: this.agents) if(agent.getId() == id) return agent;

        throw new IllegalArgumentException("Id: " + id + " not found.");
    }

    public void endGame(){

        this.gameOver = true;
    }

    public boolean isOver(){

        return this.gameOver;
    }
}
