// Charith Lanka

import java.sql.*;
import java.util.Scanner;

public class GetTopPlayersByLevel {

    // LargeList requires 50 rows per page
    private static final int PAGE_SIZE = 50;

    // no input needed, just page through results
    public static void Client_GetTopPlayersByLevel(Scanner scanner) {
        System.out.println("\n--- Top Players By Level ---");
        int page = 1;
        while (true) {
            System.out.println(Server_GetTopPlayersByLevel(page));
            System.out.print("Next page? (yes/no): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) break;
            page++;
        }
    }

    // joins Character, Player, Class, and Race to show a full row per player
    public static String Server_GetTopPlayersByLevel(int pageNum) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // offset skips past rows we already showed
            int offset = (pageNum - 1) * PAGE_SIZE;

            // join 4 tables to get name, class, race, and stats
            String sql =
                "SELECT c.Name, p.Username, cl.ClassName, r.RaceName, c.MaxHP " +
                "FROM Character c " +
                "JOIN Player p  ON c.PlayerID = p.Username " +
                "JOIN Class cl  ON c.ClassID  = cl.ID " +
                "JOIN Race r    ON c.RaceID   = r.ID " +
                "WHERE p.BanStatus = FALSE " +
                "ORDER BY c.MaxHP DESC " +
                "LIMIT ? OFFSET ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, PAGE_SIZE);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Top Players By Level (Page %d) ===\n", pageNum));
            sb.append(String.format("%-5s %-20s %-15s %-12s %-10s %-6s\n",
                "Rank", "Character", "Player", "Class", "Race", "MaxHP"));
            sb.append("-".repeat(72) + "\n");

            int rank = offset + 1;
            boolean found = false;
            while (rs.next()) {
                found = true;
                sb.append(String.format("%-5d %-20s %-15s %-12s %-10s %-6d\n",
                    rank++,
                    rs.getString("Name"),
                    rs.getString("Username"),
                    rs.getString("ClassName"),
                    rs.getString("RaceName"),
                    rs.getInt("MaxHP")));
            }

            if (!found) sb.append("No more results.\n");
            stmt.close();
            return sb.toString();

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            DBConnection.closeConnection(conn);
        }
    }
}