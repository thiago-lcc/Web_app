package com.myapp.controller;

import com.myapp.logic.GameEngine;
import com.myapp.model.GameState;
import com.myapp.model.Round;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/game")
public class GameController {

    //add more request and response formats later
    public record QuestionRequestBody(String guesserToken, String question){}
    public record AnswerRequestBody(String impostorToken, String answer){}
    public record EliminateRequestBody(String guesserToken, int eliminatedId){}
    public record GetGameResponse(String currQuestion, List<Round> rounds, GameState.Phase gamePhase){}

    private final GameEngine engine;

    public GameController(GameEngine gameEngine){

        this.engine = gameEngine;
    }


    @PostMapping
    public String createGame(@RequestBody int numImpostors){

        return engine.createGame(numImpostors);

    }

    @PostMapping("/{gameId}/join/impostor")
    public String joinAsImpostor(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return engine.assignImpostor(game);
    }

    @PostMapping("/{gameId}/join/guesser")
    public String joinAsGuesser(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return engine.assignGuesser(game);
    }

    @PostMapping("/{gameId}/start")
    public void startGame(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        engine.startGame(game);
    }

    @PostMapping("/{gameId}/question")
    public void askQuestion(@PathVariable String gameId, @RequestBody QuestionRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.addNewQuestion(game, req.question(), req.guesserToken());
    }

    @PostMapping("/{gameId}/answer")
    public void answerQuestion(@PathVariable String gameId, @RequestBody AnswerRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.addImpostorMessage(game, req.answer(), req.impostorToken());
    }

    @PostMapping("/{gameId}/round")
    public Round playRound(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return engine.playRound(game);
    }

    @PostMapping("/{gameId}/eliminate")
    public void eliminateAgent(@PathVariable String gameId, @RequestBody EliminateRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.eliminateAgent(game, req.eliminatedId(), req.guesserToken());
    }

    @GetMapping("/{gameId}")
    public GetGameResponse getGame(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);

        return new GetGameResponse(game.getCurrQuestion(), game.getRounds(), game.getPhase());
    }

}
