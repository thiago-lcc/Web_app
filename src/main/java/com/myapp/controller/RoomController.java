package com.myapp.controller;

import com.myapp.logic.room.RoomEngine;
import com.myapp.logic.room.RoomEngine.GetRoomResponse;
import com.myapp.logic.room.RoomEngine.PlayerRemovedEventMessage;
import com.myapp.logic.room.RoomEngine.PlayerLeftEventMessage;
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
@CrossOrigin(origins = "*")
public class RoomController {

    public record PlayerEventMessage(String type, String nickname){}
    public record PlayerRequestMessage(@NotBlank @Size(max =25) String nickname){}
    public record StartEventMessage(String type){}


    private final RoomEngine roomEngine;

    public RoomController(RoomEngine roomEngine){

        this.roomEngine = roomEngine;
    }


    @MessageMapping("/room/{roomId}/leave")
    @SendTo("/topic/room/{roomId}")
    public PlayerLeftEventMessage leaveRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        PlayerLeftEventMessage eventMessage = roomEngine.leaveRoom(roomId, accessor.getSessionId());

        sessionAttributes.remove("roomId");
        sessionAttributes.remove("nickname");

        return eventMessage;
    }

    @MessageMapping("/room/{roomId}/remove")
    @SendTo("/topic/room/{roomId}")
    public PlayerRemovedEventMessage removeFromRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor,
                                             @Valid PlayerRequestMessage msg){

        return roomEngine.removeFromRoom(roomId, accessor.getSessionId(), msg.nickname());
    }

    @MessageMapping("/room/{roomId}/start")
    @SendTo("/topic/room/{roomId}")
    public StartEventMessage startGame(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        roomEngine.startGame(roomId, accessor.getSessionId());

        return new StartEventMessage("GAME_STARTED");
    }

    @MessageMapping("/room/{roomId}/owner")
    @SendTo("/topic/room/{roomId}")
    public PlayerEventMessage makeOwner(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor, @Valid PlayerRequestMessage msg){

        roomEngine.makeOwner(roomId, accessor.getSessionId(), msg.nickname());

        return new PlayerEventMessage("NEW_OWNER",msg.nickname());
    }

    @MessageMapping("/room/{roomId}/guesser")
    @SendTo("/topic/room/{roomId}")
    public PlayerEventMessage makeGuesser(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor, @Valid PlayerRequestMessage msg){

        roomEngine.makeGuesser(roomId, accessor.getSessionId(), msg.nickname());

        return new PlayerEventMessage("NEW_GUESSER",msg.nickname());
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
