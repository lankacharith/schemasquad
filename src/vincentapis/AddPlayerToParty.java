package vincentapis;
// vincent huang

import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

import dbconnection.DBConnection;

public class AddPlayerToParty {

    // precompiled regex to ensure we don't bother the database with badly formatted codes
    private static final Pattern JOIN_CODE_PATTERN = Pattern.compile("^[A-Za-z]{3}-\\d{3}$");

    // client layer handles user prompts and basic format validation
    public static void Client_AddPlayerToParty(Scanner scanner) {
        System.out.println("\n--- Join a Party ---");
        System.out.print("enter character name: ");
        String charName = scanner.nextLine().trim();

        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        System.out.print("enter party join code (e.g., xyz-123): ");
        String joinCode = scanner.nextLine().trim().toUpperCase();

        if (!JOIN_CODE_PATTERN.matcher(joinCode).matches()) {
            System.out.println("client error: invalid joincode format. must be 3 letters, a hyphen, and 3 numbers.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_AddPlayerToParty(charName, joinCode));
    }

    // server layer matches the api signature and handles the database transaction
    public static String Server_AddPlayerToParty(String charName, String joinCode) {
        Connection conn = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement checkPartyStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // start transaction: if multiple people try to join the same party at the 
            // exact same millisecond, this prevents the database from overfilling the party.
            conn.setAutoCommit(false);

            // validate character
            String charSql = 
                "SELECT \n" +
                "    PartyID \n" +
                "FROM \n" +
                "    Character \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkCharStmt = conn.prepareStatement(charSql);
            checkCharStmt.setString(1, charName);
            rs = checkCharStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "error: character '" + charName + "' does not exist.";
            }

            int currentPartyId = rs.getInt("PartyID");
            if (!rs.wasNull()) {
                conn.rollback();
                return "error: character '" + charName + "' is already in a party! leave the current party first.";
            }
            rs.close(); // close immediately before reuse

            // validate party and check capacity
            String partySql = 
                "SELECT \n" +
                "    ID, \n" +
                "    MaxSize, \n" +
                "    ( \n" +
                "        SELECT \n" +
                "            COUNT(ID) \n" +
                "        FROM \n" +
                "            Character \n" +
                "        WHERE \n" +
                "            PartyID = Party.ID \n" +
                "    ) AS CurrentMembers \n" +
                "FROM \n" +
                "    Party \n" +
                "WHERE \n" +
                "    JoinCode = ? \n" +
                "FOR UPDATE"; // locks the selected party row so nobody else can read or write it
                
            checkPartyStmt = conn.prepareStatement(partySql);
            checkPartyStmt.setString(1, joinCode);
            rs = checkPartyStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "error: party with join code '" + joinCode + "' does not exist.";
            }

            // extract the surrogate key locally so the client never sees it
            int partyId = rs.getInt("ID");
            int maxSize = rs.getInt("MaxSize");
            int currentMembers = rs.getInt("CurrentMembers");

            if (currentMembers >= maxSize) {
                conn.rollback();
                return "error: party '" + joinCode + "' is already full (" + currentMembers + "/" + maxSize + ").";
            }
            rs.close();

            // --- step 3: execute the crupdate single ---
            String updateSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    PartyID = ? \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, partyId);
            updateStmt.setString(2, charName);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                return "error: failed to update character. please try again.";
            }

            // commit the transaction
            conn.commit();
            return "success! " + charName + " has joined party " + joinCode + ".";

        } catch (SQLException e) {
            // rollback everything if any step fails
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            return "database error: " + e.getMessage();
        } finally {
            // restore auto-commit behavior for the connection pool
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
            
            // safely close all resources without severing the physical database connection
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCharStmt != null) checkCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkPartyStmt != null) checkPartyStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}