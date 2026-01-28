package com.battleship.common.model;

import static com.battleship.common.model.ModelTypes.GameStatus;

/**
 * Состояние игры
 */
public class GameState {
    private final String gameId;
    private final String player1Id;
    private final String player2Id;
    private final Board player1Board;
    private final Board player2Board;

    private String currentTurnPlayerId;
    private GameStatus status;
    private String winnerId;
    private long lastTurnStartMillis;

    public GameState(String gameId, String player1Id, String player2Id) {
        this.gameId = gameId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1Board = new Board();
        this.player2Board = new Board();
        this.currentTurnPlayerId = player1Id;
        this.status = GameStatus.SETUP;
        this.lastTurnStartMillis = System.currentTimeMillis();
    }

    public Board getBoard(String playerId) {
        if (playerId.equals(player1Id)) {
            return player1Board;
        } else if (playerId.equals(player2Id)) {
            return player2Board;
        }
        throw new IllegalArgumentException("Неизвестный игрок: " + playerId);
    }

    public Board getOpponentBoard(String playerId) {
        if (playerId.equals(player1Id)) {
            return player2Board;
        } else if (playerId.equals(player2Id)) {
            return player1Board;
        }
        throw new IllegalArgumentException("Неизвестный игрок: " + playerId);
    }

    public String getOpponentId(String playerId) {
        if (playerId.equals(player1Id)) {
            return player2Id;
        } else if (playerId.equals(player2Id)) {
            return player1Id;
        }
        throw new IllegalArgumentException("Неизвестный игрок: " + playerId);
    }

    public void switchTurn() {
        currentTurnPlayerId = getOpponentId(currentTurnPlayerId);
        lastTurnStartMillis = System.currentTimeMillis();
    }

    public boolean isPlayerTurn(String playerId) {
        return currentTurnPlayerId.equals(playerId);
    }

    public void endGame(String winnerId) {
        this.winnerId = winnerId;
        this.status = GameStatus.FINISHED;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public Board getPlayer1Board() {
        return player1Board;
    }

    public Board getPlayer2Board() {
        return player2Board;
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public long getLastTurnStartMillis() {
        return lastTurnStartMillis;
    }

    public void refreshTurnTimer() {
        this.lastTurnStartMillis = System.currentTimeMillis();
    }
}
