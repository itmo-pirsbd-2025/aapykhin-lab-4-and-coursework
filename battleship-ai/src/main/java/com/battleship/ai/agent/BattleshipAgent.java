package com.battleship.ai.agent;

import com.battleship.common.model.Coordinate;
import com.battleship.common.model.Board;

/**
 * Интерфейс AI агента
 */
public interface BattleshipAgent {

    // Выбор координаты для выстрела
    Coordinate chooseAction(Board opponentBoard);

    void reset();

    String getName();
}