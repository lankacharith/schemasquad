package vincentapis;
// vincent huang

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import dbconnection.DBConnection;

public class SearchTeams {

    // client layer handles the prompt and passes the string down
    public static void Client_SearchTeams(Scanner scanner) {
        System.out.println("\n--- Search Teams ---");
        System.out.print("enter team name to search: ");
        String teamName = scanner.nextLine().trim();

        if (teamName.isEmpty()) {
            System.out.println("client error: team name cannot be empty.");
            return;
        }

        // execute server logic and print the detail abstraction
        System.out.println(Server_SearchTeams(teamName));
    }

    // server layer matches the signature and handles the single data pull
    public static String Server_SearchTeams(String teamName) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // uses left join so the team still returns even if it has 0 members.
            // joins class and rank to hide surrogate keys.
            String sql = 
                "SELECT \n" +
                "    t.Name AS TeamName, \n" +
                "    t.MaxSize, \n" +
                "    c.Name AS CharacterName, \n" +
                "    cl.ClassName, \n" +
                "    r.RankName \n" +
                "FROM \n" +
                "    Team t \n" +
                "LEFT JOIN \n" +
                "    Character c \n" +
                "        ON t.ID = c.TeamID \n" +
                "LEFT JOIN \n" +
                "    Class cl \n" +
                "        ON c.ClassID = cl.ID \n" +
                "LEFT JOIN \n" +
                "    Rank r \n" +
                "        ON c.RankID = r.ID \n" +
                "WHERE \n" +
                "    t.Name = ? \n" +
                "ORDER BY \n" +
                "    r.ID DESC, \n" +
                "    c.Name ASC";
                
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, teamName);
            rs = stmt.executeQuery();

            // if rs.next() is false on the first try, the team doesn't exist at all
            if (!rs.next()) {
                return "error: team '" + teamName + "' does not exist.";
            }

            // extract the top-level team info from the first row
            String actualTeamName = rs.getString("TeamName");
            int maxSize = rs.getInt("MaxSize");
            
            // build the roster string and count members dynamically
            int memberCount = 0;
            StringBuilder rosterBuilder = new StringBuilder();

            // do-while loop because we already called rs.next() once for the validation check
            do {
                String charName = rs.getString("CharacterName");
                
                // if charName is not null, the left join found a character
                if (charName != null) {
                    memberCount++;
                    String className = rs.getString("ClassName");
                    String rankName = rs.getString("RankName");
                    
                    rosterBuilder.append(String.format("%-20s %-15s %-15s\n", charName, className, rankName));
                }
            } while (rs.next());

            StringBuilder finalOutput = new StringBuilder();
            finalOutput.append(String.format("\n=== Team Details: %s ===\n", actualTeamName));
            finalOutput.append(String.format("Capacity: %d / %d members\n", memberCount, maxSize));
            finalOutput.append("-".repeat(52)).append("\n");

            if (memberCount > 0) {
                finalOutput.append(String.format("%-20s %-15s %-15s\n", "Member Name", "Class", "Rank"));
                finalOutput.append("-".repeat(52)).append("\n");
                finalOutput.append(rosterBuilder.toString());
            } else {
                finalOutput.append("this team currently has no members.\n");
            }

            return finalOutput.toString();

        } catch (SQLException e) {
            return "database error: " + e.getMessage();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}