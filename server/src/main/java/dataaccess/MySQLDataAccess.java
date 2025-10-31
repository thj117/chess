package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import model.*;
import java.sql.*;
import java.util.*;


public class MySQLDataAccess implements DataAccess{
    private final Gson gson = new Gson();


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
            statement.setString(2, u.password());
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
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
    }

    //auth
    @Override
    public void createAuth(AuthData a) throws DataAccessException {
        String sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.authToken());
            stmt.setString(2, a.username());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AuthData> getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM auth WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(sql)) {
            statement.setString(1, authToken);
            var rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(new AuthData(
                        rs.getString("authToken"),
                        rs.getString("username")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("SQL error fetching auth", e);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection(); var statement = conn.prepareStatement(sql)){
            statement.setString(1, authToken);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("SQL error deleting auth", e);
        }

    }


    //games
    @Override
    public int createGame(GameData g) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO games (whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, g.whiteUsername());
            stmt.setString(2, g.blackUsername());
            stmt.setString(3, g.gameName());
            stmt.setString(4, gson.toJson(g.game()));
            stmt.executeUpdate();
            var rs = stmt.getGeneratedKeys();
            if (rs.next()) { return rs.getInt(1);}
            throw new DataAccessException("failed to get gameID");
        } catch (SQLException e) {
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<GameData> getGame(int gameID) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM games WHERE gameID=?")) {
            stmt.setInt(1, gameID);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                ChessGame chessGame = null;
                String gameJson = rs.getString("game");
                if (gameJson != null && !gameJson.isEmpty()) {
                    chessGame = new Gson().fromJson(gameJson, ChessGame.class);
                }
                return Optional.of(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        var list = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM games")) {
            var rs = stmt.executeQuery();
            while (rs.next()) {
                ChessGame chessGame = null;
                String gameJson = rs.getString("game");
                if (gameJson != null && !gameJson.isEmpty()) {
                    chessGame = new Gson().fromJson(gameJson, ChessGame.class);
                }

                list.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void updateGame(GameData g) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(
                     "UPDATE games SET whiteUsername=?, blackUsername=?, gameName=?, game=? WHERE gameID=?")) {
            stmt.setString(1, g.whiteUsername());
            stmt.setString(2, g.blackUsername());
            stmt.setString(3, g.gameName());
            stmt.setString(4, gson.toJson(g.game()));
            stmt.setInt(5, g.gameID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Database access error: " + e.getMessage(), e);
        }
    }
}
