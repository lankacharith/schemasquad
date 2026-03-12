package vincentapis;
// vincent huang

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.regex.Pattern;

import dbconnection.DBConnection;

public class GetPartySuggestions {

    // precompiled regex to block bad inputs from hitting the database
    private static final Pattern JOIN_CODE_PATTERN = Pattern.compile("^[A-Za-z]{3}-\\d{3}$");

    // client layer handles the prompt and passes the joincode down
    public static void Client_GetPartySuggestions(Scanner scanner) {
        System.out.println("\n--- Party Composition Analyzer ---");
        System.out.print("enter party join code (e.g., xyz-123): ");
        String joinCode = scanner.nextLine().trim().toUpperCase();

        if (!JOIN_CODE_PATTERN.matcher(joinCode).matches()) {
            System.out.println("client error: invalid joincode format.");
            return;
        }

        // execute server logic and print the dynamic advice abstraction
        System.out.println(Server_GetPartySuggestions(joinCode));
    }

    // server layer matches the updated signature and handles the complex sql analysis
    public static String Server_GetPartySuggestions(String joinCode) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // complex api query (60 pts)
            // this sql 'pivots' row data into distinct columns using conditional aggregation.
            String sql = 
                "SELECT \n" +
                "    p.MaxSize, \n" +
                "    COUNT(c.ID) AS TotalMembers, \n" +
                "    COALESCE(SUM(CASE WHEN cl.ClassName IN ('Paladin', 'Healer') THEN 1 ELSE 0 END), 0) AS SupportCount, \n" +
                "    COALESCE(SUM(CASE WHEN cl.ClassName = 'Warrior' THEN 1 ELSE 0 END), 0) AS WarriorCount, \n" +
                "    COALESCE(SUM(CASE WHEN cl.ClassName = 'Mage' THEN 1 ELSE 0 END), 0) AS MageCount, \n" +
                "    COALESCE(SUM(CASE WHEN cl.ClassName = 'Archer' THEN 1 ELSE 0 END), 0) AS ArcherCount \n" +
                "FROM \n" +
                "    Party p \n" +
                "LEFT JOIN \n" +
                "    Character c \n" +
                "        ON p.ID = c.PartyID \n" +
                "LEFT JOIN \n" +
                "    Class cl \n" +
                "        ON c.ClassID = cl.ID \n" +
                "WHERE \n" +
                "    p.JoinCode = ? \n" +
                "GROUP BY \n" +
                "    p.MaxSize";
                
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, joinCode);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                return "error: party with join code '" + joinCode + "' does not exist.";
            }

            int maxSize = rs.getInt("MaxSize");
            int totalMembers = rs.getInt("TotalMembers");
            int supportCount = rs.getInt("SupportCount");
            int warriorCount = rs.getInt("WarriorCount");
            int mageCount = rs.getInt("MageCount");
            int archerCount = rs.getInt("ArcherCount");

            StringBuilder advice = new StringBuilder();
            advice.append(String.format("\n=== Party Analyzer: %s (%d/%d members) ===\n", joinCode, totalMembers, maxSize));

            // --- the logic tree ---
            
            if (totalMembers == 0) {
                // handle the completely empty party
                advice.append("there are no characters in this party.");
                return advice.toString();

            } else if (totalMembers == 1) {
                // handle the almost empty party based on the solo player's class
                advice.append("your party is almost empty! ");
                
                if (supportCount == 1) {
                    advice.append("with a support already here, you should recruit a warrior, a mage, and an archer for damage.");
                } else if (warriorCount == 1) {
                    advice.append("with a warrior on the frontline, you should recruit a paladin or healer, a mage, and an archer.");
                } else if (mageCount == 1 || archerCount == 1) {
                    advice.append("with ranged damage covered, you should recruit a paladin or healer, and a warrior to hold the frontline.");
                } else {
                    advice.append("you have a great start, try to recruit a tank, a healer, and some damage dealers!");
                }
                
            } else if (totalMembers < maxSize) {
                // branch a: recruitment mode (party has 2 or 3 members)
                if (supportCount >= 2) {
                    // special rule: prioritize ranged dps if there is a surplus of support
                    if (mageCount == 0 && archerCount == 0) {
                        advice.append("you have a surplus of support and healing! to balance your team, you desperately need ranged dps. prioritize recruiting an archer or a mage.");
                    } else if (mageCount >= 1 && archerCount == 0) {
                        advice.append("you have great healing and magic damage, but you are missing physical ranged attacks. you should prioritize recruiting an archer.");
                    } else if (archerCount >= 1 && mageCount == 0) {
                        advice.append("you have strong support and physical damage, but you lack magical area-of-effect damage. you should prioritize recruiting a mage.");
                    } else {
                        advice.append("you have a highly durable team! fill your last slot with any damage dealer.");
                    }
                } else if (supportCount == 0) {
                    advice.append("your party currently lacks a dedicated support. you should prioritize recruiting a healer or a paladin to keep everyone alive.");
                } else {
                    // we have exactly 1 support, so check the dps balance
                    if (warriorCount == 0) {
                        advice.append("you have your support, but you need a frontline. try to recruit a warrior.");
                    } else if (mageCount == 0 && archerCount == 0) {
                        advice.append("your frontline is secure, but you need some ranged damage. look for a mage or an archer.");
                    } else if (mageCount == 0) {
                        advice.append("you have physical attacks covered, but you need a mage for magical damage.");
                    } else {
                        advice.append("your team is looking balanced! recruit an archer to round out your damage.");
                    }
                }
            } else {
                // branch b: tactical advice mode (party is full)
                if (supportCount >= 2) {
                    advice.append("your party has a surplus of support classes. you will be incredibly difficult to kill, but your overall damage output will be very low. focus on outlasting your enemies!");
                } else if (supportCount == 0) {
                    advice.append("you have a full party of pure damage! you will melt enemies quickly, but without a paladin or healer, you need to bring plenty of health potions and dodge well.");
                } else if (mageCount == 0 && archerCount == 0) {
                    advice.append("your team is very melee-heavy. you might struggle against flying enemies or mechanics that require spreading out.");
                } else if (warriorCount == 0) {
                    advice.append("your team is almost entirely ranged! you have no frontline to hold aggro, so be prepared to kite enemies constantly.");
                } else {
                    advice.append("your team composition is perfectly balanced! you have the ideal mix of frontline presence, support, and ranged damage. you are ready for any dungeon.");
                }
            }

            return advice.toString();

        } catch (SQLException e) {
            return "database error: " + e.getMessage();
        } finally {
            // safely close resources without severing the physical database connection
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            DBConnection.closeConnection(conn);
        }
    }
}