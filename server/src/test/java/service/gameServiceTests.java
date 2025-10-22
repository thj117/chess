package service;

import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.GameService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class gameServiceTests {
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
    public void create_games_success() throws Exception {
        var r = userService.register(new RegisterRequest("u1", "p", "e"));
        var createRes = gameService.createGame(r.authToken(), new CreateGameRequest("My Game"));
        assertTrue(createRes.gameID() > 0);

        var list = gameService.listGames(r.authToken());
        assertFalse(list.games().isEmpty());
    }

    @Test
    public void create_game_unauthorized_fail() {
        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> gameService.createGame("badtoken", new CreateGameRequest("G")));
        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void join_game_success_and_already_taken_fail() throws Exception {
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
}
