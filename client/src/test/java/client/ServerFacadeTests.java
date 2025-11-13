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



}
