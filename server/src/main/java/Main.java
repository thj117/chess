import chess.*;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import server.Server;

public class Main {
    public static void main(String[] args) throws DataAccessException {
        DatabaseManager.createDatabase();
        Server server = new Server();
        server.run(8080);

        System.out.println("♕ 240 Chess Server");
    }
}