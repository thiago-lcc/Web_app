package com.myapp.logic.room;

import com.myapp.logic.game.GameEngine;
import com.myapp.model.game.GameState;
import com.myapp.model.room.Player;
import com.myapp.model.room.RoomState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomEngine {

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    private final GameEngine gameEngine;

    public RoomEngine(GameEngine gameEngine) {

        this.gameEngine = gameEngine;
    }


    public String createRoom(String nickname, String ownerToken){

        String roomId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        Player owner = new Player(nickname, true, true);

        rooms.put(roomId, new RoomState(ownerToken, owner));
        return roomId;
    }

    public RoomState getRoom(String roomId){

        RoomState room = rooms.get(roomId);
        if(room == null)
            throw new NoSuchElementException("Room not found");

        return room;
    }

    public void removeRoom(RoomState room){

        rooms.values().remove(room);
    }

    public void joinRoom(RoomState room, String playerToken, String nickname){

        synchronized (room){

            if(room.isFull())
                throw new IllegalStateException("Room is full");

            if(room.nicknameTaken(nickname))
                throw new IllegalArgumentException("Nickname is already taken");

            if(room.getPlayerByToken(playerToken) != null)
                throw new IllegalStateException("User is already this game");

            room.addNewPlayer(playerToken, new Player(nickname, false, false));
        }
    }

    public void throwIfPlayerIsNull(Player player){

        if(player == null)
            throw new NoSuchElementException("Player does not exist");
    }

    public void throwIfNotOwner(RoomState room, String ownerToken){

        synchronized (room){

            Player owner = room.getPlayerByToken(ownerToken);

            if(owner == null || !owner.isRoomOwner())
                throw new IllegalArgumentException("Owner token does not match");
        }
    }

    public void makeRandomGuesser(RoomState room){

        synchronized (room){

            Player player = room.getPlayers()
                    .get(new Random().nextInt(room.numPlayers()));

            room.setGuesser(player);
        }
    }

    public void makeRandomOwner(RoomState room){

        synchronized (room){

            Player player = room.getPlayers()
                    .get(new Random().nextInt(room.numPlayers()));

            room.setOwner(player);
        }
    }

    public String leaveRoom(RoomState room,String playerToken){

        synchronized (room) {

            Player player = room.getPlayerByToken(playerToken);
            throwIfPlayerIsNull(player);

            room.removePlayer(player);

            if (room.isEmpty())
                removeRoom(room);
            else{
                if (player.isGuesser())
                    makeRandomGuesser(room);
                if (player.isRoomOwner())
                    makeRandomOwner(room);
            }

            return player.getNickname();
        }
    }

    public void removeFromRoom(RoomState room, String ownerToken, String nickname){

        synchronized (room){
            throwIfNotOwner(room, ownerToken);

            Player player = room.getPlayerByNickname(nickname);
            throwIfPlayerIsNull(player);

            if(room.getPlayerByToken(ownerToken) == player)
                throw new IllegalStateException("Owner cannot be removed");

            room.removePlayer(player);

            if(player.isGuesser())
                makeRandomGuesser(room);
        }
    }

    public void startGame(String roomId, String ownerToken){

        RoomState room = rooms.get(roomId);

        synchronized (room){

            throwIfNotOwner(room, ownerToken);

            if(room.numPlayers() == 1)
                throw new IllegalStateException("Room needs at least 2 players to start game");

            gameEngine.createGame(room.numPlayers() - 1, roomId);
            GameState game = gameEngine.getGame(roomId);

            for(String playerToken: room.getTokens()){

                Player player = room.getPlayerByToken(playerToken);

                if(player.isGuesser())
                    gameEngine.assignGuesser(game, playerToken);
                else
                    gameEngine.assignImpostor(game, playerToken);

            }

            removeRoom(room);
        }
    }
}
