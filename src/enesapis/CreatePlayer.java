package enesapis;

import java.sql.*;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Case 9: CreatePlayer API (40 pts)
 * Creates a new player account with authentication
 */
public class CreatePlayer {
    
    /**
     * Main execution method for the CreatePlayer API
     * Can be called from case 9 in switch statement
     */
    public static void execute(Scanner scanner, Connection conn) {
        System.out.println("\n=== Create Player Account ===");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        
        String result = createPlayer(conn, username, password, email);
        System.out.println("\n" + result);
    }
    
    /**
     * Creates a new player account
     * @param conn Database connection
     * @param username Unique player username
     * @param password Player password (will be hashed)
     * @param email Player email address
     * @return Success message or error string
     */
    public static String createPlayer(Connection conn, String username, String password, String email) {
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;
        
        try {
            // Check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM player WHERE username = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                return "Error: Username already exists";
            }
            
            // Hash the password
            String passwordHash = hashPassword(password);
            
            // Insert new player
            String insertQuery = "INSERT INTO player (username, passwordhash, email, banstatus) VALUES (?, ?, ?, false)";
            insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, passwordHash);
            insertStmt.setString(3, email);
            
            int rowsAffected = insertStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return "Success: Player account created for username: " + username;
            } else {
                return "Error: Failed to create player account";
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
     * Helper method to hash passwords using SHA-256
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}