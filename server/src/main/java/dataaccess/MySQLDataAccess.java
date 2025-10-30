package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;



public class MySQLDataAccess implements DataAccess{
    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection(); var statement = conn.createStatement()){
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            statement.executeUpdate("TRUNCATE TABLE auth");
            statement.executeUpdate("TRUNCATE TABLE users");
            statement.executeUpdate("TRUNCATE TABLE games");
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    //user
    @Override
    public void createUser(UserData u) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?,?,?)")){
            statement.setString(1, u.username());
            statement.setString(2, BCrypt.hashpw(u.password(), BCrypt.gensalt()));
            statement.setString(3, u.email());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("already taken");
        }
    }

    @Override
    public Optional<UserData> getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement("SELECT * FROM users WHERE username=?")){
            statement.setString(1, username);
            var rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(new UserData(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")));
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    //auth
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


    //games
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
