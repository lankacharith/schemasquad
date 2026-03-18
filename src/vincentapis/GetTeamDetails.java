package vincentapis;
// vincent huang

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetTeamDetails {

    /**
     * client layer
     * handles the user prompt for the guild roster view.
     */
    public static void Client_GetTeamDetails(Scanner scanner) {
        System.out.println("\n--- View Team Details ---");
        System.out.print("enter team name: ");
        String teamName = scanner.nextLine().trim();

        if (teamName.isEmpty()) {
            System.out.println("client error: team name cannot be empty.");
            return;
        }

        // execute server logic and print the roster abstraction
        System.out.println(Server_GetTeamDetails(teamName));
    }

    /**
     * server layer
     * returns character names, levels, and ranks for a specific team.
     */
    public static String Server_GetTeamDetails(String teamName) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // joins character, class, and rank tables.
            // calculates a temporary level based on stat totals since level isn't a column yet.
            String sql = 
                "SELECT \n" +
                "    c.Name AS CharacterName, \n" +
                "    cl.ClassName, \n" +
                "    r.RankName, \n" +
                "    c.IsOnline, \n" +
                "    (c.MaxHP + c.MaxMP + c.MaxStam) / 100 AS CalculatedLevel \n" +
                "FROM \n" +
                "    Team t \n" +
                "JOIN \n" +
                "    Character c \n" +
                "        ON t.ID = c.TeamID \n" +
                "JOIN \n" +
                "    Class cl \n" +
                "        ON c.ClassID = cl.ID \n" +
                "JOIN \n" +
                "    Rank r \n" +
                "        ON c.RankID = r.ID \n" +
                "WHERE \n" +
                "    t.Name = ? \n" +
                "ORDER BY \n" +
                "    c.IsOnline DESC, \n" +
                "    r.ID DESC";
                
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, teamName);
            rs = stmt.executeQuery();

            // build the guild roster table
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Guild Roster: %s ===\n", teamName));
            sb.append(String.format("%-20s %-10s %-15s %-15s %-10s\n", 
                "Character", "Level", "Class/Role", "Rank", "Status"));
            sb.append("-".repeat(75)).append("\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                String name = rs.getString("CharacterName");
                int level = rs.getInt("CalculatedLevel");
                String role = rs.getString("ClassName");
                String rank = rs.getString("RankName");
                String status = rs.getBoolean("IsOnline") ? "[ONLINE]" : "offline";

                sb.append(String.format("%-20s %-10d %-15s %-15s %-10s\n", 
                    name, level, role, rank, status));
            }

            if (!found) {
                return "the team '" + teamName + "' either does not exist or has no members.";
            }

            return sb.toString();

        } catch (SQLException e) {
            return "database error: " + e.getMessage();
        } finally {
            // close resources safely
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}