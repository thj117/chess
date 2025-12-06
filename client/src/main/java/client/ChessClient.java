package client;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import client.requests.GameplayClient;
import model.*;

import java.util.*;

public class ChessClient {
    private final ServerFacade server;
    private boolean running = true;
    private boolean loggedin = false;
    private String authToken;
    private List<GameData> lastGameList = new ArrayList<>();
    private GameplayClient gameplayClient;
    private ChessGame currentGame;
    private final Scanner scanner = new Scanner(System.in);

    public ChessClient(int port) {
        this.server = new ServerFacade(port);
    }

    public void run() throws Exception {
        System.out.println("Welcome to Chess! you can type help for a list of commands");

        while (running) {
            if (!loggedin) {
                System.out.println("Pre-Login");
                handlePreLogin(scanner.nextLine().trim().toLowerCase(), scanner);
            } else {
                System.out.println("Post-Login");
                handlePostLogin(scanner.nextLine().trim().toLowerCase(), scanner);
            }
        }
        scanner.close();
    }

    private void handlePreLogin(String input, Scanner scanner) {
        switch (input) {
            case "help" -> printPreHelp();
            case "quit" -> {
                System.out.println("Goodbye!");
                running = false;
            }
            case "register" -> {
                System.out.println("username: ");
                String u = scanner.nextLine().trim();
                System.out.println("password: ");
                String p = scanner.nextLine().trim();
                System.out.println("email: ");
                String e = scanner.nextLine().trim();
                try {
                    var auth = server.register(u, p, e);
                    authToken = auth.authToken();
                    loggedin = true;
                    System.out.println("Registered and logged in as: " + auth.username());
                } catch (Exception ex) {
                    System.out.println("Registration failed: " + ex.getMessage());
                }
            }
            case "login" -> {
                System.out.print("Username: ");
                String u = scanner.nextLine();
                System.out.print("Password: ");
                String p = scanner.nextLine();
                try {
                    var auth = server.login(u, p);
                    authToken = auth.authToken();
                    loggedin = true;
                    System.out.println("Logged in as: " + auth.username());
                } catch (Exception ex) {
                    System.out.println("Login failed: " + ex.getMessage());
                }
            }
            default -> System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private void handlePostLogin(String input, Scanner scanner) throws Exception {
        switch (input) {
            case "help" -> printPostHelp();
            case "logout" -> {
                try {
                    server.logout(authToken);
                    loggedin = false;
                    authToken = null;
                    System.out.println("Logged out.");
                } catch (Exception e) {
                    System.out.println("Logout failed: " + e.getMessage());
                }
            }
            case "create" -> {
                System.out.println("Enter game name: ");
                String name = scanner.nextLine().trim();
                int gameID = server.createGame(authToken, name);
                System.out.println("game created with = " + gameID);
            }
            case "list" -> {
                List<GameData> games = server.listGames(authToken);
                lastGameList = games;

                if (games.isEmpty()) {
                    System.out.println("No games exist");
                    return;
                }
                System.out.println("\n Games:");
                for (int i = 0; i < games.size(); i++) {
                    GameData g = games.get(i);
                    System.out.printf(
                            "%d. %s   (white: %s, black: %s)%n",
                            i + 1,
                            g.gameName(),
                            g.whiteUsername() == null ? "-" : g.whiteUsername(),
                            g.blackUsername() == null ? "-" : g.blackUsername());
                }
            }
            case "play" -> {
                if (lastGameList.isEmpty()) {
                    System.out.println("You must run 'list' first to see the list of games ");
                    break;
                }
                try {
                    System.out.println("Enter game number: ");
                    String id = scanner.nextLine().trim();
                    int idx = Integer.parseInt(id);

                    if (idx < 1 || idx > lastGameList.size()) {
                        System.out.println("Invalid game number");
                        break;
                    }
                    GameData g = lastGameList.get(idx - 1);
                    System.out.println("Choose your color: (white or black)");
                    String color = scanner.nextLine().trim().toUpperCase();

                    if (!color.equals("WHITE") && !color.equals("BLACK")) {
                        System.out.println("Invalid color");
                        break;
                    }
                    server.joinGame(authToken, color, g.gameID());
                    System.out.println("Joined game " + g.gameName() + " as the color " + color);
                    drawBoard(color);
                    startGameplay(g.gameID(), color, false);

                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a valid number");
                } catch (Exception e) {
                    System.out.println("Play game failed: " + e.getMessage());
                }
            }
            case "observe" -> {
                if (lastGameList.isEmpty()) {
                    System.out.println("You must run 'list' first to see the list of games ");
                    break;
                }
                try {
                    System.out.println("Enter game number: ");
                    String id = scanner.nextLine().trim();
                    int idx = Integer.parseInt(id);

                    if (idx < 1 || idx > lastGameList.size()) {
                        System.out.println("Invalid game number");
                        
                        break;
                    }
                    GameData g = lastGameList.get(idx - 1);
                    server.joinGame(authToken, "observe", g.gameID());
                    System.out.println("Observing game: " + g.gameName());
                    drawBoard("WHITE"); // Observers see white perspective
                    startGameplay(g.gameID(), "WHITE", true);
                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a valid number.");
                } catch (Exception ex) {
                    System.out.println("Observe game failed: " + ex.getMessage());
                }
            }
            default -> System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private void startGameplay(int gameId, String color, boolean observer) {
            gameplayClient = new GameplayClient(server.toString());

            gameplayClient.connect(authToken,gameId, game -> {currentGame = game; drawBoard(color);},
                    notification -> System.out.println("Notice " + notification),
                    error -> System.out.println("Error " + error));
            gameplayLoop(color,observer);
    }

    private void gameplayLoop(String color, boolean observer) {
        System.out.println("""
                GAMEPLAY COMMANDS:
                help      - Show commands
                redraw    - Redraw board
                move      - Make a move (player only)
                highlight - Highlight legal moves
                resign    - resign a game (player only)
                leave     - leave a game (back to menu)    
                """);

        while (true){
            System.out.print("[game] >>> ");
            String command = scanner.nextLine().trim();

            switch (command) {
                case "help" -> {
                    System.out.println("""
                help      - Show commands
                redraw    - Redraw board
                move      - Make a move (player only)
                highlight - Highlight legal moves
                resign    - resign a game (player only)
                leave     - leave a game (back to menu)   
                            """);
                }
                case "redraw" -> {
                    drawBoard(color);
                }
                case "move" -> {
                    if (observer){
                        System.out.println("Observer can't move, command for players only");
                        break;
                    }
                    handleMakeMove(color);
                }
                case "highlight" -> {
                    handleHighlight(color);
                }
                case "resign" -> {
                    if (observer) {
                        System.out.println("Observer can not resign");
                        break;
                    }
                    System.out.println("Are you sure you want to resign? (yes/no): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")){
                        gameplayClient.resign();
                    }
                    if (!scanner.nextLine().trim().equalsIgnoreCase("no")){
                        System.out.println("I'll take that as a no, keep playing ");
                    }
                }
                case "leave" -> {
                    gameplayClient.leave();
                    System.out.println("Leaving game");
                    return;
                }
                default -> {
                    System.out.print("Unknown command type and enter 'help' for valid commands");
                }
            }
        }
    }

    private void handleHighlight(String color) {
        try{
            System.out.println("Enter start piece position: ");
            String input = scanner.nextLine().trim();
            ChessPosition pos = parsePos(input);
            var moves = currentGame.validMoves(pos);
            Set<ChessPosition> highlights = new HashSet<>();
            highlights.add(pos);
            for (var m: moves){
                highlights.add(m.getEndPosition());
            }
            drawBoardWithHighlights(color, highlights);
        } catch (Exception e) {
            System.out.println("Not valid position" + e.getMessage());
        }
    }

    private ChessPosition parsePos(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }

        input = input.trim().toLowerCase();

        if (input.length() != 2) {
            throw new IllegalArgumentException("Position must be like e2 (file + rank)");
        }

        char file = input.charAt(0);
        char rankChar = input.charAt(1);

        if (file < 'a' || file > 'h') {
            throw new IllegalArgumentException("File must be a–h");
        }
        if (rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Rank must be 1–8");
        }

        int col = file - 'a' + 1;
        int row = rankChar - '0';

        return new ChessPosition(row, col);
    }


    private void drawBoardWithHighlights(String color, Set<ChessPosition> highlights) {
    }

    private void handleMakeMove(String color) {
        try {
            System.out.println("Enter start position: ");
            String origin = scanner.nextLine().trim();
            System.out.println("Enter end position: ");
            String moved = scanner.nextLine().trim();

            ChessPosition start = parsePos(origin);
            ChessPosition end = parsePos(moved);
            ChessMove move = new ChessMove(start, end, null);
            gameplayClient.makeMove(move);
        } catch (Exception e){
            System.out.println("Not valid input:  " + e.getMessage());
        }
    }


    private void printPostHelp() {
        System.out.println("""
                Available commands:
                help  -  Shows this help message
                logout  -  Logout user and takes you back to pre-login
                create  -  Creates a new game
                list  -  List existing games
                play  - Join a game to play
                observe  -  Watch an existing game
                """);
    }

    private void printPreHelp() {
        System.out.println("""
                Available commands:
                help  -  Shows this help message
                register  -  Helps to create a new account
                login  -  Login with an existing account
                quit  -  Exit the program
                """);
    }

    public static final String LIGHT = "\u001B[47m";
    public static final String DARK = "\u001B[100m";
    public static final String RESET = "\u001B[0m";


    private void drawBoard(String Perspective) {
        ChessGame game = new ChessGame();
        var board = game.getBoard();

        boolean whitePerspective = Perspective.equalsIgnoreCase("WHITE");

        int startRow = whitePerspective ? 8 : 1;
        int endRow = whitePerspective ? 1 : 8;
        int rowStep = whitePerspective ? -1 : 1;

        char startCol = whitePerspective ? 'a' : 'h';
        char endCol = whitePerspective ? 'h' : 'a';
        int colStep = whitePerspective ? 1 : -1;

        for (int row = startRow; row != endRow + rowStep; row += rowStep) {
            System.out.print(row + " ");
            for (char col = startCol; col != endCol + colStep; col += colStep) {

                ChessPosition pos = new ChessPosition(row, col - 'a' + 1);
                ChessPiece piece = board.getPiece(pos);

                boolean lightSquare = ((row + (col - 'a' + 1)) % 2 == 0);
                String bg = lightSquare ? DARK : LIGHT;

                if (piece != null) System.out.print(bg + pieceToChar(piece) + RESET);
                else System.out.print(bg + "   " + RESET);
            }
            System.out.println();
        }

        System.out.print("  ");
        for (char col = startCol; col != endCol + colStep; col += colStep) {
            System.out.print(" " + col + " ");
        }
        System.out.println();
    }

    private String pieceToChar(ChessPiece piece) {
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                case KING -> " K ";
                case QUEEN -> " Q ";
                case ROOK -> " R ";
                case BISHOP -> " B ";
                case KNIGHT -> " N ";
                case PAWN -> " P ";
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING -> " k ";
                case QUEEN -> " q ";
                case ROOK -> " r ";
                case BISHOP -> " b ";
                case KNIGHT -> " n ";
                case PAWN -> " p ";
            };
        };
    }
}

