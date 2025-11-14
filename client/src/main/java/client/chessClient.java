package client;

import model.GameData;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class chessClient {
    private final ServerFacade server;
    private boolean running = true;
    private boolean loggedin = false;
    private String authToken;
    private List<GameData> lastGameList = new ArrayList<>();

    public chessClient(int port){
        this.server = new ServerFacade(port);
    }

    public void run() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Chess! you can type help for a list of commands");

        while (running){
            if(!loggedin){
                System.out.println("Pre-Login");
                handlePreLogin(scanner.nextLine().trim().toLowerCase(), scanner);
            } else {
                System.out.println("Post-Login");
                handlePostLogin(scanner.nextLine().trim().toLowerCase(), scanner);
            }
        }
        scanner.close();
    }

    private void handlePreLogin(String input, Scanner scanner){
        switch (input){
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
                    var auth = server.register(u,p,e);
                    authToken = auth.authToken();
                    loggedin = true;
                    System.out.println("Registered and logged in as" + auth.username());
                } catch (Exception ex) {
                    System.out.println("Registration failed: " +  ex.getMessage());
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
                    System.out.println("Logged in as " + auth.username());
                } catch (Exception ex) {
                    System.out.println("Login failed: " + ex.getMessage());
                }
            }
            default -> System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private void handlePostLogin(String input, Scanner scanner) throws Exception {
        switch (input){
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
                System.out.println("game created with =" + gameID);
            }
            case "list" -> {
                List<GameData> games = server.listGames(authToken);
                lastGameList = games;

                if (games.isEmpty()){
                    System.out.println("No games exist");
                    return;
                }
                System.out.println("\n Games:");
                for(int i = 0; i < games.size(); i++){
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
                if (lastGameList.isEmpty()){
                    System.out.println("You must run 'list' first to see the list of games ");
                    break;
                }
                try {
                    System.out.println("Enter game number: ");
                    String id = scanner.nextLine().trim();
                    int idx = Integer.parseInt(id);

                    if (idx < 1 || idx > lastGameList.size()){
                        System.out.println("Invalid game number");
                        break;
                    }

                    GameData g = lastGameList.get(idx - 1);
                    System.out.println("Choose your color: (white or black)");
                    String color = scanner.nextLine().trim().toUpperCase();

                    if(!color.equals("WHITE") && !color.equals("BLACK")){
                        System.out.println("Invalid color");
                        break;
                    }

                    server.joinGame(authToken, color, g.gameID());
                    System.out.println("Joined game " + g.gameName() + "as the color " + color);
                    drawBoard(color.equals("WHITE"));

                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a valid number");
                } catch (Exception e) {
                    System.out.println("Play game failed: " + e.getMessage());
                }
            }
            case "observe game" -> {
                if (lastGameList.isEmpty()){
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

                    server.joinGame(authToken,null, g.gameID());
                    System.out.println("Observing game: " + g.gameName());
                    drawBoard(true); // Observers see white perspective
                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a valid number.");
                } catch (Exception ex) {
                    System.out.println("Observe game failed: " + ex.getMessage());
                }
            }

        }
    }

    private void printPostHelp(){
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

    private void printPreHelp(){
        System.out.println("""
               Available commands:
               help  -  Shows this help message
               register  -  Helps to create a new account
               login  -  Login with an existing account
               quit  -  Exit the program
               """);
    }

    private void drawBoard(boolean whitePerspective) {
        // uppercase = white, lowercase = black
        char[][] board = new char[8][8];

        // Black pieces
        String backRank = "rnbqkbnr";
        for (int i = 0; i < 8; i++) {
            board[0][i] = Character.toLowerCase(backRank.charAt(i)); // 8th rank
            board[1][i] = 'p'; // pawns
        }

        // Empty middle
        for (int r = 2; r <= 5; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = '.';
            }
        }

        // White pieces
        for (int i = 0; i < 8; i++) {
            board[6][i] = 'P';
            board[7][i] = Character.toUpperCase(backRank.charAt(i));
        }

        System.out.println();
        if (whitePerspective) {
            // White at the bottom (ranks 8 → 1 visually)
            for (int r = 7; r >= 0; r--) {
                System.out.print((r + 1) + " ");
                for (int c = 0; c < 8; c++) {
                    System.out.print(board[r][c] + " ");
                }
                System.out.println();
            }
            System.out.println("  a b c d e f g h");
        } else {
            // Black perspective: flip rows and columns
            for (int r = 0; r < 8; r++) {
                System.out.print((8 - r) + " ");
                for (int c = 7; c >= 0; c--) {
                    System.out.print(board[r][c] + " ");
                }
                System.out.println();
            }
            System.out.println("  h g f e d c b a");
        }
        System.out.println();
    }
}

