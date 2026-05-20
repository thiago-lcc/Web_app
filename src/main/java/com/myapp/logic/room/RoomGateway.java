package com.myapp.logic.room;

import com.myapp.model.room.Player;
import com.myapp.model.room.RoomState;
import org.springframework.stereotype.Service;
import com.myapp.logic.room.RoomStorage.RoomSummary;

import java.util.List;
import java.util.UUID;

@Service
public class RoomGateway {

    private final RoomStorage roomStorage;

    public RoomGateway(RoomStorage roomStorage){

        this.roomStorage = roomStorage;
    }

    public String createRoom(String nickname, String ownerToken, String name){

        String roomId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        Player owner = new Player(nickname, true, true);

        roomStorage.addRoom(roomId, new RoomState(ownerToken, owner, name));
        return roomId;
    }

    public void joinRoom(String roomId, String playerToken, String nickname){

        RoomState room = roomStorage.getRoom(roomId);

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

    public List<RoomSummary> getRoomSummaries(){

        return roomStorage.getRoomSummaries();
    }
}
