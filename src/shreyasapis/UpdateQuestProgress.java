package shreyasapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class UpdateQuestProgress {
    public static void Client_UpdateQuestProgress(Scanner scanner) {
        System.out.println("--- Update Quest Progress ---");
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim().toLowerCase();
        System.out.print("Enter quest name: ");
        String questName = scanner.nextLine().trim().toLowerCase();

        System.out.println(Server_UpdateQuestProgress(charName, questName));
    }

    public static String Server_UpdateQuestProgress(String charName, String questName) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // get character ID
            PreparedStatement charStmt = conn.prepareStatement(
                "SELECT ID FROM Character WHERE Name ILIKE ?");
            charStmt.setString(1, charName);
            ResultSet charRs = charStmt.executeQuery();
            if (!charRs.next()) return "Error: Character '" + charName + "' not found.";
            int charID = charRs.getInt("ID");
            charStmt.close();

            // get quest ID
            PreparedStatement questStmt = conn.prepareStatement(
                "SELECT ID FROM Quest WHERE Name ILIKE ?");
            questStmt.setString(1, questName);
            ResultSet questRs = questStmt.executeQuery();
            if (!questRs.next()) return "Error: Quest '" + questName + "' not found.";
            int questID = questRs.getInt("ID");
            questStmt.close();

            // check if the character has that quest assigned
            PreparedStatement progressCheck = conn.prepareStatement(
                "SELECT CurrentStep FROM CharacterQuestProgress WHERE CharacterID = ? AND QuestID = ?");
            progressCheck.setInt(1, charID);
            progressCheck.setInt(2, questID);
            ResultSet progressRs = progressCheck.executeQuery();
            if (!progressRs.next()) return "Error: Quest '" + questName + "' is not assigned to character '" + charName + "'.";

            // mark the quest as completed (iscomplete = True)
            PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE CharacterQuestProgress SET iscomplete = True WHERE CharacterID = ? AND QuestID = ?");
            updateStmt.setInt(1, charID);
            updateStmt.setInt(2, questID);
            int rowsUpdated = updateStmt.executeUpdate();
            updateStmt.close();

            if (rowsUpdated > 0) {
                return "Success: Quest '" + questName + "' marked as completed for character '" + charName + "'.";
            } else {
                return "Error: Failed to mark quest as completed.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database error occurred.";
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
