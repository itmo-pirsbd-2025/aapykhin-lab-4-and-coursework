package com.battleship.common.model;

public record Coordinate(int x, int y) {

    public static final int BOARD_SIZE = 10;

    public Coordinate {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            throw new IllegalArgumentException(
                "Ошибка размещения"
            );
        }
    }

    public int toIndex() {
        return y * BOARD_SIZE + x;
    }

    public static Coordinate fromIndex(int index) {
        if (index < 0 || index >= BOARD_SIZE * BOARD_SIZE) {
            throw new IllegalArgumentException("Индекс вне допустимого диапазона: " + index);
        }
        return new Coordinate(index % BOARD_SIZE, index / BOARD_SIZE);
    }

    public static Coordinate parse(String str) {
        if (str == null || str.length() < 2) {
            throw new IllegalArgumentException("Неверный формат координат: " + str);
        }

        char columnChar = Character.toUpperCase(str.charAt(0));
        if (columnChar < 'A' || columnChar >= 'A' + BOARD_SIZE) {
            throw new IllegalArgumentException("Неверная колонка: " + columnChar);
        }

        int x = columnChar - 'A';
        int y = Integer.parseInt(str.substring(1));

        return new Coordinate(x, y);
    }

    @Override
    public String toString() {
        return String.format("%c%d", (char) ('A' + x), y);
    }
}
