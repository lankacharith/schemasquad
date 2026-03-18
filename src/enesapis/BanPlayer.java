package enesapis;
import java.util.Scanner;
import java.sql.*;

/**
 * Case 11: BanPlayer API (40 pts)
 * Bans a player by setting their IsBanned flag to true
 */
public class BanPlayer {
    
    /**
     * Main execution method for the BanPlayer API
     * Can be called from case 11 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Ban Player ===");
        System.out.print("Enter username to ban: ");
        String username = scanner.nextLine().trim();
        
        String result = banPlayer(conn, username);
        System.out.println("\n" + result);
    }
    
    /**
     * Bans a player by setting their IsBanned flag to true
     * @param conn Database connection
     * @param username Username of player to ban
     * @return Success message
     */
    public static String banPlayer(Connection conn, String username) {
        PreparedStatement stmt = null;
        
        try {
            String query = "UPDATE player SET banstatus = true WHERE username = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return "Success: Player " + username + " has been banned";
            } else {
                return "Success: No changes made (player may not exist or already banned)";
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
}
