// Charith Lanka

import java.sql.*;
import java.util.Scanner;

public class UseAbility {

    // asks for character name and ability name
    public static void Client_UseAbility(Scanner scanner) {
        System.out.println("\n--- Use Ability ---");
        System.out.print("Character name: ");
        String charName = scanner.nextLine().trim();
        System.out.print("Ability name: ");
        String abilityName = scanner.nextLine().trim();
        System.out.println(Server_UseAbility(charName, abilityName));
    }

    // checks cooldown and updates the DB if the ability is ready
    public static String Server_UseAbility(String charName, String abilityName) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // use EXTRACT to calculate seconds since last use on the database side
            // this avoids timezone mismatches between Java and Supabase
            String sql =
                "SELECT c.ID, a.Cooldown, " +
                "       EXTRACT(EPOCH FROM (NOW() - c.LastOnline)) AS seconds_since_use " +
                "FROM Character c " +
                "JOIN CharacterAbility ca ON ca.CharacterID = c.ID " +
                "JOIN Ability a           ON ca.AbilityID   = a.ID " +
                "WHERE c.Name ILIKE ? AND a.Name ILIKE ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, charName);
            stmt.setString(2, abilityName);
            ResultSet rs = stmt.executeQuery();

            // character doesnt have this ability
            if (!rs.next()) {
                stmt.close();
                return "Error: " + charName + " doesn't have ability '" + abilityName + "'.";
            }

            int charID          = rs.getInt("ID");
            int cooldown        = rs.getInt("Cooldown");
            double secondsSince = rs.getDouble("seconds_since_use");
            stmt.close();

            // check if the cooldown is still going
            if (cooldown > 0 && secondsSince >= 0 && secondsSince < cooldown) {
                long remaining = (long)(cooldown - secondsSince);
                return abilityName + " is on cooldown. " + remaining + " seconds left.";
            }

            // cooldown is done -- update LastOnline to now to start the new cooldown
            PreparedStatement update = conn.prepareStatement(
                "UPDATE Character SET LastOnline = NOW() WHERE ID = ?");
            update.setInt(1, charID);
            update.executeUpdate();
            update.close();

            return "Success! " + charName + " used " + abilityName + ". Cooldown: " + cooldown + "s.";

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}