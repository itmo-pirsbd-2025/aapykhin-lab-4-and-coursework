package com.battleship.client;

import com.battleship.client.console.GameController;
import com.battleship.client.network.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс клиента Морского боя
 */
public class BattleshipClient {
    private static final Logger logger = LoggerFactory.getLogger(BattleshipClient.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final String WEBSOCKET_PATH = "/game";

    public static void main(String[] args) {
        String utf8 = java.nio.charset.StandardCharsets.UTF_8.name();
        System.setProperty("file.encoding", utf8);
        System.setProperty("stdout.encoding", utf8);
        System.setProperty("stderr.encoding", utf8);

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                logger.error("Неверный порт: {}", args[1]);
            }
        }

        logger.info("Запуск клиента: {}:{}{}", host, port, WEBSOCKET_PATH);

        GameClient client = new GameClient(host, port, WEBSOCKET_PATH);
        GameController controller = new GameController(client);

        try {
            controller.start();
        } catch (Exception e) {
            logger.error("Ошибка клиента: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}