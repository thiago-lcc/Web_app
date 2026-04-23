package com.myapp.controller;

import com.myapp.logic.GameEngine;
import com.myapp.model.Agent;
import com.myapp.model.GameState;
import com.myapp.model.Message;
import com.myapp.model.Round;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/game")
public class GameController {

    public record RoundRequest(String question, List<Message> impostorMessages){}


    private final GameEngine engine;

    public GameController(GameEngine gameEngine){

        this.engine = gameEngine;
    }

    @PostMapping
    public String startGame(@RequestBody List<Integer> impostorIds){

        List<Agent> agents = engine.createAgents(impostorIds);

        String gameId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        engine.games.put(gameId, new GameState(agents));

        return gameId;

    }

    @PostMapping("/{gameId}/round")
    public Round playRound(@PathVariable String gameId, @RequestBody RoundRequest request){

        GameState game = engine.games.get(gameId);
        if(game == null) throw new NullPointerException("Game not found");

        return engine.playRound(game, request.impostorMessages, request.question);
    }

    @PostMapping("/{gameId}/eliminate")
    public void eliminateAgent(@PathVariable String gameId, @RequestBody int agentId){

        GameState game = engine.games.get(gameId);
        if(game == null) throw new NullPointerException("Game not found");

        engine.eliminateAgent(game, agentId);
        game.isOver();
    }

    @GetMapping("/{gameId}")
    public GameState getGame(@PathVariable String gameId){

        return engine.games.get(gameId);
    }

}
