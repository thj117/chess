package client;

import java.util.Scanner;

public class chessClient {
    private final ServerFacade server;
    private boolean running = true;
    private boolean loggedin = false;
    private String authToken;

    public chessClient(int port){
        server = new ServerFacade(port);
    }

    public void run(){
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
                String u = scanner.nextLine();
                System.out.println("password: ");
                String p = scanner.nextLine();
                System.out.println("email: ");
                String e = scanner.nextLine();
                try {
                    var auth = server.register(u,p,e);
                    authToken = auth.authToken();
                    loggedin = true;
                    System.out.println("Registered and logged in as" + auth.username());
                } catch (Exception ex) {
                    System.out.println("Registration failed" + ex.getMessage());
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

    private void handlePostLogin(String input, Scanner scanner){
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
            case "create" -> System.out.println("create game");
            case "list" -> System.out.println("list games");
            case "play" -> System.out.println("play game");
            case "observe game" -> System.out.println("observe game");
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

}
