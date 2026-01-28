package com.battleship.server.ai;

import com.battleship.ai.agent.HuntTargetAgent;
import com.battleship.common.model.*;
import com.battleship.common.rules.GameRules;
import com.battleship.server.game.GameEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления AI игроками
 */
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final Map<String, AIPlayer> aiPlayers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    public void createAIPlayer(String aiPlayerId, GameEngine game) {
        AIPlayer aiPlayer = new AIPlayer(aiPlayerId);
        aiPlayers.put(aiPlayerId, aiPlayer);
        logger.info("Создан AI игрок {}", aiPlayerId);

        autoPlaceShips(aiPlayer, game);
    }

    private void autoPlaceShips(AIPlayer aiPlayer, GameEngine game) {
        Board board = new Board();
        int retries = 0;

        while (!GameRules.autoPlaceShips(board, random) && retries < 100) {
            board = new Board();
            retries++;
        }

        if (board.getShipCount() > 0) {
            for (Ship ship : board.getShips()) {
                game.placeShip(
                    aiPlayer.getPlayerId(),
                    ship.getType(),
                    ship.getStart().x(),
                    ship.getStart().y(),
                    ship.getOrientation()
                );
            }
            logger.info("AI {} расставил корабли автоматически", aiPlayer.getPlayerId());
        } else {
            logger.error("Не удалось автоматически расставить корабли для AI {}", aiPlayer.getPlayerId());
        }
    }

    public AITurnResult processAITurn(String gameId, GameEngine game) {
        var state = game.getGameState();
        String currentPlayer = state.getCurrentTurnPlayerId();

        if (!currentPlayer.startsWith("AI-")) {
            return new AITurnResult(false, null);
        }

        AIPlayer aiPlayer = aiPlayers.get(currentPlayer);
        if (aiPlayer == null) {
            logger.warn("AI игрок {} не найден для игры {}", currentPlayer, gameId);
            return new AITurnResult(false, null);
        }

        Coordinate move = aiPlayer.chooseMove(game);
        if (move == null) {
            logger.error("AI {} не смог выбрать ход", currentPlayer);
            return new AITurnResult(false, null);
        }

        logger.debug("AI {} выстрелил в {}{}", currentPlayer, (char)('A' + move.x()), move.y());

        var fireResult = game.fire(currentPlayer, move.x(), move.y());

        if (fireResult.success()) {
            return new AITurnResult(true, move);
        }

        return new AITurnResult(false, null);
    }

    public void removeAIPlayer(String aiPlayerId) {
        aiPlayers.remove(aiPlayerId);
        logger.debug("AI игрок {} удален", aiPlayerId);
    }

    public record AITurnResult(boolean success, Coordinate coordinate) {}

    private static class AIPlayer {
        private final String playerId;
        private final HuntTargetAgent agent;

        public AIPlayer(String playerId) {
            this.playerId = playerId;
            this.agent = new HuntTargetAgent();
        }

        public String getPlayerId() {
            return playerId;
        }

        public Coordinate chooseMove(GameEngine game) {
            var state = game.getGameState();
            Board opponentBoard = state.getOpponentBoard(playerId);
            return agent.chooseAction(opponentBoard);
        }
    }
}
