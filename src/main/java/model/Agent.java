package model;

public class Agent{

    private int game_id;
    private boolean is_alive = true;
    private boolean is_impostor = false;

    public Agent(int id){

        this.game_id = id;
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
    public Message getMessage(String question) {
        return new Message(-1, "a");
    }
}