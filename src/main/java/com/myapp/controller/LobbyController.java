package com.myapp.controller;

import com.myapp.logic.room.RoomGateway;
import com.myapp.logic.room.RoomStorage.RoomSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
public class LobbyController {

    public record CreateRequestMessage(@NotBlank @Size(max = 25) String creatorNickname,
                                       @Size(max = 32) String roomName){}
    public record CreateResponseMessage(String roomId){}
    public record JoinMessage(@NotBlank @Size(max = 25) String joinedNickname){}

    public record GetLobbyResponse(List<RoomSummary> lobby){}

    private final RoomGateway roomGateway;

    public LobbyController(RoomGateway roomGateway){

        this.roomGateway = roomGateway;
    }

    @MessageMapping("/lobby/create-room")
    @SendToUser("/queue/lobby")
    public CreateResponseMessage createRoom(@Valid CreateRequestMessage msg, SimpMessageHeaderAccessor accessor){

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        if(sessionAttributes.get("roomId") != null || sessionAttributes.get("nickname") != null)
            throw new IllegalStateException("Cannot create room while in another room");

        String sessionId = accessor.getSessionId();
        String roomId = roomGateway.createRoom(msg.creatorNickname(), sessionId, msg.roomName());

        sessionAttributes.put("roomId", roomId);
        sessionAttributes.put("nickname", msg.creatorNickname());

        return new CreateResponseMessage(roomId);
    }

    @MessageMapping("/lobby/join-room/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public JoinMessage joinRoom(@DestinationVariable String roomId, @Valid JoinMessage msg,
                                                    SimpMessageHeaderAccessor accessor){

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        if(sessionAttributes.get("roomId") != null || sessionAttributes.get("nickname") != null)
            throw new IllegalStateException("Cannot join room while in another room");

        String nickname = msg.joinedNickname();

        sessionAttributes.put("roomId", roomId);
        sessionAttributes.put("nickname", nickname);

        roomGateway.joinRoom(roomId, accessor.getSessionId(),nickname);

        return new JoinMessage(nickname);
    }

    @GetMapping("/lobby")
    @ResponseBody
    public GetLobbyResponse getLobby(){

        return new GetLobbyResponse(roomGateway.getRoomSummaries());
    }
}
