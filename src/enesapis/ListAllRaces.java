package enesapis;

import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 41: ListAllRaces API (40 pts)
 * Lists all available character races/species
 */
public class ListAllRaces {
    
    /**
     * Main execution method for the ListAllRaces API
     * Can be called from case 41 in switch statement
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
            String query = "SELECT racename FROM race ORDER BY racename";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== All Character Races ===\n");
            result.append(String.format("%-20s\n", "Race Name"));
            result.append("-".repeat(20)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                String raceName = rs.getString("racename");
                result.append(String.format("%-20s\n", raceName));
                count++;
            }
            
            if (count == 0) {
                result.append("No races found.\n");
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