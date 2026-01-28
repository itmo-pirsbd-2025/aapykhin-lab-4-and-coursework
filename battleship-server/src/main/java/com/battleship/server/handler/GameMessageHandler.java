package com.battleship.server.handler;

import com.battleship.common.protocol.Messages;
import com.battleship.server.ai.AIService;
import com.battleship.server.game.GameEngine;
import com.battleship.server.matchmaking.MatchmakingService;
import com.battleship.server.session.PlayerSession;
import com.battleship.server.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler для обработки игровых сообщений
 */
public class GameMessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(GameMessageHandler.class);
    private static final String SERVER_VERSION = "1.0.0";

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final AIService aiService;

    public GameMessageHandler(SessionManager sessionManager, MatchmakingService matchmakingService, AIService aiService) {
        this.objectMapper = new ObjectMapper();
        this.sessionManager = sessionManager;
        this.matchmakingService = matchmakingService;
        this.aiService = aiService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String json = frame.text();
        Channel channel = ctx.channel();

        try {
            Messages.Message message = objectMapper.readValue(json, Messages.Message.class);
            handleMessage(channel, message);
        } catch (Exception e) {
            logger.error("Ошибка обработки сообщения: {}", e.getMessage());
            sendError(channel, 400, "Неверный формат сообщения: " + e.getMessage());
        }
    }

    private void handleMessage(Channel channel, Messages.Message message) {
        switch (message.getType()) {
            case CONNECT -> handleConnect(channel, (Messages.Connect) message);
            case FIND_GAME -> handleFindGame(channel, (Messages.FindGame) message);
            case PLACE_SHIP -> handlePlaceShip(channel, (Messages.PlaceShip) message);
            case READY -> handleReady(channel);
            case FIRE -> handleFire(channel, (Messages.Fire) message);
            case SURRENDER -> handleSurrender(channel);
            default -> sendError(channel, 400, "Неизвестный тип сообщения");
        }
    }

    private void handleConnect(Channel channel, Messages.Connect msg) {
        PlayerSession session = sessionManager.createSession(msg.getPlayerName(), channel);
        Messages.Connected response = new Messages.Connected(session.getPlayerId(), SERVER_VERSION);
        sendMessage(channel, response);
    }

    private void handleFindGame(Channel channel, Messages.FindGame msg) {
        sessionManager.getSessionByChannel(channel).ifPresentOrElse(
            session -> {
                var result = matchmakingService.findGame(session, msg.getGameMode());

                if (result.found()) {
                    String opponentName = result.opponent() != null ?
                        result.opponent().getPlayerName() : "AI";

                    Messages.GameFound response = new Messages.GameFound(
                        result.game().getGameState().getGameId(),
                        opponentName
                    );
                    sendMessage(channel, response);

                    if (result.opponent() != null) {
                        Messages.GameFound opponentMsg = new Messages.GameFound(
                            result.game().getGameState().getGameId(),
                            session.getPlayerName()
                        );
                        sendMessage(result.opponent().getChannel(), opponentMsg);
                    } else {
                        String aiPlayerId = result.game().getGameState().getPlayer2Id();
                        aiService.createAIPlayer(aiPlayerId, result.game());
                    }
                }
            },
            () -> sendError(channel, 401, "Сессия не найдена")
        );
    }

    private void handlePlaceShip(Channel channel, Messages.PlaceShip msg) {
        sessionManager.getSessionByChannel(channel).ifPresentOrElse(
            session -> {
                if (!session.isInGame()) {
                    sendError(channel, 400, "Вы не в игре");
                    return;
                }

                GameEngine game = matchmakingService.getGame(session.getCurrentGameId());
                if (game == null) {
                    sendError(channel, 404, "Игра не найдена");
                    return;
                }

                var result = game.placeShip(session.getPlayerId(), msg.getShipType(),
                                           msg.getStartX(), msg.getStartY(), msg.getOrientation());

                Messages.ShipPlaced response = new Messages.ShipPlaced(
                    result.success(),
                    result.message()
                );
                sendMessage(channel, response);
            },
            () -> sendError(channel, 401, "Сессия не найдена")
        );
    }

    private void handleReady(Channel channel) {
        sessionManager.getSessionByChannel(channel).ifPresentOrElse(
            session -> {
                if (!session.isInGame()) {
                    sendError(channel, 400, "Вы не в игре");
                    return;
                }

                GameEngine game = matchmakingService.getGame(session.getCurrentGameId());
                if (game == null) {
                    sendError(channel, 404, "Игра не найдена");
                    return;
                }

                if (!game.checkReady(session.getPlayerId())) {
                    sendError(channel, 400, "Не все корабли расставлены");
                    return;
                }

                if (game.startGame()) {
                    var state = game.getGameState();
                    boolean player1Turn = state.getCurrentTurnPlayerId().equals(state.getPlayer1Id());

                    sessionManager.getSession(state.getPlayer1Id()).ifPresent(p1 -> {
                        sendMessage(p1.getChannel(), new Messages.GameStart(player1Turn));
                    });

                    sessionManager.getSession(state.getPlayer2Id()).ifPresent(p2 -> {
                        sendMessage(p2.getChannel(), new Messages.GameStart(!player1Turn));
                    });

                    processAITurnIfNeeded(game);
                }
            },
            () -> sendError(channel, 401, "Сессия не найдена")
        );
    }

    private void handleFire(Channel channel, Messages.Fire msg) {
        sessionManager.getSessionByChannel(channel).ifPresentOrElse(
            session -> {
                if (!session.isInGame()) {
                    sendError(channel, 400, "Вы не в игре");
                    return;
                }

                GameEngine game = matchmakingService.getGame(session.getCurrentGameId());
                if (game == null) {
                    sendError(channel, 404, "Игра не найдена");
                    return;
                }

                var fireResult = game.fire(session.getPlayerId(), msg.getX(), msg.getY());

                if (!fireResult.success()) {
                    sendError(channel, 400, fireResult.errorMessage());
                    return;
                }

                var shotResult = fireResult.shotResult();
                var state = game.getGameState();

                Messages.ShotResult shotMsg = new Messages.ShotResult(
                    session.getPlayerName(),
                    msg.getX(), msg.getY(),
                    shotResult.type(),
                    shotResult.ship() != null ? shotResult.ship().getType() : null,
                    state.isPlayerTurn(session.getPlayerId())
                );

                sendMessage(channel, shotMsg);

                String opponentId = state.getOpponentId(session.getPlayerId());
                sessionManager.getSession(opponentId).ifPresent(opponent -> {
                    Messages.ShotResult opponentMsg = new Messages.ShotResult(
                        session.getPlayerName(),
                        msg.getX(), msg.getY(),
                        shotResult.type(),
                        shotResult.ship() != null ? shotResult.ship().getType() : null,
                        state.isPlayerTurn(opponentId)
                    );
                    sendMessage(opponent.getChannel(), opponentMsg);
                });

                if (state.getStatus() == com.battleship.common.model.ModelTypes.GameStatus.FINISHED) {
                    sendGameOver(game);
                } else {
                    processAITurnIfNeeded(game);
                }
            },
            () -> sendError(channel, 401, "Сессия не найдена")
        );
    }

    private void handleSurrender(Channel channel) {
        sessionManager.getSessionByChannel(channel).ifPresentOrElse(
            session -> {
                if (!session.isInGame()) {
                    sendError(channel, 400, "Вы не в игре");
                    return;
                }

                GameEngine game = matchmakingService.getGame(session.getCurrentGameId());
                if (game == null) {
                    sendError(channel, 404, "Игра не найдена");
                    return;
                }

                game.surrender(session.getPlayerId());
                sendGameOver(game);
            },
            () -> sendError(channel, 401, "Сессия не найдена")
        );
    }

    private void processAITurnIfNeeded(GameEngine game) {
        var state = game.getGameState();
        String currentPlayer = state.getCurrentTurnPlayerId();

        if (!currentPlayer.startsWith("AI-")) {
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(500);
                executeAITurn(game, state, currentPlayer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("AI turn interrupted", e);
            }
        }).start();
    }

    private void executeAITurn(GameEngine game, com.battleship.common.model.GameState state, String currentPlayer) {
        var aiResult = aiService.processAITurn(state.getGameId(), game);

        if (!aiResult.success()) {
            logger.error("AI failed to make a turn");
            return;
        }

        var coord = aiResult.coordinate();

        var updatedState = game.getGameState();
        var targetBoard = updatedState.getOpponentBoard(currentPlayer);
        var cellState = targetBoard.getCellState(coord);

        var shotType = switch (cellState) {
            case HIT -> com.battleship.common.model.ModelTypes.ShotResultType.HIT;
            case MISS -> com.battleship.common.model.ModelTypes.ShotResultType.MISS;
            case SUNK -> com.battleship.common.model.ModelTypes.ShotResultType.SUNK;
            default -> com.battleship.common.model.ModelTypes.ShotResultType.MISS;
        };

        sessionManager.getSession(updatedState.getPlayer1Id()).ifPresent(player -> {
            Messages.ShotResult shotMsg = new Messages.ShotResult(
                "AI",
                coord.x(), coord.y(),
                shotType,
                null,
                updatedState.isPlayerTurn(updatedState.getPlayer1Id())
            );
            sendMessage(player.getChannel(), shotMsg);
        });

        if (updatedState.getStatus() == com.battleship.common.model.ModelTypes.GameStatus.FINISHED) {
            sendGameOver(game);
            aiService.removeAIPlayer(currentPlayer);
        } else if (updatedState.isPlayerTurn(currentPlayer)) {
            processAITurnIfNeeded(game);
        }
    }

    private void sendGameOver(GameEngine game) {
        var state = game.getGameState();
        String winnerId = state.getWinnerId();

        sessionManager.getSession(state.getPlayer1Id()).ifPresent(p1 -> {
            Messages.GameOver msg = new Messages.GameOver(
                winnerId.equals(p1.getPlayerId()) ? "Победа" : "Поражение",
                "Игра завершена",
                0, 0
            );
            sendMessage(p1.getChannel(), msg);
            p1.setCurrentGameId(null);
        });

        sessionManager.getSession(state.getPlayer2Id()).ifPresent(p2 -> {
            Messages.GameOver msg = new Messages.GameOver(
                winnerId.equals(p2.getPlayerId()) ? "Победа" : "Поражение",
                "Игра завершена",
                0, 0
            );
            sendMessage(p2.getChannel(), msg);
            p2.setCurrentGameId(null);
        });

        matchmakingService.removeGame(state.getGameId());
    }

    private void sendMessage(Channel channel, Messages.Message message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void sendError(Channel channel, int code, String message) {
        Messages.Error error = new Messages.Error(code, message);
        sendMessage(channel, error);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        sessionManager.getSessionByChannel(ctx.channel()).ifPresent(session -> {
            if (session.isInGame()) {
                String gameId = session.getCurrentGameId();
                GameEngine game = matchmakingService.getGame(gameId);

                if (game != null) {
                    var status = game.getGameState().getStatus();

                    if (status == com.battleship.common.model.ModelTypes.GameStatus.IN_PROGRESS) {
                        logger.info("Игрок {} отключился во время игры {}, засчитываем поражение", session.getPlayerName(), gameId);
                        game.surrender(session.getPlayerId());
                        sendGameOver(game);
                    } else if (status == com.battleship.common.model.ModelTypes.GameStatus.SETUP) {
                        logger.info("Игрок {} отключился во время расстановки кораблей в игре {}", session.getPlayerName(), gameId);
                        String opponentId = game.getGameState().getOpponentId(session.getPlayerId());

                        sessionManager.getSession(opponentId).ifPresent(opponent -> {
                            Messages.Error cancelMsg = new Messages.Error(
                                410,
                                "Противник отключился во время расстановки кораблей. Игра отменена."
                            );
                            sendMessage(opponent.getChannel(), cancelMsg);
                            opponent.setCurrentGameId(null);
                        });

                        matchmakingService.removeGame(gameId);
                    }
                } else {
                    matchmakingService.cancelSearch(session);
                }
            }
            sessionManager.removeSession(ctx.channel());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Ошибка в обработчике: {}", cause.getMessage());
        ctx.close();
    }
}
