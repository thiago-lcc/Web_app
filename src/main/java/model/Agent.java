package model;

import java.util.Scanner;

public class Agent{

    private final int game_id;
    private boolean is_alive = true;
    private boolean is_impostor = false;

    public Agent(int id, boolean is_impostor){

        this.game_id = id;
        this.is_impostor = is_impostor;
    }

    public int getId(){

        return this.game_id;
    }

    public boolean isALive(){

        return this.is_alive;
    }

    public void eliminate(){

        this.is_alive = false;
    }

    public boolean isImpostor(){

        return this.is_impostor;
    }

    //placeholder
    public Message getMessage(String question){

        System.out.println(question);
        Scanner scanner = new Scanner(System.in);
        return new Message(this.getId(), scanner.nextLine());
    }
}