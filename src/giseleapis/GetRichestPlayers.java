package giseleapis;

import dbconnection.DBConnection;
import java.sql.*;
import java.util.Scanner;

public class GetRichestPlayers {

    public static void Client_GetRichestPlayers(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("\n--- Richest Characters ---");

        System.out.print("How many rows per request? ");
        int limit;

        try {
            limit = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        int offset = 0;

        while (true) {

            System.out.println(Server_GetRichestPlayers(limit, offset));

            System.out.print("\nGet more rows? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (!choice.equals("y")) {
                break;
            }

            offset += limit;
        }
    }


    public static String Server_GetRichestPlayers(int limit, int offset) {

        Connection conn = null;

        try {

            conn = DBConnection.getConnection();

            // get characters ordered by currency, excluding banned players, with pagination
            String sql =
                "SELECT c.Name, p.Username, c.Currency " +
                "FROM Character c " +
                "JOIN Player p ON c.PlayerID = p.Username " +
                "WHERE p.BanStatus = FALSE " +
                "ORDER BY c.Currency DESC " +
                "LIMIT ? OFFSET ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();

            // build output string
            sb.append("\n=== Richest Characters ===\n");
            sb.append(String.format("%-20s %-15s %-10s\n",
                    "Character", "Player", "Gold"));
            sb.append("-".repeat(50)).append("\n");

            boolean found = false;

            // loop through results and build output string
            while (rs.next()) {
                found = true;

                sb.append(String.format("%-20s %-15s %-10d\n",
                        rs.getString("Name"),
                        rs.getString("Username"),
                        rs.getInt("Currency")));
            }

            if (!found) {
                sb.append("No more characters.\n");
            }

            stmt.close();

            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}