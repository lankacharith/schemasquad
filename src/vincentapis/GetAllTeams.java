package vincentapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetAllTeams {

    // client layer handles the 'get more' loop and allows user to specify row count
    public static void Client_GetAllTeams(Scanner scanner) {
        System.out.println("\n--- Get All Teams ---");
        System.out.print("enter number of rows per page: ");
        
        int pageSize;
        try {
            pageSize = Integer.parseInt(scanner.nextLine().trim());
            if (pageSize <= 0) pageSize = 50; // fallback to 50 if they type 0 or a negative number
        } catch (NumberFormatException e) {
            System.out.println("invalid number. defaulting to 50 rows per page.");
            pageSize = 50;
        }

        // start at page 1 and keep going until user says no
        int page = 1;
        while (true) {
            System.out.println(Server_GetAllTeams(page, pageSize));
            System.out.print("next page? (yes/no): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) break;
            page++;
        }
    }

    public static String Server_GetAllTeams(int pageNum, int pageSize) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();


            int offset = (pageNum - 1) * pageSize;

            // joins character table to provide actual member counts instead of just dumping raw table columns.
            String sql =
                "SELECT \n" +
                "    t.Name AS TeamName, \n" +
                "    t.MaxSize, \n" +
                "    COUNT(c.ID) AS CurrentMembers \n" +
                "FROM \n" +
                "    Team t \n" +
                "LEFT JOIN \n" +
                "    Character c \n" +
                "        ON t.ID = c.TeamID \n" +
                "GROUP BY \n" +
                "    t.Name, \n" +
                "    t.MaxSize \n" +
                "ORDER BY \n" +
                "    t.Name ASC \n" +
                "LIMIT ? \n" +
                "OFFSET ?";

            // prepare statement exactly once
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            
            rs = stmt.executeQuery();

            // build the output table (prevents leaky abstraction)
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Registered Teams (Page %d) ===\n", pageNum));
            sb.append(String.format("%-30s %-15s\n", "Team Name", "Occupancy"));
            sb.append("-".repeat(46) + "\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                String teamName = rs.getString("TeamName");
                int current = rs.getInt("CurrentMembers");
                int max = rs.getInt("MaxSize");
                
                // format into a clean user-facing string
                String occupancy = current + " / " + max;
                
                sb.append(String.format("%-30s %-15s\n", teamName, occupancy));
            }

            if (!found) {
                sb.append("no more results.\n");
            }
            
            return sb.toString();

        } catch (SQLException e) {
            return "database error: " + e.getMessage();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}