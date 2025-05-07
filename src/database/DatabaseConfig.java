package database;

public class DatabaseConfig {
    private static final String URL = "jdbc:mysql://localhost:3306/groupchat";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "mysql";

    public static String getUrl() {
        return URL;
    }

    public static String getUsername() {
        return USERNAME;
    }

    public static String getPassword() {
        return PASSWORD;
    }
} 
