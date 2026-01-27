package com.battleship.common.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.battleship.common.model.ModelTypes.ShipType;
import static com.battleship.common.model.ModelTypes.Orientation;
import static com.battleship.common.model.ModelTypes.ShotResultType;

/**
 * Все типы сообщений протокола WebSocket
 */
public class Messages {

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Connect.class, name = "CONNECT"),
        @JsonSubTypes.Type(value = FindGame.class, name = "FIND_GAME"),
        @JsonSubTypes.Type(value = PlaceShip.class, name = "PLACE_SHIP"),
        @JsonSubTypes.Type(value = Ready.class, name = "READY"),
        @JsonSubTypes.Type(value = Fire.class, name = "FIRE"),
        @JsonSubTypes.Type(value = Surrender.class, name = "SURRENDER"),
        @JsonSubTypes.Type(value = Connected.class, name = "CONNECTED"),
        @JsonSubTypes.Type(value = GameFound.class, name = "GAME_FOUND"),
        @JsonSubTypes.Type(value = ShipPlaced.class, name = "SHIP_PLACED"),
        @JsonSubTypes.Type(value = GameStart.class, name = "GAME_START"),
        @JsonSubTypes.Type(value = ShotResult.class, name = "SHOT_RESULT"),
        @JsonSubTypes.Type(value = GameOver.class, name = "GAME_OVER"),
        @JsonSubTypes.Type(value = Error.class, name = "ERROR")
    })
    public abstract static class Message {
        private final MessageType type;

        protected Message(MessageType type) {
            this.type = type;
        }

        public MessageType getType() {
            return type;
        }
    }

    // Клиент -> Сервер

    public static class Connect extends Message {
        private String playerName;
        private String clientVersion;

        public Connect() {
            super(MessageType.CONNECT);
        }

        public Connect(String playerName, String clientVersion) {
            super(MessageType.CONNECT);
            this.playerName = playerName;
            this.clientVersion = clientVersion;
        }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    }

    public static class FindGame extends Message {
        private GameMode gameMode;

        public FindGame() {
            super(MessageType.FIND_GAME);
        }

        public FindGame(GameMode gameMode) {
            super(MessageType.FIND_GAME);
            this.gameMode = gameMode;
        }

        public GameMode getGameMode() { return gameMode; }
        public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }
    }

    public static class PlaceShip extends Message {
        private ShipType shipType;
        private int startX;
        private int startY;
        private Orientation orientation;

        public PlaceShip() {
            super(MessageType.PLACE_SHIP);
        }

        public PlaceShip(ShipType shipType, int startX, int startY, Orientation orientation) {
            super(MessageType.PLACE_SHIP);
            this.shipType = shipType;
            this.startX = startX;
            this.startY = startY;
            this.orientation = orientation;
        }

        public ShipType getShipType() { return shipType; }
        public void setShipType(ShipType shipType) { this.shipType = shipType; }
        public int getStartX() { return startX; }
        public void setStartX(int startX) { this.startX = startX; }
        public int getStartY() { return startY; }
        public void setStartY(int startY) { this.startY = startY; }
        public Orientation getOrientation() { return orientation; }
        public void setOrientation(Orientation orientation) { this.orientation = orientation; }
    }

    public static class Ready extends Message {
        public Ready() {
            super(MessageType.READY);
        }
    }

    public static class Fire extends Message {
        private int x;
        private int y;

        public Fire() {
            super(MessageType.FIRE);
        }

        public Fire(int x, int y) {
            super(MessageType.FIRE);
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    public static class Surrender extends Message {
        public Surrender() {
            super(MessageType.SURRENDER);
        }
    }

    // Сервер -> Клиент

    public static class Connected extends Message {
        private String playerId;
        private String serverVersion;

        public Connected() {
            super(MessageType.CONNECTED);
        }

        public Connected(String playerId, String serverVersion) {
            super(MessageType.CONNECTED);
            this.playerId = playerId;
            this.serverVersion = serverVersion;
        }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getServerVersion() { return serverVersion; }
        public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    }

    public static class GameFound extends Message {
        private String gameId;
        private String opponentName;

        public GameFound() {
            super(MessageType.GAME_FOUND);
        }

        public GameFound(String gameId, String opponentName) {
            super(MessageType.GAME_FOUND);
            this.gameId = gameId;
            this.opponentName = opponentName;
        }

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        public String getOpponentName() { return opponentName; }
        public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    }

    public static class ShipPlaced extends Message {
        private boolean success;
        private String message;

        public ShipPlaced() {
            super(MessageType.SHIP_PLACED);
        }

        public ShipPlaced(boolean success, String message) {
            super(MessageType.SHIP_PLACED);
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class GameStart extends Message {
        private boolean yourTurn;

        public GameStart() {
            super(MessageType.GAME_START);
        }

        public GameStart(boolean yourTurn) {
            super(MessageType.GAME_START);
            this.yourTurn = yourTurn;
        }

        public boolean isYourTurn() { return yourTurn; }
        public void setYourTurn(boolean yourTurn) { this.yourTurn = yourTurn; }
    }

    public static class ShotResult extends Message {
        private String shooter;
        private int x;
        private int y;
        private ShotResultType result;
        private ShipType sunkShip;
        private boolean yourTurn;

        public ShotResult() {
            super(MessageType.SHOT_RESULT);
        }

        public ShotResult(String shooter, int x, int y, ShotResultType result, ShipType sunkShip, boolean yourTurn) {
            super(MessageType.SHOT_RESULT);
            this.shooter = shooter;
            this.x = x;
            this.y = y;
            this.result = result;
            this.sunkShip = sunkShip;
            this.yourTurn = yourTurn;
        }

        public String getShooter() { return shooter; }
        public void setShooter(String shooter) { this.shooter = shooter; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public ShotResultType getResult() { return result; }
        public void setResult(ShotResultType result) { this.result = result; }
        public ShipType getSunkShip() { return sunkShip; }
        public void setSunkShip(ShipType sunkShip) { this.sunkShip = sunkShip; }
        public boolean isYourTurn() { return yourTurn; }
        public void setYourTurn(boolean yourTurn) { this.yourTurn = yourTurn; }
    }

    public static class GameOver extends Message {
        private String winner;
        private String reason;
        private int totalShots;
        private int hits;

        public GameOver() {
            super(MessageType.GAME_OVER);
        }

        public GameOver(String winner, String reason, int totalShots, int hits) {
            super(MessageType.GAME_OVER);
            this.winner = winner;
            this.reason = reason;
            this.totalShots = totalShots;
            this.hits = hits;
        }

        public String getWinner() { return winner; }
        public void setWinner(String winner) { this.winner = winner; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public int getTotalShots() { return totalShots; }
        public void setTotalShots(int totalShots) { this.totalShots = totalShots; }
        public int getHits() { return hits; }
        public void setHits(int hits) { this.hits = hits; }
    }

    public static class Error extends Message {
        private int code;
        private String message;

        public Error() {
            super(MessageType.ERROR);
        }

        public Error(int code, String message) {
            super(MessageType.ERROR);
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
