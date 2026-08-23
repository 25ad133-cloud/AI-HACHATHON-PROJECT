import com.sun.net.httpserver.HttpServer;
import controller.ChatHandler;
import database.DatabaseManager;
import service.KnowledgeBaseService;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== College AI Chatbot Backend Startup ===");

        // 1. Initialize SQLite Database Schema
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

        // 2. Initialize Knowledge Base Service
        KnowledgeBaseService kbService = new KnowledgeBaseService();

        // 3. Start HTTP Server
        int port = 8080;
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Register Chat Controller endpoint
            server.createContext("/api/chat", new ChatHandler(kbService));
            
            // Use multi-threaded executor for handling concurrent requests
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            server.start();
            System.out.println("Backend Server is successfully running at http://localhost:" + port);
            System.out.println("Endpoint: POST http://localhost:" + port + "/api/chat");
            System.out.println("Press Ctrl+C to stop.");
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server on port " + port);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
