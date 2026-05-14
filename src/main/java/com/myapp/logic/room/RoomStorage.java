package com.myapp.logic.room;

import com.myapp.model.room.RoomState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomStorage {

    public record RoomSummary(String roomId, String name, int numPlayers){}
    public record LobbyUpdateEvent(List<RoomSummary> msg){}

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    public RoomStorage(ApplicationEventPublisher eventPublisher){

        this.eventPublisher = eventPublisher;
    }

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

    // runs every 3 seconds
    @Scheduled(fixedRate = 3000)
    public void publishRoomSummaries(){

        List<RoomSummary> summaries = new ArrayList<>();
        rooms.forEach((id, room) ->
                summaries.add(new RoomSummary(id, room.getName(), room.numPlayers())));

        eventPublisher.publishEvent(new LobbyUpdateEvent(summaries));
    }
}
