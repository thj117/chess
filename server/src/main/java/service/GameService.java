package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import chess.ChessGame;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao){
        this.dao = dao;
    }

    public void clear() throws DataAccessException {
        dao.clear();
    }
}
