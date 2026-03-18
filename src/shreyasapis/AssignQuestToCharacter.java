package shreyasapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class AssignQuestToCharacter {
    public static void Client_AssignQuestToCharacter(Scanner scanner) {
        System.out.println("--- Assign Quest To Character ---");
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim().toLowerCase();
        System.out.print("Enter quest name: ");
        String questName = scanner.nextLine().trim().toLowerCase();

        System.out.println(Server_AssignQuestToCharacter(charName, questName));
    }

    public static String Server_AssignQuestToCharacter(String charName, String questName) {
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

            // avoid assigning a quest twice to the same character
            PreparedStatement existsStmt = conn.prepareStatement(
                "SELECT 1 FROM CharacterQuestProgress WHERE CharacterID = ? AND QuestID = ?");
            existsStmt.setInt(1, charID);
            existsStmt.setInt(2, questID);
            ResultSet existsRs = existsStmt.executeQuery();
            if (existsRs.next()) {
                existsRs.close();
                existsStmt.close();
                return "Info: Quest '" + questName + "' is already assigned to character '" + charName + "'.";
            }
            existsRs.close();
            existsStmt.close();

            // ensure the sequence for the primary key is not behind existing rows
            // (prevents duplicate-key errors if the sequence was not advanced)
            PreparedStatement fixSeqStmt = conn.prepareStatement(
                "SELECT setval(pg_get_serial_sequence('characterquestprogress', 'id'), (SELECT COALESCE(MAX(id),0) FROM characterquestprogress))");
            fixSeqStmt.execute();
            fixSeqStmt.close();

            // insert into CharacterQuestProgress with 1 step completed
            PreparedStatement assignStmt = conn.prepareStatement(
                "INSERT INTO CharacterQuestProgress (CharacterID, QuestID, currentstep) VALUES (?, ?, 1)");
            assignStmt.setInt(1, charID);
            assignStmt.setInt(2, questID);
            assignStmt.executeUpdate();
            assignStmt.close();

            return "Success: Quest '" + questName + "' assigned to character '" + charName + "'.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database error occurred.";
        } finally {
            if (conn != null) {
                // Don't close the shared connection; project uses a single pooled/shared connection.
                DBConnection.closeConnection(conn);
            }
        }
    }
}
