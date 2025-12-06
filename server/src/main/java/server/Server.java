package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.http.Context;
import io.javalin.json.JavalinGson;
import io.javalin.websocket.WsCloseContext;
import model.GameData;
import org.jetbrains.annotations.NotNull;
import service.*;
import io.javalin.Javalin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.javalin.websocket.WsContext;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class Server {
    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final DataAccess dao = new MySQLDataAccess();
    private final UserService userService = new UserService(dao);
    private final GameService gameService = new GameService(dao);
    private final Map<Integer, Set<WsContext>> gameSession = new HashMap<>();
    private final Map<WsContext, Integer> toGame = new HashMap<>();
    private final Set<Integer> finishedGames = new HashSet<>();

    public Server() {
        try {
            DatabaseManager.createDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
        javalin = Javalin.create(config -> {
            config.staticFiles.add("web");
            config.jsonMapper(new JavalinGson());
        });
        javalin.delete("/db", ctx -> { // clear DB
            try {
                sweep();
                ctx.status(200).result("{}");
            } catch (Exception ex) {
                ctx.status(500).json(Map.of("message", "Error: " + ex.getMessage()));
            }
        });
        // Register
        javalin.post("/user", this::registerEndpoint);
        // Login
        javalin.post("/session", this::loginEndpoint);
        javalin.delete("/session", ctx -> { // Logout
            try{
                String token = ctx.header("authorization");
                userService.logout(token);
                ctx.status(200).result("{}");
            } catch (UnauthorizedException e) {
                ctx.status(401).json(Map.of("message", "Error: unauthorized"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
            }
        });
        javalin.post("/game", ctx -> { // Create Game
            try {
                String token = ctx.header("authorization");
                CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);
                CreateGameResult res = gameService.createGame(token, req);
                ctx.status(200).json(Map.of("gameID", res.gameID()));
            } catch (BadRequestException e) {ctx.status(400).json(Map.of("message", "Error: bad request"));
            } catch (UnauthorizedException e){ctx.status(401).json(Map.of("message", "Error: unauthorized"));
            } catch (Exception e) {ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
            }
        });
        // Join Game
        javalin.put("/game", this::joinGameEndpoint);
        javalin.get("/game", ctx -> {  // List Games
            try {
                String token = ctx.header("authorization");
                ListGamesResult res = gameService.listGames(token);
                ctx.status(200).json(Map.of("games", res.games()));
            } catch (UnauthorizedException e) {
                ctx.status(401).json(Map.of("message", "Error: unauthorized"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
            }
        });
        javalin.ws("/ws", ws -> {
            ws.onConnect(ctx -> System.out.println("WS connected: " + ctx.sessionId()));
            ws.onMessage(ctx-> {
                var json = ctx.message();
                UserGameCommand command = gson.fromJson(json, UserGameCommand.class);
                handleCommand(ctx, command);
            });
            ws.onClose(ctx->{Integer gameId = toGame.remove(ctx);
                checkGameId(gameId, ctx);
            });
            ws.onError(ctx-> System.out.println("Error: " +ctx.error()));
        });
    }

    private void joinGameEndpoint(@NotNull Context ctx) {
        try {
            String token = ctx.header("authorization");
            JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);

            if (req.playerColor() == null){
                throw new BadRequestException("invalid color");
            } if (req.playerColor().equals("observe")){
                gameService.addObserver(token, req.gameID());
                ctx.status(200).json(Map.of("message", "Observing game"));
            }
            else {
                gameService.joinGame(token, req);
                ctx.status(200).json(Map.of("message", "Joined game"));
            }
        } catch (BadRequestException e) {ctx.status(400).json(Map.of("message", "Error: bad request"));
        } catch (UnauthorizedException e) {ctx.status(401).json(Map.of("message", "Error: unauthorized"));
        } catch (AlreadyTakenException e) { ctx.status(403).json(Map.of("message", "Error: already taken"));
        } catch (Exception e) {ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void loginEndpoint(@NotNull Context ctx) {
        try {
            LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);
            LoginResult result = userService.login(req);
            ctx.status(200).json(result);
        } catch (BadRequestException e) {
            ctx.status(400).json((Map.of("message", "Error: bad request")));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Error: " + e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void registerEndpoint(@NotNull Context ctx) {
        try {
            RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);
            RegisterResult res = userService.register(req);
            ctx.status(200).json(res);
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        } catch (AlreadyTakenException e) {
            ctx.status(403).json(Map.of("message", "Error: already taken"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void sweep() throws DataAccessException {
        gameService.clear();
        gameSession.clear();
        toGame.clear();
        finishedGames.clear();
    }

    private void checkGameId(Integer gameId, WsCloseContext ctx) {
        if (gameId != null){
            var set = gameSession.get(gameId);
            if (set != null){set.remove(ctx);
            }
        }
    }

    private void handleCommand(WsContext ctx, UserGameCommand command) throws Exception {
        switch (command.getCommandType()){
            case CONNECT -> handleConnect(ctx, command);
            case MAKE_MOVE -> handleMakeMove(ctx, command);
            case LEAVE -> handleLeave(ctx, command);
            case RESIGN -> handleResign(ctx, command);
        }
    }

    private void handleResign(WsContext ctx, UserGameCommand command) {
        try {
            String authToken = command.getAuthToken();
            int gameId = command.getGameID();
            if (authToken == null || gameId == 0) {
                ctx.send(gson.toJson(ServerMessage.error("Error: missing authToken or gameID")));
                return;
            }
            if (finishedGames.contains(gameId)) {
                ctx.send(gson.toJson(ServerMessage.error("Error: game is already over")));
                return;
            }
            var auth = dao.getAuth(authToken).orElseThrow(() -> new Exception("Invalid authToken"));
            String username = auth.username();
            var gameData = dao.getGame(gameId).orElseThrow(() -> new Exception("Game doesn't exist"));
            ChessGame game = gameData.game();

            String whiteUser = gameData.whiteUsername();
            String blackUser = gameData.blackUsername();

            boolean isWhite = username.equals(whiteUser);
            boolean isBlack = username.equals(blackUser);

            if (!isWhite && !isBlack) {
                ctx.send(gson.toJson(ServerMessage.error("Error: observers cannot resign")));
                return;
            }

            finishedGames.add(gameId);

            GameData updated = new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            dao.updateGame(updated);
            broadcastToAll(gameId, ServerMessage.notification(username + " resigned."));
        } catch (Exception e) {
                ctx.send(gson.toJson(ServerMessage.error("Error: " + e.getMessage())));
        }
    }

    private void handleLeave(WsContext ctx, UserGameCommand command) throws Exception {
        String authToken = command.getAuthToken();
        int gameId = command.getGameID();
        var auth = dao.getAuth(authToken).orElseThrow(() -> new Exception("Invalid authToken"));
        String username = auth.username();
        var gameData = dao.getGame(gameId).orElseThrow(() -> new Exception("Game doesn't exist"));

        String w = gameData.whiteUsername();
        String b = gameData.blackUsername();
        if (username.equals(w)) {
            w = null;
        }
        if (username.equals(b)) {
            b = null;
        }

        GameData updated = new GameData(gameData.gameID(), w, b, gameData.gameName(), gameData.game());
        dao.updateGame(updated);
        broadcastToOthers(gameId, ctx, ServerMessage.notification(username + " left the game"));

        Integer grid = toGame.remove(ctx);
        if (grid != null) {
            var set = gameSession.get(grid);
            if (set != null) {
                set.remove(ctx);
            }
        }
    }

    private void handleMakeMove(WsContext ctx, UserGameCommand command) {
        try {
            String authToken = command.getAuthToken();
            int gameId = command.getGameID();
            var move = command.getMove();

            if (authToken == null || gameId == 0 || move == null) {
                ctx.send(gson.toJson(ServerMessage.error("Error: missing authToken, gameID, or move")));
                return;
            }

            if (finishedGames.contains(gameId)) {
                ctx.send(gson.toJson(ServerMessage.error("Error: game is over")));
                return;
            }

            var auth = dao.getAuth(authToken).orElseThrow(() -> new Exception("Invalid authToken"));
            String username = auth.username();
            var gameData = dao.getGame(gameId).orElseThrow(() -> new Exception("Game doesn't exist"));

            ChessGame game = gameData.game();

            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            }
            else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            }

            if (playerColor == null) {
                ctx.send(gson.toJson(ServerMessage.error("Error: observers cannot move")));
                return;
            }

            if (game.getTeamTurn() != playerColor) {
                ctx.send(gson.toJson(ServerMessage.error("Error: not your turn")));
                return;
            }

            try {
                game.makeMove(move);
            } catch (Exception e) {
                ctx.send(gson.toJson(ServerMessage.error("Error: illegal move")));
                return;
            }
            GameData updated = new GameData(gameData.gameID(),
                    gameData.whiteUsername(), gameData.blackUsername(),
                    gameData.gameName(), game);
            dao.updateGame(updated);

            broadcastToAll(gameId, ServerMessage.loadGame(game));
            String desc = username + " moved from " +
                    move.getStartPosition().getRow() + "," +
                    move.getStartPosition().getColumn() + " to " +
                    move.getEndPosition().getRow() + "," +
                    move.getEndPosition().getColumn();

            broadcastToOthers(gameId, ctx, ServerMessage.notification(desc));
            ChessGame.TeamColor opponent =
                    (playerColor == ChessGame.TeamColor.WHITE)
                            ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

            if (game.isInCheckmate(opponent)) {
                broadcastToAll(gameId, ServerMessage.notification(opponent + " is in checkmate"));
            } else if (game.isInCheck(opponent)) {
                broadcastToAll(gameId, ServerMessage.notification(opponent + " is in check"));
            }
        } catch (Exception e) {
            ctx.send(gson.toJson(ServerMessage.error("Error: " + e.getMessage())));
        }
    }

    private void handleConnect(WsContext ctx,UserGameCommand command) {
        try {
            String authToken = command.getAuthToken();
            int gameId = command.getGameID();

            if (authToken == null || gameId == 0) {
                ctx.send(gson.toJson(ServerMessage.error("Error: missing authToken or gameID")));
                return;
            }
            var auth = dao.getAuth(authToken).orElseThrow(() -> new DataAccessException("Invalid authToken"));
            String username = auth.username();
            var gameData = dao.getGame(gameId).orElseThrow(() -> new DataAccessException("Game doesn't exist"));

            toGame.put(ctx, gameId);
            gameSession.computeIfAbsent(gameId, k -> new HashSet<>()).add(ctx);
            var loadMsg = ServerMessage.loadGame(gameData.game());
            ctx.send(gson.toJson(loadMsg));

            String color = null;
            if (username.equals(gameData.whiteUsername())) {
                color = "white";
            }
            else if (username.equals(gameData.blackUsername())) {
                color = "black";
            }

            String text = (color != null)
                    ? username + "connected as : " + color
                    : username + "connected as observer";

            broadcastToOthers(gameId, ctx, ServerMessage.notification(text));
        } catch (Exception e) {
            ctx.send(gson.toJson(ServerMessage.error("Error: " + e.getMessage())));
        }
    }

    private void broadcastToOthers(int gameId, WsContext ctx, ServerMessage message) {
        var set = gameSession.get(gameId);
        if (set == null){
            return;
        }
        String json = gson.toJson(message);
        String senderSession = ctx.sessionId();

        for (var c : set){
            if (!c.sessionId().equals(senderSession)){
                c.send(json);
            }
        }
    }

    private void broadcastToAll(int gameId, ServerMessage message) {
        var set = gameSession.get(gameId);
        if (set == null) {
            return;
        }
        String json = gson.toJson(message);
        for (var c : set) {
            c.send(json);
        }
    }


    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }
    public void stop() {
        javalin.stop();
    }
}
