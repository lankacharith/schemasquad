package charithapis;
// Charith Lanka

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetPlayerEventCooldown {

    // cooldown is 24 hours after the event ends
    private static final int COOLDOWN_HOURS = 24;

    // asks for character name and event name
    public static void Client_GetPlayerEventCooldown(Scanner scanner) {
        System.out.println("\n--- Get Player Event Cooldown ---");
        System.out.print("Character name: ");
        String charName = scanner.nextLine().trim();
        System.out.print("Event name: ");
        String eventName = scanner.nextLine().trim();
        System.out.println(Server_GetPlayerEventCooldown(charName, eventName));
    }

    // checks when the character last did the event and calculates time left
    public static String Server_GetPlayerEventCooldown(String charName, String eventName) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // make sure the character exists
            PreparedStatement charCheck = conn.prepareStatement(
                "SELECT ID FROM Character WHERE Name ILIKE ?");
            charCheck.setString(1, charName);
            ResultSet charRs = charCheck.executeQuery();
            if (!charRs.next()) return "Error: Character '" + charName + "' not found.";
            int charID = charRs.getInt("ID");
            charCheck.close();

            // make sure the event exists
            PreparedStatement eventCheck = conn.prepareStatement(
                "SELECT ID FROM Events WHERE Name ILIKE ?");
            eventCheck.setString(1, eventName);
            ResultSet eventRs = eventCheck.executeQuery();
            if (!eventRs.next()) return "Error: Event '" + eventName + "' not found.";
            int eventID = eventRs.getInt("ID");
            eventCheck.close();

            // join CharacterEventHistory with Events to get the end time
            String sql =
                "SELECT e.EndTime " +
                "FROM CharacterEventHistory ceh " +
                "JOIN Events e ON ceh.EventID = e.ID " +
                "WHERE ceh.CharacterID = ? AND e.ID = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, charID);
            stmt.setInt(2, eventID);
            ResultSet rs = stmt.executeQuery();

            // character has never done this event
            if (!rs.next()) {
                stmt.close();
                return charName + " has never participated in this event. No cooldown!";
            }

            Timestamp endTime = rs.getTimestamp("EndTime");
            stmt.close();

            if (endTime == null) return "This event has no cooldown timer.";

            // calculate how much cooldown is left
            long secondsPassed = (System.currentTimeMillis() - endTime.getTime()) / 1000;
            long cooldownTotal = COOLDOWN_HOURS * 3600;
            long remaining    = cooldownTotal - secondsPassed;

            if (remaining <= 0) return "Cooldown done! " + charName + " can rejoin this event.";

            long hours   = remaining / 3600;
            long minutes = (remaining % 3600) / 60;
            return "Cooldown remaining: " + hours + " hours, " + minutes + " minutes.";

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}