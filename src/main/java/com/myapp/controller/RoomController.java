package com.myapp.controller;

import com.myapp.logic.room.RoomEngine;
import com.myapp.model.room.Player;
import com.myapp.model.room.RoomState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@Controller
public class RoomController {

    public record JoinRequestMessage(@NotBlank @Size(max = 25) String joinedNickname){}
    public record JoinEventMessage(String joinedNickname){}
    public record CreateRequestMessage(@NotBlank @Size(max = 25) String creatorNickname){}
    public record CreateResponseMessage(String roomId){}
    public record LeaveEventMessage(String leftNickname){}
    public record RemoveRequestMessage(@NotBlank @Size(max = 25) String removedNickname){}
    public record RemoveEventMessage(String removedNickname){}
    public record StartEventMessage(boolean gameStarted){}

    public record GetRoomResponse(List<Player> players){}

    private final RoomEngine roomEngine;
    private final SimpMessagingTemplate messageTemplate;

    public RoomController(RoomEngine roomEngine, SimpMessagingTemplate messageTemplate){

        this.roomEngine = roomEngine;
        this.messageTemplate = messageTemplate;
    }

    @MessageMapping("/room/create")
    public void createRoom(@Valid CreateRequestMessage msg, SimpMessageHeaderAccessor accessor){

        String sessionId = accessor.getSessionId();
        String roomId = roomEngine.createRoom(msg.creatorNickname(), sessionId);

        // temporary: replace once guest logic is created
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);

        messageTemplate.convertAndSendToUser(
                sessionId,
                "/queue/room/created",
                new CreateResponseMessage(roomId),
                headerAccessor.getMessageHeaders());
    }

    @MessageMapping("/room/{roomId}/join")
    @SendTo("/topic/room/{roomId}")
    public JoinEventMessage joinRoom(@DestinationVariable String roomId, @Valid JoinRequestMessage msg,
                                     SimpMessageHeaderAccessor accessor){

        RoomState room = roomEngine.getRoom(roomId);
        String nickname = msg.joinedNickname();

        roomEngine.joinRoom(room, accessor.getSessionId(),nickname);

        return new JoinEventMessage(nickname);
    }

    @MessageMapping("/room/{roomId}/leave")
    @SendTo("/topic/room/{roomId}")
    public LeaveEventMessage leaveRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        RoomState room = roomEngine.getRoom(roomId);

        String nickname = roomEngine.leaveRoom(room, accessor.getSessionId());

        return new LeaveEventMessage(nickname);
    }

    @MessageMapping("/room/{roomId}/remove")
    @SendTo("/topic/room/{roomId}")
    public RemoveEventMessage removeFromRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor,
                                             @Valid RemoveRequestMessage msg){

        RoomState room = roomEngine.getRoom(roomId);

        String nickname = msg.removedNickname();

        roomEngine.removeFromRoom(room, accessor.getSessionId(), nickname);

        return new RemoveEventMessage(nickname);
    }

    @MessageMapping("/room/{roomId}/start")
    @SendTo("/topic/room/{roomId}")
    public StartEventMessage startGame(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor){

        roomEngine.startGame(roomId, accessor.getSessionId());

        return new StartEventMessage(true);
    }

    @GetMapping("/room/{roomId}")
    public GetRoomResponse getRoom(@PathVariable String roomId){

        try{

            RoomState room = roomEngine.getRoom(roomId);
            return new GetRoomResponse(room.getPlayers());
        }
        catch (NoSuchElementException e){

            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            e.getMessage());
        }
    }
}
