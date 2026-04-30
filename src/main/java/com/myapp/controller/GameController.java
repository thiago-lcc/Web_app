package com.myapp.controller;

import com.myapp.logic.game.GameEngine;
import com.myapp.model.game.GameState;
import com.myapp.model.game.Round;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/game")
public class GameController {

    public record CreateGameRequest(@Min(1) @Max(4) int numImpostors){}
    public record QuestionRequestBody(@NotBlank String guesserToken, @NotBlank @Size(max = 100) String question){}
    public record AnswerRequestBody(@NotBlank String impostorToken, @NotBlank @Size(max = 100) String answer){}
    public record EliminateRequestBody(@NotBlank String guesserToken, @Min(1) @Max(8) int eliminatedId){}

    public record CreateGameResponse(@NotBlank String gameId){}
    public record JoinResponseBody(String token){}
    public record GetGameResponse(String currQuestion, List<Round> rounds, GameState.Phase gamePhase, String whoWon){}

    private final GameEngine engine;

    public GameController(GameEngine gameEngine){

        this.engine = gameEngine;
    }


    @PostMapping
    public CreateGameResponse createGame(@RequestBody @Valid CreateGameRequest req){

        return new CreateGameResponse(engine.createGame(req.numImpostors()));

    }

    @PostMapping("/{gameId}/join/impostor")
    public JoinResponseBody joinAsImpostor(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return new JoinResponseBody(engine.assignImpostor(game));
    }

    @PostMapping("/{gameId}/join/guesser")
    public JoinResponseBody joinAsGuesser(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return new JoinResponseBody(engine.assignGuesser(game));
    }

    @PostMapping("/{gameId}/start")
    public void startGame(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        engine.startGame(game);
    }

    @PostMapping("/{gameId}/question")
    public void askQuestion(@PathVariable String gameId, @RequestBody @Valid QuestionRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.addNewQuestion(game, req.question(), req.guesserToken());
    }

    @PostMapping("/{gameId}/answer")
    public void answerQuestion(@PathVariable String gameId, @RequestBody @Valid AnswerRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.addImpostorMessage(game, req.answer(), req.impostorToken());
    }

    @PostMapping("/{gameId}/round")
    public Round playRound(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);
        return engine.playRound(game);
    }

    @PostMapping("/{gameId}/eliminate")
    public void eliminateAgent(@PathVariable String gameId, @RequestBody @Valid EliminateRequestBody req){

        GameState game = engine.getGame(gameId);
        engine.eliminateAgent(game, req.eliminatedId(), req.guesserToken());
    }

    @GetMapping("/{gameId}")
    public GetGameResponse getGame(@PathVariable String gameId){

        GameState game = engine.getGame(gameId);

        return new GetGameResponse(game.getCurrQuestion(), game.getRounds(), game.getPhase(), game.whoWon());
    }

}
