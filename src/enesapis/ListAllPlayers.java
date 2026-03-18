package enesapis;

import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 35: ListAllPlayers API (40 pts)
 * Lists all players with pagination support
 */
public class ListAllPlayers {
    
    /**
     * Main execution method for the ListAllPlayers API
     * Can be called from case 35 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== List All Players ===");
        System.out.print("Enter page number: ");
        
        int pageNum = 1;
        try {
            String input = scanner.nextLine().trim();
            pageNum = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid page number, using page 1");
        }
        
        String result = listAllPlayers(conn, pageNum);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all players with pagination support
     * @param conn Database connection
     * @param pageNum Page number (1-indexed)
     * @return Formatted table of player usernames and ban status
     */
    public static String listAllPlayers(Connection conn, int pageNum) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        final int PAGE_SIZE = 50; // Players per page
        int offset = (pageNum - 1) * PAGE_SIZE;
        
        try {
            String query = "SELECT username, banstatus FROM player ORDER BY username LIMIT ? OFFSET ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, PAGE_SIZE);
            stmt.setInt(2, offset);
            
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== All Players (Page ").append(pageNum).append(") ===\n");
            result.append(String.format("%-20s | %-10s\n", "Username", "Banned"));
            result.append("-".repeat(35)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                boolean isBanned = rs.getBoolean("banstatus");
                result.append(String.format("%-20s | %-10s\n", 
                    username, 
                    isBanned ? "Yes" : "No"));
                count++;
            }
            
            if (count == 0) {
                result.append("No players found on this page.\n");
            } else {
                result.append("\nShowing ").append(count).append(" players\n");
            }
            
            return result.toString();
            
        } catch (SQLException e) {
            return "Error: Database error - " + e.getMessage();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}