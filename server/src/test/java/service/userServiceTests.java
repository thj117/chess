package service;

import dataaccess.InMemoryDataAccess;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class userServiceTests {
    private DataAccess dao;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        dao = new InMemoryDataAccess();
        userService = new UserService(dao);
    }

    @Test
    public void register_success() throws Exception {
        var req = new RegisterRequest("alice", "pw", "a@a.com");
        var res = userService.register(req);
        assertEquals("alice", res.username());
        assertNotNull(res.authToken());
        assertTrue(dao.getUser("alice").isPresent());
        assertTrue(dao.getAuth(res.authToken()).isPresent());
    }

    @Test
    public void register_duplicate_fail() throws Exception {
        var req = new RegisterRequest("bob", "pw", "b@b.com");
        userService.register(req);

        // second register should throw DataAccessException with message "already taken"
        DataAccessException ex = assertThrows(DataAccessException.class, () -> userService.register(req));
        assertEquals("already taken", ex.getMessage());
    }

    @Test
    public void login_success() throws Exception {
        var req = new RegisterRequest("c", "pw", "c@c.com");
        var r = userService.register(req);

        var loginReq = new LoginRequest("c", "pw");
        var res = userService.login(loginReq);
        assertEquals("c", res.username());
        assertNotNull(res.authToken());
    }

    @Test
    public void login_wrong_password_fail() throws Exception {
        var req = new RegisterRequest("d", "pw", "d@d.com");
        userService.register(req);

        var loginReq = new LoginRequest("d", "wrong");
        DataAccessException ex = assertThrows(DataAccessException.class, () -> userService.login(loginReq));
        assertEquals("Unauthorized", ex.getMessage());
    }
}