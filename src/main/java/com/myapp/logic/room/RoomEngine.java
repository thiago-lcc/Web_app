package com.myapp.logic.room;

import com.myapp.logic.game.GameEngine;
import com.myapp.logic.game.GameGateway;
import com.myapp.model.room.Player;
import com.myapp.model.room.RoomState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoomEngine {

    private final RoomStorage roomStorage;
    private final GameGateway gameGateway;
    private final GameEngine gameEngine;

    public record GetRoomResponse(List<Player> players){}
    public record PlayerLeftEventMessage(String type, String nickname, String owner, String guesser){}
    public record PlayerRemovedEventMessage(String type, String nickname, String guesser){}

    public RoomEngine(GameGateway gameGateway, RoomStorage roomStorage, GameEngine gameEngine) {

        this.roomStorage = roomStorage;
        this.gameGateway = gameGateway;
        this.gameEngine = gameEngine;
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

    public void makeOwner(String roomId, String ownerToken, String nickname){

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room){
            throwIfNotOwner(room, ownerToken);

            Player player = room.getPlayerByNickname(nickname);
            throwIfPlayerIsNull(player);

            if(nickname.equals(room.getPlayerByToken(ownerToken).getNickname()))
                throw new IllegalStateException("Player is already owner");

            room.setOwner(player);
        }
    }

    public void makeGuesser(String roomId, String ownerToken, String nickname){

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room){
            throwIfNotOwner(room, ownerToken);

            Player player = room.getPlayerByNickname(nickname);
            throwIfPlayerIsNull(player);

            if(nickname.equals(room.getGuesser().getNickname()))
                throw new IllegalStateException("Player is already guesser");

            room.setGuesser(player);
        }
    }

    public PlayerLeftEventMessage leaveRoom(String roomId,String playerToken){

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

                return new PlayerLeftEventMessage("PLAYER_LEFT",
                        player.getNickname(),
                        room.getOwner().getNickname(),
                        room.getGuesser().getNickname());
            }

            return new PlayerLeftEventMessage("PLAYER_LEFT",
                    player.getNickname(),
                    null,
                    null);
        }
    }

    public PlayerRemovedEventMessage removeFromRoom(String roomId, String ownerToken, String nickname){

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

            return new PlayerRemovedEventMessage("PLAYER_REMOVED",
                    nickname,
                    room.getGuesser().getNickname());
        }
    }

    public void startGame(String roomId, String ownerToken){

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room){

            throwIfNotOwner(room, ownerToken);

            if(room.numPlayers() == 1)
                throw new IllegalStateException("Room needs at least 2 players to start game");

            gameGateway.createGame(room.numPlayers() - 1, roomId, room.getName(), ownerToken);

            gameGateway.assignPlayers(roomId, room.getPlayerMap());

            gameEngine.startGame(roomId);

            roomStorage.removeRoom(room);
        }
    }

    public GetRoomResponse getRoomResponse(String roomId){

        RoomState room = roomStorage.getRoom(roomId);

        synchronized (room){

            return new GetRoomResponse(
                    room.getPlayers());
        }
    }
}
