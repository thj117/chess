package client;

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
        Assertions.assertTrue(true);
    }

    @Test
    void registerSuccess() throws Exception {
        var auth = facade.register("p1", "pw", "p1@email.com");
        assertNotNull(auth.authToken());
    }

    @Test
    void loginSuccess() throws Exception {
        var reg = facade.register("loginUser", "pw", "loginUser@email.com");
        var auth = facade.login("loginUser", "pw");

        assertNotNull(auth.authToken(), "Auth token should not be null after successful login");
        assertEquals("loginUser", auth.username(), "Username should match registered username");
    }

}
