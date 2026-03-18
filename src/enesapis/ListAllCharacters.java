package enesapis;

import java.sql.*;
import java.util.Scanner;
import dbconnection.DBConnection;

public class ListAllCharacters {
    public static void Client_ListAllCharacters(Scanner scanner) {
        System.out.println("--- List All Characters ---");
        System.out.print("Enter player username: ");
        String playerName = scanner.nextLine().trim().toLowerCase();

        System.out.println(Server_ListAllCharacters(playerName));
    }

    public static String Server_ListAllCharacters(String playerName) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // character table does not store "Level" or "Class" text, so we compute level and join to class name.
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT " +
                "  c.name AS CharacterName, " +
                "  ((c.maxhp + c.maxmp + c.maxstam) / 100) AS CalculatedLevel, " +
                "  cl.classname AS ClassName " +
                "FROM character c " +
                "JOIN player p ON c.playerid = p.username " +
                "JOIN class cl ON c.classid = cl.id " +
                "WHERE p.username ILIKE ? " +
                "ORDER BY c.name");
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            StringBuilder result = new StringBuilder("Characters for player '" + playerName + "':\n");
            boolean hasCharacters = false;
            while (rs.next()) {
                hasCharacters = true;
                String charName = rs.getString("CharacterName");
                int level = rs.getInt("CalculatedLevel");
                String charClass = rs.getString("ClassName");
                result.append("- ").append(charName)
                    .append(" (Level ").append(level)
                    .append(", ").append(charClass).append(")\n");
            }
            stmt.close();

            if (!hasCharacters) {
                return "No characters found for player '" + playerName + "'.";
            }
            return result.toString();

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Could not retrieve characters.";
        } finally {
            if (conn != null) {
                // Don't close the shared/pool connection here.
                DBConnection.closeConnection(conn);
            }
        }
    }
}
