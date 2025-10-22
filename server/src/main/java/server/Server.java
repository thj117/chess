package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final DataAccess dao = new InMemoryDataAccess();
    private final UserService userService = new UserService(dao);
    private final GameService gameService = new GameService(dao);

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Clear DB
        javalin.delete("/db", ctx -> {
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
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
            } catch (DataAccessException e) {
                if ("already taken".equals(e.getMessage())) {
                    ctx.status(403).json(Map.of("message", "Error: already taken"));
                } else {
                    ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
                }
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
            } catch (IllegalArgumentException e) {
                ctx.status(400).json((Map.of("message", "Error: bad request")));
            } catch (DataAccessException e) {
                ctx.status(401).json(Map.of("message", "Error: unauthorized"));
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
            } catch (DataAccessException e) {
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
            } catch (DataAccessException e) {
                if ("bad request".equals(e.getMessage())) ctx.status(400).json(Map.of("message", "Error: bad request"));
                else ctx.status(401).json(Map.of("message", "Error: unauthorized"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        // Join Game
        javalin.put("/game", ctx -> {
            try {
                String token = ctx.header("authorization");
                JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);
                gameService.joinGame(token, req);
                ctx.status(200).result("{}");
            } catch (DataAccessException e) {
                String msg = e.getMessage();
                if ("bad request".equals(msg)) ctx.status(400).json(Map.of("message", "Error: bad request"));
                else if ("already taken".equals(msg)) ctx.status(403).json(Map.of("message", "Error: already taken"));
                else ctx.status(401).json(Map.of("message", "Error: unauthorized"));
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
