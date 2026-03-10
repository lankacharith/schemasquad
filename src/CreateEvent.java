import java.sql.*;
import java.util.Scanner;
// creates a new in-game event. admin level function.
public class CreateEvent {

    public static void Client_CreateEvent(Scanner scanner) {

        System.out.println("\n--- Create Event (Admin) ---");

        System.out.print("Enter event name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Enter reward XP (just the number): ");
        String xpInput = scanner.nextLine().trim();

        int rewardXP;
        try {
            rewardXP = Integer.parseInt(xpInput);
        } catch (NumberFormatException e) {
            System.out.println("Error: XP must be a number.");
            return;
        }

        String result = serverCreateEvent(name, description, rewardXP);
        System.out.println(result);
    }

    public static String serverCreateEvent(String name, String description, int rewardXP) {
        
        String sql = 
            "INSERT INTO Events (\n" +
            "    Name,\n" +
            "    Description,\n" +
            "    RewardXP\n" +
            ") \n" +
            "VALUES (\n" +
            "    ?,\n" +
            "    ?,\n" +
            "    ?\n" +
            ")";

        // try-with-resources automatically closes the Connection and PreparedStatement
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, rewardXP);

            stmt.executeUpdate();

            return "Success: Event '" + name + "' created with " + rewardXP + " XP reward.";

        } catch (SQLException e) {
            // "23505" is the standard PostgreSQL state code for a unique_violation
            if ("23505".equals(e.getSQLState())) {
                return "Unique Violation! The database says: " + e.getMessage();
            }
            return "Database error: " + e.getMessage();
        }
    }
}