package com.battleship.server;

import com.battleship.server.ai.AIService;
import com.battleship.server.handler.WebSocketServerInitializer;
import com.battleship.server.matchmaking.MatchmakingService;
import com.battleship.server.session.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс сервера Морского боя
 */
public class BattleshipServer {
    private static final Logger logger = LoggerFactory.getLogger(BattleshipServer.class);

    private static final int DEFAULT_PORT = 8080;
    private static final String WEBSOCKET_PATH = "/game";

    private final int port;
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final AIService aiService;

    public BattleshipServer(int port) {
        this.port = port;
        this.sessionManager = new SessionManager();
        this.matchmakingService = new MatchmakingService();
        this.aiService = new AIService();
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer(
                            WEBSOCKET_PATH,
                            sessionManager,
                            matchmakingService,
                            aiService
                    ));

            Channel channel = bootstrap.bind(port).sync().channel();

            logger.info("Battleship Server запущен на порту {}", port);
            logger.info("WebSocket endpoint: ws://localhost:{}{}", port, WEBSOCKET_PATH);

            startStatsThread();

            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void startStatsThread() {
        Thread statsThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000);

                    int sessions = sessionManager.getSessionCount();
                    int games = matchmakingService.getActiveGameCount();
                    int queue = matchmakingService.getPvpQueueSize();

                    logger.info("Статистика: {} сессий, {} активных игр, {} в очереди",
                               sessions, games, queue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }

    public static void main(String[] args) {
        String utf8 = java.nio.charset.StandardCharsets.UTF_8.name();
        System.setProperty("file.encoding", utf8);
        System.setProperty("stdout.encoding", utf8);
        System.setProperty("stderr.encoding", utf8);

        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Неверный порт: {}, используется порт по умолчанию {}", args[0], DEFAULT_PORT);
            }
        }

        try {
            BattleshipServer server = new BattleshipServer(port);
            server.start();
        } catch (InterruptedException e) {
            logger.error("Сервер прерван", e);
            Thread.currentThread().interrupt();
        }
    }
}
