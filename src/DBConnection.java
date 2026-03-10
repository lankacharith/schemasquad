import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Added ?sslmode=require to the end of the URL
    private static final String URL      = "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String USER     = "postgres.pmpbhslcpywhqpmcflmw";
    private static final String PASSWORD = "TheSchemaSquad123";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: PostgreSQL JDBC Driver not found! Make sure the .jar file is in this directory.");
            e.printStackTrace();
            return null;
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}