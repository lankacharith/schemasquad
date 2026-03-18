package enesapis;
import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 44: ListAllLocations API (40 pts)
 * Lists all game locations/zones/biomes
 */
public class ListAllLocations {
    
    /**
     * Main execution method for the ListAllLocations API
     * Can be called from case 44 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== List All Locations ===");
        String result = listAllLocations(conn);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all game locations/zones/biomes
     * @param conn Database connection
     * @return Formatted table of all locations
     */
    public static String listAllLocations(Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String query = "SELECT id, name FROM location ORDER BY name";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== All Game Locations ===\n");
            result.append(String.format("%-5s | %-25s\n", "ID", "Location Name"));
            result.append("-".repeat(35)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                int locationID = rs.getInt("id");
                String locationName = rs.getString("name");
                result.append(String.format("%-5d | %-25s\n", locationID, locationName));
                count++;
            }
            
            if (count == 0) {
                result.append("No locations found.\n");
            } else {
                result.append("\nTotal: ").append(count).append(" locations\n");
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