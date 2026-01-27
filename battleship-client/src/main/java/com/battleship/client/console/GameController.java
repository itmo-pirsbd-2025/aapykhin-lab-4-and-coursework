package com.battleship.client.console;

import com.battleship.common.model.*;
import com.battleship.common.protocol.GameMode;
import com.battleship.common.protocol.Messages;
import com.battleship.common.rules.GameRules;
import com.battleship.client.network.GameClient;
import static com.battleship.common.model.ModelTypes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Контроллер игрового процесса
 */
public class GameController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GameController.class);

    private final GameClient client;
    private final Scanner scanner;

    private Board ownBoard;
    private Board opponentBoard;
    private String gameId;
    private String playerName;
    private boolean myTurn;
    private boolean gameStarted;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private ScheduledFuture<?> connectRetryTask;
    private ScheduledFuture<?> connectTimeoutTask;

    public GameController(GameClient client) {
        this.client = client;
        this.scanner = new Scanner(System.in);
        this.ownBoard = new Board();
        this.opponentBoard = new Board();

        client.setMessageHandler(this::handleMessage);
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (gameStarted && client.isConnected()) {
                client.sendMessage(new Messages.Surrender());
            }
        }));

        System.out.println("=== Морской бой ===");
        System.out.print("Введите ваше имя: ");
        this.playerName = scanner.nextLine();

        client.connect().thenRun(() -> {
            System.out.println("Подключение к серверу...");
            connectRetryTask = scheduler.scheduleAtFixedRate(
                () -> {
                    if (connected.get()) {
                        connectRetryTask.cancel(false);
                        return;
                    }
                    client.sendMessage(new Messages.Connect(playerName, "1.0.0"));
                },
                0, 1, TimeUnit.SECONDS
            );
            connectTimeoutTask = scheduler.schedule(() -> {
                if (connected.compareAndSet(false, false)) {
                    System.err.println("Не удалось подключиться за 10 секунд. Проверьте сервер и попробуйте снова.");
                    connectRetryTask.cancel(false);
                    client.disconnect();
                }
            }, 10, TimeUnit.SECONDS);
        }).exceptionally(e -> {
            System.err.println("Не удалось подключиться: " + e.getMessage());
            return null;
        });

        mainLoop();
    }

    private void mainLoop() {
        while (client.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleMessage(Messages.Message message) {
        switch (message) {
            case Messages.Connected msg -> handleConnected(msg);
            case Messages.GameFound msg -> handleGameFound(msg);
            case Messages.ShipPlaced msg -> handleShipPlaced(msg);
            case Messages.GameStart msg -> handleGameStart(msg);
            case Messages.ShotResult msg -> handleShotResult(msg);
            case Messages.GameOver msg -> handleGameOver(msg);
            case Messages.Error msg -> handleError(msg);
            default -> System.out.println("Неизвестное сообщение: " + message.getType());
        }
    }

    private void handleConnected(Messages.Connected msg) {
        connected.set(true);
        if (connectRetryTask != null) connectRetryTask.cancel(false);
        if (connectTimeoutTask != null) connectTimeoutTask.cancel(false);

        System.out.println("Подключено! ID: " + msg.getPlayerId());
        System.out.println("Версия сервера: " + msg.getServerVersion());

        showMenu();
    }

    private void showMenu() {
        System.out.println("\n=== Меню ===");
        System.out.println("1. PvP игра");
        System.out.println("2. PvE игра (против AI)");
        System.out.print("Выбор: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        GameMode mode = choice == 2 ? GameMode.PVE : GameMode.PVP;
        client.sendMessage(new Messages.FindGame(mode));

        System.out.println("Поиск игры...");
    }

    private void handleGameFound(Messages.GameFound msg) {
        this.gameId = msg.getGameId();
        System.out.println("\nИгра найдена! ID: " + gameId);
        System.out.println("Противник: " + msg.getOpponentName());

        setupShips();
    }

    private void setupShips() {
        System.out.println("\n=== Расстановка кораблей ===");
        System.out.println("Автоматическая расстановка (Enter) или ручная (manual)?");
        String choice = scanner.nextLine();

        if (choice.equalsIgnoreCase("manual")) {
            manualSetup();
        } else {
            autoSetup();
        }
    }

    private void autoSetup() {
        Random random = new Random();
        int retries = 0;

        while (!GameRules.autoPlaceShips(ownBoard, random) && retries < 10) {
            ownBoard = new Board();
            retries++;
        }

        if (ownBoard.getShipCount() > 0) {
            System.out.println("Корабли расставлены автоматически");
            System.out.println(BoardRenderer.renderOwnBoard(ownBoard));

            for (Ship ship : ownBoard.getShips()) {
                client.sendMessage(new Messages.PlaceShip(
                    ship.getType(),
                    ship.getStart().x(),
                    ship.getStart().y(),
                    ship.getOrientation()
                ));
            }

            client.sendMessage(new Messages.Ready());
            System.out.println("Ожидание противника...");
        } else {
            System.out.println("Ошибка автоматической расстановки");
        }
    }

    private void manualSetup() {
        var required = GameRules.getRequiredShips();

        var sortedShips = required.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getKey().getSize(), a.getKey().getSize()))
            .toList();

        for (var entry : sortedShips) {
            ShipType type = entry.getKey();
            int count = entry.getValue();

            for (int i = 0; i < count; i++) {
                System.out.println("\nРазместите " + type.getDisplayName() +
                                 " (размер " + type.getSize() + ")");
                System.out.println(BoardRenderer.renderOwnBoard(ownBoard));

                System.out.print("Координата (например, A0): ");
                String coord = scanner.nextLine();

                Orientation orientation = Orientation.HORIZONTAL;

                if (type.getSize() > 1) {
                    String orient;
                    while (true) {
                        System.out.print("Ориентация (H/V): ");
                        orient = scanner.nextLine().trim().toUpperCase();
                        if (orient.equals("H") || orient.equals("V")) {
                            break;
                        }
                        System.out.println("Неверный ввод. Введите H (горизонтально) или V (вертикально)");
                    }
                    orientation = orient.equals("V") ? Orientation.VERTICAL : Orientation.HORIZONTAL;
                }

                try {
                    Coordinate start = Coordinate.parse(coord);
                    Ship ship = new Ship(type, start, orientation);
                    if (ownBoard.placeShip(ship)) {
                        client.sendMessage(new Messages.PlaceShip(type, start.x(), start.y(), orientation));
                    } else {
                        System.out.println("Невозможно разместить корабль, попробуйте снова");
                        i--;
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка! " + e.getMessage());
                    i--;
                }
            }
        }

        System.out.println("\nВсе корабли расставлены!");
        System.out.println(BoardRenderer.renderOwnBoard(ownBoard));

        client.sendMessage(new Messages.Ready());
        System.out.println("Ожидание противника...");
    }

    private void handleShipPlaced(Messages.ShipPlaced msg) {
        if (!msg.isSuccess()) {
            System.out.println("Ошибка! " + msg.getMessage());
        }
    }

    private void handleGameStart(Messages.GameStart msg) {
        this.gameStarted = true;
        this.myTurn = msg.isYourTurn();

        System.out.println("\n=== ИГРА НАЧАЛАСЬ! ===");
        System.out.println(BoardRenderer.renderLegend());

        if (myTurn) {
            promptFire();
        } else {
            System.out.println("Ход противника...");
        }
    }

    private void promptFire() {
        System.out.println("\n--- Ваш ход ---");
        System.out.print("Выстрел (например, A5): ");

        try {
            String input = scanner.nextLine();
            Coordinate coord = Coordinate.parse(input);
            client.sendMessage(new Messages.Fire(coord.x(), coord.y()));
        } catch (java.util.NoSuchElementException e) {
            logger.debug("Scanner closed, game ended");
        } catch (Exception e) {
            System.out.println("Неверный формат, попробуйте снова");
            promptFire();
        }
    }

    private void handleShotResult(Messages.ShotResult msg) {
        Coordinate coord = new Coordinate(msg.getX(), msg.getY());
        boolean isMyShot = playerName.equals(msg.getShooter());

        if (isMyShot) {
            updateBoard(opponentBoard, coord, msg.getResult());
        } else {
            updateBoard(ownBoard, coord, msg.getResult());
        }

        String shooter = isMyShot ? "Ваш выстрел" : "Выстрел противника";
        String coordStr = "" + (char) ('A' + msg.getX()) + msg.getY();
        System.out.println("\n" + shooter + ": " + coordStr + " - " + translateResult(msg.getResult()));

        this.myTurn = msg.isYourTurn();

        System.out.println(BoardRenderer.renderBothBoards(ownBoard, opponentBoard));

        if (myTurn && gameStarted) {
            promptFire();
        } else if (gameStarted) {
            System.out.println("\nХод противника...");
        }
    }

    private void updateBoard(Board board, Coordinate coord, ShotResultType result) {
        CellState state = switch (result) {
            case HIT -> CellState.HIT;
            case MISS -> CellState.MISS;
            case SUNK -> {
                markSunkShipCells(board, coord);
                yield CellState.SUNK;
            }
            case ALREADY_SHOT -> board.getCellState(coord);
        };
        board.setCellState(coord, state);
    }

    private void markSunkShipCells(Board board, Coordinate sunkCoord) {
        List<Coordinate> shipCells = new ArrayList<>();
        shipCells.add(sunkCoord);

        for (int x = sunkCoord.x() - 1; x >= 0; x--) {
            Coordinate c = new Coordinate(x, sunkCoord.y());
            if (board.getCellState(c) == CellState.HIT || board.getCellState(c) == CellState.SUNK) {
                shipCells.add(c);
            } else {
                break;
            }
        }
        for (int x = sunkCoord.x() + 1; x < Coordinate.BOARD_SIZE; x++) {
            Coordinate c = new Coordinate(x, sunkCoord.y());
            if (board.getCellState(c) == CellState.HIT || board.getCellState(c) == CellState.SUNK) {
                shipCells.add(c);
            } else {
                break;
            }
        }

        for (int y = sunkCoord.y() - 1; y >= 0; y--) {
            Coordinate c = new Coordinate(sunkCoord.x(), y);
            if (board.getCellState(c) == CellState.HIT || board.getCellState(c) == CellState.SUNK) {
                shipCells.add(c);
            } else {
                break;
            }
        }
        for (int y = sunkCoord.y() + 1; y < Coordinate.BOARD_SIZE; y++) {
            Coordinate c = new Coordinate(sunkCoord.x(), y);
            if (board.getCellState(c) == CellState.HIT || board.getCellState(c) == CellState.SUNK) {
                shipCells.add(c);
            } else {
                break;
            }
        }

        for (Coordinate c : shipCells) {
            board.setCellState(c, CellState.SUNK);
        }
    }

    private String translateResult(ShotResultType result) {
        return switch (result) {
            case HIT -> "Попадание!";
            case MISS -> "Промах";
            case SUNK -> "Потоплен!";
            case ALREADY_SHOT -> "Уже стреляли";
        };
    }

    private void handleGameOver(Messages.GameOver msg) {
        this.gameStarted = false;

        System.out.println("\n=== ИГРА ОКОНЧЕНА ===");
        System.out.println("Результат: " + msg.getWinner());
        System.out.println("Причина: " + msg.getReason());

        System.out.println("\nНажмите Enter для выхода...");
        scanner.nextLine();

        client.disconnect();
        System.exit(0);
    }

    private void handleError(Messages.Error msg) {
        System.err.println("ОШИБКА [" + msg.getCode() + "]: " + msg.getMessage());

        if (msg.getCode() == 410) {
            System.out.println("\nИгра отменена. Нажмите Enter для выхода...");
            try {
                scanner.nextLine();
            } catch (Exception e) {
                // Игнорируем ошибки scanner
            }
            client.disconnect();
            System.exit(0);
        }
    }
}
