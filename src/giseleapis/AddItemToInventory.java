package giseleapis;

import dbconnection.DBConnection;
import java.sql.*;
import java.util.Scanner;

public class AddItemToInventory {

    public static void Client_AddItemToInventory(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("\n--- Add Item To Character Inventory ---");

        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();

        System.out.print("Enter item name: ");
        String itemName = scanner.nextLine().trim();

        System.out.print("Enter quantity to add: ");
        int quantity;

        try {
            quantity = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity.");
            return;
        }

        System.out.println(Server_AddItemToInventory(charName, itemName, quantity));
    }


    public static String Server_AddItemToInventory(String charName, String itemName, int quantity) {

        Connection conn = null;

        try {

            conn = DBConnection.getConnection();

            // get character inventory container
            String getContainer =
                "SELECT InvContainerID FROM Character WHERE Name ILIKE ?";

            PreparedStatement stmt1 = conn.prepareStatement(getContainer);
            stmt1.setString(1, charName);

            ResultSet rs1 = stmt1.executeQuery();

            if (!rs1.next()) {
                stmt1.close();
                return "Character not found.";
            }

            int containerID = rs1.getInt("InvContainerID");
            stmt1.close();


            // get item id
            String getItem =
                "SELECT ID FROM Item WHERE Name ILIKE ?";

            PreparedStatement stmt2 = conn.prepareStatement(getItem);
            stmt2.setString(1, itemName);

            ResultSet rs2 = stmt2.executeQuery();

            if (!rs2.next()) {
                stmt2.close();
                return "Item not found.";
            }

            int itemID = rs2.getInt("ID");
            stmt2.close();


            // check if item already exists in inventory
            String check =
                "SELECT Quantity FROM Inventory WHERE ContainerID = ? AND ItemID = ?";

            PreparedStatement stmt3 = conn.prepareStatement(check);
            stmt3.setInt(1, containerID);
            stmt3.setInt(2, itemID);

            ResultSet rs3 = stmt3.executeQuery();


            if (rs3.next()) {

                // item exists → update quantity
                String update =
                    "UPDATE Inventory SET Quantity = Quantity + ? " +
                    "WHERE ContainerID = ? AND ItemID = ?";

                PreparedStatement stmt4 = conn.prepareStatement(update);
                stmt4.setInt(1, quantity);
                stmt4.setInt(2, containerID);
                stmt4.setInt(3, itemID);

                stmt4.executeUpdate();
                stmt4.close();

            } else {

                // item doesn't exist → insert new row
                String insert =
                    "INSERT INTO Inventory (ContainerID, Quantity, ItemID) VALUES (?, ?, ?)";

                PreparedStatement stmt4 = conn.prepareStatement(insert);
                stmt4.setInt(1, containerID);
                stmt4.setInt(2, quantity);
                stmt4.setInt(3, itemID);

                stmt4.executeUpdate();
                stmt4.close();
            }

            stmt3.close();

            return quantity + " " + itemName + "(s) added to " + charName + "'s inventory.";

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}