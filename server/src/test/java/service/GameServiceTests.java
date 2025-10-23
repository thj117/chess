package service;

import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTests {
    private DataAccess dao;
    private UserService userService;
    private GameService gameService;

    @BeforeEach
    public void setUp() {
        dao = new InMemoryDataAccess();
        userService = new UserService(dao);
        gameService = new GameService(dao);
    }

    @Test
    public void createGamesSuccess() throws Exception {
        var r = userService.register(new RegisterRequest("u1", "p", "e"));
        var createRes = gameService.createGame(r.authToken(), new CreateGameRequest("My Game"));
        var createRes2 = gameService.createGame(r.authToken(), new CreateGameRequest("Your Game"));
        assertTrue(createRes.gameID() > 0);
    }

    @Test
    public void createGameUnauthorizedFail() {
        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> gameService.createGame("badtoken", new CreateGameRequest("G")));
        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void joinGameSuccessAndAlreadyTakenFail() throws Exception {
        var regA = userService.register(new RegisterRequest("a", "p", "a@a"));
        var regB = userService.register(new RegisterRequest("b", "p", "b@b"));
        var createRes = gameService.createGame(regA.authToken(), new CreateGameRequest("G"));
        int id = createRes.gameID();

        // a joins white
        gameService.joinGame(regA.authToken(), new JoinGameRequest("WHITE", id));
        // b tries to join white -> already taken
        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> gameService.joinGame(regB.authToken(), new JoinGameRequest("WHITE", id)));
        assertEquals("already taken", ex.getMessage());
    }

    @Test
    public void gamesInListTestSuccess() throws Exception {
        var r = userService.register(new RegisterRequest("u1", "p", "e"));
        var createRes = gameService.createGame(r.authToken(), new CreateGameRequest("My Game"));
        var createRes2 = gameService.createGame(r.authToken(), new CreateGameRequest("Your Game"));
        assertTrue(createRes.gameID() > 0);

        var list = gameService.listGames(r.authToken());
        assertFalse(list.games().isEmpty());
    }

    @Test
    public void listGamesUnauthorizedFails() {
        DataAccessException ex = assertThrows(DataAccessException.class, () ->gameService.listGames("invaild token"));
        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void joinGameColorTakenFail() throws Exception {
        var reg1 = userService.register(new RegisterRequest("u1", "p", "e"));
        var reg2 = userService.register(new RegisterRequest("u2", "pass", "e2"));
        var createRes = gameService.createGame(reg1.authToken(), new CreateGameRequest("My Game"));

        gameService.joinGame(reg1.authToken(), new JoinGameRequest("WHITE", createRes.gameID()));
        DataAccessException ex = assertThrows(DataAccessException.class, () ->gameService.joinGame(reg2.authToken(),
                new JoinGameRequest("WHITE", createRes.gameID())));
        assertEquals("already taken", ex.getMessage()) ;
    }
    @Test
    public void clearSuccess() throws Exception{
        var reg = userService.register(new RegisterRequest("u1", "p", "e"));
        var createRes = gameService.createGame(reg.authToken(), new CreateGameRequest("My Game"));

        userService.clear();
        assertTrue(dao.listGames().isEmpty());
        assertTrue(dao.getUser("u1").isEmpty());

    }
}
