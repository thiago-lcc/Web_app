package com.myapp.model.room;

public class Player {

    private final String nickname;
    private boolean isRoomOwner;
    private boolean isGuesser;

    public Player(String nickname, boolean isRoomOwner, boolean isGuesser){

        this.nickname = nickname;
        this.isRoomOwner = isRoomOwner;
        this.isGuesser = isGuesser;
    }


    public String getNickname() {

        return nickname;
    }

    public boolean isRoomOwner() {

        return isRoomOwner;
    }

    public void setAsRoomOwner(){

        this.isRoomOwner = true;
    }

    public void setAsRoomGuest(){

        this.isRoomOwner = false;
    }

    public boolean isGuesser() {

        return isGuesser;
    }

    public void setAsGuesser() {

        this.isGuesser = true;
    }

    public void setAsImpostor(){

        this.isGuesser = false;
    }
}
