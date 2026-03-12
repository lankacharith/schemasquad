package vincentapis;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

import dbconnection.DBConnection;

public class CreateParty {
    public static void Client_CreateParty(Scanner scanner) {
        System.out.println("\n--- Create New Party ---");
        System.out.print("Enter your Character Name: ");
        String charName = scanner.nextLine().trim();

        if (charName.isEmpty()) {
            System.out.println("Client Error: Character name cannot be empty.");
            return;
        }

        String result = Server_CreateParty(charName);
        System.out.println(result);
    }

    public static String Server_CreateParty(String charName) {
        Connection conn = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement checkCodeStmt = null;
        PreparedStatement insertPartyStmt = null;
        PreparedStatement updateCharStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // START TRANSACTION

            conn.setAutoCommit(false);

            // Validate Character
            String checkCharSql = 
                "SELECT \n" +
                "    PartyID \n" +
                "FROM \n" +
                "    Character \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkCharStmt = conn.prepareStatement(checkCharSql);
            checkCharStmt.setString(1, charName);
            rs = checkCharStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "Error: Character '" + charName + "' does not exist.";
            }

            int currentPartyId = rs.getInt("PartyID");
            if (!rs.wasNull()) {
                conn.rollback();
                return "Error: Character '" + charName + "' is already in a party! Leave the current party first.";
            }
            rs.close();

            // Generate Unique JoinCode
            String newJoinCode = "";
            boolean isUnique = false;
            
            String checkCodeSql = 
                "SELECT \n" +
                "    ID \n" +
                "FROM \n" +
                "    Party \n" +
                "WHERE \n" +
                "    JoinCode = ?";
                
            checkCodeStmt = conn.prepareStatement(checkCodeSql);

            // Loop until we generate a JoinCode that isn't in the database
            while (!isUnique) {
                newJoinCode = generateJoinCode();
                checkCodeStmt.setString(1, newJoinCode);
                rs = checkCodeStmt.executeQuery();
                if (!rs.next()) {
                    isUnique = true; // No match found, the code is unique!
                }
                rs.close();
            }

            // Insert New Party ---
            String insertPartySql = 
                "INSERT INTO Party (\n" +
                "    JoinCode,\n" +
                "    MaxSize\n" +
                ")\n" +
                "VALUES (\n" +
                "    ?,\n" +
                "    4\n" +
                ")";
                
            insertPartyStmt = conn.prepareStatement(insertPartySql, Statement.RETURN_GENERATED_KEYS);
            insertPartyStmt.setString(1, newJoinCode);
            insertPartyStmt.executeUpdate();

            // Get the newly created Party ID
            rs = insertPartyStmt.getGeneratedKeys();
            int newPartyId;
            if (rs.next()) {
                newPartyId = rs.getInt(1);
            } else {
                conn.rollback();
                return "Database Error: Failed to retrieve new Party ID.";
            }

            // --- STEP 4: Update Character ---
            String updateCharSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    PartyID = ? \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateCharStmt = conn.prepareStatement(updateCharSql);
            updateCharStmt.setInt(1, newPartyId);
            updateCharStmt.setString(2, charName);
            updateCharStmt.executeUpdate();

            // COMMIT TRANSACTION
            conn.commit();

            return "Success! Party created. Your unique Join Code is: " + newJoinCode;

        } catch (SQLException e) {
            // If ANYTHING fails, roll the database back to exactly how it was before the method ran.
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return "Transaction Error: " + e.getMessage();
        } finally {
            // Restore auto-commit behavior for the connection pool
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
            // Close all resources
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCharStmt != null) checkCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCodeStmt != null) checkCodeStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (insertPartyStmt != null) insertPartyStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (updateCharStmt != null) updateCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }


     // Helper Method: Generates a random JoinCode in the format XXX-999

    private static String generateJoinCode() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        
        // 3 Letters
        for (int i = 0; i < 3; i++) {
            sb.append(letters.charAt(rnd.nextInt(letters.length())));
        }
        sb.append("-");
        // 3 Numbers
        for (int i = 0; i < 3; i++) {
            sb.append(rnd.nextInt(10));
        }
        
        return sb.toString();
    }
}