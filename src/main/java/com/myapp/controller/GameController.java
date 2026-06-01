package com.myapp.controller;

import com.myapp.logic.game.GameEngine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@CrossOrigin(origins = "*")
public class GameController {

    public record QuestionRequestMsg(@NotBlank @Size(max = 100) String question){}
    public record AnswerMsg(@NotBlank @Size(max = 100) String answer){}
    public record EliminateRequestMsg(@Min(1) @Max(8) int eliminatedId){}
    public record RematchResponseMessage(String type, String roomId){}
    public record CreateRematchEventMessage(String type){}
    public record JoinRematchEventMessage(String type, String nickname){}

    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(GameEngine gameEngine, SimpMessagingTemplate messagingTemplate){

        this.gameEngine = gameEngine;
        this.messagingTemplate = messagingTemplate;
    }


    @MessageMapping("/game/{gameId}/question")
    public void askQuestion(@DestinationVariable String gameId, @Valid GameController.QuestionRequestMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.addNewQuestion(gameId, msg.question(), accessor.getSessionId());
    }

    @MessageMapping("/game/{gameId}/answer")
    public void answerQuestion(@DestinationVariable String gameId, @Valid AnswerMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.addImpostorMessage(gameId, msg.answer(), accessor.getSessionId());
    }

    @MessageMapping("/game/{gameId}/eliminate")
    public void eliminateAgent(@DestinationVariable String gameId, @Valid EliminateRequestMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.eliminateAgent(gameId, msg.eliminatedId(), accessor.getSessionId());

    }

    @MessageMapping("/game/{gameId}/create-rematch")
    @SendToUser("/queue/game")
    public RematchResponseMessage createRematch(@DestinationVariable String gameId, SimpMessageHeaderAccessor accessor){

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        String ownerToken = accessor.getSessionId();

        if(!gameId.equals(sessionAttributes.get("roomId")))
            throw new IllegalStateException("Game Id does not match");

        String rematchId = gameEngine.createRematch(gameId, ownerToken, sessionAttributes.get("nickname").toString());

        sessionAttributes.put("roomId", rematchId);

        messagingTemplate.convertAndSend("/topic/game/" + gameId, new CreateRematchEventMessage("REMATCH_CREATED"));
        return new RematchResponseMessage("REMATCH_ID", rematchId);
    }

    @MessageMapping("/game/{gameId}/join-rematch")
    @SendToUser("/queue/game")
    public RematchResponseMessage joinRematch(@DestinationVariable String gameId, SimpMessageHeaderAccessor accessor){

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if(sessionAttributes == null)
            throw new IllegalStateException("Invalid WebSocket connection or with no attributes");

        String playerToken = accessor.getSessionId();

        if(!gameId.equals(sessionAttributes.get("roomId")))
            throw new IllegalStateException("Game Id does not match");

        String nickname = sessionAttributes.get("nickname").toString();
        String rematchId = gameEngine.joinRematch(gameId, playerToken, nickname);

        sessionAttributes.put("roomId", rematchId);

        messagingTemplate.convertAndSend("/topic/room/" + rematchId,new JoinRematchEventMessage("PLAYER_JOINED", nickname));
        return new RematchResponseMessage("REMATCH_ID", rematchId);
    }

    @GetMapping("/game/{gameId}")
    @ResponseBody
    public GameEngine.GetGameResponse getGame(@PathVariable String gameId){

        try{
            return gameEngine.getGameResponse(gameId);
        }
        catch (NoSuchElementException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

}
