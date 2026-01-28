package com.battleship.server.session;

import io.netty.channel.Channel;

/**
 * Сессия игрока
 */
public class PlayerSession {
    private final String playerId;
    private final String playerName;
    private final Channel channel;
    private String currentGameId;

    public PlayerSession(String playerId, String playerName, Channel channel) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.channel = channel;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getCurrentGameId() {
        return currentGameId;
    }

    public void setCurrentGameId(String gameId) {
        this.currentGameId = gameId;
    }

    public boolean isInGame() {
        return currentGameId != null;
    }
}
