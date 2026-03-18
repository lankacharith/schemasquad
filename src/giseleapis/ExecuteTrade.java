package giseleapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class ExecuteTrade {
    public static void Client_ExecuteTrade(Scanner scanner) {
        // gets user input and calls the server function
        System.out.println("--- Execute Trade ---");

        // get player 1 trade details
        System.out.print("Enter Player 1's character name: ");
        String char1 = scanner.nextLine().trim();
        System.out.print("Enter item name to trade from Player 1 to Player 2 " + 
        "(if trading nothing, press enter): ");
        String itemName1 = scanner.nextLine().trim();
        int quantity1 = 0;
        if (!itemName1.isEmpty()) {
            System.out.print("Enter quantity to trade: ");
            try {
                quantity1 = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid quantity.");
                return;
            }
        }
        System.out.print("Enter gold amount to trade from Player 1 to Player 2: ");
        int gold1;
        try {
            gold1 = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid gold amount.");
            return;
        }

        // get player 2 trade details
        System.out.print("Enter Player 2's character name: ");
        String char2 = scanner.nextLine().trim();
        System.out.print("Enter item name to trade from Player 2 to Player 1 " + 
        "(if trading nothing, press enter): ");
        String itemName2 = scanner.nextLine().trim();
        int quantity2 = 0;
        if (!itemName2.isEmpty()) {
            System.out.print("Enter quantity to trade: ");
            try {
                quantity2 = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid quantity.");
                return;
            }
        }
        System.out.print("Enter gold amount to trade from Player 2 to Player 1: ");
        int gold2;
        try {
            gold2 = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid gold amount.");
            return;
        }

        System.out.println(
            Server_ExecuteTrade(char1, itemName1, quantity1, gold1, char2, itemName2, quantity2, gold2));
    }

    public static String Server_ExecuteTrade(
        String char1, String itemName1, int quantity1, int gold1,
        String char2, String itemName2, int quantity2, int gold2) {

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            
            // get player 1 character ID
            String getCharID1 =
                "SELECT ID FROM Character WHERE Name ILIKE ?";
            PreparedStatement stmt1 = conn.prepareStatement(getCharID1);
            stmt1.setString(1, char1);
            ResultSet rs1 = stmt1.executeQuery();
            if (!rs1.next()) {
                stmt1.close();
                return "Character 1 not found.";
            }

            // get player 2 character ID
            String getCharID2 =
                "SELECT ID FROM Character WHERE Name ILIKE ?";
            PreparedStatement stmt2 = conn.prepareStatement(getCharID2);
            stmt2.setString(1, char2);
            ResultSet rs2 = stmt2.executeQuery();
            if (!rs2.next()) {
                stmt2.close();
                return "Character 2 not found.";
            }

            // get item ID of item being traded from player 1 to player 2 (if any)
            PreparedStatement stmt3 = null;
            ResultSet rs3 = null;
            if (!itemName1.isEmpty()) {
                String getItemID1 =
                    "SELECT ID FROM Item WHERE Name ILIKE ?";
                stmt3 = conn.prepareStatement(getItemID1);
                stmt3.setString(1, itemName1);
                rs3 = stmt3.executeQuery();
                if (!rs3.next()) {
                    stmt3.close();
                    return "Item 1 not found.";
                }
            }

            // get item ID of item being traded from player 2 to player 1 (if any)
            PreparedStatement stmt4 = null;
            ResultSet rs4 = null;
            if (!itemName2.isEmpty()) {
                String getItemID2 =
                    "SELECT ID FROM Item WHERE Name ILIKE ?";
                stmt4 = conn.prepareStatement(getItemID2);
                stmt4.setString(1, itemName2);
                rs4 = stmt4.executeQuery();
                if (!rs4.next()) {
                    stmt4.close();
                    return "Item 2 not found.";
                }
            }

            // check if player 1 has the item and quantity they want to trade (if any)
            PreparedStatement stmt5 = null;
            ResultSet rs5 = null;
            if (!itemName1.isEmpty()) {
                String checkItem1 = 
                    "SELECT Quantity FROM Inventory WHERE ContainerID = " +
                    "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                    "AND ItemID = ?";
                stmt5 = conn.prepareStatement(checkItem1);
                stmt5.setString(1, char1);
                stmt5.setInt(2, rs3.getInt("ID"));
                rs5 = stmt5.executeQuery();
                if (!rs5.next() || rs5.getInt("Quantity") < quantity1) {
                    stmt5.close();
                    return char1 + " does not have enough " + itemName1 + "s to trade.";
                }
            }

            // check if player 2 has the item and quantity they want to trade (if any)
            PreparedStatement stmt6 = null;
            ResultSet rs6 = null;
            if (!itemName2.isEmpty()) {
                String checkItem2 = 
                    "SELECT Quantity FROM Inventory WHERE ContainerID = " +
                    "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                    "AND ItemID = ?";
                stmt6 = conn.prepareStatement(checkItem2);
                stmt6.setString(1, char2);
                stmt6.setInt(2, rs4.getInt("ID"));
                rs6 = stmt6.executeQuery();
                if (!rs6.next() || rs6.getInt("Quantity") < quantity2) {
                    stmt6.close();
                    return char2 + " does not have enough " + itemName2 + "s to trade.";
                }
            }

            // check if player 1 has enough gold to trade
            String checkGold1 = 
                "SELECT Currency FROM Character WHERE Name ILIKE ?";
            PreparedStatement stmt7 = conn.prepareStatement(checkGold1);
            stmt7.setString(1, char1);
            ResultSet rs7 = stmt7.executeQuery();
            if (!rs7.next() || rs7.getInt("Currency") < gold1) {
                stmt7.close();
                return char1 + " does not have enough gold to trade.";
            }

            // check if player 2 has enough gold to trade
            String checkGold2 = 
                "SELECT Currency FROM Character WHERE Name ILIKE ?";
            PreparedStatement stmt8 = conn.prepareStatement(checkGold2);
            stmt8.setString(1, char2);
            ResultSet rs8 = stmt8.executeQuery();
            if (!rs8.next() || rs8.getInt("Currency") < gold2) {
                stmt8.close();
                return char2 + " does not have enough gold to trade.";
            }

            // remove items to be traded from player 1 inventory
            if (!itemName1.isEmpty()) {
                if (rs5.getInt("Quantity") == quantity1) {
                    String deleteItem1 = 
                        "DELETE FROM Inventory WHERE ContainerID = " +
                        "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                        "AND ItemID = ?";
                    PreparedStatement stmt9 = conn.prepareStatement(deleteItem1);
                    stmt9.setString(1, char1);
                    stmt9.setInt(2, rs3.getInt("ID"));
                    stmt9.executeUpdate();
                    stmt9.close();
                } else {
                    String updateItem1 = 
                        "UPDATE Inventory SET Quantity = Quantity - ? WHERE ContainerID = " +
                        "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                        "AND ItemID = ?";
                    PreparedStatement stmt9 = conn.prepareStatement(updateItem1);
                    stmt9.setInt(1, quantity1);
                    stmt9.setString(2, char1);
                    stmt9.setInt(3, rs3.getInt("ID"));
                    stmt9.executeUpdate();
                    stmt9.close();
                }
            }

            // remove items to be traded from player 2 inventory
            if (!itemName2.isEmpty()) {
                if (rs6.getInt("Quantity") == quantity2) {
                    String deleteItem2 = 
                        "DELETE FROM Inventory WHERE ContainerID = " +
                        "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                        "AND ItemID = ?";
                    PreparedStatement stmt10 = conn.prepareStatement(deleteItem2);
                    stmt10.setString(1, char2);
                    stmt10.setInt(2, rs4.getInt("ID"));
                    stmt10.executeUpdate();
                    stmt10.close();
                } else {
                    String updateItem2 = 
                        "UPDATE Inventory SET Quantity = Quantity - ? WHERE ContainerID = " +
                        "(SELECT InvContainerID FROM Character WHERE Name ILIKE ?) " +
                        "AND ItemID = ?";
                    PreparedStatement stmt10 = conn.prepareStatement(updateItem2);
                    stmt10.setInt(1, quantity2);
                    stmt10.setString(2, char2);
                    stmt10.setInt(3, rs4.getInt("ID"));
                    stmt10.executeUpdate();
                    stmt10.close();
                }
            }

            // update gold for player 1
            String updateGold1 = 
                "UPDATE Character SET Currency = Currency - ? + ? WHERE Name ILIKE ?";
            PreparedStatement stmt11 = conn.prepareStatement(updateGold1);
            stmt11.setInt(1, gold1);
            stmt11.setInt(2, gold2);
            stmt11.setString(3, char1);
            stmt11.executeUpdate();

            // update gold for player 2
            String updateGold2 = 
                "UPDATE Character SET Currency = Currency - ? + ? WHERE Name ILIKE ?";
            PreparedStatement stmt12 = conn.prepareStatement(updateGold2);
            stmt12.setInt(1, gold2);
            stmt12.setInt(2, gold1);
            stmt12.setString(3, char2);
            stmt12.executeUpdate();

            // add items to player inventories
            AddItemToInventory.Server_AddItemToInventory(char2, itemName1, quantity1);
            AddItemToInventory.Server_AddItemToInventory(char1, itemName2, quantity2);

            // record trade transaction
            String transaction = 
                "INSERT INTO TradeTransaction (Player1ID, Item1ID, Quantity1, Currency1, Player2ID, Item2ID, Quantity2, Currency2)" + 
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt15 = conn.prepareStatement(transaction);
            stmt15.setInt(1, rs1.getInt("ID"));

            if (itemName1.isEmpty()) stmt15.setNull(2, java.sql.Types.INTEGER);
            else stmt15.setInt(2, rs3.getInt("ID"));

            stmt15.setInt(3, quantity1);
            stmt15.setInt(4, gold1);
            stmt15.setInt(5, rs2.getInt("ID"));

            if (itemName2.isEmpty()) stmt15.setNull(6, java.sql.Types.INTEGER);
            else stmt15.setInt(6, rs4.getInt("ID"));

            stmt15.setInt(7, quantity2);
            stmt15.setInt(8, gold2);
            stmt15.executeUpdate();

            if (rs1 != null) rs1.close();
            if (rs2 != null) rs2.close();
            if (rs3 != null) rs3.close();
            if (rs4 != null) rs4.close();
            if (rs5 != null) rs5.close();
            if (rs6 != null) rs6.close();
            if (rs7 != null) rs7.close();
            if (rs8 != null) rs8.close();

            if (stmt1 != null) stmt1.close();
            if (stmt2 != null) stmt2.close();
            if (stmt3 != null) stmt3.close();
            if (stmt4 != null) stmt4.close();
            if (stmt5 != null) stmt5.close();
            if (stmt6 != null) stmt6.close();
            if (stmt7 != null) stmt7.close();
            if (stmt8 != null) stmt8.close();
            if (stmt15 != null) stmt15.close();

            return "Trade executed successfully";
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn); //dummy method to not break
        }
    }
}
