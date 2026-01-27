package com.battleship.common.model;

/**
 * Все enum'ы и вспомогательные типы для моделей игры
 */
public class ModelTypes {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public enum ShipType {
        BATTLESHIP(4, "Линкор", 1),
        CRUISER(3, "Крейсер", 2),
        DESTROYER(2, "Эсминец", 3),
        BOAT(1, "Катер", 4);

        private final int size;
        private final String displayName;
        private final int count;

        ShipType(int size, String displayName, int count) {
            this.size = size;
            this.displayName = displayName;
            this.count = count;
        }

        public int getSize() {
            return size;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCount() {
            return count;
        }

        public static int getTotalShipCount() {
            int total = 0;
            for (ShipType type : values()) {
                total += type.count;
            }
            return total;
        }
    }

    public enum CellState {
        EMPTY,
        SHIP,
        MISS,
        HIT,
        SUNK 
    }

    public enum ShotResultType {
        HIT,
        MISS,
        SUNK,
        ALREADY_SHOT
    }

    public enum GameStatus {
        SETUP,
        IN_PROGRESS,
        FINISHED
    }

    public record ShotResult(ShotResultType type, Ship ship) {

        public boolean isHit() {
            return type == ShotResultType.HIT || type == ShotResultType.SUNK;
        }

        public boolean isSunk() {
            return type == ShotResultType.SUNK;
        }
    }
}
