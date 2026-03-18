package enesapis;
import java.sql.*;
import java.util.Scanner;
 
/**
 * Case 40: UpdateCharacterAppearance API (40 pts)
 * Updates a character's appearance (hair and skin) using foreign key IDs
 */
public class UpdateCharacterAppearance {
    
    /**
     * Main execution method for the UpdateCharacterAppearance API
     * Can be called from case 40 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Update Character Appearance ===");
        System.out.println("First, let's see what options are available...\n");
        
        // Automatically display available hair options
        System.out.println(GetHairOptions.getHairOptions(conn));
        
        // Automatically display available skin colors
        System.out.println(ListAllSkinColors.listAllSkinColors(conn));
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Now, let's update your character's appearance:");
        System.out.println("=".repeat(70) + "\n");
        
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        
        System.out.print("Enter new hair type (or press Enter to skip): ");
        String hairType = scanner.nextLine().trim();
        if (hairType.isEmpty()) hairType = null;
        
        System.out.print("Enter new hair color (or press Enter to skip): ");
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
            // Check if at least one field is being updated
            if (hairType == null && hairColor == null && skinColor == null) {
                return "Error: No changes specified. Please provide at least one field to update.";
            }
            
            // Build dynamic update query based on which fields are provided
            StringBuilder queryBuilder = new StringBuilder("UPDATE character SET ");
            boolean needsComma = false;
            
            // Get IDs for the provided values
            Integer hairTypeID = null;
            if (hairType != null) {
                hairTypeID = getIDByName(conn, "hairtype", "hairtype", hairType);
                if (hairTypeID == null) {
                    return "Error: Invalid hair type '" + hairType + "'";
                }
                queryBuilder.append("hairtypeid = ?");
                needsComma = true;
            }
            
            Integer hairColorID = null;
            if (hairColor != null) {
                hairColorID = getIDByName(conn, "haircolor", "color", hairColor);
                if (hairColorID == null) {
                    return "Error: Invalid hair color '" + hairColor + "'";
                }
                if (needsComma) queryBuilder.append(", ");
                queryBuilder.append("haircolorid = ?");
                needsComma = true;
            }
            
            Integer skinColorID = null;
            if (skinColor != null) {
                skinColorID = getIDByName(conn, "skincolor", "skincolor", skinColor);
                if (skinColorID == null) {
                    return "Error: Invalid skin color '" + skinColor + "'";
                }
                if (needsComma) queryBuilder.append(", ");
                queryBuilder.append("skincolorid = ?");
            }
            
            queryBuilder.append(" WHERE name = ?");
            
            stmt = conn.prepareStatement(queryBuilder.toString());
            
            // Set parameters based on what was provided
            int paramIndex = 1;
            if (hairTypeID != null) stmt.setInt(paramIndex++, hairTypeID);
            if (hairColorID != null) stmt.setInt(paramIndex++, hairColorID);
            if (skinColorID != null) stmt.setInt(paramIndex++, skinColorID);
            stmt.setString(paramIndex, charName);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                StringBuilder changes = new StringBuilder();
                if (hairType != null) changes.append("hair type to '" + hairType + "' ");
                if (hairColor != null) changes.append("hair color to '" + hairColor + "' ");
                if (skinColor != null) changes.append("skin color to '" + skinColor + "' ");
                
                return "Success: Character appearance updated for '" + charName + "' - changed " + changes.toString();
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
     * Helper method to get ID from a lookup table by name
     */
    private static Integer getIDByName(Connection conn, String table, String nameColumn, 
                                      String value) throws SQLException {
        String query = "SELECT id FROM " + table + " WHERE " + nameColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return null;
            }
        }
    }
}