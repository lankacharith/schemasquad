package enesapis;
import java.sql.*;
import java.util.Scanner;

/**
 * Case 15: GetHairOptions API (40 pts)
 * Lists all available hair customization options
 */
public class GetHairOptions {
    
    /**
     * Main execution method for the GetHairOptions API
     * Can be called from case 15 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Get Hair Options ===");
        String result = getHairOptions(conn);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all available hair customization options (types and colors)
     * @param conn Database connection
     * @return Formatted table of all hair options
     */
    public static String getHairOptions(Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("=== Hair Customization Options ===\n\n");
            
            // Get hair types
            result.append("Available Hair Types:\n");
            result.append("-".repeat(30)).append("\n");
            String typeQuery = "SELECT hairtype FROM hairtype ORDER BY hairtype";
            stmt = conn.prepareStatement(typeQuery);
            rs = stmt.executeQuery();
            
            int typeCount = 0;
            while (rs.next()) {
                String hairType = rs.getString("hairtype");
                result.append(String.format("  - %s\n", hairType));
                typeCount++;
            }
            rs.close();
            stmt.close();
            
            result.append("\n");
            
            // Get hair colors
            result.append("Available Hair Colors:\n");
            result.append("-".repeat(30)).append("\n");
            String colorQuery = "SELECT color FROM haircolor ORDER BY color";
            stmt = conn.prepareStatement(colorQuery);
            rs = stmt.executeQuery();
            
            int colorCount = 0;
            while (rs.next()) {
                String hairColor = rs.getString("color");
                result.append(String.format("  - %s\n", hairColor));
                colorCount++;
            }
            
            result.append("\nNote: You can combine any hair type with any hair color.\n");
            result.append("Total: ").append(typeCount).append(" hair types × ");
            result.append(colorCount).append(" hair colors = ");
            result.append(typeCount * colorCount).append(" possible combinations\n");
            
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
