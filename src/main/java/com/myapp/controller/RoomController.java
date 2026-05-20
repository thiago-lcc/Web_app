package com.myapp.controller;

import com.myapp.logic.room.RoomEngine;
import com.myapp.logic.room.RoomEngine.GetRoomResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@CrossOrigin
public class RoomController {

    public record LeaveEventMessage(String leftNickname){}
    public record RemoveRequestMessage(@NotBlank @Size(max = 25) String removedNickname){}
    public record RemoveEventMessage(String removedNickname){}
    public record StartEventMessage(boolean gameStarted){}

    private final RoomEngine roomEngine;

    public RoomController(RoomEngine roomEngine){

        this.roomEngine = roomEngine;
    }


    @MessageMapping("/room/{roomId}/leave")
    @SendTo("/topic/room/{roomId}")
    public LeaveEventMessage leaveRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        String nickname = roomEngine.leaveRoom(roomId, accessor.getSessionId());

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        sessionAttributes.remove("roomId");
        sessionAttributes.remove("nickname");

        return new LeaveEventMessage(nickname);
    }

    @MessageMapping("/room/{roomId}/remove")
    @SendTo("/topic/room/{roomId}")
    public RemoveEventMessage removeFromRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor,
                                             @Valid RemoveRequestMessage msg){

        String nickname = msg.removedNickname();

        roomEngine.removeFromRoom(roomId, accessor.getSessionId(), nickname);

        return new RemoveEventMessage(nickname);
    }

    @MessageMapping("/room/{roomId}/start")
    @SendTo("/topic/room/{roomId}")
    public StartEventMessage startGame(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        roomEngine.startGame(roomId, accessor.getSessionId());

        return new StartEventMessage(true);
    }

    @GetMapping("/room/{roomId}")
    @ResponseBody
    public GetRoomResponse getRoom(@PathVariable String roomId){

        try{

           return roomEngine.getRoomResponse(roomId);
        }
        catch (NoSuchElementException e){

            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            e.getMessage());
        }
    }
}
