package com.battleship.common.model;

import static com.battleship.common.model.ModelTypes.ShipType;
import static com.battleship.common.model.ModelTypes.Orientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Корабль на игровом поле
 */
public class Ship {
    private final ShipType type;
    private final Coordinate start;
    private final Orientation orientation;
    private final boolean[] hits;
    private final List<Coordinate> coordinates;

    public Ship(ShipType type, Coordinate start, Orientation orientation) {
        this.type = type;
        this.start = start;
        this.orientation = orientation;
        this.hits = new boolean[type.getSize()];
        this.coordinates = calculateCoordinates();

        validatePlacement();
    }

    private List<Coordinate> calculateCoordinates() {
        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < type.getSize(); i++) {
            int x = orientation == Orientation.HORIZONTAL ? start.x() + i : start.x();
            int y = orientation == Orientation.VERTICAL ? start.y() + i : start.y();
            coords.add(new Coordinate(x, y));
        }
        return Collections.unmodifiableList(coords);
    }

    private void validatePlacement() {
        int lastX = orientation == Orientation.HORIZONTAL ? start.x() + type.getSize() - 1 : start.x();
        int lastY = orientation == Orientation.VERTICAL ? start.y() + type.getSize() - 1 : start.y();

        if (lastX >= Coordinate.BOARD_SIZE || lastY >= Coordinate.BOARD_SIZE) {
            throw new IllegalArgumentException("Корабль выходит за границы доски");
        }
    }

    public boolean hit(Coordinate coord) {
        for (int i = 0; i < coordinates.size(); i++) {
            if (coordinates.get(i).equals(coord)) {
                hits[i] = true;
                return true;
            }
        }
        return false;
    }

    public boolean isSunk() {
        for (boolean hit : hits) {
            if (!hit) return false;
        }
        return true;
    }

    public boolean occupies(Coordinate coord) {
        return coordinates.contains(coord);
    }

    public ShipType getType() {
        return type;
    }

    public Coordinate getStart() {
        return start;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public boolean isHitAt(int index) {
        return hits[index];
    }
}
