package model;


public class Agent{

    private final int game_id;
    private boolean is_alive = true;
    private final boolean is_impostor;

    public Agent(int id, boolean is_impostor){

        this.game_id = id;
        this.is_impostor = is_impostor;
    }

    public int getId(){

        return game_id;
    }

    public boolean isALive(){

        return is_alive;
    }

    public void eliminate(){

        this.is_alive = false;
    }

    public boolean isImpostor(){

        return is_impostor;
    }

}