package shreyasapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class UpdateCharacterStats {
    public static void Client_UpdateCharacterStats(Scanner scanner) {
        System.out.println("--- Update Character Stats ---");
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        System.out.print("Enter current HP: ");
        int currentHP = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter current MP: ");
        int currentMP = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter current Stamina: ");
        int currentStam = Integer.parseInt(scanner.nextLine().trim());

        System.out.println(Server_UpdateCharacterStats(charName, currentHP, currentMP, currentStam));
    }

    public static String Server_UpdateCharacterStats(String charName, int currentHP, int currentMP, int currentStam) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Character " +
                "SET CurrentHP = ?, CurrentMP = ?, CurrentStam = ? " +
                "WHERE Name ILIKE ?");
            stmt.setInt(1, currentHP);
            stmt.setInt(2, currentMP);
            stmt.setInt(3, currentStam);
            stmt.setString(4, charName);

            int rowsUpdated = stmt.executeUpdate();
            stmt.close();

            if (rowsUpdated > 0) {
                return "Success: Updated stats for character '" + charName + "'.";
            } else {
                return "Error: Character '" + charName + "' not found.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database error occurred while updating character stats.";
        } finally {
            if (conn != null) {
                // Don't close the shared connection; project uses a single pooled/shared connection.
                DBConnection.closeConnection(conn);
            }
        }
    }
}
