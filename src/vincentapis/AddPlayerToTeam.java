package vincentapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class AddPlayerToTeam {

    // client layer handles user prompts and passes data to the server
    public static void Client_AddPlayerToTeam(Scanner scanner) {
        System.out.println("\n--- Join a Team ---");
        
        System.out.print("enter character name: ");
        String charName = scanner.nextLine().trim();
        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        System.out.print("enter team name to join: ");
        String teamName = scanner.nextLine().trim();
        if (teamName.isEmpty()) {
            System.out.println("client error: team name cannot be empty.");
            return;
        }

        System.out.print("enter starting rank (e.g., Recruit, Member): ");
        String rank = scanner.nextLine().trim();
        if (rank.isEmpty()) {
            System.out.println("client error: rank cannot be empty.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_AddPlayerToTeam(charName, teamName, rank));
    }

    // server layer matches the api signature and handles the single update transaction
    public static String Server_AddPlayerToTeam(String charName, String teamName, String rank) {
        Connection conn = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement checkRankStmt = null;
        PreparedStatement checkTeamStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // start transaction: locks the data so nobody else fills the team while we check
            conn.setAutoCommit(false);

            // validate character and check if they are already in a team
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

            // check if they are already in a team using wasNull()
            rs.getInt("TeamID");
            if (!rs.wasNull()) {
                conn.rollback();
                return "error: character '" + charName + "' is already in a team! leave the current team first.";
            }
            rs.close(); // close immediately before reuse

            // --- step 2: validate rank and get its surrogate key ---
            String rankSql = 
                "SELECT \n" +
                "    ID \n" +
                "FROM \n" +
                "    Rank \n" +
                "WHERE \n" +
                "    RankName = ?";
                
            checkRankStmt = conn.prepareStatement(rankSql);
            checkRankStmt.setString(1, rank);
            rs = checkRankStmt.executeQuery();

            int rankId;
            if (rs.next()) {
                rankId = rs.getInt("ID");
            } else {
                conn.rollback();
                return "error: the rank '" + rank + "' does not exist.";
            }
            rs.close();

            // validate team and check capacity
            // vertically formatted subquery to dynamically count team members
            String teamSql = 
                "SELECT \n" +
                "    ID, \n" +
                "    MaxSize, \n" +
                "    ( \n" +
                "        SELECT \n" +
                "            COUNT(ID) \n" +
                "        FROM \n" +
                "            Character \n" +
                "        WHERE \n" +
                "            TeamID = Team.ID \n" +
                "    ) AS CurrentMembers \n" +
                "FROM \n" +
                "    Team \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkTeamStmt = conn.prepareStatement(teamSql);
            checkTeamStmt.setString(1, teamName);
            rs = checkTeamStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "error: team '" + teamName + "' does not exist.";
            }

            // extract surrogate key and capacity locally
            int teamId = rs.getInt("ID");
            int maxSize = rs.getInt("MaxSize");
            int currentMembers = rs.getInt("CurrentMembers");

            if (currentMembers >= maxSize) {
                conn.rollback();
                return "error: team '" + teamName + "' is currently full (" + currentMembers + "/" + maxSize + ").";
            }
            rs.close();

            String updateSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    TeamID = ?, \n" +
                "    RankID = ? \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, teamId);
            updateStmt.setInt(2, rankId);
            updateStmt.setString(3, charName);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                return "error: failed to update character. please try again.";
            }

            // commit the transaction
            conn.commit();
            return "success! " + charName + " has joined team '" + teamName + "' as a " + rank + ".";

        } catch (SQLException e) {
            // rollback everything if any step fails
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            return "database error: " + e.getMessage();
        } finally {
            // restore auto-commit behavior for the connection pool
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
            
            // safely close all resources without severing the physical db connection
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCharStmt != null) checkCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkRankStmt != null) checkRankStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkTeamStmt != null) checkTeamStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}