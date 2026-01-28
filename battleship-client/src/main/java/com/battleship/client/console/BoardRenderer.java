package com.battleship.client.console;

import com.battleship.common.model.*;
import static com.battleship.common.model.ModelTypes.*;

/**
 * Отрисовка игровой доски в консоли
 */
public class BoardRenderer {

    public static String renderOwnBoard(Board board) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Ваша доска ===\n");
        sb.append(renderBoard(board, true));
        return sb.toString();
    }

    public static String renderOpponentBoard(Board board) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Доска противника ===\n");
        sb.append(renderBoard(board, false));
        return sb.toString();
    }

    private static String renderBoard(Board board, boolean showShips) {
        StringBuilder sb = new StringBuilder();

        sb.append("  ");
        for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
            sb.append((char) ('A' + x)).append(" ");
        }
        sb.append("\n");

        for (int y = 0; y < Coordinate.BOARD_SIZE; y++) {
            sb.append(y).append(" ");

            for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
                Coordinate coord = new Coordinate(x, y);
                CellState state = board.getCellState(coord);

                String symbol = getCellSymbol(state, showShips);
                sb.append(symbol).append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String getCellSymbol(CellState state, boolean showShips) {
        return switch (state) {
            case EMPTY -> "·";
            case SHIP -> showShips ? "■" : "·";
            case MISS -> "o";
            case HIT -> "x";
            case SUNK -> "#";
        };
    }

    public static String renderLegend() {
        return """

                Легенда:
                · - пусто    ■ - корабль    o - промах    x - попадание    # - потоплен
                """;
    }

    public static String renderStats(Board ownBoard, Board opponentBoard) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Статистика ===\n");
        sb.append("Ваши корабли: ").append(ownBoard.getShipCount())
          .append(" (потоплено: ").append(ownBoard.getSunkShipCount()).append(")\n");
        sb.append("Корабли противника: ").append(opponentBoard.getShipCount())
          .append(" (потоплено: ").append(opponentBoard.getSunkShipCount()).append(")\n");
        sb.append("Ваших выстрелов: ").append(opponentBoard.getShots().size()).append("\n");
        return sb.toString();
    }

    public static String renderBothBoards(Board ownBoard, Board opponentBoard) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("    ВАША ДОСКА                 ДОСКА ПРОТИВНИКА\n");

        String ownHeader = renderBoardHeader();
        String oppHeader = renderBoardHeader();
        sb.append(ownHeader).append("   ").append(oppHeader).append("\n");

        for (int y = 0; y < Coordinate.BOARD_SIZE; y++) {
            String ownRow = renderBoardRow(ownBoard, y, true);
            String oppRow = renderBoardRow(opponentBoard, y, false);
            sb.append(ownRow).append("   ").append(oppRow).append("\n");
        }

        return sb.toString();
    }

    private static String renderBoardHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
            sb.append((char) ('A' + x)).append(" ");
        }
        return sb.toString();
    }

    private static String renderBoardRow(Board board, int y, boolean showShips) {
        StringBuilder sb = new StringBuilder();
        sb.append(y).append(" ");

        for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
            Coordinate coord = new Coordinate(x, y);
            CellState state = board.getCellState(coord);
            String symbol = getCellSymbol(state, showShips);
            sb.append(symbol).append(" ");
        }

        return sb.toString();
    }
}
