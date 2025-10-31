package service;

import dataaccess.InMemoryDataAccess;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {
    private DataAccess dao;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        dao = new InMemoryDataAccess();
        userService = new UserService(dao);
    }

    @Test
    public void registeringSuccess() throws Exception {
        var req = new RegisterRequest("alice", "pw", "a@a.com");
        var res = userService.register(req);
        assertEquals("alice", res.username());
        assertNotNull(res.authToken());
        assertTrue(dao.getUser("alice").isPresent());
        assertTrue(dao.getAuth(res.authToken()).isPresent());
    }

    @Test
    public void registeringDuplicateFail() throws Exception {
        var req = new RegisterRequest("bob", "pw", "b@b.com");
        userService.register(req);

        // second register should throw DataAccessException with message "already taken"
        AlreadyTakenException ex = assertThrows(AlreadyTakenException.class, () -> userService.register(req));
        assertEquals("already taken", ex.getMessage());
    }

    @Test
    public void loginSuccess() throws Exception {
        var req = new RegisterRequest("c", "pw", "c@c.com");
        var r = userService.register(req);

        var loginReq = new LoginRequest("c", "pw");
        var res = userService.login(loginReq);
        assertEquals("c", res.username());
        assertNotNull(res.authToken());
    }

    @Test
    public void loginWrongPasswordFail() throws Exception {
        var req = new RegisterRequest("d", "pw", "d@d.com");
        userService.register(req);

        var loginReq = new LoginRequest("d", "wrong");
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> userService.login(loginReq));
        assertEquals("unauthorized 3", ex.getMessage());
    }

    @Test
    public void logoutSuccess() throws
            Exception {
            var reg = userService.register(new RegisterRequest("bob", "password", "email@email.com"));
            userService.logout(reg.authToken());
            assertTrue(dao.getAuth(reg.authToken()).isEmpty());
        }


    @Test
    public void logoutInvalidTokenFail() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> userService.logout("nope"));
        assertEquals("unauthorized", ex.getMessage());
    }

}