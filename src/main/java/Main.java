import logic.GameEngine;
import model.Agent;
import model.GameState;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        List<Agent> agents = new ArrayList<>();
        agents.add(new Agent(1,false));
        agents.add(new Agent(2,true));
        agents.add(new Agent(3,false));
        agents.add(new Agent(4,false));
        agents.add(new Agent(5,false));
        agents.add(new Agent(6,false));
        agents.add(new Agent(7,false));
        agents.add(new Agent(8,true));

        for(Agent agent: agents) System.out.println(agent.isImpostor());

        GameState gameState = new GameState(agents);

        GameEngine engine = new GameEngine();
        engine.run(gameState);
    }
}
