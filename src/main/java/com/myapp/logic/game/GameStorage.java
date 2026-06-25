package com.myapp.logic.game;

import com.myapp.model.game.GameState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameStorage {

    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    public void addGame(String gameId,GameState game){

        games.put(gameId, game);
    }

    public GameState getGame(String gameId) {

        GameState game = games.get(gameId);
        if (game == null)
            throw new NoSuchElementException("Game not found");

        return game;
    }

    //runs every minute
    @Scheduled(fixedRate = 60000)
    public void deleteFinishedGames(){

        //removes game if it's over for more than 5 minutes
        games.entrySet().removeIf(entry -> {
            GameState game = entry.getValue();
            synchronized (game) {
                return game.getPhase() == GameState.Phase.OVER &&
                        game.phaseStartedAt().isBefore(Instant.now().minus(Duration.ofMinutes(5)));
            }
        });
    }
}
