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
        var dao = new MySQLDataAccess();
        dao.clear();
        var user = new UserData("alice", "p", "a@a");
        dao.createUser(user);
        assertTrue(dao.getUser("alice").isPresent());

    }

    @Test
    public void createUser_alreadyTaken_fail() throws Exception {
        dao.createUser(new UserData("u1", "p", "e"));
        assertThrows(DataAccessException.class, () -> dao.createUser(new UserData("u1", "p2", "e2")));
    }

}
