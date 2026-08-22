package database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:database/certitrace.db";
    private static final String SCHEMA_PATH = "database/schema.sql";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found in classpath!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // SQLite foreign keys are disabled by default. We must enable them.
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initializeDatabase() {
        System.out.println("Initializing SQLite database...");
        try {
            // Ensure parent directory exists
            Files.createDirectories(Paths.get("database"));
        } catch (Exception e) {
            System.err.println("Could not create database directory: " + e.getMessage());
        }

        try (Connection conn = getConnection()) {
            if (!Files.exists(Paths.get(SCHEMA_PATH))) {
                System.err.println("Schema SQL file not found at: " + SCHEMA_PATH);
                return;
            }

            String schemaSql = Files.readAllLines(Paths.get(SCHEMA_PATH)).stream()
                    .map(line -> {
                        int commentIdx = line.indexOf("--");
                        if (commentIdx >= 0) {
                            return line.substring(0, commentIdx);
                        }
                        return line;
                    })
                    .collect(Collectors.joining("\n"));

            // Split the script by semicolons
            String[] queries = schemaSql.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String query : queries) {
                    String trimmedQuery = query.trim();
                    if (!trimmedQuery.isEmpty()) {
                        stmt.execute(trimmedQuery);
                    }
                }
            }
            System.out.println("Database tables and schema initialized successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization failed!");
            e.printStackTrace();
        }
    }
}
