package shreyasapis;

import java.sql.*;
import java.util.Scanner;

public class GetCharacterStats {
    public static void Client_GetCharacterStats(Scanner scanner) {
        System.out.println("--- Get Character Stats ---");
        System.out.print("Enter Character Name: ");
        String charName = scanner.nextLine().trim().toLowerCase();

        System.out.println(Server_GetCharacterStats(charName));
    }

    public static String Server_GetCharacterStats(String charName) {
        Connection conn = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = dbconnection.DBConnection.getConnection();

            String sql =
            "SELECT C.name AS character_name, T.name AS team_name, R.RankName AS rank_name, CA.ClassName AS class_name, " +
            "C.Currency, C.CurrentHP, C.CurrentMP, C.CurrentStam " +
            "FROM Character C " +
            "JOIN Team T ON C.TeamID = T.ID " +
            "JOIN Rank R ON C.RankID = R.ID " +
            "JOIN Class CA ON C.ClassID = CA.ID " +
            "WHERE C.name ILIKE ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + charName + "%");
            ResultSet rs = stmt.executeQuery();

            sb.append(String.format("\n=== Stats for %s ===\n", charName));
            sb.append(String.format("%-12s %-30s %-15s %-15s %-12s %-12s %-12s %-12s\n",
                "Name", "Team", "Rank", "Class", "Currency", "CurrentHP", "CurrentMP", "CurrentStam"));
            sb.append("-".repeat(130) + "\n");

            boolean found = false;

            while (rs.next()) {
                found = true;
                String name = rs.getString("character_name");
                String teamName = rs.getString("team_name");
                String rankName = rs.getString("rank_name");
                String className = rs.getString("class_name");
                int currency = rs.getInt("Currency");
                int currentHP = rs.getInt("CurrentHP");
                int currentMP = rs.getInt("CurrentMP");
                int currentStam = rs.getInt("CurrentStam");

                sb.append(String.format("%-12s %-30s %-15s %-15s %-12d %-12d %-12d %-12d\n",
                    name, teamName, rankName, className, currency, currentHP, currentMP, currentStam));
            }

            if (!found) {
                sb.append("No character found with the name '" + charName + "'.\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error retrieving character stats.";
        } finally {
            if (conn != null) {
                // Don't close the shared connection; project uses a single pooled/shared connection.
                dbconnection.DBConnection.closeConnection(conn);
            }
        }
        return sb.toString();
    }
}
