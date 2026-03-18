package shreyasapis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetActiveQuests {
    public static void Client_GetActiveQuests(Scanner scanner) {
        System.out.println("--- Get Active Quests ---");
        System.out.print("Enter Character name: ");
        String charName = scanner.nextLine().trim().toLowerCase();

        System.out.println(Server_GetActiveQuests(charName));

    }

    public static String Server_GetActiveQuests(String charName) {
        Connection conn = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = DBConnection.getConnection();

            String sql =
            "SELECT Q.Name, Q.description, Q.maxsteps, Q.rewardxp, Q.rewardcurrency, Q.rewarditems " +
            "FROM Quest Q " +
            "JOIN CharacterQuestProgress CQP ON Q.ID = CQP.questID " +
            "JOIN Character C ON CQP.characterID = C.ID " +
            "WHERE C.name ILIKE ? AND CQP.iscomplete = False " +
            "ORDER BY Q.ID";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + charName + "%");
            ResultSet rs = stmt.executeQuery();

            sb.append(String.format("\n=== Active Quests for %s ===\n", charName));
            sb.append(String.format("%-30s %-30s %-10s %-10s %-15s %-20s\n",
                "Name", "Description", "MaxSteps", "RewardXP", "RewardCurrency", "RewardItems"));
            sb.append("-".repeat(115) + "\n");

            boolean found = false;

            while (rs.next()) {
                found = true;
                String name = rs.getString("Name");
                String description = rs.getString("description");
                int maxSteps = rs.getInt("MaxSteps");
                int rewardXP = rs.getInt("RewardXP");
                int rewardCurrency = rs.getInt("RewardCurrency");
                String rewardItems = rs.getString("RewardItems");

                String desc = description == null ? "" : description.replaceAll("\r?\n", " ").trim();

                // Wrap description into 30-char chunks (word boundaries) so the table stays aligned
                int wrapWidth = 30;
                List<String> wrapped = new ArrayList<>();
                StringBuilder line = new StringBuilder();
                for (String word : desc.split(" ")) {
                    if (line.length() > 0 && line.length() + 1 + word.length() > wrapWidth) {
                        wrapped.add(line.toString());
                        line.setLength(0);
                    }
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
                if (line.length() > 0) {
                    wrapped.add(line.toString());
                }

                if (wrapped.isEmpty()) {
                    sb.append(String.format("%-30s %-30s %-10d %-10d %-15d %-20s\n",
                        name, "", maxSteps, rewardXP, rewardCurrency, rewardItems));
                } else {
                    for (int i = 0; i < wrapped.size(); i++) {
                        String lineText = wrapped.get(i);
                        if (i == 0) {
                            sb.append(String.format("%-30s %-30s %-10d %-10d %-15d %-20s\n",
                                name, lineText, maxSteps, rewardXP, rewardCurrency, rewardItems));
                        } else {
                            sb.append(String.format("%-30s %-30s %-10s %-10s %-15s %-20s\n",
                                "", lineText, "", "", "", ""));
                        }
                    }
                }
            }
            if (!found) {
                return "Info: No active quests found for character '" + charName + "'.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database error occurred.";
        } finally {
            if (conn != null) {
                // Don't close the shared connection; project uses a single pooled/shared connection.
                DBConnection.closeConnection(conn);
            }
        }
        return sb.toString();
    }
}
