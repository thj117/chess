package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.json.JavalinGson;
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

        //clear
        javalin.delete("/db", ctx -> { // clear DB
            try {
                gameService.clear();
                ctx.status(200).result("{}");
            } catch (Exception ex) {
                ctx.status(500).json(Map.of("message", "Error: " + ex.getMessage()));
            }
        });
        // Register
        javalin.post("/user", ctx -> {
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
        });
        // Login
        javalin.post("/session", ctx -> {
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
        });
        // Logout
        javalin.delete("/session", ctx -> {
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
        // Create Game
        javalin.post("/game", ctx -> {
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
        javalin.put("/game", ctx -> {
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
        });
        // List Games
        javalin.get("/game", ctx -> {
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

            ws.onConnect(ctx -> {
                System.out.println("WS connected: " + ctx.sessionId());
                    });

            ws.onMessage(ctx-> {
                var json = ctx.message();
                UserGameCommand command = gson.fromJson(json, UserGameCommand.class);
                handleCommand(ctx, command);
            });

            ws.onClose(ctx->{
                Integer gameId = toGame.remove(ctx);
                if (gameId != null){
                    var set = gameSession.get(gameId);
                    if (set != null){
                        set.remove(ctx);
                    }
                }
            });

            ws.onError(ctx->{
                System.out.println("Error: " +ctx.error());
            });

        });
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
    }

    private void handleLeave(WsContext ctx, UserGameCommand command) {
    }

    private void handleMakeMove(WsContext ctx, UserGameCommand command) {
    }

    private void handleConnect(WsContext ctx,UserGameCommand command) throws Exception {
        String authToken = command.getAuthToken();
        int gameId = command.getGameID();
        var auth = dao.getAuth(authToken).orElseThrow(() -> new Exception("Invalid authToken"));
        String username = auth.username();
        var gameData = dao.getGame(gameId).orElseThrow(() -> new Exception("Game doesn't exist"));

        toGame.put(ctx, gameId);
        gameSession.computeIfAbsent(gameId, k -> new HashSet<>()).add(ctx);
        var loadMsg = ServerMessage.loadGame(gameData.game());
        ctx.send(gson.toJson(loadMsg));

        String color = null;
        if (username.equals(gameData.whiteUsername())) color = "white";
        else if (username.equals(gameData.blackUsername())) color = "black";

        String Text = (color != null)
                ? username + "connected as : " + color
                : username + "connected as observer";
        
        broadcastToOthers(gameId, ctx, ServerMessage.notification(Text));
    }

    private void broadcastToOthers(int gameId, WsContext ctx, ServerMessage notification) {
    }


    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }
    public void stop() {
        javalin.stop();
    }
}
