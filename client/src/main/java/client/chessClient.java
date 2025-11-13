package client;

import java.util.Scanner;

public class chessClient {
    private final ServerFacade server;
    private boolean running = true;
    private boolean loggedin = false;

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
            case "register" -> System.out.println("register");
            case "login" -> System.out.println("login!");
                default -> System.out.println("Unknown command, please type 'help' for a list of valid commands");
        }
    }

    private void handlePostLogin(String input, Scanner scanner){
        switch (input){
            case "help" -> printPostHelp();
            case "logout" -> System.out.println("logout");
            case "create" -> System.out.println("create game");
            case "list" -> System.out.println("list games");
            case "play" -> System.out.println("play game");
            case "observe game" -> System.out.println("observe game");
        }
    }

    private void printPreHelp(){
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

    private void printPostHelp(){
        System.out.println("""
               Available commands:
               help  -  Shows this help message
               register  -  Helps to create a new account
               login  -  Login with an existing account
               quit  -  Exit the program
               """);
    }

}
