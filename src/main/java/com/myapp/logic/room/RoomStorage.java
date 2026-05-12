package com.myapp.logic.room;

import com.myapp.model.room.RoomState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomStorage {

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    public void addRoom(String roomId, RoomState room){

        rooms.put(roomId, room);
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

}
