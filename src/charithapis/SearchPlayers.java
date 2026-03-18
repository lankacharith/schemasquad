package charithapis;
// Charith Lanka

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class SearchPlayers {

    // asks user what to search by then calls server
    public static void Client_SearchPlayers(Scanner scanner) {
        System.out.println("\n--- Search Players ---");
        System.out.println("1. By character name");
        System.out.println("2. By class");
        System.out.println("3. By minimum HP");
        System.out.print("Choose (1/2/3): ");
        String choice = scanner.nextLine().trim();

        String type, value;
        switch (choice) {
            case "1":
                type = "name";
                System.out.print("Enter character name: ");
                value = scanner.nextLine().trim();
                break;
            case "2":
                type = "class";
                System.out.print("Enter class (Warrior, Mage, Healer, Archer, Paladin): ");
                value = scanner.nextLine().trim();
                break;
            case "3":
                type = "minhp";
                System.out.print("Enter minimum HP: ");
                value = scanner.nextLine().trim();
                break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        System.out.println(Server_SearchPlayers(type, value));
    }

    // complex query with multiple joins and a subquery
    public static String Server_SearchPlayers(String searchType, String searchValue) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // subquery counts completed quests per character
            // LEFT JOIN on Location so chars without a location still show up
            String base =
                "SELECT c.Name, p.Username, cl.ClassName, r.RaceName, " +
                "       l.Name AS Location, c.MaxHP, c.Currency, " +
                "       (SELECT COUNT(*) FROM CharacterQuestProgress q " +
                "        WHERE q.CharacterID = c.ID AND q.IsComplete = TRUE) AS QuestsDone " +
                "FROM Character c " +
                "JOIN Player p        ON c.PlayerID   = p.Username " +
                "JOIN Class cl        ON c.ClassID    = cl.ID " +
                "JOIN Race r          ON c.RaceID     = r.ID " +
                "LEFT JOIN Location l ON c.LocationID = l.ID " +
                "WHERE p.BanStatus = FALSE ";

            PreparedStatement stmt;

            // add the right filter based on search type
            if (searchType.equals("name")) {
                stmt = conn.prepareStatement(base + "AND c.Name ILIKE ? ORDER BY c.Name");
                stmt.setString(1, "%" + searchValue + "%");
            } else if (searchType.equals("class")) {
                stmt = conn.prepareStatement(base + "AND cl.ClassName ILIKE ? ORDER BY c.MaxHP DESC");
                stmt.setString(1, searchValue);
            } else {
                // make sure HP is a number before running
                int minHp;
                try { minHp = Integer.parseInt(searchValue); }
                catch (NumberFormatException e) { return "Error: HP must be a number."; }
                stmt = conn.prepareStatement(base + "AND c.MaxHP >= ? ORDER BY c.MaxHP DESC");
                stmt.setInt(1, minHp);
            }

            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Search Results (%s: %s) ===\n", searchType, searchValue));
            sb.append(String.format("%-20s %-15s %-10s %-10s %-15s %-6s %-8s %-6s\n",
                "Character", "Player", "Class", "Race", "Location", "MaxHP", "Gold", "Quests"));
            sb.append("-".repeat(95) + "\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                sb.append(String.format("%-20s %-15s %-10s %-10s %-15s %-6d %-8d %-6d\n",
                    rs.getString("Name"),
                    rs.getString("Username"),
                    rs.getString("ClassName"),
                    rs.getString("RaceName"),
                    rs.getString("Location") != null ? rs.getString("Location") : "Unknown",
                    rs.getInt("MaxHP"),
                    rs.getInt("Currency"),
                    rs.getInt("QuestsDone")));
            }

            if (!found) sb.append("No characters found.\n");
            stmt.close();
            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}