package com.myapp.model.room;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomState {

    private final Map<String, Player> playerByToken = new HashMap<>();
    private final String name;

    public RoomState(String playerToken, Player player, String name){

        addNewPlayer(playerToken, player);

        if(!(name == null || name.isEmpty()))
            this.name = name;
        else
            this.name = player.getNickname() + "'s room";

    }

    public Map<String, Player> getPlayerMap(){

        return playerByToken;
    }

    public String getName(){

        return name;
    }

    public void addNewPlayer(String playerToken, Player player){

        playerByToken.put(playerToken, player);
    }

    public Player getPlayerByToken(String playerToken){

        return playerByToken.get(playerToken);
    }

    public List<Player> getPlayers(){

        return playerByToken.values()
                .stream().toList();
    }

    public int numPlayers(){

        return getPlayers().size();
    }

    public void removePlayer(Player player){

        playerByToken.values().remove(player);
    }

    public boolean isEmpty(){

        return playerByToken.isEmpty();
    }

    public boolean isFull(){

        return (numPlayers() == 5);
    }

    public void setGuesser(Player player){

        playerByToken.values()
                .forEach(Player::setAsImpostor);
        player.setAsGuesser();
    }

    public void setOwner(Player player){

        playerByToken.values()
                .forEach(Player::setAsRoomGuest);
        player.setAsRoomOwner();
    }

    public boolean nicknameTaken(String nickname){

        return playerByToken.values().stream()
                .map(Player::getNickname)
                .anyMatch(s -> s.equals(nickname));
    }

    public Player getPlayerByNickname(String nickname){

        return playerByToken.values().stream()
                .filter(p -> nickname.equals(p.getNickname()))
                .findFirst()
                .orElse(null);

    }

}
