package apis;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Added ?sslmode=require to the end of the URL
    private static final String URL      = "jdbc:postgresql://db.pmpbhslcpywhqpmcflmw.supabase.co:5432/postgres?sslmode=require";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "TheSchemaSquad123";

    public static Connection getConnection() throws SQLException {
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