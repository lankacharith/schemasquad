package vincentapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class UpdateTeamRank {

    // client layer handles the prompt and passes the strings down
    public static void Client_UpdateTeamRank(Scanner scanner) {
        System.out.println("\n--- Update Team Rank ---");
        
        System.out.print("enter character name: ");
        String charName = scanner.nextLine().trim();
        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        System.out.print("enter new rank (e.g., Recruit, Member, Veteran, Co-Owner): ");
        String newRank = scanner.nextLine().trim();
        if (newRank.isEmpty()) {
            System.out.println("client error: rank cannot be empty.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_UpdateTeamRank(charName, newRank));
    }

    // server layer matches the signature and handles the single update transaction
    public static String Server_UpdateTeamRank(String charName, String newRank) {
        Connection conn = null;
        PreparedStatement checkRankStmt = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // start transaction: protects data integrity while we run our check-then-update logic
            conn.setAutoCommit(false);

            // validate rank and get its surrogate key
            String rankSql = 
                "SELECT \n" +
                "    ID \n" +
                "FROM \n" +
                "    Rank \n" +
                "WHERE \n" +
                "    RankName = ?";
                
            checkRankStmt = conn.prepareStatement(rankSql);
            checkRankStmt.setString(1, newRank);
            rs = checkRankStmt.executeQuery();

            int rankId;
            if (rs.next()) {
                rankId = rs.getInt("ID");
            } else {
                conn.rollback();
                return "error: the rank '" + newRank + "' does not exist in the system.";
            }
            rs.close(); // close before reuse

            // validate character and ensure they are currently in a team
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
                return "error: character '" + charName + "' is not currently in a team and cannot have a rank assigned.";
            }
            rs.close();

            // updates the rankid on the character table
            String updateSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    RankID = ? \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, rankId);
            updateStmt.setString(2, charName);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                return "error: failed to update rank. please try again.";
            }

            // commit the transaction
            conn.commit();
            return "success! " + charName + " has been updated to the rank of " + newRank + ".";

        } catch (SQLException e) {
            // rollback everything if any step fails
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            return "database error: " + e.getMessage();
        } finally {
            // restore auto-commit behavior for the connection pool
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
            
            // safely close all resources without severing the physical database connection
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkRankStmt != null) checkRankStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCharStmt != null) checkCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}