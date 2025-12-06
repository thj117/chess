import chess.*;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import server.Server;

public class Main {
    public static void main(String[] args) throws DataAccessException {
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }

        Server server = new Server();
        server.run(8080);

        System.out.println("♕ 240 Chess Server");
    }
}