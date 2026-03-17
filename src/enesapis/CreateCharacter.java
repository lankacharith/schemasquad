package enesapis;
import java.sql.*;
import java.util.Scanner;

/**
 * Case 10: CreateCharacter API (40 pts)
 * Creates a new character with customization options
 */
public class CreateCharacter {
    
    /**
     * Main execution method for the CreateCharacter API
     * Can be called from case 10 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Create Character ===");
        
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        
        System.out.print("Enter hair color: ");
        String hairColor = scanner.nextLine().trim();
        
        System.out.print("Enter hair type: ");
        String hairType = scanner.nextLine().trim();
        
        System.out.print("Enter skin color: ");
        String skinColor = scanner.nextLine().trim();
        
        System.out.print("Enter race: ");
        String race = scanner.nextLine().trim();
        
        System.out.print("Enter class: ");
        String charClass = scanner.nextLine().trim();
        
        String result = createCharacter(conn, charName, hairColor, hairType, skinColor, race, charClass);
        System.out.println("\n" + result);
    }
    
    /**
     * Creates a new character with customization options
     * @return Success message or error string
     */
    public static String createCharacter(Connection conn, String charName, String hairColor, 
                                        String hairType, String skinColor, 
                                        String race, String charClass) {
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;
        
        try {
            // Check if character name already exists
            String checkQuery = "SELECT COUNT(*) FROM Character WHERE CharName = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, charName);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                return "Error: CharName already exists";
            }
            
            // Validate race exists
            if (!validateExists(conn, "Race", "RaceName", race)) {
                return "Error: Invalid race '" + race + "'";
            }
            
            // Validate class exists
            if (!validateExists(conn, "Class", "ClassName", charClass)) {
                return "Error: Invalid class '" + charClass + "'";
            }
            
            // Validate hair options
            if (!validateHairOption(conn, hairType, hairColor)) {
                return "Error: Invalid hair type or color combination";
            }
            
            // Insert new character with default stats
            String insertQuery = "INSERT INTO Character (CharName, HairColor, HairType, " +
                               "SkinColor, Race, Class, Level, HP, MP, Stamina) " +
                               "VALUES (?, ?, ?, ?, ?, ?, 1, 100, 50, 100)";
            
            insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, charName);
            insertStmt.setString(2, hairColor);
            insertStmt.setString(3, hairType);
            insertStmt.setString(4, skinColor);
            insertStmt.setString(5, race);
            insertStmt.setString(6, charClass);
            
            int rowsAffected = insertStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return "Success: Character '" + charName + "' created as a Level 1 " + 
                       race + " " + charClass;
            } else {
                return "Error: Failed to create character";
            }
            
        } catch (SQLException e) {
            return "Error: Database error - " + e.getMessage();
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to validate if a value exists in a table
     */
    private static boolean validateExists(Connection conn, String table, 
                                         String column, String value) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Helper method to validate hair options exist in HairOptions table
     */
    private static boolean validateHairOption(Connection conn, String hairType, 
                                             String hairColor) throws SQLException {
        String query = "SELECT COUNT(*) FROM HairOptions WHERE HairType = ? AND HairColor = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, hairType);
            stmt.setString(2, hairColor);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
