// Main.java
// This is the driver program. It shows the menu and calls the right API.
// To add your API: add a println in printMenu() and a case in the switch below.

import java.util.Scanner;
import apis.*;

public class Main {

    // shared scanner so we dont make a new one every time
    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("==============================");
        System.out.println("  Welcome to Schema Squad MMO ");
        System.out.println("==============================");

        // keep showing the menu until the user quits
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {

                // --- Charith's APIs ---
                // TODO: replace printNotImplemented(choice) with your function call
                // example: case "1": GetPlayerLeaderboard.Client_GetPlayerLeaderboard(scanner); break;
                case "1":  printNotImplemented(choice); break;
                case "2":  printNotImplemented(choice); break;
                case "3":  printNotImplemented(choice); break;
                case "4":  CreateEvent.Client_CreateEvent(scanner); break;
                case "5":  printNotImplemented(choice); break;
                case "6":  printNotImplemented(choice); break;
                case "7":  printNotImplemented(choice); break;
                case "8":  printNotImplemented(choice); break;

                // --- Enes's APIs ---
                // TODO: add cases 9-18 and call Enes's Client functions here

                // --- Vincent's APIs ---
                // TODO: add cases and call Vincent's Client functions here

                // --- Gisele's APIs ---
                // TODO: add cases and call Gisele's Client functions here

                // --- Shreyas's APIs ---
                // TODO: add cases and call Shreyas's Client functions here

                case "0":
                    System.out.println("Goodbye!");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        scanner.close();
    }

    // add your API name here so it shows up in the menu
    private static void printMenu() {
        System.out.println("\n--- API Menu ---");

        // Charith
        System.out.println("1.  Get Player Leaderboard");
        System.out.println("2.  Get Top Players By Level");
        System.out.println("3.  Search Players");
        System.out.println("4.  Create Event");
        System.out.println("5.  Get Active Events");
        System.out.println("6.  Get Player Event Cooldown");
        System.out.println("7.  Use Ability");
        System.out.println("8.  Get Ability Cooldowns");

        // TODO: Enes -- add your API names here

        // TODO: Vincent -- add your API names here

        // TODO: Gisele -- add your API names here

        // TODO: Shreyas -- add your API names here

        System.out.println("0.  Quit");
        System.out.print("Enter choice: ");
    }

    // placeholder until an API is implemented
    private static void printNotImplemented(String choice) {
        System.out.println("Option " + choice + " is not implemented yet.");
    }
}