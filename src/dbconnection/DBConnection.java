package dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL      = "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String USER     = "postgres.pmpbhslcpywhqpmcflmw";
    private static final String PASSWORD = "TheSchemaSquad123";


    // hold the one single connection in memory
    private static Connection singleConn = null;
    //  neutralize the constructor to prevent creating multiple instances of DBConnection
    public DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        // only make a new network connection if one doesn't exist or got severed
        if (singleConn == null || singleConn.isClosed()) {
            singleConn = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        
        // return the already open connection
        return singleConn;
    }

    public static void closeConnection(Connection conn) {
        // deliberately do nothing here to not break existing APIs.
    }
    
    // only call this when the entire java application is shutting down
    public static void shutdown() {
        if (singleConn != null) {
            try {
                singleConn.close();
            } catch (SQLException e) {
                System.out.println("error closing connection: " + e.getMessage());
            }
        }
    }
}