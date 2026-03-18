package enesapis;
import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 43: ListAllClasses API (40 pts)
 * Lists all available character classes
 */
public class ListAllClasses {
    
    /**
     * Main execution method for the ListAllClasses API
     * Can be called from case 43 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== List All Classes ===");
        String result = listAllClasses(conn);
        System.out.println("\n" + result);
    }
    
    /**
     * Lists all available character classes
     * @param conn Database connection
     * @return Formatted table of all character classes
     */
    public static String listAllClasses(Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
                try {
            String query = "SELECT classname FROM class ORDER BY classname";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            
            StringBuilder result = new StringBuilder();
            result.append("=== All Character Classes ===\n");
            result.append(String.format("%-20s\n", "Class Name"));
            result.append("-".repeat(20)).append("\n");
            
            int count = 0;
            while (rs.next()) {
                String className = rs.getString("classname");
                result.append(String.format("%-20s\n", className));
                count++;
            }
            
            if (count == 0) {
                result.append("No classes found.\n");
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