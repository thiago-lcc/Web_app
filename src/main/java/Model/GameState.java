package Model;

import java.util.List;

public class GameState{

    public List<Agent> agents;
    public List<Round> rounds;

    private boolean gameOver = false;
    

    public GameState(List<Agent> agents){

        this.agents = agents;
    }


    public int roundNum(){

        return this.rounds.size();
    }
    
    public int currAgents(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive());

        return count;
    }

    public int currImpostors(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive() && agent.isImpostor());

        return count;
    }

    public Agent getAgent(int id){

        for(Agent agent: this.agents) if(agent.getId() == id) return agent;

        throw new IllegalArgumentException("Id: " + id + " not found.");
    }

    public boolean isOver(){

        return this.gameOver;
    }
}
