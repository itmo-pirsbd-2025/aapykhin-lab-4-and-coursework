package com.battleship.common.protocol;

/**
 * Типы сообщений в протоколе
 */
public enum MessageType {
    // Клиент -> Сервер
    CONNECT,
    FIND_GAME,
    PLACE_SHIP,
    READY,
    FIRE,
    SURRENDER,

    // Сервер -> Клиент
    CONNECTED,
    GAME_FOUND,
    SHIP_PLACED,
    GAME_START,
    SHOT_RESULT,
    GAME_OVER,
    ERROR
}
