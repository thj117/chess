package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryDataAccess implements DataAccess {
    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Map<String, AuthData> auths = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> games = new ConcurrentHashMap<>();
    private final AtomicInteger gameIdCounter = new AtomicInteger(1);

    @Override
    public void createUser(UserData u) throws DataAccessException {
        Objects.requireNonNull(u);
        if (users.putIfAbsent(u.username(), u) != null){
            throw new DataAccessException("user already exists");
        }
    }

    @Override
    public Optional<UserData> getUser(String username) throws DataAccessException {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public void clear(){
        users.clear();
        games.clear();
        auths.clear();
        gameIdCounter.set(1);
    }

    @Override
    public void createAuth(AuthData a) throws DataAccessException {
        Objects.requireNonNull(a);
        auths.put(a.authToken(), a);

    }

    @Override
    public Optional<AuthData> getAuth(String authToken) throws DataAccessException {
        return Optional.ofNullable(auths.get(authToken));
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        auths.remove(authToken);
    }

    @Override
    public int createGame(GameData g) throws DataAccessException {
        int id = gameIdCounter.getAndIncrement();
        GameData stored = new GameData(id, g.whiteUsername(), g.blackUsername(), g.gameName(), g.game());
        games.put(id, stored);
        return id;
    }

    @Override
    public Optional<GameData> getGame(int gameID) throws DataAccessException {
        return Optional.empty();
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData g) throws DataAccessException {

    }


}


