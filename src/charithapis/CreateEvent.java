package charithapis;
// Charith Lanka

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class CreateEvent {

    // collects event info from user then calls server
    public static void Client_CreateEvent(Scanner scanner) {
        System.out.println("\n--- Create Event ---");

        System.out.print("Event name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();

        System.out.print("Reward XP: ");
        String xpInput = scanner.nextLine().trim();

        // make sure XP is actually a number
        int xp;
        try { xp = Integer.parseInt(xpInput); }
        catch (NumberFormatException e) {
            System.out.println("Error: XP must be a number.");
            return;
        }

        System.out.println(Server_CreateEvent(name, desc, xp));
    }

    // inserts the new event into the database
    public static String Server_CreateEvent(String name, String desc, int rewardXP) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // check if an event with this name already exists
            PreparedStatement check = conn.prepareStatement(
                "SELECT ID FROM Events WHERE Name = ?");
            check.setString(1, name);
            if (check.executeQuery().next()) {
                check.close();
                return "Error: Event '" + name + "' already exists.";
            }
            check.close();

            // insert the new event -- start/end time are null until set later
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Events (Name, Description, RewardXP) VALUES (?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, desc);
            stmt.setInt(3, rewardXP);
            stmt.executeUpdate();
            stmt.close();

            return "Success: Event '" + name + "' created with " + rewardXP + " XP reward.";

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}