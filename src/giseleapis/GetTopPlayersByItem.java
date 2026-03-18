package giseleapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetTopPlayersByItem {

    public static void Client_GetTopPlayersByItem(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("\n--- Top Characters By Item ---");
        System.out.print("Enter item name: ");
        String itemName = scanner.nextLine().trim();

        System.out.println(Server_GetTopPlayersByItem(itemName));
    }


    public static String Server_GetTopPlayersByItem(String itemName) {

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();

            // get characters owning the specified item, ordered by quantity
            String sql =
                "SELECT c.Name AS CharacterName, i.Quantity " +
                "FROM Character c " +
                "JOIN Inventory i ON i.ContainerID = c.InvContainerID " +
                "JOIN Item it ON it.ID = i.ItemID " +
                "WHERE it.Name ILIKE ? " +
                "ORDER BY i.Quantity DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + itemName + "%");

            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();

            // build output string
            sb.append("\n=== Top Characters Owning: ").append(itemName).append(" ===\n");
            sb.append(String.format("%-20s %-10s\n", "Character", "Quantity"));
            sb.append("-".repeat(35)).append("\n");

            boolean found = false;

            // loop through results and build output string
            while (rs.next()) {
                found = true;

                sb.append(String.format("%-20s %-10d\n",
                        rs.getString("CharacterName"),
                        rs.getInt("Quantity")));
            }

            if (!found) {
                sb.append("No characters own this item.\n");
            }

            stmt.close();

            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}