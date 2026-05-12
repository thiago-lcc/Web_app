package com.myapp.controller;

import com.myapp.logic.game.GameEngine;
import com.myapp.model.game.GameState;
import com.myapp.model.game.Round;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class GameController {

    public record QuestionMsg(@NotBlank @Size(max = 100) String question){}
    public record AnswerMsg(@NotBlank @Size(max = 100) String answer){}
    public record EliminateMsg(@Min(1) @Max(8) int eliminatedId){}

    public record GetGameResponse(String currQuestion, List<Round> rounds, GameState.Phase gamePhase, String whoWon){}

    private final GameEngine engine;

    public GameController(GameEngine gameEngine){

        this.engine = gameEngine;
    }


    @MessageMapping("/game/{gameId}/question")
    @SendTo("/topic/game/{gameId}")
    public QuestionMsg askQuestion(@PathVariable String gameId, @Valid QuestionMsg msg, SimpMessageHeaderAccessor accessor){

        GameState game = engine.getGame(gameId);
        engine.addNewQuestion(game, msg.question(), accessor.getSessionId());

        return msg;
    }

    @MessageMapping("/game/{gameId}/answer")
    @SendTo("/topic/game/{gameId}")
    public void answerQuestion(@PathVariable String gameId, @Valid AnswerMsg req, SimpMessageHeaderAccessor accessor){

        GameState game = engine.getGame(gameId);
        engine.addImpostorMessage(game, req.answer(), accessor.getSessionId());
    }

    @MessageMapping("/game/{gameId}/eliminate")
    @SendTo("/topic/game/{gameId}")
    public EliminateMsg eliminateAgent(@PathVariable String gameId, @Valid EliminateMsg msg, SimpMessageHeaderAccessor accessor){

        GameState game = engine.getGame(gameId);
        engine.eliminateAgent(game, msg.eliminatedId(), accessor.getSessionId());

        return msg;
    }

    @GetMapping("/game/{gameId}")
    @ResponseBody
    public GetGameResponse getGame(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);

        return new GetGameResponse(game.getCurrQuestion(), game.getRounds(), game.getPhase(), game.whoWon());
    }

}
