package com.myapp.controller.event;

import com.myapp.logic.room.RoomEngine;
import com.myapp.model.game.GameEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    public record DisconnectEventMessage(String disconnectedNickname){}

    private final RoomEngine roomEngine;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(RoomEngine roomEngine, SimpMessagingTemplate messagingTemplate){

        this.roomEngine = roomEngine;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleGameEvent(GameEvent<?> event){
        messagingTemplate.convertAndSend(
                "/topic/game/" + event.gameId(),
                event.msg()
                );
    }

    @EventListener
    public void handlePlayerDisconnect(SessionDisconnectEvent event){

        String sessionId = event.getSessionId();

        Map<String, Object> sessionAttributes = StompHeaderAccessor.wrap(event.getMessage())
                .getSessionAttributes();

        if (sessionAttributes == null)
            return;

        Object roomIdObj = sessionAttributes.get("roomId");
        Object nicknameObj = sessionAttributes.get("nickname");

        if(roomIdObj == null || nicknameObj == null)
            return;

        String roomId = roomIdObj.toString();
        String nickname = nicknameObj.toString();


        roomEngine.leaveRoom(roomId, sessionId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, new DisconnectEventMessage(nickname));
    }

}
