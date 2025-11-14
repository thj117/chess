package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;


public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public void clear() throws DataAccessException {
        dao.clear();
    }

    public RegisterResult register(RegisterRequest req) throws Exception{
        if (req == null || req.username() == null || req.password() == null || req.email() == null){
            throw new BadRequestException("bad request");
        }
        UserData u = new UserData(req.username(), BCrypt.hashpw(req.password(), BCrypt.gensalt()), req.email());
        if (dao.getUser(u.username()).isPresent()) {
            throw new AlreadyTakenException("already taken");
        }
        dao.createUser(u);

        String token = generateToken();
        AuthData a = new AuthData(token, u.username());
        dao.createAuth(a);
        return new RegisterResult(u.username(), token);
    }

    public LoginResult login(LoginRequest log) throws DataAccessException, UnauthorizedException, BadRequestException{
        if (log == null || log.password() == null|| log.username() == null){
            throw new BadRequestException("bad request");
        }
        var maybe = dao.getUser(log.username());
        if (maybe.isEmpty()){ throw new UnauthorizedException("unauthorized 1");}
        UserData u = maybe.get();
        if(!u.username().equals(log.username()))
        { throw new UnauthorizedException("unauthorized 2");}
        if(!BCrypt.checkpw(log.password(), u.password()))
        { throw new UnauthorizedException("unauthorized 3");
        }

        String token = generateToken();
        AuthData a = new AuthData(token, u.username());
        dao.createAuth(a);
        return new LoginResult(u.username(), token);

    }

    public void logout(String authToken) throws DataAccessException, UnauthorizedException {
        if (authToken == null || dao.getAuth(authToken).isEmpty()){
            throw new UnauthorizedException("unauthorized");
        }
        dao.deleteAuth(authToken);
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

}
