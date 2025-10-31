package dataaccess;

import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;


public class MySQLDataAccessTests {
    private DataAccess dao;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MySQLDataAccess();
        dao.clear();
    }
    @Test
    public void createUserSuccess() throws DataAccessException {
        var user = new UserData("alice", "p", "a@a");
        dao.createUser(user);
        assertTrue(dao.getUser("alice").isPresent());

    }

    @Test
    public void createUserAlreadyTakenFail() throws Exception {
        dao.createUser(new UserData("u1", "p", "e"));
        assertThrows(DataAccessException.class, () -> dao.createUser(new UserData("u1", "p2", "e2")));
    }

    @Test
    public void getAuthSuccess() throws Exception {
        dao.createUser(new UserData("maverick", "pass", "b@b.com"));
        dao.createAuth(new AuthData("token123", "maverick"));
        Optional<AuthData> found = dao.getAuth("token123");
        assertTrue(found.isPresent());
        assertEquals("maverick", found.get().username());
    }

    @Test
    void getAuthNotFoundReturnsEmpty() throws Exception {
        Optional<AuthData> found = dao.getAuth("ghosttoken");
        assertTrue(found.isEmpty());
    }

    @Test
    void createAuthNoUserFails() throws Exception {
        var auth = new AuthData("badToken", "ghost"); // "ghost" not in users

        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            dao.createAuth(auth);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("foreign key")
                        || ex.getMessage().toLowerCase().contains("constraint"),
                "Expected foreign key constraint violation");
    }

    @Test
    public void clearSuccess() throws Exception {
        dao.createUser(new UserData("x", "y", "z"));
        dao.clear();
        assertTrue(dao.listGames().isEmpty());
        assertTrue(dao.getUser("x").isEmpty());
    }

    @Test
    public void createGameSuccess() throws Exception {
        int id = dao.createGame(new GameData(0, null, null, "MyGame", null));
        assertTrue(dao.getGame(id).isPresent());
    }

    @Test
    void getGameNotFoundReturnsEmpty() throws Exception {
        Optional<GameData> game = dao.getGame(9999); // doesn’t exist
        assertTrue(game.isEmpty());
    }

    @Test
    void updateGameSuccess() throws Exception {
        dao.createUser(new UserData("ellen", "pw", "e@e.com"));
        GameData game = new GameData(0, null, null, "Game1", null);
        int id = dao.createGame(game);

        GameData updated = new GameData(id, "ellen", null, "Updated Game", null);
        dao.updateGame(updated);

        Optional<GameData> found = dao.getGame(id);
        assertTrue(found.isPresent());
        assertEquals("ellen", found.get().whiteUsername());
        assertEquals("Updated Game", found.get().gameName());
    }

    @Test
    void updateGameNotFoundFails() {
        GameData bad = new GameData(9999, "x", null, "Nope", null);
        DataAccessException ex = assertThrows(DataAccessException.class, () -> dao.updateGame(bad));
        assertTrue(ex.getMessage().toLowerCase().contains("no game") || ex.getMessage().toLowerCase().contains("failed"));
    }
    @Test
    void listGamesSuccess() throws Exception {
        dao.createUser(new UserData("listUser", "pw", "email@domain.com"));
        dao.createGame(new GameData(0, null, null, "Game One", null));
        dao.createGame(new GameData(0, null, null, "Game Two", null));

        List<GameData> games = dao.listGames();

        assertNotNull(games);
        assertEquals(2, games.size());
        assertTrue(games.stream().anyMatch(g -> "Game One".equals(g.gameName())));
        assertTrue(games.stream().anyMatch(g -> "Game Two".equals(g.gameName())));
    }
    @Test
    void listGamesEmpty() throws Exception {
        List<GameData> games = dao.listGames();

        assertNotNull(games, "listGames() should not return null");
        assertTrue(games.isEmpty(), "Expected no games but found some");
    }

    @Test
    void getGameSuccess() throws Exception {
        dao.createUser(new UserData("getGameUser", "pw", "email@domain.com"));
        GameData created = new GameData(0, "getGameUser", null, "Retrieve Game", null);
        int id = dao.createGame(created);

        Optional<GameData> found = dao.getGame(id);
        assertTrue(found.isPresent(), "Expected to find the created game");
        assertEquals("Retrieve Game", found.get().gameName());
        assertEquals(id, found.get().gameID());
    }

    @Test
    void getGameNotFound() throws Exception {
        Optional<GameData> found = dao.getGame(9999); // ID that doesn't exist
        assertTrue(found.isEmpty(), "Expected empty Optional for non-existent game");
    }

    @Test
    void deleteAuthSuccess() throws Exception {
        dao.createUser(new UserData("authUser", "pw", "a@a.com"));
        AuthData auth = new AuthData("tokenToDelete", "authUser");
        dao.createAuth(auth);
        dao.deleteAuth("tokenToDelete");

        Optional<AuthData> result = dao.getAuth("tokenToDelete");
        assertTrue(result.isEmpty(), "Auth token should be deleted from database");
    }

    @Test
    void deleteAuthNotFoundDoesNotThrow() {
        assertDoesNotThrow(() -> dao.deleteAuth("nonexistentToken"),
                "Deleting a non-existent auth should not throw an exception");
    }

    @Test
    void getUserNotFoundReturnsEmpty() throws Exception {
        Optional<UserData> found = dao.getUser("ghost");
        assertTrue(found.isEmpty());
    }

    @Test
    void getUserSuccess() throws Exception {
        UserData u = new UserData("chris", "pw", "c@c.com");
        dao.createUser(u);
        Optional<UserData> found = dao.getUser("chris");
        assertTrue(found.isPresent());
        assertEquals("chris", found.get().username());
    }

    @Test
    void createAuthSuccess() throws Exception {
        dao.createUser(new UserData("authUser", "pw", "a@a.com"));
        AuthData auth = new AuthData("token1", "authUser");
        dao.createAuth(auth);
        Optional<AuthData> found = dao.getAuth("token1");
        assertTrue(found.isPresent());
        assertEquals("authUser", found.get().username());
    }


}
