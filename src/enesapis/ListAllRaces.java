package enesapis;

import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 14: ListAllRaces API (40 pts)
 * Lists all available character races/species
 */
public class ListAllRaces {
    
    /**
     * Main execution method for the ListAllRaces API
     * Can be called from case 14 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== List All Races ===");
        String result = listAllRaces(conn);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all available character races/species
     * @param conn Database connection
     * @return Formatted table of all races
     */
    public static String listAllRaces(Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String query = "SELECT RaceName, Description FROM Race ORDER BY RaceName";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== All Character Races ===\n");
            result.append(String.format("%-15s | %-50s\n", "Race Name", "Description"));
            result.append("-".repeat(70)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                String raceName = rs.getString("RaceName");
                String description = rs.getString("Description");
                result.append(String.format("%-15s | %-50s\n", raceName, description));
                count++;
            }
            
            if (count == 0) {
                result.append("No races found.\n");
            } else {
                result.append("\nTotal: ").append(count).append(" races\n");
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