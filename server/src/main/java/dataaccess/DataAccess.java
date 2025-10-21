package dataaccess;

import model.GameData;
import model.UserData;
import model.AuthData;
import java.util.List;
import java.util.Optional;


public interface DataAccess {

    //for the user
    void createUser(UserData u) throws DataAccessException;
    Optional<UserData> getUser(String username) throws DataAccessException;
    void clear() throws DataAccessException;

    // for auth
    void createAuth(AuthData a) throws DataAccessException;
    Optional<AuthData> getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    //for game
    int createGame(GameData g) throws DataAccessException;
    Optional<GameData> getGame(int gameID) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;
    void updateGame(GameData g) throws DataAccessException;


}
