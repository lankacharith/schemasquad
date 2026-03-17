package enesapis;
import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 18: UpdateCharacterAppearance API (40 pts)
 * Updates a character's appearance (hair and skin)
 */
public class UpdateCharacterAppearance {
    
    /**
     * Main execution method for the UpdateCharacterAppearance API
     * Can be called from case 18 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Update Character Appearance ===");
        System.out.println("First, let's see what hair options are available...\n");
        
        // Automatically display available hair options
        System.out.println(GetHairOptions.GetHairOptions(conn));
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Now, let's update your character's appearance:");
        System.out.println("=".repeat(70) + "\n");
        
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        
        System.out.print("Enter new hair type (or press Enter to skip): ");
        String hairType = scanner.nextLine().trim();
        if (hairType.isEmpty()) hairType = null;
        
        System.out.print("Enter new hair color (must match hair type, or press Enter to skip): ");
        String hairColor = scanner.nextLine().trim();
        if (hairColor.isEmpty()) hairColor = null;
        
        System.out.print("Enter new skin color (or press Enter to skip): ");
        String skinColor = scanner.nextLine().trim();
        if (skinColor.isEmpty()) skinColor = null;
        
        String result = updateCharacterAppearance(conn, charName, hairType, hairColor, skinColor);
        System.out.println("\n" + result);
    }
    
    /**
     * Updates a character's appearance (hair and skin)
     * @param conn Database connection
     * @param charName Character name to update
     * @param hairType New hair type (optional, pass null to keep current)
     * @param hairColor New hair color (optional, pass null to keep current)
     * @param skinColor New skin color (optional, pass null to keep current)
     * @return Success message or error string
     */
    public static String updateCharacterAppearance(Connection conn, String charName, 
                                                  String hairType, String hairColor, 
                                                  String skinColor) {
        PreparedStatement stmt = null;
        
        try {
            // Validate hair options if provided
            if (hairType != null && hairColor != null) {
                if (!validateHairOption(conn, hairType, hairColor)) {
                    return "Error: Invalid hair type or color combination";
                }
            }
            
            // Build dynamic update query based on which fields are provided
            StringBuilder queryBuilder = new StringBuilder("UPDATE Character SET ");
            boolean needsComma = false;
            
            if (hairType != null) {
                queryBuilder.append("HairType = ?");
                needsComma = true;
            }
            
            if (hairColor != null) {
                if (needsComma) queryBuilder.append(", ");
                queryBuilder.append("HairColor = ?");
                needsComma = true;
            }
            
            if (skinColor != null) {
                if (needsComma) queryBuilder.append(", ");
                queryBuilder.append("SkinColor = ?");
            }
            
            queryBuilder.append(" WHERE CharName = ?");
            
            stmt = conn.prepareStatement(queryBuilder.toString());
            
            // Set parameters based on what was provided
            int paramIndex = 1;
            if (hairType != null) stmt.setString(paramIndex++, hairType);
            if (hairColor != null) stmt.setString(paramIndex++, hairColor);
            if (skinColor != null) stmt.setString(paramIndex++, skinColor);
            stmt.setString(paramIndex, charName);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return "Success: Character appearance updated for '" + charName + "'";
            } else {
                return "Error: Character '" + charName + "' not found";
            }
            
        } catch (SQLException e) {
            return "Error: Database error - " + e.getMessage();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
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