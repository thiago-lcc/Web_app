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
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@Controller
@CrossOrigin
public class GameController {

    public record QuestionRequestMsg(@NotBlank @Size(max = 100) String question){}
    public record AnswerMsg(@NotBlank @Size(max = 100) String answer){}
    public record EliminateRequestMsg(@Min(1) @Max(8) int eliminatedId){}

    private final GameEngine gameEngine;

    public GameController(GameEngine gameEngine){

        this.gameEngine = gameEngine;
    }


    @MessageMapping("/game/{gameId}/question")
    @SendTo("/topic/game/{gameId}")
    public void askQuestion(@DestinationVariable String gameId, @Valid GameController.QuestionRequestMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.addNewQuestion(gameId, msg.question(), accessor.getSessionId());
    }

    @MessageMapping("/game/{gameId}/answer")
    public void answerQuestion(@DestinationVariable String gameId, @Valid AnswerMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.addImpostorMessage(gameId, msg.answer(), accessor.getSessionId());
    }

    @MessageMapping("/game/{gameId}/eliminate")
    @SendTo("/topic/game/{gameId}")
    public void eliminateAgent(@DestinationVariable String gameId, @Valid EliminateRequestMsg msg, SimpMessageHeaderAccessor accessor){

        gameEngine.eliminateAgent(gameId, msg.eliminatedId(), accessor.getSessionId());

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
