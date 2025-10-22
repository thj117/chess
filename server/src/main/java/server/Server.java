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

    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
