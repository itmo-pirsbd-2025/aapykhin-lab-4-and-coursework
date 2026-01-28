package com.battleship.server.session;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер сессий игроков
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, PlayerSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<Channel, PlayerSession> sessionsByChannel = new ConcurrentHashMap<>();

    public PlayerSession createSession(String playerName, Channel channel) {
        String playerId = UUID.randomUUID().toString();
        PlayerSession session = new PlayerSession(playerId, playerName, channel);

        sessionsById.put(playerId, session);
        sessionsByChannel.put(channel, session);

        logger.info("Создана сессия для игрока {} ({})", playerName, playerId);
        return session;
    }

    public Optional<PlayerSession> getSession(String playerId) {
        return Optional.ofNullable(sessionsById.get(playerId));
    }

    public Optional<PlayerSession> getSessionByChannel(Channel channel) {
        return Optional.ofNullable(sessionsByChannel.get(channel));
    }

    public void removeSession(Channel channel) {
        PlayerSession session = sessionsByChannel.remove(channel);
        if (session != null) {
            sessionsById.remove(session.getPlayerId());
            logger.info("Удалена сессия игрока {} ({})", session.getPlayerName(), session.getPlayerId());
        }
    }

    public int getSessionCount() {
        return sessionsById.size();
    }

    public boolean hasSession(String playerId) {
        return sessionsById.containsKey(playerId);
    }
}
