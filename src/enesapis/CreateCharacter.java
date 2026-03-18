package enesapis;
import java.sql.*;
import java.util.Scanner;

/**
 * Case 39: CreateCharacter API (60 pts)
 * Creates a new character with customization options and links to player account
 */
public class CreateCharacter {
    
    /**
     * Main execution method for the CreateCharacter API
     * Can be called from case 39 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Create Character ===");
        System.out.println("First, let's see what options are available...\n");
        
        // Automatically display available races
        System.out.println(ListAllRaces.listAllRaces(conn));
        
        // Automatically display available classes
        System.out.println(ListAllClasses.listAllClasses(conn));
        
        // Automatically display available hair options
        System.out.println(GetHairOptions.getHairOptions(conn));
        
        // Automatically display available skin colors
        System.out.println(ListAllSkinColors.listAllSkinColors(conn));
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Now, let's create your character:");
        System.out.println("=".repeat(70) + "\n");
        
        System.out.print("Enter player username (your account): ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        
        System.out.print("Enter gender: ");
        String gender = scanner.nextLine().trim();
        
        System.out.print("Enter race (from list above): ");
        String race = scanner.nextLine().trim();
        
        System.out.print("Enter class (from list above): ");
        String charClass = scanner.nextLine().trim();
        
        System.out.print("Enter hair type (from list above): ");
        String hairType = scanner.nextLine().trim();
        
        System.out.print("Enter hair color (must match hair type above): ");
        String hairColor = scanner.nextLine().trim();
        
        System.out.print("Enter skin color (from list above): ");
        String skinColor = scanner.nextLine().trim();
        
        String result = createCharacter(conn, username, charName, gender, hairColor, 
                                       hairType, skinColor, race, charClass);
        System.out.println("\n" + result);
    }
    
    /**
     * Creates a new character with customization options
     * Automatically sets default values and creates inventory
     * @return Success message or error string
     */
    public static String createCharacter(Connection conn, String username, String charName, 
                                        String gender, String hairColor, String hairType, 
                                        String skinColor, String race, String charClass) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // Start transaction
            conn.setAutoCommit(false);
            
            // 1. Verify player exists (playerid references player.username directly)
            String checkPlayerQuery = "SELECT COUNT(*) FROM player WHERE username = ?";
            stmt = conn.prepareStatement(checkPlayerQuery);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            
            if (!rs.next() || rs.getInt(1) == 0) {
                conn.rollback();
                return "Error: Player username '" + username + "' not found";
            }
            rs.close();
            stmt.close();
            
            // 2. Check if character name already exists
            String checkQuery = "SELECT COUNT(*) FROM character WHERE name = ?";
            stmt = conn.prepareStatement(checkQuery);
            stmt.setString(1, charName);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return "Error: Character name already exists";
            }
            rs.close();
            stmt.close();
            
            // 3. Get foreign key IDs
            Integer raceID = getIDByName(conn, "race", "racename", race);
            if (raceID == null) {
                conn.rollback();
                return "Error: Invalid race '" + race + "'";
            }
            
            Integer classID = getIDByName(conn, "class", "classname", charClass);
            if (classID == null) {
                conn.rollback();
                return "Error: Invalid class '" + charClass + "'";
            }
            
            Integer hairTypeID = getIDByName(conn, "hairtype", "hairtype", hairType);
            if (hairTypeID == null) {
                conn.rollback();
                return "Error: Invalid hair type '" + hairType + "'";
            }
            
            Integer hairColorID = getIDByName(conn, "haircolor", "color", hairColor);
            if (hairColorID == null) {
                conn.rollback();
                return "Error: Invalid hair color '" + hairColor + "'";
            }
            
            Integer skinColorID = getIDByName(conn, "skincolor", "skincolor", skinColor);
            if (skinColorID == null) {
                conn.rollback();
                return "Error: Invalid skin color '" + skinColor + "'";
            }
            
            // 4. Create InventoryContainer with maxcapacity=32 and typeid=1 (Player)
            String invQuery = "INSERT INTO inventorycontainer (maxcapacity, typeid) VALUES (32, 1) RETURNING id";
            stmt = conn.prepareStatement(invQuery);
            rs = stmt.executeQuery();
            
            Integer invContainerID = null;
            if (rs.next()) {
                invContainerID = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            
            if (invContainerID == null) {
                conn.rollback();
                return "Error: Failed to create inventory container";
            }
            
            // 5. Insert Character with all default values (all lowercase column names)
            String insertQuery = "INSERT INTO character (" +
                "playerid, name, gender, haircolorid, hairtypeid, skincolorid, raceid, classid, " +
                "invcontainerid, currency, isonline, lastonline, " +
                "maxhp, maxmp, maxstam, currenthp, currentmp, currentstam, " +
                "posx, posy, posz, rotx, roty, locationid" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, false, CURRENT_TIMESTAMP, " +
                "100, 100, 100, 100, 100, 100, 0, 0, 0, 0, 0, 1)";
            
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, username);  // playerid is VARCHAR, use username directly
            stmt.setString(2, charName);
            stmt.setString(3, gender);
            stmt.setInt(4, hairColorID);
            stmt.setInt(5, hairTypeID);
            stmt.setInt(6, skinColorID);
            stmt.setInt(7, raceID);
            stmt.setInt(8, classID);
            stmt.setInt(9, invContainerID);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                conn.commit();
                return "Success: Character '" + charName + "' created for player '" + username + 
                       "' as a " + race + " " + charClass + 
                       " with inventory container ID " + invContainerID;
            } else {
                conn.rollback();
                return "Error: Failed to create character";
            }
            
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            return "Error: Database error - " + e.getMessage();
        } finally {
            try {
                conn.setAutoCommit(true);
                if (rs != null) rs.close();
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