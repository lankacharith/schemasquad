package vincentapis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import dbconnection.DBConnection;

public class CreateTeam {

    // client layer handles user prompts and basic validation
    public static void Client_CreateTeam(Scanner scanner) {
        System.out.println("\n--- Create New Team ---");
        
        System.out.print("enter your character name: ");
        String charName = scanner.nextLine().trim();

        if (charName.isEmpty()) {
            System.out.println("client error: character name cannot be empty.");
            return;
        }

        System.out.print("enter desired team name: ");
        String teamName = scanner.nextLine().trim();
        
        if (teamName.isEmpty()) {
            System.out.println("client error: team name cannot be empty.");
            return;
        }

        System.out.print("enter max team size: ");
        int maxSize;
        try {
            maxSize = Integer.parseInt(scanner.nextLine().trim());
            if (maxSize <= 0) {
                System.out.println("client error: max size must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("client error: invalid number format for max size.");
            return;
        }

        // execute server logic and print abstraction
        System.out.println(Server_CreateTeam(teamName, charName, maxSize));
    }

    // server layer executes the crupdate multiple transaction securely
    public static String Server_CreateTeam(String teamName, String charName, int maxSize) {
        Connection conn = null;
        PreparedStatement checkTeamStmt = null;
        PreparedStatement checkCharStmt = null;
        PreparedStatement getRankStmt = null;
        PreparedStatement insertTeamStmt = null;
        PreparedStatement updateCharStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // check if team name is already taken
            String checkTeamSql = 
                "SELECT \n" +
                "    ID \n" +
                "FROM \n" +
                "    Team \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkTeamStmt = conn.prepareStatement(checkTeamSql);
            checkTeamStmt.setString(1, teamName);
            rs = checkTeamStmt.executeQuery();

            if (rs.next()) {
                conn.rollback();
                return "error: the team name '" + teamName + "' is already taken. please choose another.";
            }
            rs.close(); // close before reuse

            // validate character and ensure they aren't already in a team
            String checkCharSql = 
                "SELECT \n" +
                "    TeamID \n" +
                "FROM \n" +
                "    Character \n" +
                "WHERE \n" +
                "    Name = ?";
                
            checkCharStmt = conn.prepareStatement(checkCharSql);
            checkCharStmt.setString(1, charName);
            rs = checkCharStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return "error: character '" + charName + "' does not exist.";
            }

            rs.getInt("TeamID");
            if (!rs.wasNull()) {
                conn.rollback();
                return "error: character '" + charName + "' is already in a team! leave your current team first.";
            }
            rs.close();

            String getRankSql = 
                "SELECT \n" +
                "    ID \n" +
                "FROM \n" +
                "    Rank \n" +
                "WHERE \n" +
                "    RankName = 'Owner'";
                
            getRankStmt = conn.prepareStatement(getRankSql);
            rs = getRankStmt.executeQuery();
            
            int ownerRankId;
            if (rs.next()) {
                ownerRankId = rs.getInt("ID");
            } else {
                conn.rollback();
                return "database error: the 'owner' rank does not exist in the rank table.";
            }
            rs.close();

            // insert the new team
            String insertTeamSql = 
                "INSERT INTO Team (\n" +
                "    Name,\n" +
                "    MaxSize\n" +
                ")\n" +
                "VALUES (\n" +
                "    ?,\n" +
                "    ?\n" +
                ")";
                
            // request the newly generated team id back from the database
            insertTeamStmt = conn.prepareStatement(insertTeamSql, Statement.RETURN_GENERATED_KEYS);
            insertTeamStmt.setString(1, teamName);
            insertTeamStmt.setInt(2, maxSize);
            insertTeamStmt.executeUpdate();

            // extract the new surrogate team id
            rs = insertTeamStmt.getGeneratedKeys();
            int newTeamId;
            if (rs.next()) {
                newTeamId = rs.getInt(1);
            } else {
                conn.rollback();
                return "database error: failed to retrieve new team id.";
            }

            // update character with new team id and rank id
            String updateCharSql = 
                "UPDATE \n" +
                "    Character \n" +
                "SET \n" +
                "    TeamID = ?, \n" +
                "    RankID = ? \n" +
                "WHERE \n" +
                "    Name = ?";
                
            updateCharStmt = conn.prepareStatement(updateCharSql);
            updateCharStmt.setInt(1, newTeamId);
            updateCharStmt.setInt(2, ownerRankId);
            updateCharStmt.setString(3, charName);
            
            updateCharStmt.executeUpdate();

            // commit the multi-table transaction
            conn.commit();
            return "success! team '" + teamName + "' created, and " + charName + " is now the owner.";

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            return "transaction error: " + e.getMessage();
        } finally {
            // restore auto-commit behavior
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
            
            // safely close all resources
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkTeamStmt != null) checkTeamStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (checkCharStmt != null) checkCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (getRankStmt != null) getRankStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (insertTeamStmt != null) insertTeamStmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (updateCharStmt != null) updateCharStmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}