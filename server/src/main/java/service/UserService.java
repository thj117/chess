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
        UserData u = new UserData(req.username(), req.password(), req.email());

        try {
           dao.createUser(u);
        } catch (DataAccessException except) {
            // if duplicate user exists, translate
            if (dao.getUser(u.username()).isPresent()) {
                throw new DataAccessException("already taken");
            }
            throw except;
        }

        String token = generateToken();
        AuthData a = new AuthData(token, u.username());
        dao.createAuth(a);
        return new RegisterResult(u.username(), token);
    }



    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

}
