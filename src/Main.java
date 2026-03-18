// Main.java
// This is the driver program. It shows the menu and calls the right API.
// To add your API: add a println in printMenu() and a case in the switch below.

import charithapis.*;
import giseleapis.*;
import shreyasapis.*;
import java.util.Scanner;
import vincentapis.*;

public class Main {

    public static Scanner scanner = new Scanner(System.in);
    private static final int W = 42;
    private static final int CONTENT_W = W - 4;  // for "| " and " |"
    private static final String LINE  = "+" + "-".repeat(W - 2) + "+";

    public static void main(String[] args) {

        System.out.println("==============================");
        System.out.println("  Welcome to Schema Squad MMO ");
        System.out.println("==============================");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {

                // Players & leaderboards
                case "1": GetPlayerLeaderboard.Client_GetPlayerLeaderboard(scanner);   break;
                case "2": GetTopPlayersByLevel.Client_GetTopPlayersByLevel(scanner);   break;
                case "3": SearchPlayers.Client_SearchPlayers(scanner);                  break;

                // Events
                case "4": CreateEvent.Client_CreateEvent(scanner);                      break;
                case "5": GetActiveEvents.Client_GetActiveEvents(scanner);              break;
                case "6": GetPlayerEventCooldown.Client_GetPlayerEventCooldown(scanner); break;

                // Abilities
                case "7": UseAbility.Client_UseAbility(scanner);                         break;
                case "8": GetAbilityCooldowns.Client_GetAbilityCooldowns(scanner);      break;

                // Parties
                case "9": GetPartyMembers.Client_GetPartyMembers(scanner);              break;
                case "10": CreateParty.Client_CreateParty(scanner);                      break;
                case "11": GetAllParties.Client_GetAllParties(scanner);                  break;
                case "12": AddPlayerToParty.Client_AddPlayerToParty(scanner);           break;
                case "13": RemovePlayerFromParty.Client_RemovePlayerFromParty(scanner); break;
                case "14": GetPartySuggestions.Client_GetPartySuggestions(scanner);     break;

                // Teams
                case "15": CreateTeam.Client_CreateTeam(scanner);                       break;
                case "16": AddPlayerToTeam.Client_AddPlayerToTeam(scanner);           break;
                case "17": RemovePlayerFromTeam.Client_RemovePlayerFromTeam(scanner);  break;
                case "18": GetTeamDetails.Client_GetTeamDetails(scanner);               break;
                case "19": UpdateTeamRank.Client_UpdateTeamRank(scanner);               break;
                case "20": GetAllTeams.Client_GetAllTeams(scanner);                     break;

                // Inventory & trading
                case "21": GetPlayerInventory.Client_GetPlayerInventory(scanner);       break;
                case "22": GetRichestPlayers.Client_GetRichestPlayers(scanner);        break;
                case "23": GetTopPlayersByItem.Client_GetTopPlayersByItem(scanner);    break;
                case "24": AddItemToInventory.Client_AddItemToInventory(scanner);        break;
                case "25": RemoveItemFromInventory.Client_RemoveItemFromInventory(scanner); break;
                case "26": ExecuteTrade.Client_ExecuteTrade(scanner);                   break;
                case "27": GetTradeHistory.Client_GetTradeHistory(scanner);             break;

                // Quest Assignment & Tracking
                case "28": ListQuests.Client_ListQuests(scanner);                         break;
                case "29": AssignQuestToCharacter.Client_AssignQuestToCharacter(scanner); break;
                case "30": UpdateQuestProgress.Client_UpdateQuestProgress(scanner);       break;
                case "31": GetActiveQuests.Client_GetActiveQuests(scanner);               break;
                case "32": GetCompletedQuests.Client_GetCompletedQuests(scanner);         break;

                // Character stats
                case "33": GetCharacterStats.Client_GetCharacterStats(scanner);         break;
                case "34": UpdateCharacterStats.Client_UpdateCharacterStats(scanner);   break;

                case "0":
                    System.out.println("Goodbye!");
                    dbconnection.DBConnection.shutdown();
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("| " + pad("SCHEMA SQUAD MENU", CONTENT_W) + " |");
        System.out.println(LINE);

        section("LEADERBOARDS & RANKINGS",
            "  1. Get Player Leaderboard",
            "  2. Get Top Players By Level");
        gap();
        section("PLAYER SEARCH",
            "  3. Search Players");
        gap();
        section("EVENTS",
            "  4. Create Event",
            "  5. Get Active Events",
            "  6. Get Player Event Cooldown");
        gap();
        section("ABILITIES",
            "  7. Use Ability",
            "  8. Get Ability Cooldowns");
        gap();
        section("PARTIES",
            "  9. Get Party Members",
            " 10. Create Party",
            " 11. Get All Parties",
            " 12. Add Player To Party",
            " 13. Remove Player From Party",
            " 14. Get Party Suggestions");
        gap();
        section("TEAMS",
            " 15. Create Team",
            " 16. Add Player To Team",
            " 17. Remove Player From Team",
            " 18. Get Team Details",
            " 19. Update Team Rank",
            " 20. Get All Teams");
        gap();
        section("INVENTORY & TRADING",
            " 21. Get Player Inventory",
            " 22. Get Richest Players",
            " 23. Get Top Players By Item",
            " 24. Add Item To Inventory",
            " 25. Remove Item From Inventory",
            " 26. Execute Trade",
            " 27. Get Trade History");
        gap();
        section ("QUEST ASSIGNMENT & TRACKING",
            " 28. List Quests",
            " 29. Assign Quest To Character",
            " 30. Update Quest Progress",
            " 31. Get Active Quests",
            " 32. Get Completed Quests");
        gap();
        section (" CHARACTER STATS",
            " 33. Get Character Stats",
            " 34. Update Character Stats");

        section("", "  0. Quit");

        System.out.print("Enter choice: ");
    }

    private static void gap() {
        System.out.println();
    }

    private static void section(String header, String... lines) {
        if (header.isEmpty()) {
            System.out.println(LINE);
            for (String line : lines) {
                System.out.println("| " + pad(line, CONTENT_W) + " |");
            }
        } else {
            String headerLine = " " + header + " ";
            int dashLen = W - 3 - headerLine.length();  // 2 + headerLine + dashLen + 1 = W
            if (dashLen < 0) dashLen = 0;
            System.out.println("+-" + headerLine + "-".repeat(dashLen) + "+");
            for (String line : lines) {
                System.out.println("| " + pad(line, CONTENT_W) + " |");
            }
        }
        System.out.println(LINE);
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

}