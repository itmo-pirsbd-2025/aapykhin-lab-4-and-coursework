package com.battleship.ai.agent;

import com.battleship.common.model.Coordinate;
import com.battleship.common.model.Board;
import static com.battleship.common.model.ModelTypes.CellState;

import java.util.*;

/**
 * AI агент Hunt/Target.
 * Hunt: стреляет по «чёрным» клеткам (x+y чётное), чтобы быстро зацепить любой корабль.
 * Target: группирует попадания, определяет ориентацию кластера и добивает, стреляя только по продолжению линии.
 * Ограничения: не стреляет по уже обстрелянным клеткам, по соседям потопленных (8 направлений)
 * и по диагоналям от любых HIT, т.к. корабли не касаются по диагонали.
 */
public class HuntTargetAgent implements BattleshipAgent {
    private final Random random = new Random();

    @Override
    public Coordinate chooseAction(Board board) {
        List<Coordinate> hits = findActiveHits(board);

        if (!hits.isEmpty()) {
            Coordinate target = findTargetShot(hits, board);
            if (target != null) {
                return target;
            }
        }

        return findHuntShot(board);
    }

    private List<Coordinate> findActiveHits(Board board) {
        List<Coordinate> hits = new ArrayList<>();
        for (int y = 0; y < Coordinate.BOARD_SIZE; y++) {
            for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
                Coordinate c = new Coordinate(x, y);
                if (board.getCellState(c) == CellState.HIT) {
                    hits.add(c);
                }
            }
        }
        return hits;
    }

    private Coordinate findTargetShot(List<Coordinate> hits, Board board) {
        List<List<Coordinate>> groups = groupConnectedHits(hits);

        for (List<Coordinate> group : groups) {
            Coordinate target = findGroupTarget(group, board);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    private List<List<Coordinate>> groupConnectedHits(List<Coordinate> hits) {
        List<List<Coordinate>> groups = new ArrayList<>();
        Set<Coordinate> visited = new HashSet<>();

        for (Coordinate hit : hits) {
            if (!visited.contains(hit)) {
                List<Coordinate> group = new ArrayList<>();
                Queue<Coordinate> queue = new LinkedList<>();
                queue.add(hit);
                visited.add(hit);

                while (!queue.isEmpty()) {
                    Coordinate current = queue.poll();
                    group.add(current);

                    int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
                    for (int[] d : dirs) {
                        int nx = current.x() + d[0];
                        int ny = current.y() + d[1];

                        if (nx >= 0 && nx < Coordinate.BOARD_SIZE && ny >= 0 && ny < Coordinate.BOARD_SIZE) {
                            Coordinate neighbor = new Coordinate(nx, ny);
                            if (hits.contains(neighbor) && !visited.contains(neighbor)) {
                                queue.add(neighbor);
                                visited.add(neighbor);
                            }
                        }
                    }
                }
                groups.add(group);
            }
        }

        return groups;
    }

    private Coordinate findGroupTarget(List<Coordinate> group, Board board) {
        if (group.size() == 1) {
            return findAdjacentTarget(group.get(0), board);
        }

        boolean horizontal = group.stream().mapToInt(Coordinate::y).distinct().count() == 1;

        if (horizontal) {
            int y = group.get(0).y();
            int minX = group.stream().mapToInt(Coordinate::x).min().orElse(0);
            int maxX = group.stream().mapToInt(Coordinate::x).max().orElse(0);

            if (minX - 1 >= 0) {
                Coordinate left = new Coordinate(minX - 1, y);
                if (isValidTarget(left, board)) return left;
            }

            if (maxX + 1 < Coordinate.BOARD_SIZE) {
                Coordinate right = new Coordinate(maxX + 1, y);
                if (isValidTarget(right, board)) return right;
            }
        } else {
            int x = group.get(0).x();
            int minY = group.stream().mapToInt(Coordinate::y).min().orElse(0);
            int maxY = group.stream().mapToInt(Coordinate::y).max().orElse(0);

            if (minY - 1 >= 0) {
                Coordinate top = new Coordinate(x, minY - 1);
                if (isValidTarget(top, board)) return top;
            }

            if (maxY + 1 < Coordinate.BOARD_SIZE) {
                Coordinate bottom = new Coordinate(x, maxY + 1);
                if (isValidTarget(bottom, board)) return bottom;
            }
        }

        for (Coordinate hit : group) {
            Coordinate adj = findAdjacentTarget(hit, board);
            if (adj != null) return adj;
        }

        return null;
    }

    private Coordinate findAdjacentTarget(Coordinate hit, Board board) {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] d : dirs) {
            int nx = hit.x() + d[0];
            int ny = hit.y() + d[1];

            if (nx >= 0 && nx < Coordinate.BOARD_SIZE && ny >= 0 && ny < Coordinate.BOARD_SIZE) {
                Coordinate c = new Coordinate(nx, ny);
                if (isValidTarget(c, board)) {
                    return c;
                }
            }
        }
        return null;
    }

    private Coordinate findHuntShot(Board board) {
        List<Coordinate> candidates = new ArrayList<>();

        for (int y = 0; y < Coordinate.BOARD_SIZE; y++) {
            for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
                if ((x + y) % 2 == 0) {
                    Coordinate c = new Coordinate(x, y);
                    if (isValidTarget(c, board)) {
                        candidates.add(c);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            for (int y = 0; y < Coordinate.BOARD_SIZE; y++) {
                for (int x = 0; x < Coordinate.BOARD_SIZE; x++) {
                    Coordinate c = new Coordinate(x, y);
                    if (isValidTarget(c, board)) {
                        candidates.add(c);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No valid moves");
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean isValidTarget(Coordinate c, Board board) {
        // Проверка границ
        if (c.x() < 0 || c.x() >= Coordinate.BOARD_SIZE ||
            c.y() < 0 || c.y() >= Coordinate.BOARD_SIZE) {
            return false;
        }

        if (board.getShots().contains(c)) {
            return false;
        }

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;

                int nx = c.x() + dx;
                int ny = c.y() + dy;

                if (nx >= 0 && nx < Coordinate.BOARD_SIZE &&
                    ny >= 0 && ny < Coordinate.BOARD_SIZE) {

                    CellState state = board.getCellState(new Coordinate(nx, ny));

                    if (state == CellState.SUNK) {
                        return false;
                    }

                    if (state == CellState.HIT && dx != 0 && dy != 0) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void reset() {
        // Состояние не хранится между играми
    }

    @Override
    public String getName() {
        return "HuntTarget";
    }
}
