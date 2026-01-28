package com.battleship.common.rules;

import com.battleship.common.model.Board;
import com.battleship.common.model.Coordinate;
import com.battleship.common.model.Ship;
import static com.battleship.common.model.ModelTypes.ShipType;
import static com.battleship.common.model.ModelTypes.Orientation;

import java.util.*;

/**
 * Правила игры и валидация
 */
public class GameRules {

    public static boolean autoPlaceShips(Board board, Random random) {
        List<ShipType> shipsToPlace = new ArrayList<>();

        for (ShipType type : ShipType.values()) {
            for (int i = 0; i < type.getCount(); i++) {
                shipsToPlace.add(type);
            }
        }

        shipsToPlace.sort((a, b) -> Integer.compare(b.getSize(), a.getSize()));

        for (ShipType shipType : shipsToPlace) {
            boolean placed = false;
            int attempts = 0;
            int maxAttempts = 100;

            while (!placed && attempts < maxAttempts) {
                Orientation orientation;
                int x, y;

                if (shipType.getSize() == 1) {
                    orientation = Orientation.HORIZONTAL;
                    x = random.nextInt(Coordinate.BOARD_SIZE);
                    y = random.nextInt(Coordinate.BOARD_SIZE);
                } else {
                    orientation = random.nextBoolean() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                    int maxStart = Coordinate.BOARD_SIZE - shipType.getSize();

                    if (orientation == Orientation.HORIZONTAL) {
                        x = random.nextInt(maxStart + 1);
                        y = random.nextInt(Coordinate.BOARD_SIZE);
                    } else {
                        x = random.nextInt(Coordinate.BOARD_SIZE);
                        y = random.nextInt(maxStart + 1);
                    }
                }

                try {
                    Ship ship = new Ship(shipType, new Coordinate(x, y), orientation);
                    placed = board.placeShip(ship);
                } catch (IllegalArgumentException e) {
                    // Попытка не удалась
                }

                attempts++;
            }

            if (!placed) {
                return false;
            }
        }

        return true;
    }

    public static boolean validateShipPlacement(Board board) {
        Map<ShipType, Integer> shipCounts = new HashMap<>();
        for (ShipType type : ShipType.values()) {
            shipCounts.put(type, 0);
        }

        for (Ship ship : board.getShips()) {
            ShipType type = ship.getType();
            shipCounts.put(type, shipCounts.get(type) + 1);
        }

        for (ShipType type : ShipType.values()) {
            if (shipCounts.get(type) != type.getCount()) {
                return false;
            }
        }

        return true;
    }

    public static Map<ShipType, Integer> getRequiredShips() {
        Map<ShipType, Integer> required = new HashMap<>();
        for (ShipType type : ShipType.values()) {
            required.put(type, type.getCount());
        }
        return required;
    }
}
