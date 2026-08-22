import database.DatabaseManager;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== CertiTrace Backend Startup ===");
        
        // Initialize Database Schema
        DatabaseManager.initializeDatabase();

        // Verify tables
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseMetaData dbmd = conn.getMetaData();
            try (ResultSet rs = dbmd.getTables(null, null, "%", new String[]{"TABLE"})) {
                System.out.println("Verified tables in database:");
                while (rs.next()) {
                    System.out.println(" - " + rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            System.err.println("Database verification failed!");
            e.printStackTrace();
        }
        
        System.out.println("Phase 1 initialization complete!");
    }
}
