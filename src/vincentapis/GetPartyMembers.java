package vincentapis;

import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

import dbconnection.DBConnection;

public class GetPartyMembers {

    // Precompiled regex pattern for exactly 3 letters, a hyphen, and 3 digits (e.g., ABC-123)
    private static final Pattern JOIN_CODE_PATTERN = Pattern.compile("^[A-Za-z]{3}-\\d{3}$");

    // Handles user prompts, reads input, and requests the data from the server.
    public static void Client_GetPartyMembers(Scanner scanner) {
        System.out.println("\n--- Get Party Members ---");
        System.out.print("Enter Party Join Code (Format: XXX-999): ");
        String joinCode = scanner.nextLine().trim().toUpperCase();

        // Client-side validation to prevent bad data from ever hitting the server
        if (!JOIN_CODE_PATTERN.matcher(joinCode).matches()) {
            System.out.println("Client Error: Invalid JoinCode format. Must be 3 letters, a hyphen, and 3 numbers (e.g., XYZ-123).");
            return;
        }

        // Call server and print formatted abstraction, preventing state leakage
        String result = Server_GetPartyMembers(joinCode);
        System.out.println(result);
    }

    /**
     * SERVER LAYER
     * Matches API signature: table GetPartyMembers(String JoinCode)
     * Executes the SQL transaction and returns formatted data, never raw ResultSets or Surrogate Keys.
     */
    public static String Server_GetPartyMembers(String joinCode) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // Joins the Class and Race tables.
            String sql = 
                "SELECT \n" +
                "    c.Name AS CharacterName, \n" +
                "    cl.ClassName, \n" +
                "    r.RaceName, \n" +
                "    c.CurrentHP, \n" +
                "    c.MaxHP \n" +
                "FROM \n" +
                "    Character c \n" +
                "JOIN \n" +
                "    Party p \n" +
                "        ON c.PartyID = p.ID \n" +
                "JOIN \n" +
                "    Class cl \n" +
                "        ON c.ClassID = cl.ID \n" +
                "JOIN \n" +
                "    Race r \n" +
                "        ON c.RaceID = r.ID \n" +
                "WHERE \n" +
                "    p.JoinCode = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, joinCode);
            
            rs = stmt.executeQuery();

            // Build the presentation layer string (Abstracting the database away from the client)
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Party Roster: %s ===\n", joinCode));
            sb.append(String.format("%-20s %-15s %-15s %-15s\n", "Character Name", "Race", "Class", "Health"));
            sb.append("-".repeat(68)).append("\n");

            boolean hasMembers = false;

            while (rs.next()) {
                hasMembers = true;
                String charName = rs.getString("CharacterName");
                String race = rs.getString("RaceName");
                String className = rs.getString("ClassName");
                String health = rs.getInt("CurrentHP") + " / " + rs.getInt("MaxHP");

                sb.append(String.format("%-20s %-15s %-15s %-15s\n", charName, race, className, health));
            }

            if (!hasMembers) {
                return "Error: No party found with Join Code '" + joinCode + "', or the party is currently empty.";
            }

            return sb.toString();

        } catch (SQLException e) {
            return "Database Error: " + e.getMessage();
        } finally {
            // Safely close resources to prevent memory leaks
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            // Do not close physical connection if utilizing a connection pool, just return to pool.
            DBConnection.closeConnection(conn);
        }
    }
}