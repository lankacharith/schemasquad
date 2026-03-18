package vincentapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class RemovePlayerFromTeam {

    // client layer handles the prompt and passes the single string down
    public static void Client_RemovePlayerFromTeam(Scanner scanner) {
        System.out.println("\n--- Leave or Kick from Team (Guild) ---");
        
        System.out.print("enter character name: ");
        String charName = scanner.nextLine().trim();
        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_RemovePlayerFromTeam(charName));
    }

    // server layer matches the new simplified signature
    public static String Server_RemovePlayerFromTeam(String charName) {
        Connection conn = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // start transaction: protects data integrity while we run our check-then-update logic
            conn.setAutoCommit(false);

            // validate character and check if they are currently in a team
            // vertical formatting applied
            String charSql = 
                "SELECT \n" +
                "    TeamID \n" +
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

            rs.getInt("TeamID");
            if (rs.wasNull()) {
                conn.rollback();
                return "error: character '" + charName + "' is not currently in a team.";
            }
            rs.close();

            // sets both teamid and rankid to null, cleanly severing all guild ties
            String updateSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    TeamID = NULL, \n" +
                "    RankID = NULL \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, charName);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                return "error: failed to remove character from team. please try again.";
            }

            // commit the transaction
            conn.commit();
            return "success! " + charName + " has been removed from their team.";

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
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}