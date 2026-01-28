package com.battleship.server.game;

import com.battleship.common.model.*;
import com.battleship.common.rules.GameRules;
import static com.battleship.common.model.ModelTypes.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Движок игровой логики
 */
public class GameEngine {
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

    private final GameState gameState;

    public GameEngine(String gameId, String player1Id, String player2Id) {
        this.gameState = new GameState(gameId, player1Id, player2Id);
        logger.info("Создана игра {} между {} и {}", gameId, player1Id, player2Id);
    }

    public synchronized PlaceShipResult placeShip(String playerId, ShipType shipType,
                                                    int startX, int startY, Orientation orientation) {
        if (gameState.getStatus() != GameStatus.SETUP) {
            return new PlaceShipResult(false, "Игра уже началась");
        }

        try {
            Board board = gameState.getBoard(playerId);
            Coordinate start = new Coordinate(startX, startY);
            Orientation resolvedOrientation = orientation;
            if (shipType.getSize() == 1) {
                resolvedOrientation = Orientation.HORIZONTAL;
            } else if (resolvedOrientation == null) {
                resolvedOrientation = Orientation.HORIZONTAL;
            }

            Ship ship = new Ship(shipType, start, resolvedOrientation);

            if (board.placeShip(ship)) {
                logger.debug("Игрок {} разместил корабль {}", playerId, shipType);
                return new PlaceShipResult(true, "Корабль размещён");
            } else {
                return new PlaceShipResult(false, "Невозможно разместить корабль в этой позиции");
            }
        } catch (IllegalArgumentException e) {
            return new PlaceShipResult(false, e.getMessage());
        }
    }

    public synchronized boolean checkReady(String playerId) {
        Board board = gameState.getBoard(playerId);
        return GameRules.validateShipPlacement(board);
    }

    public synchronized boolean startGame() {
        if (gameState.getStatus() != GameStatus.SETUP) {
            return false;
        }

        boolean player1Ready = checkReady(gameState.getPlayer1Id());
        boolean player2Ready = checkReady(gameState.getPlayer2Id());

        if (player1Ready && player2Ready) {
            gameState.setStatus(GameStatus.IN_PROGRESS);
            logger.info("Игра {} началась", gameState.getGameId());
            return true;
        }

        return false;
    }

    public synchronized FireResult fire(String shooterId, int x, int y) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) {
            return new FireResult(false, null, "Игра не начата или уже завершена");
        }

        if (!gameState.isPlayerTurn(shooterId)) {
            return new FireResult(false, null, "Сейчас не ваш ход");
        }

        long now = System.currentTimeMillis();
        if (now - gameState.getLastTurnStartMillis() > 10 * 60 * 1000L) { // 10 минут
            String opponent = gameState.getOpponentId(shooterId);
            gameState.endGame(opponent);
            logger.warn("Игра {} завершена по таймауту хода (>10 мин), победитель {}", gameState.getGameId(), opponent);
            return new FireResult(false, null, "Ход превысил 10 минут, игра завершена");
        }

        try {
            Coordinate coord = new Coordinate(x, y);
            Board targetBoard = gameState.getOpponentBoard(shooterId);
            ShotResult shotResult = targetBoard.shoot(coord);

            boolean gameOver = targetBoard.allShipsSunk();

            if (gameOver) {
                gameState.endGame(shooterId);
                logger.info("Игра {} завершена, победитель: {}", gameState.getGameId(), shooterId);
            } else if (shotResult.type() != ShotResultType.HIT && shotResult.type() != ShotResultType.SUNK) {
                gameState.switchTurn();
            } else {
                gameState.refreshTurnTimer();
            }

            return new FireResult(true, shotResult, null);
        } catch (IllegalArgumentException e) {
            return new FireResult(false, null, e.getMessage());
        }
    }

    public synchronized void surrender(String playerId) {
        String opponentId = gameState.getOpponentId(playerId);
        gameState.endGame(opponentId);
        logger.info("Игрок {} сдался в игре {}", playerId, gameState.getGameId());
    }

    public GameState getGameState() {
        return gameState;
    }

    public record PlaceShipResult(boolean success, String message) {}

    public record FireResult(boolean success, ShotResult shotResult, String errorMessage) {}
}
