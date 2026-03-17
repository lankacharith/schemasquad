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

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT c.Name AS CharacterName, c.Level, c.Class " +
                "FROM Character c " +
                "JOIN Player p ON c.PlayerID = p.ID " +
                "WHERE p.Name ILIKE ?");
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            StringBuilder result = new StringBuilder("Characters for player '" + playerName + "':\n");
            boolean hasCharacters = false;
            while (rs.next()) {
                hasCharacters = true;
                String charName = rs.getString("CharacterName");
                int level = rs.getInt("Level");
                String charClass = rs.getString("Class");
                result.append("- ").append(charName).append(" (Level ").append(level).append(", ").append(charClass).append(")\n");
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
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
