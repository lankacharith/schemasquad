package vincentapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class RemovePlayerFromParty {

    // client layer handles user prompt and passes data to the server
    public static void Client_RemovePlayerFromParty(Scanner scanner) {
        System.out.println("\n--- Leave Party ---");
        System.out.print("enter character name: ");
        String charName = scanner.nextLine().trim();

        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_RemovePlayerFromParty(charName));
    }

    // server layer matches the api signature and handles the single update transaction
    public static String Server_RemovePlayerFromParty(String charName) {
        Connection conn = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // start transaction: protects data integrity while we run our check-then-update logic
            conn.setAutoCommit(false);

            String checkSql = 
                "SELECT \n" +
                "    PartyID \n" +
                "FROM \n" +
                "    Character \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkCharStmt = conn.prepareStatement(checkSql);
            checkCharStmt.setString(1, charName);
            rs = checkCharStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "error: character '" + charName + "' does not exist.";
            }

            rs.getInt("PartyID");
            if (rs.wasNull()) {
                conn.rollback();
                return "error: character '" + charName + "' is not currently in a party.";
            }
            rs.close(); // close immediately before moving to update phase

            // sets the partyid to null, cleanly removing them from the party
            String updateSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    PartyID = NULL \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, charName);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                return "error: failed to remove character from party. please try again.";
            }

            // commit the transaction
            conn.commit();
            return "success! " + charName + " has left their party.";

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
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}