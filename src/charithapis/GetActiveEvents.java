package charithapis;
// Charith Lanka

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetActiveEvents {

    // no input needed, just print active events
    public static void Client_GetActiveEvents(Scanner scanner) {
        System.out.println("\n--- Active Events ---");
        System.out.println(Server_GetActiveEvents());
    }

    // finds events where start is in the past and end is in the future or null
    public static String Server_GetActiveEvents() {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // NOW() gives the current timestamp
            // events with no EndTime are considered permanently active
            String sql =
                "SELECT Name, Description, StartTime, EndTime, RewardXP " +
                "FROM Events " +
                "WHERE (StartTime IS NULL OR StartTime <= NOW()) " +
                "  AND (EndTime IS NULL OR EndTime >= NOW()) " +
                "ORDER BY RewardXP DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-30s %-10s %-20s\n", "Event", "XP", "Ends"));
            sb.append("-".repeat(63) + "\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                // show "No end date" if EndTime is null
                String end = rs.getTimestamp("EndTime") != null
                    ? rs.getTimestamp("EndTime").toString() : "No end date";
                sb.append(String.format("%-30s %-10d %-20s\n",
                    rs.getString("Name"), rs.getInt("RewardXP"), end));
                sb.append("  " + rs.getString("Description") + "\n");
            }

            if (!found) sb.append("No active events right now.\n");
            stmt.close();
            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}