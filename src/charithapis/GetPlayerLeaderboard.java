package charithapis;
// Charith Lanka

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetPlayerLeaderboard {

    private static final int MAX_ROWS = 100;
    private static final int MIN_ROWS = 1;

    // Asks user for rank type, how many rows to show, then loops through pages.
    public static void Client_GetPlayerLeaderboard(Scanner scanner) {
        System.out.println("\n--- Get Player Leaderboard ---");
        System.out.println("Rank types: gold, hp, mp.");
        System.out.print("Enter rank type: ");
        String rankType = scanner.nextLine().trim().toLowerCase();

        int rowsPerPage = 0;
        while (rowsPerPage < MIN_ROWS || rowsPerPage > MAX_ROWS) {
            System.out.print("How many rows to display? (" + MIN_ROWS + "-" + MAX_ROWS + "): ");
            String input = scanner.nextLine().trim();
            try {
                rowsPerPage = Integer.parseInt(input);
                if (rowsPerPage < MIN_ROWS || rowsPerPage > MAX_ROWS) {
                    System.out.println("Please enter a number between " + MIN_ROWS + " and " + MAX_ROWS + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        int page = 1;
        while (true) {
            System.out.println(Server_GetPlayerLeaderboard(rankType, page, rowsPerPage));
            System.out.print("Next page? (yes/no): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) break;
            page++;
        }
    }

    // Runs the SQL and returns results as a formatted string.
    public static String Server_GetPlayerLeaderboard(String rankType, int pageNum, int rowsPerPage) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // Pick which column to sort by -- never put user input directly in SQL.
            String sortCol;
            switch (rankType) {
                case "gold": sortCol = "c.Currency";  break;
                case "hp":   sortCol = "c.CurrentHP"; break;
                case "mp":   sortCol = "c.CurrentMP"; break;
                default: return "Error: pick gold, hp, or mp.";
            }

            int offset = (pageNum - 1) * rowsPerPage;

            String sql =
                "SELECT c.Name, p.Username, c.Currency, c.CurrentHP, c.CurrentMP " +
                "FROM Character c " +
                "JOIN Player p ON c.PlayerID = p.Username " +
                "WHERE p.BanStatus = FALSE " +
                "ORDER BY " + sortCol + " DESC " +
                "LIMIT ? OFFSET ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rowsPerPage);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();

            // build the output table
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Leaderboard: %s (Page %d) ===\n", rankType, pageNum));
            sb.append(String.format("%-5s %-20s %-20s %-10s\n", "Rank", "Character", "Player", rankType.toUpperCase()));
            sb.append("-".repeat(58) + "\n");

            int rank = offset + 1;
            boolean found = false;
            while (rs.next()) {
                found = true;
                int val;
                switch (rankType) {
                    case "gold": val = rs.getInt("Currency");  break;
                    case "hp":   val = rs.getInt("CurrentHP"); break;
                    default:     val = rs.getInt("CurrentMP"); break;
                }
                sb.append(String.format("%-5d %-20s %-20s %-10d\n",
                    rank++, rs.getString("Name"), rs.getString("Username"), val));
            }

            if (!found) sb.append("No more results.\n");
            stmt.close();
            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}