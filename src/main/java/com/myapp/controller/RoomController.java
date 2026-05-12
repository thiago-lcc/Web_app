package com.myapp.controller;

import com.myapp.logic.room.RoomEngine;
import com.myapp.logic.room.RoomGateway;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
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

    private final RoomEngine roomEngine;
    private final RoomGateway roomGateway;
    private final SimpMessagingTemplate messageTemplate;

    public RoomController(RoomEngine roomEngine, RoomGateway roomGateway, SimpMessagingTemplate messageTemplate){

        this.roomEngine = roomEngine;
        this.roomGateway = roomGateway;
        this.messageTemplate = messageTemplate;
    }

    @MessageMapping("/room/create")
    public void createRoom(@Valid CreateRequestMessage msg, SimpMessageHeaderAccessor accessor){

        String sessionId = accessor.getSessionId();
        String roomId = roomGateway.createRoom(msg.creatorNickname(), sessionId);

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        sessionAttributes.put("roomId", roomId);
        sessionAttributes.put("nickname", msg.creatorNickname());

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

        String nickname = msg.joinedNickname();

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        sessionAttributes.put("roomId", roomId);
        sessionAttributes.put("nickname", nickname);

        roomGateway.joinRoom(roomId, accessor.getSessionId(),nickname);

        return new JoinEventMessage(nickname);
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
    public RoomEngine.GetRoomResponse getRoom(@PathVariable String roomId){

        try{

           return roomEngine.getRoomResponse(roomId);
        }
        catch (NoSuchElementException e){

            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            e.getMessage());
        }
    }
}
