package client;


import client.requests.CreateGameRequest;
import client.requests.JoinGameRequest;
import client.requests.LoginRequest;
import client.requests.RegisterRequest;
import com.google.gson.Gson;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public void logout(String authToken) throws Exception {
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/session"))
                .header("authorization", authToken)
                .DELETE()
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
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

    public List<GameData> listGames(String authToken) throws Exception {
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/game"))
                .header("authorization", authToken)
                .GET()
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() == 200) {
            Map map = gson.fromJson(res.body(), Map.class);
            List<?> rawGames = (List<?>) map.get("games");
            List<GameData> games = new ArrayList<>();
            for (Object obj : rawGames) {
                String json = gson.toJson(obj);
                games.add(gson.fromJson(json, GameData.class));
            }
            return games;
        }

        throw new Exception(parseError(res.body()));
    }

    public void joinGame(String authToken, String color, int gameID)throws Exception{
        JoinGameRequest req = new JoinGameRequest(color, gameID);
        var body = gson.toJson(req);
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/game"))
                .header("authorization", authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new Exception(parseError(res.body()));
        }
    }

    public void clear() throws Exception {
        var httpReq = HttpRequest.newBuilder()
                .uri(URI.create(serverurl + "/db"))
                .DELETE()
                .build();
        var res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new Exception(parseError(res.body()));
        }
    }


    private String parseError(String bodyResponse) {
        try {
            Map<?, ?> error = gson.fromJson(bodyResponse, Map.class);
            Object msg = error.get("message");
            return msg == null ? "Unknown error" : msg.toString();
        } catch (Exception ex) {
            return "Server error: " + bodyResponse;
        }
    }

}

