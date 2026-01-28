package com.battleship.server.matchmaking;

import com.battleship.common.protocol.GameMode;
import com.battleship.server.game.GameEngine;
import com.battleship.server.session.PlayerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Сервис поиска игр
 */
public class MatchmakingService {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private final Queue<PlayerSession> pvpQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, GameEngine> activeGames = new ConcurrentHashMap<>();

    public synchronized MatchResult findGame(PlayerSession player, GameMode gameMode) {
        if (gameMode == GameMode.PVP) {
            return findPvPGame(player);
        } else {
            return findPvEGame(player);
        }
    }

    private MatchResult findPvPGame(PlayerSession player) {
        PlayerSession opponent = pvpQueue.poll();

        if (opponent == null) {
            pvpQueue.offer(player);
            logger.info("Игрок {} ({}) добавлен в очередь PvP", player.getPlayerName(), player.getPlayerId());
            return new MatchResult(false, null, null);
        }

        String gameId = UUID.randomUUID().toString();
        GameEngine game = new GameEngine(gameId, player.getPlayerId(), opponent.getPlayerId());

        activeGames.put(gameId, game);
        player.setCurrentGameId(gameId);
        opponent.setCurrentGameId(gameId);

        logger.info("Создана PvP игра {} между {} и {}",
                    gameId, player.getPlayerName(), opponent.getPlayerName());

        return new MatchResult(true, game, opponent);
    }

    private MatchResult findPvEGame(PlayerSession player) {
        String gameId = UUID.randomUUID().toString();
        String aiPlayerId = "AI-" + UUID.randomUUID();

        GameEngine game = new GameEngine(gameId, player.getPlayerId(), aiPlayerId);

        activeGames.put(gameId, game);
        player.setCurrentGameId(gameId);

        logger.info("Создана PvE игра {} для игрока {} против AI", gameId, player.getPlayerName());

        return new MatchResult(true, game, null);
    }

    public GameEngine getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(String gameId) {
        GameEngine game = activeGames.remove(gameId);
        if (game != null) {
            logger.info("Игра {} удалена", gameId);
        }
    }

    public void cancelSearch(PlayerSession player) {
        pvpQueue.remove(player);
        logger.info("Игрок {} ({}) отменил поиск игры", player.getPlayerName(), player.getPlayerId());
    }

    public int getActiveGameCount() {
        return activeGames.size();
    }

    public int getPvpQueueSize() {
        return pvpQueue.size();
    }

    public record MatchResult(boolean found, GameEngine game, PlayerSession opponent) {}
}
