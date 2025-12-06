package websocket.messages;

import chess.ChessGame;

import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 * <p>
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    ServerMessageType serverMessageType;
    ChessGame game;
    String message;
    String errorMessage;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public static ServerMessage loadGame(ChessGame game){
        ServerMessage message = new ServerMessage(ServerMessageType.LOAD_GAME);
        message.game = game;
        return message;
    }

    public static ServerMessage notification(String message){
        ServerMessage msg = new ServerMessage(ServerMessageType.NOTIFICATION);
        msg.message = message;
        return msg;
    }

    public static ServerMessage error(String errorMessage){
        ServerMessage msg = new ServerMessage(ServerMessageType.ERROR);
        msg.errorMessage = errorMessage;
        return msg;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public ChessGame getGame(){
        return game;
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public String getMessage(){
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage that)) {
            return false;
        }
        return getServerMessageType() == that.getServerMessageType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType());
    }
}
