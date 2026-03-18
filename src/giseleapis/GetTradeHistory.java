package giseleapis;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetTradeHistory {
    public static void Client_GetTradeHistory(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("\n--- Get Trade History ---");
        System.out.print("Enter character name: ");
        String name = scanner.nextLine().trim();
        System.out.println(Server_GetTradeHistory(name));
    }

    public static String Server_GetTradeHistory(String name) {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();

            // get character id
            String getChar =
                "SELECT ID FROM Character WHERE Name ILIKE ?";

            PreparedStatement stmt1 = conn.prepareStatement(getChar);
            stmt1.setString(1, name);
            ResultSet rs1 = stmt1.executeQuery();

            if (!rs1.next()) {
                stmt1.close();
                return "Character not found.";
            }

            int charID = rs1.getInt("ID");
            stmt1.close();

            // get trade history
            String query =
                "SELECT t.TimeOfTransaction, " +
                "c1.Name AS Player1Name, i1.Name AS Item1Name, t.Quantity1, t.Currency1, " +
                "c2.Name AS Player2Name, i2.Name AS Item2Name, t.Quantity2, t.Currency2 " +
                "FROM TradeTransaction t " +
                "JOIN Character c1 ON t.Player1ID = c1.ID " +
                "JOIN Character c2 ON t.Player2ID = c2.ID " +
                "LEFT JOIN Item i1 ON t.Item1ID = i1.ID " +
                "LEFT JOIN Item i2 ON t.Item2ID = i2.ID " +
                "WHERE t.Player1ID = ? OR t.Player2ID = ? " +
                "ORDER BY t.TimeOfTransaction DESC";

            PreparedStatement stmt2 = conn.prepareStatement(query);
            stmt2.setInt(1, charID);
            stmt2.setInt(2, charID);

            ResultSet rs2 = stmt2.executeQuery();

            // build output string
            StringBuilder sb = new StringBuilder();

            sb.append("\n=== Trade History for ").append(name).append(" ===\n");
            sb.append(String.format(
                "%-20s %-15s %-15s %-10s %-8s %-15s %-15s %-10s %-8s\n",
                "Time", "Player 1", "Item", "Quantity", "Gold", "Player 2", "Item", "Quantity", "Gold"));
            sb.append("-".repeat(120)).append("\n");

            boolean found = false;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // loop through results and build output string
            while (rs2.next()) {
                found = true;

                String time = rs2.getTimestamp("TimeOfTransaction")
                                .toLocalDateTime()
                                .format(formatter);

                sb.append(String.format(
                "%-20s %-15s %-15s %-10s %-8s %-15s %-15s %-10s %-8s\n",
                time,
                rs2.getString("Player1Name"),
                rs2.getString("Item1Name") == null ? "None" : rs2.getString("Item1Name"),
                rs2.getInt("Quantity1"),
                rs2.getInt("Currency1"),
                rs2.getString("Player2Name"),
                rs2.getString("Item2Name") == null ? "None" : rs2.getString("Item2Name"),
                rs2.getInt("Quantity2"),
                rs2.getInt("Currency2")
            ));
            }

            if (!found) {
                sb.append("No trades found.\n");
            }

            stmt2.close();

            return sb.toString();
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}
