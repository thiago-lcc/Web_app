package com.myapp.logic.room;

import com.myapp.logic.game.GameGateway;
import com.myapp.model.room.Player;
import com.myapp.model.room.RoomState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoomEngine {

    private final RoomStorage roomStorage;
    private final GameGateway gameGateway;

    public record GetRoomResponse(List<Player> players){}

    public RoomEngine(GameGateway gameGateway, RoomStorage roomStorage) {

        this.roomStorage = roomStorage;
        this.gameGateway = gameGateway;
    }


    private void throwIfPlayerIsNull(Player player){

        if(player == null)
            throw new NoSuchElementException("Player does not exist");
    }

    private void throwIfNotOwner(RoomState room, String ownerToken){

        synchronized (room){

            Player owner = room.getPlayerByToken(ownerToken);

            if(owner == null || !owner.isRoomOwner())
                throw new IllegalArgumentException("Owner token does not match");
        }
    }

    private void makeRandomGuesser(RoomState room){

        synchronized (room){

            Player player = room.getPlayers()
                    .get(new Random().nextInt(room.numPlayers()));

            room.setGuesser(player);
        }
    }

    private void makeRandomOwner(RoomState room){

        synchronized (room){

            Player player = room.getPlayers()
                    .get(new Random().nextInt(room.numPlayers()));

            room.setOwner(player);
        }
    }

    public String leaveRoom(String roomId,String playerToken){

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room) {

            Player player = room.getPlayerByToken(playerToken);
            throwIfPlayerIsNull(player);

            room.removePlayer(player);

            if (room.isEmpty())
                roomStorage.removeRoom(room);
            else{
                if (player.isGuesser())
                    makeRandomGuesser(room);
                if (player.isRoomOwner())
                    makeRandomOwner(room);
            }

            return player.getNickname();
        }
    }

    public void removeFromRoom(String roomId, String ownerToken, String nickname){

        RoomState room = roomStorage.getRoom(roomId);

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

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room){

            throwIfNotOwner(room, ownerToken);

            if(room.numPlayers() == 1)
                throw new IllegalStateException("Room needs at least 2 players to start game");

            gameGateway.createGame(room.numPlayers() - 1, roomId);

            gameGateway.assignPlayers(roomId, room.getPlayerMap());

            roomStorage.removeRoom(room);
        }
    }

    public GetRoomResponse getRoomResponse(String roomId){

        RoomState room = roomStorage.getRoom(roomId);

        return new GetRoomResponse(
                room.getPlayers()
        );
    }
}
