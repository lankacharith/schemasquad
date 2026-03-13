// Main.java
// This is the driver program. It shows the menu and calls the right API.
// To add your API: add a println in printMenu() and a case in the switch below.

import java.util.Scanner;
import dbconnection.DBConnection;
import charithapis.*;
import vincentapis.*;

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
                case "1": GetPlayerLeaderboard.Client_GetPlayerLeaderboard(scanner);   break;
                case "2": GetTopPlayersByLevel.Client_GetTopPlayersByLevel(scanner);   break;
                case "3": SearchPlayers.Client_SearchPlayers(scanner);                 break;
                case "4": CreateEvent.Client_CreateEvent(scanner);                     break;
                case "5": GetActiveEvents.Client_GetActiveEvents(scanner);             break;
                case "6": GetPlayerEventCooldown.Client_GetPlayerEventCooldown(scanner); break;
                case "7": UseAbility.Client_UseAbility(scanner);                       break;
                case "8": GetAbilityCooldowns.Client_GetAbilityCooldowns(scanner);     break;

                // --- Enes's APIs ---
                // TODO: add cases 9-18 and call Enes's Client functions here

                // --- Vincent's APIs ---
                case "9": GetPartyMembers.Client_GetPartyMembers(scanner);              break;
                case "10": CreateParty.Client_CreateParty(scanner);                     break;
                case "11": GetAllParties.Client_GetAllParties(scanner);                 break;
                case "12": AddPlayerToParty.Client_AddPlayerToParty(scanner);           break;
                case "13": RemovePlayerFromParty.Client_RemovePlayerFromParty(scanner); break;
                case "14": GetPartySuggestions.Client_GetPartySuggestions(scanner);     break;
                case "15": CreateTeam.Client_CreateTeam(scanner);                       break;
                case "16": AddPlayerToTeam.Client_AddPlayerToTeam(scanner);             break;
                case "17": RemovePlayerFromTeam.Client_RemovePlayerFromTeam(scanner);   break;
                case "18": GetTeamDetails.Client_GetTeamDetails(scanner);               break;
                case "19": UpdateTeamRank.Client_UpdateTeamRank(scanner);               break;
                case "20": GetAllTeams.Client_GetAllTeams(scanner);                     break;
                // --- Gisele's APIs ---
                // TODO: add cases and call Gisele's Client functions here

                // --- Shreyas's APIs ---
                // TODO: add cases and call Shreyas's Client functions here

                case "0":
                    System.out.println("Goodbye!");
                    dbconnection.DBConnection.shutdown(); // close the connection pool when quitting
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
        System.out.println("9.  Get Party Members");
        System.out.println("10. Create Party");
        System.out.println("11. Get All Parties");
        System.out.println("12. Add Player To Party");
        System.out.println("13. Remove Player From Party");
        System.out.println("14. Get Party Suggestions");
        System.out.println("15. Create Team");
        System.out.println("16. Add Player To Team");
        System.out.println("17. Remove Player From Team");
        System.out.println("18. Get Team Details");
        System.out.println("19. Update Team Rank");
        System.out.println("20. Get All Teams");

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