package client.requests;

import com.google.gson.Gson;
import websocket.messages.ServerMessage;
import websocket.commands.UserGameCommand;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class GameplayClient implements WebSocket.Listener {

    private final Gson gson = new Gson();
    private WebSocket socket;

    private Consumer<ChessGame> onGameLoad;
    private Consumer<String> onNotification;
    private Consumer<String> onError;

    private final String serverUrl;
    private String authToken;
    private int gameID;

    public GameplayClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void connect(String authToken, int gameID,
                        Consumer<ChessGame> onGameLoad,
                        Consumer<String> onNotification,
                        Consumer<String> onError) {

        this.authToken = authToken;
        this.gameID = gameID;
        this.onGameLoad = onGameLoad;
        this.onNotification = onNotification;
        this.onError = onError;

        this.socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl.replace("http", "ws") + "/ws"), this)
                .join();

        // After connect, send CONNECT command
        UserGameCommand cmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        send(cmd);
    }

    public void send(UserGameCommand cmd) {
        String json = gson.toJson(cmd);
        socket.sendText(json, true);
    }

    public void makeMove(ChessMove move) {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.MAKE_MOVE, authToken, gameID, move);
        send(cmd);
    }

    public void leave() {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        send(cmd);
    }

    public void resign() {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        send(cmd);
    }

    // WebSocket.Listener callbacks
    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String json = data.toString();
        ServerMessage msg = gson.fromJson(json, ServerMessage.class);

        switch (msg.getServerMessageType()) {
            case LOAD_GAME -> {
                if (onGameLoad != null) {
                    onGameLoad.accept(msg.getGame());
                }
            }
            case ERROR -> {
                if (onError != null) {
                    onError.accept(msg.getErrorMessage());
                }
            }
            case NOTIFICATION -> {
                if (onNotification != null) {
                    onNotification.accept(msg.getMessage());
                }
            }
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

}
