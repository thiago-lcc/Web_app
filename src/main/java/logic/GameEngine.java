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
    public String getQuestion(){

        System.out.println("Enter your question:");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    //placeholder
    public void eliminatePlayer(List<Agent> agents){

        Scanner scanner = new Scanner(System.in);
        int guess = scanner.nextInt();
        for(Agent agent: agents) if(agent.getId() == guess && agent.isALive()) agent.eliminate();
    }

    public Round playRound(GameState gameState){

        String question = this.getQuestion();

        List<Agent> agents = gameState.agents;
        List<Message> messages = AiService.generateAnswers(agents);

        for(Agent agent: agents) if(agent.isImpostor() && agent.isALive())
            messages.add(agent.getMessage(question));

        for(Message message: messages) System.out.println(message.agentId() + ": " + message.content());
        this.eliminatePlayer(agents);

        if(gameState.currImpostors() == 0 || gameState.currAIs() <= 4 - gameState.totalImpostors())
            gameState.endGame();

        return new Round(question, messages, gameState.roundNum()+1);
    }

    public void run(GameState gameState){

        while(!gameState.isOver()) gameState.rounds.add(this.playRound(gameState));

    }
}
