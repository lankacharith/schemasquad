// Charith Lanka

import java.sql.*;
import java.util.Scanner;

public class GetAbilityCooldowns {

    // asks for a character name then prints their abilities
    public static void Client_GetAbilityCooldowns(Scanner scanner) {
        System.out.println("\n--- Get Ability Cooldowns ---");
        System.out.print("Character name: ");
        String charName = scanner.nextLine().trim();
        System.out.println(Server_GetAbilityCooldowns(charName));
    }

    // joins 3 tables to get all abilities and figure out cooldown status
    public static String Server_GetAbilityCooldowns(String charName) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // use EXTRACT to calculate seconds since last use on the database side
            // this avoids timezone mismatches between Java and Supabase
            String sql =
                "SELECT a.Name AS Ability, a.Description, a.Cooldown, " +
                "       EXTRACT(EPOCH FROM (NOW() - c.LastOnline)) AS seconds_since_use " +
                "FROM Character c " +
                "JOIN CharacterAbility ca ON ca.CharacterID = c.ID " +
                "JOIN Ability a           ON ca.AbilityID   = a.ID " +
                "WHERE c.Name ILIKE ? " +
                "ORDER BY a.Name";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, charName);
            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Abilities for %s ===\n", charName));
            sb.append(String.format("%-25s %-10s %-15s\n", "Ability", "Cooldown", "Status"));
            sb.append("-".repeat(53) + "\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                int cooldown         = rs.getInt("Cooldown");
                double secondsSince  = rs.getDouble("seconds_since_use");

                // figure out the cooldown status using database time
                String status;
                if (cooldown == 0) {
                    status = "No cooldown";
                } else if (secondsSince < 0) {
                    status = "Ready";
                } else {
                    long remaining = (long)(cooldown - secondsSince);
                    status = remaining <= 0 ? "Ready" : remaining + "s left";
                }

                sb.append(String.format("%-25s %-10s %-15s\n",
                    rs.getString("Ability"),
                    cooldown == 0 ? "None" : cooldown + "s",
                    status));
                sb.append("  " + rs.getString("Description") + "\n");
            }

            if (!found) sb.append("Character '" + charName + "' not found or has no abilities.\n");
            stmt.close();
            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}