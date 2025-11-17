package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.json.JavalinGson;
import service.*;
import io.javalin.Javalin;
import java.util.Map;

public class Server {
    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final DataAccess dao = new MySQLDataAccess();
    private final UserService userService = new UserService(dao);
    private final GameService gameService = new GameService(dao);

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
    }
    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }
    public void stop() {
        javalin.stop();
    }
}
