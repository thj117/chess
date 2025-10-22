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
}
