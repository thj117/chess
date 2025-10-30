package dataaccess;

import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;


public class MySQLDataAccessTests {
    private DataAccess dao;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MySQLDataAccess();
        dao.clear();
    }
    @Test
    public void createUser_success() throws DataAccessException {
        var user = new UserData("alice", "p", "a@a");
        dao.createUser(user);
        assertTrue(dao.getUser("alice").isPresent());

    }

    @Test
    public void createUser_alreadyTaken_fail() throws Exception {
        dao.createUser(new UserData("u1", "p", "e"));
        assertThrows(DataAccessException.class, () -> dao.createUser(new UserData("u1", "p2", "e2")));
    }

    @Test
    public void getAuth_success() throws Exception {
        dao.createUser(new UserData("maverick", "pass", "b@b.com"));
        dao.createAuth(new AuthData("token123", "maverick"));
        Optional<AuthData> found = dao.getAuth("token123");
        assertTrue(found.isPresent());
        assertEquals("maverick", found.get().username());
    }

    @Test
    void createAuth_noUser_fails() throws Exception {
        var auth = new AuthData("badToken", "ghost"); // "ghost" not in users

        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            dao.createAuth(auth);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("foreign key")
                        || ex.getMessage().toLowerCase().contains("constraint"),
                "Expected foreign key constraint violation");
    }

    @Test
    public void clear_success() throws Exception {
        dao.createUser(new UserData("x", "y", "z"));
        dao.clear();
        assertTrue(dao.listGames().isEmpty());
        assertTrue(dao.getUser("x").isEmpty());
    }

    @Test
    public void createGame_success() throws Exception {
        int id = dao.createGame(new GameData(0, null, null, "MyGame", null));
        assertTrue(dao.getGame(id).isPresent());
    }

}
