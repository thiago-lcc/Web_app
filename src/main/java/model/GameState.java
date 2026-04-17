package model;

import java.util.ArrayList;
import java.util.List;

public class GameState{

    public List<Agent> agents;
    public List<Round> rounds = new ArrayList<>();

    private boolean gameOver = false;
    private final int totalImpostors;
    

    public GameState(List<Agent> agents){

        this.agents = agents;

        int num = 0;
        for(Agent agent: agents)if(agent.isImpostor())num++;
        this.totalImpostors = num;
    }


    public int roundNum(){

        try {return this.rounds.size();}
        catch(NullPointerException e) {return 0;}
    }
    
    public int numCurrAgents(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive()) count++;

        return count;
    }

    public List<Agent> currImpostors(){

        List<Agent> impostors = new ArrayList<>();

        for(Agent agent: agents) if(agent.isALive() && agent.isImpostor()) impostors.add(agent);
        return impostors;
    }

    public List<Agent> currAis(){

        List<Agent> ais = new ArrayList<>();

        for(Agent agent: agents) if(agent.isALive() && !agent.isImpostor()) ais.add(agent);
        return ais;
    }

    public int numCurrImpostors(){

        int count = 0;
        for(Agent agent: this.agents) if(agent.isALive() && agent.isImpostor()) count++;

        return count;
    }

    public int numCurrAIs(){

        return numCurrAgents() - numCurrImpostors();
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
