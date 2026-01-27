package com.battleship.common.model;

import static com.battleship.common.model.ModelTypes.CellState;
import static com.battleship.common.model.ModelTypes.ShotResult;
import static com.battleship.common.model.ModelTypes.ShotResultType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Игровое поле
 */
public class Board {
    private static final int SIZE = Coordinate.BOARD_SIZE;

    private final CellState[][] cells;
    private final List<Ship> ships;
    private final Set<Coordinate> shots;

    public Board() {
        this.cells = new CellState[SIZE][SIZE];
        this.ships = new ArrayList<>();
        this.shots = new HashSet<>();

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                cells[y][x] = CellState.EMPTY;
            }
        }
    }

    public boolean placeShip(Ship ship) {
        if (!canPlaceShip(ship)) {
            return false;
        }

        ships.add(ship);
        for (Coordinate coord : ship.getCoordinates()) {
            cells[coord.y()][coord.x()] = CellState.SHIP;
        }
        return true;
    }

    private boolean canPlaceShip(Ship ship) {
        for (Coordinate coord : ship.getCoordinates()) {
            if (coord.x() >= SIZE || coord.y() >= SIZE) {
                return false;
            }

            if (cells[coord.y()][coord.x()] == CellState.SHIP) {
                return false;
            }

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = coord.x() + dx;
                    int ny = coord.y() + dy;

                    if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE) {
                        if (cells[ny][nx] == CellState.SHIP) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public ShotResult shoot(Coordinate coord) {
        if (shots.contains(coord)) {
            return new ShotResult(ShotResultType.ALREADY_SHOT, null);
        }

        shots.add(coord);

        for (Ship ship : ships) {
            if (ship.hit(coord)) {
                cells[coord.y()][coord.x()] = CellState.HIT;

                if (ship.isSunk()) {
                    markSunkShip(ship);
                    return new ShotResult(ShotResultType.SUNK, ship);
                }

                return new ShotResult(ShotResultType.HIT, ship);
            }
        }

        cells[coord.y()][coord.x()] = CellState.MISS;
        return new ShotResult(ShotResultType.MISS, null);
    }

    private void markSunkShip(Ship ship) {
        for (Coordinate coord : ship.getCoordinates()) {
            cells[coord.y()][coord.x()] = CellState.SUNK;
        }
    }

    public boolean allShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return !ships.isEmpty();
    }

    public CellState getCellState(Coordinate coord) {
        return cells[coord.y()][coord.x()];
    }

    public void setCellState(Coordinate coord, CellState state) {
        cells[coord.y()][coord.x()] = state;
        shots.add(coord);
    }

    public List<Ship> getShips() {
        return List.copyOf(ships);
    }

    public Set<Coordinate> getShots() {
        return Set.copyOf(shots);
    }

    public int getShipCount() {
        return ships.size();
    }

    public int getSunkShipCount() {
        return (int) ships.stream().filter(Ship::isSunk).count();
    }
}
