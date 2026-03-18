package enesapis;
import java.sql.*;
import java.util.Scanner;
 
/**
 * Helper API: ListAllSkinColors
 * Lists all available skin color options for character customization
 */
public class ListAllSkinColors {
    
    /**
     * Main execution method (can be called standalone if needed)
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== List All Skin Colors ===");
        String result = listAllSkinColors(conn);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all available skin color options
     * @param conn Database connection
     * @return Formatted list of all skin colors
     */
    public static String listAllSkinColors(Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String query = "SELECT skincolor FROM skincolor ORDER BY skincolor";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== Available Skin Colors ===\n");
            result.append("-".repeat(30)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                String skinColor = rs.getString("skincolor");
                result.append(String.format("  - %s\n", skinColor));
                count++;
            }
            
            if (count == 0) {
                result.append("No skin colors found.\n");
            } else {
                result.append("\nTotal: ").append(count).append(" skin colors\n");
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