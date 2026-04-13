package logic;

import ai.AiService;
import model.Agent;
import model.GameState;
import model.Message;
import model.Round;
import java.util.List;
import java.util.Scanner;


public class GameEngine {

    //placeholder
    public static String getQuestion(){

        System.out.println("Enter your question:");
        Scanner scanner = new Scanner(System.in);
        return scanner.next();
    }

    //placeholder
    public static void eliminatePlayer(List<Agent> agents){

        Scanner scanner = new Scanner(System.in);
        int guess = scanner.nextInt();
        for(Agent agent: agents) if(agent.getId() == guess && agent.isALive()) agent.eliminate();
    }

    public static Round playRound(GameState gameState){

        String question = getQuestion();

        List<Agent> agents = gameState.agents;
        List<Message> messages = AiService.generateAnswers(agents);

        for(Agent agent: agents) if(agent.isImpostor() && agent.isALive())
            messages.add(agent.getMessage(question));

        for(Message message: messages) System.out.println(message.agentId() + ": " + message.content());
        eliminatePlayer(agents);

        if(gameState.currImpostors() == 0 || gameState.currAIs() <= 4 - gameState.totalImpostors())
            gameState.endGame();

        return new Round(question, messages, gameState.roundNum());
    }

    public static void gameloop(List<Agent> agents){

        GameState gameState = new GameState(agents);

        while(!gameState.isOver()) playRound(gameState);

    }
}
