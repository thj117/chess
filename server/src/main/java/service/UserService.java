package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;

import java.util.UUID;


public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public RegisterResult register(RegisterRequest req) throws DataAccessException, IllegalArgumentException{
        if (req == null || req.username() == null || req.password() == null || req.email() == null){
            throw new IllegalArgumentException("bad request");
        }
        return null;
    }
}
