package giseleapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetPlayerInventory {

    public static void Client_GetPlayerInventory(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("\n--- Get Character Inventory ---");
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();
        System.out.println(Server_GetPlayerInventory(charName));
    }

    public static String Server_GetPlayerInventory(String charName) {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();

            // get character inventory container
            String sql =
                "SELECT c.Name AS CharacterName, it.Name AS ItemName, i.Quantity " +
                "FROM Character c " +
                "JOIN Inventory i ON i.ContainerID = c.InvContainerID " +
                "JOIN Item it ON it.ID = i.ItemID " +
                "WHERE c.Name ILIKE ? " +
                "ORDER BY it.Name";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + charName + "%");

            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append("\n=== Inventory for ").append(charName).append(" ===\n");

            boolean found = false;

            // loop through results and build output string
            while (rs.next()) {
                found = true;

                sb.append("- ").append(rs.getString("ItemName"))
                .append(" (").append(rs.getString("Quantity")).append(")\n");
            }

            if (!found) {
                sb.append("No items found for this character.\n");
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