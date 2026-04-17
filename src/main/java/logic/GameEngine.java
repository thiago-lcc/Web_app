package logic;

import ai.AiService;
import model.Agent;
import model.GameState;
import model.Message;
import model.Round;

import java.io.IOException;
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
    public void eliminatePlayer(GameState gameState){

        Scanner scanner = new Scanner(System.in);
        int guess = scanner.nextInt();
        gameState.getAgent(guess).eliminate();
    }

    public Round playRound(GameState gameState){

        String question = this.getQuestion();

        List<Agent> ais = gameState.currAis();
        List<Agent> impostors = gameState.currImpostors();


        try {

            List<Message> messages = AiService.getMessages(question, ais);
            for(Agent impostor: impostors) messages.add(impostor.getMessage(question));

            messages.sort((a,b) -> a.agentId() - b.agentId());


            for(Message message: messages) System.out.println(message.agentId() + ": " + message.content());
            this.eliminatePlayer(gameState);


            if(gameState.numCurrImpostors() == 0 || gameState.numCurrAIs() <= 4 - gameState.totalImpostors())
                gameState.endGame();

            return new Round(question, messages, gameState.roundNum()+1);
            }

        catch (IOException e){

            throw new RuntimeException("Failed to get AI answers", e);
        }
    }

    public void run(GameState gameState){

        while(!gameState.isOver()) gameState.rounds.add(this.playRound(gameState));

    }
}
