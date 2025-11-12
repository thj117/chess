package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.AuthData;
import model.GameData;
import service.CreateGameRequest;
import service.ListGamesResult;
import service.LoginRequest;
import service.RegisterRequest;


import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import model.*;



public class ServerFacade {
    private final String serverurl;
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    public ServerFacade(int port){
        this.serverurl = "http://localhost:" + port;
    }

    public AuthData register(String username, String password, String email) throws Exception{
        RegisterRequest req = new RegisterRequest(username, password, email);
        var body = gson.toJson(req);
        var httpReq  = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/user"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() == 200){
            return gson.fromJson(res.body(), AuthData.class);
        } else {
            throw new Exception(parseError(res.body()));
        }
    }

    public  AuthData login(String username, String password) throws Exception{
        LoginRequest req = new LoginRequest(username, password);
        var body = gson.toJson(req);
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/session"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() == 200){
            return gson.fromJson(res.body(), AuthData.class);
        } else {
            throw new Exception(parseError(res.body()));
        }
    }

    public void logout(String authToken) throws Exception{
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/session"))
                .header("Content-Type", authToken)
                .DELETE()
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200){
            throw new Exception(parseError(res.body()));
        }
    }

    public int createGame(String authToken, String gameName) throws Exception {
        CreateGameRequest req = new CreateGameRequest(gameName);
        var body = gson.toJson(req);
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/game"))
                .header("authorization", authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            Map<?, ?> map = gson.fromJson(res.body(), Map.class);
            Double gameID = (Double) map.get("gameID");
            return gameID.intValue();
        } else {
            throw new Exception(parseError(res.body()));
        }
    }

    public List<GameData> listGames(String authtoken)throws Exception{
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/game"))
                .header("authorization", authtoken)
                .GET()
                .build();

        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() == 200) {
            Type resultType = new TypeToken<ListGamesResult>() {}.getType();
            return gson.fromJson(res.body(), resultType);
        } else {
            throw new Exception(parseError(res.body()));
        }
    }


    private String parseError(String bodyResponse){
        try {
            var error = gson.fromJson(bodyResponse, Map.class);
            return (String) error.getOrDefault("message", "unknown error occurred");
        } catch (Exception e){
            return "Sever error:" + bodyResponse;
        }
    }

}

