package shreyasapis;

import java.sql.*;
import java.time.format.DateTimeFormatter;
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
            "C.Currency, C.CurrentHP, C.MaxHP, C.CurrentMP, C.MaxMP, C.CurrentStam, C.MaxStam, L.name AS location_name, C.isOnline, C.LastOnline " +
            "FROM Character C " +
            "JOIN Team T ON C.TeamID = T.ID " +
            "JOIN Rank R ON C.RankID = R.ID " +
            "JOIN Class CA ON C.ClassID = CA.ID " +
            "JOIN Location L ON C.LocationID = L.ID " +
            "WHERE C.name ILIKE ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + charName + "%");
            ResultSet rs = stmt.executeQuery();

            sb.append(String.format("\n=== Stats for %s ===\n", charName));
            sb.append(String.format("%-12s %-25s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-15s %-15s\n",
                "Name", "Team", "Rank", "Class", "Currency", "Health", "Mana", "Stamina", "Location", "Online Status", "Last Online"));
            sb.append("-".repeat(170) + "\n");

            boolean found = false;

            while (rs.next()) {
                found = true;
                String name = rs.getString("character_name");
                String teamName = rs.getString("team_name");
                String rankName = rs.getString("rank_name");
                String className = rs.getString("class_name");
                int currency = rs.getInt("Currency");

                int currentHP = rs.getInt("CurrentHP");
                int maxHP = rs.getInt("MaxHP");
                int currentMP = rs.getInt("CurrentMP");
                int maxMP = rs.getInt("MaxMP");
                int currentStam = rs.getInt("CurrentStam");
                int maxStam = rs.getInt("MaxStam");
                String locationName = rs.getString("location_name");
                boolean isOnline = rs.getBoolean("isOnline");
                                Timestamp lastOnline = rs.getTimestamp("LastOnline");
                String lastOnlineStr = "N/A";
                if (lastOnline != null) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    lastOnlineStr = lastOnline.toLocalDateTime().format(fmt);
                }

                sb.append(String.format("%-12s %-25s %-12s %-12s %-12d %-12s %-12s %-12s %-12s %-15s %-15s\n",
                    name, teamName, rankName, className, currency,
                    currentHP + "/" + maxHP,
                    currentMP + "/" + maxMP,
                    currentStam + "/" + maxStam,
                    locationName, isOnline ? "Online" : "Offline", lastOnlineStr));
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
