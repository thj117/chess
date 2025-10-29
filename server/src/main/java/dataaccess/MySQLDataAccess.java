package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import java.util.List;
import java.util.Optional;



public class MySQLDataAccess implements DataAccess{
    private final Gson gson = new Gson();


    @Override
    public void createUser(UserData u) throws DataAccessException {

    }

    @Override
    public Optional<UserData> getUser(String username) throws DataAccessException {
        return Optional.empty();
    }

    @Override
    public void clear() throws DataAccessException {

    }

    @Override
    public void createAuth(AuthData a) throws DataAccessException {

    }

    @Override
    public Optional<AuthData> getAuth(String authToken) throws DataAccessException {
        return Optional.empty();
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {

    }

    @Override
    public int createGame(GameData g) throws DataAccessException {
        return 0;
    }

    @Override
    public Optional<GameData> getGame(int gameID) throws DataAccessException {
        return Optional.empty();
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        return List.of();
    }

    @Override
    public void updateGame(GameData g) throws DataAccessException {

    }
}
