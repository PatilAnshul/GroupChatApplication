package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("Attempting to connect to database...");
                System.out.println("URL: " + DatabaseConfig.getUrl());
                System.out.println("Username: " + DatabaseConfig.getUsername());
                
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
                );
                System.out.println("Database connected successfully!");
                
                // Test the connection with a simple query
                connection.createStatement().executeQuery("SELECT 1");
                System.out.println("Database connection test successful!");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace(); // Print the full stack trace
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Create users table if not exists
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL" +
                ")";
            
            // Create chat_history table if not exists
            String createChatHistoryTable = "CREATE TABLE IF NOT EXISTS chat_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "sender VARCHAR(50) NOT NULL," +
                "message TEXT NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (sender) REFERENCES users(username)" +
                ")";

            // Create files table if not exists
            String createFilesTable = "CREATE TABLE IF NOT EXISTS files (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "filename VARCHAR(255) NOT NULL," +
                "content LONGBLOB NOT NULL," +
                "sender VARCHAR(50) NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (sender) REFERENCES users(username)" +
                ")";

            Statement stmt = conn.createStatement();
            stmt.execute(createUsersTable);
            stmt.execute(createChatHistoryTable);
            stmt.execute(createFilesTable);
            
            System.out.println("Database tables initialized successfully");
        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }
} 