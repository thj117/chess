package client;

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;


    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @BeforeEach
    void clear() throws Exception {
        facade.clear();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    public void sampleTest() {
        assertTrue(true);
    }

    @Test
    void registerSuccess() throws Exception {
        var auth = facade.register("p1", "pw", "p1@email.com");
        assertNotNull(auth.authToken());
    }

    @Test
    void registerFail() throws Exception {
        facade.register("dupeUser", "pw", "e@e.com");
        Exception ex = assertThrows(Exception.class, () -> {
            facade.register("dupeUser", "pw", "e@e.com");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("already taken"));
    }


    @Test
    void loginSuccess() throws Exception {
        var reg = facade.register("loginUser", "pw", "loginUser@email.com");
        var auth = facade.login("loginUser", "pw");

        assertNotNull(auth.authToken(), "Auth token should not be null after successful login");
        assertEquals("loginUser", auth.username(), "Username should match registered username");
    }

    @Test
    void loginFail() throws Exception {
        facade.register("badLogin", "pw", "email@email.com");
        Exception ex = assertThrows(Exception.class, () -> {
            facade.login("badLogin", "wrongpw");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    void logoutSuccess() throws Exception {
        var auth = facade.register("logoutUser", "pw", "logout@email.com");
        System.out.println("Token received: " + auth.authToken());
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    void logoutFail() {
        Exception ex = assertThrows(Exception.class, () -> {
            facade.logout("invalid-token");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    void createGameSuccess() throws Exception {
        var auth = facade.register("creator", "pw", "c@c.com");
        var game = facade.createGame(auth.authToken(), "Epic Match");
        assertTrue(game > 0, "Game ID should be greater than 0");
    }

    @Test
    void createGameFail() {
        Exception ex = assertThrows(Exception.class, () -> {
            facade.createGame("fake-token", "Invalid Match");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    void listGamesSuccess() throws Exception {
        var auth = facade.register("lister", "pw", "l@l.com");
        facade.createGame(auth.authToken(), "My Game");
        var games = facade.listGames(auth.authToken());
        assertFalse(games.isEmpty(), "Games list should not be empty");
    }

    @Test
    void listGamesFail() {
        Exception ex = assertThrows(Exception.class, () -> {
            facade.listGames("bad-token");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    void joinGameSuccess() throws Exception {
        var auth = facade.register("joiner", "pw", "j@j.com");
        var createRes = facade.createGame(auth.authToken(), "Joinable Game");
        assertDoesNotThrow(() -> facade.joinGame(auth.authToken(), "WHITE", createRes));
    }

    @Test
    void joinGameFail() throws Exception {
        var auth = facade.register("joinFail", "pw", "jf@jf.com");
        Exception ex = assertThrows(Exception.class, () -> {
            facade.joinGame(auth.authToken(), "WHITE", 9999); // bad ID
        });
        assertTrue(ex.getMessage().toLowerCase().contains("bad request"));
    }

    @Test
    void clearSuccess() throws Exception {
        var auth = facade.register("clearUser", "pw", "clear@clear.com");
        assertNotNull(auth.authToken());
        assertDoesNotThrow(() -> facade.clear());
    }



}
