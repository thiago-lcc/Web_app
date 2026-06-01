package com.myapp.logic.game;

import com.myapp.model.game.Agent;
import com.myapp.model.game.GameState;
import com.myapp.model.room.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class GameGateway {

    private final GameStorage gameStorage;

    public GameGateway(GameStorage gameStorage){

        this.gameStorage = gameStorage;
    }

    private List<Integer> createImpostorIds(int numImpostors) {

        return new Random()
                //Ids must be in the interval [1,8]
                .ints(1, 9)
                .distinct()
                .limit(numImpostors)
                .boxed()
                .toList();
    }

    public void assignImpostor(GameState game, String impostorToken){

        List<Integer> availableIds = game.availableImpostorIds();

        int impostorId = availableIds.get(
                new Random().nextInt(availableIds.size()));

        game.setImpostorToken(impostorToken, impostorId);

    }

    public void assignPlayers(String gameId, Map<String, Player> playerMap) {

        GameState game = gameStorage.getGame(gameId);

        synchronized (game){

            playerMap.forEach((token, player) -> {

                if(player.isGuesser())
                    game.setGuesserToken(token);

                else {
                    assignImpostor(game, token);
                }

            });

            game.setPhase(GameState.Phase.WAITING_FOR_QUESTION);
        }
    }

    private List<Agent> createAgents(int numImpostors) {

        List<Integer> impostorIds = createImpostorIds(numImpostors);

        List<Agent> agents = new ArrayList<>();

        for (int impostorId : impostorIds) agents.add(new Agent(impostorId, true));
        for (int i = 1; i < 9; i++)
            if (!impostorIds.contains(i))
                agents.add(new Agent(i, false));

        return agents;
    }

    public void createGame(int numImpostors, String gameId, String name, String ownerToken) {

        List<Agent> agents = createAgents(numImpostors);

        gameStorage.addGame(gameId, new GameState(agents, ownerToken, name));
    }
}
