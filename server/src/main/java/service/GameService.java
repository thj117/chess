package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import chess.ChessGame;

import java.util.Optional;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao){
        this.dao = dao;
    }

    public void clear() throws DataAccessException {
        dao.clear();
    }

    private String verifyAuth(String authToken) throws DataAccessException {
        if (authToken == null) throw new DataAccessException("unauthorized");
        Optional<AuthData> maybe = dao.getAuth(authToken);
        if (maybe.isEmpty()) throw new DataAccessException("unauthorized");
        return maybe.get().username();
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req) throws DataAccessException {
        String username = verifyAuth(authToken);
        if (req == null || req.gameName() == null) throw new DataAccessException("bad request");
        // new game: creator is not automatically assigned to white/black
        GameData g = new GameData(0, null, null, req.gameName(), new ChessGame());
        int id = dao.createGame(g);
        return new CreateGameResult(id);
    }

    public void joinGame(String authToken, JoinGameRequest req) throws DataAccessException {
        String username = verifyAuth(authToken);
        if (req == null || req.playerColor() == null) throw new DataAccessException("bad request");
        int id = req.gameID();
        var maybe = dao.getGame(id);
        if (maybe.isEmpty()) throw new DataAccessException("bad request");
        GameData g = maybe.get();
        String color = req.playerColor().toUpperCase();

        String white = g.whiteUsername();
        String black = g.blackUsername();

        if (color.equals("WHITE")) {
            if (white != null) throw new DataAccessException("already taken");
            GameData updated = new GameData(g.gameID(), username, black, g.gameName(), g.game());
            dao.updateGame(updated);
            return;
        } else if (color.equals("BLACK")) {
            if (black != null) throw new DataAccessException("already taken");
            GameData updated = new GameData(g.gameID(), white, username, g.gameName(), g.game());
            dao.updateGame(updated);
            return;
        } else {
            throw new DataAccessException("bad request");
        }
    }
}
