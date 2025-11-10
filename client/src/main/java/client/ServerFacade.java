package client;

import com.google.gson.Gson;
import model.AuthData;


import java.net.http.HttpClient;


public class ServerFacade {
    private final String serverurl;
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    public ServerFacade(int port){
        this.serverurl = "http://localhost:" + port;
    }

    public AuthData register(String username, String password, String email) throws Exception{
        return null;
    }

    public  AuthData login(String username, String password) throws Exception{
        return null;
    }

    public void logout(String authToken) throws Exception{

    }


}

