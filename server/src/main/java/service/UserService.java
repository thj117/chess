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

    public void clear() throws DataAccessException {
        dao.clear();
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

    public LoginResult login(LoginRequest log) throws DataAccessException, IllegalArgumentException{
        if (log == null || log.password() == null|| log.username() == null){
            throw new IllegalArgumentException("bad request");
        }
        var maybe = dao.getUser(log.username());
        if (maybe.isEmpty()) throw new DataAccessException("unauthorized");
        UserData u = maybe.get();
        if(!u.password().equals(log.password())) throw new DataAccessException("unauthorized");

        String token = generateToken();
        AuthData a = new AuthData(token, u.username());
        dao.createAuth(a);
        return new LoginResult(u.username(), token);

    }

    public void logout(String authToken) throws DataAccessException {
        if (authToken == null || dao.getAuth(authToken).isEmpty()){
            throw new DataAccessException("unauthorized");
        }
        dao.deleteAuth(authToken);
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

}
