package vincentapis;
// vincent huang

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class GetAllParties {

    // client handles the 'get more' loop and allows user to specify row count
    public static void Client_GetAllParties(Scanner scanner) {
        System.out.println("\n--- Get All Parties ---");
        System.out.print("enter number of rows per page: ");
        
        int pageSize;
        try {
            pageSize = Integer.parseInt(scanner.nextLine().trim());
            if (pageSize <= 0) pageSize = 50; // fallback just in case
        } catch (NumberFormatException e) {
            System.out.println("invalid number. defaulting to 50.");
            pageSize = 50;
        }

        // start at page 1 and keep going until user says no (this is the 'get more' function)
        int page = 1;
        while (true) {
            System.out.println(Server_GetAllParties(page, pageSize));
            System.out.print("next page? (yes/no): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) break;
            page++;
        }
    }

    // server is completely stateless. it takes page num and page size and returns a string abstraction.
    // it does not hold a db cursor open between calls.
    public static String Server_GetAllParties(int pageNum, int pageSize) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // offset skips past rows we already showed using a stateless mathematical scheme
            int offset = (pageNum - 1) * pageSize;

            String sql =
                "SELECT \n" +
                "    p.JoinCode, \n" +
                "    p.MaxSize, \n" +
                "    COUNT(c.ID) AS CurrentMembers \n" +
                "FROM \n" +
                "    Party p \n" +
                "LEFT JOIN \n" +
                "    Character c \n" +
                "        ON p.ID = c.PartyID \n" +
                "GROUP BY \n" +
                "    p.JoinCode, \n" +
                "    p.MaxSize \n" +
                "ORDER BY \n" +
                "    p.JoinCode ASC \n" +
                "LIMIT ? \n" +
                "OFFSET ?";

            // prepare statement exactly once
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            
            rs = stmt.executeQuery();

            // build the output table
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n=== Active Parties (Page %d) ===\n", pageNum));
            sb.append(String.format("%-15s %-15s\n", "Join Code", "Occupancy"));
            sb.append("-".repeat(32) + "\n");

            boolean found = false;
            while (rs.next()) {
                found = true;
                String code = rs.getString("JoinCode");
                int current = rs.getInt("CurrentMembers");
                int max = rs.getInt("MaxSize");
                
                String occupancy = current + " / " + max;
                sb.append(String.format("%-15s %-15s\n", code, occupancy));
            }

            if (!found) {
                sb.append("no more results.\n");
            }
            
            return sb.toString();

        } catch (SQLException e) {
            return "database error: " + e.getMessage();
        } finally {
            // closing the resultset completely destroys the db cursor so it is not held open
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}