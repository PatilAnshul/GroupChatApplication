package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;
import database.DatabaseConnection;
import java.util.Base64;
import java.util.Random;
public class ChatServer {
    private static final int PORT = 5000;
    private static HashSet < PrintWriter > clients = new HashSet < > ();
    private static HashSet < String > usernames = new HashSet < > ();
    private static HashMap < String, PrintWriter > userWriters = new HashMap < > ();
    private static HashMap < String, byte[] > fileStore = new HashMap < > ();
    private static HashMap < String, String > verificationCodes = new HashMap < > ();
    public static void main(String[] args) {
        // Initialize database tables
        DatabaseConnection.initializeDatabase();

        System.out.println("Chat Server is running...");
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(PORT);
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Error in the server: " + e.getMessage());
        } finally {
            try {
                if (listener != null) {
                    listener.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing server: " + e.getMessage());
            }
        }
    }
    private static class ClientHandler extends Thread {
        private String username;
        private Socket socket;
        private BufferedReader in ;
        private PrintWriter out;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                while (true) {
                    String command = in.readLine();
                    if (command == null) {
                        return;
                    }
                    if (command.startsWith("REGISTER")) {
                        handleRegistration(command);
                    } else if (command.startsWith("LOGIN")) {
                        handleLogin(command);
                    } else if (command.startsWith("VERIFY")) {
                        handleVerification(command);
                    } else if (command.startsWith("MESSAGE")) {
                        handleMessage(command);
                    } else if (command.startsWith("FILE")) {
                        handleFile(command);
                    } else if (command.startsWith("IMAGE")) {
                        handleImage(command);
                    } else if (command.startsWith("DOWNLOAD")) {
                        handleDownload(command);
                    } else if (command.startsWith("TYPING_START") ||
                        command.startsWith("TYPING_STOP")) {
                        handleTyping(command);
                    } else if (command.startsWith("LOAD_HISTORY")) {
                        handleLoadHistory();
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (username != null) {
                    usernames.remove(username);
                    userWriters.remove(username);
                    clients.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
        private void handleRegistration(String command) throws IOException {
            String[] parts = command.split(" ");
            if (parts.length != 3) {
                out.println("REGISTER_FAILED Invalid format");
                return;
            }
            String username = parts[1];
            String password = parts[2];
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check if username already exists
                String checkSql = "SELECT username FROM users WHERE username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    out.println("REGISTER_FAILED Username already exists");
                    return;
                }
                // Insert new user
                String insertSql = "INSERT INTO users (username, password) VALUES ( ? , ? )";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                out.println("REGISTER_SUCCESS");
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                out.println("REGISTER_FAILED Database error");
            }
        }
        private void handleLogin(String command) throws IOException {
            String[] parts = command.split(" ");
            if (parts.length != 3) {
                out.println("LOGIN_FAILED Invalid format");
                return;
            }
            String username = parts[1];
            String password = parts[2];
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT * FROM users WHERE username = ? AND password = ? ";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Generate verification code
                    String verificationCode = generateVerificationCode();
                    verificationCodes.put(username, verificationCode);

                    // Send verification code to client
                    out.println("VERIFICATION_CODE " + verificationCode);
                    System.out.println("Generated verification code for " +
                        username + ": " + verificationCode);
                } else {
                    out.println("LOGIN_FAILED Invalid credentials");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                out.println("LOGIN_FAILED Database error");
            }
        }
        private void handleVerification(String command) throws IOException {
            String[] parts = command.split(" ");
            if (parts.length != 3) {
                out.println("VERIFY_FAILED Invalid format");
                return;
            }
            String username = parts[1];
            String code = parts[2];
            String storedCode = verificationCodes.get(username);
            if (storedCode != null && storedCode.equals(code)) {
                this.username = username;
                usernames.add(username);
                userWriters.put(username, out);
                clients.add(out);
                verificationCodes.remove(username);
                out.println("VERIFY_SUCCESS");
            } else {
                out.println("VERIFY_FAILED Invalid code");
            }
        }
        private String generateVerificationCode() {
            Random random = new Random();
            int code = 100000 + random.nextInt(900000);
            return String.valueOf(code);
        }
        private void handleMessage(String command) throws IOException {
            if (username == null) {
                out.println("ERROR Not logged in");
                return;
            }
            String message = command.substring(7);

            // Save message to database
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First get the user's ID
                String getUserIdSql = "SELECT id FROM users WHERE username = ?";
                PreparedStatement userStmt = conn.prepareStatement(getUserIdSql);
                userStmt.setString(1, username);
                ResultSet rs = userStmt.executeQuery();

                if (rs.next()) {
                    int senderId = rs.getInt("id");

                    // Now insert the message with the correct sender_id
                    String sql = "INSERT INTO messages (sender_id, content) VALUES( ? , ? )";
                    PreparedStatement msgStmt = conn.prepareStatement(sql);
                    msgStmt.setInt(1, senderId);
                    msgStmt.setString(2, message);
                    msgStmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("Error saving message: " + e.getMessage());
                e.printStackTrace();
            }
            // Broadcast message to all clients
            for (PrintWriter writer: clients) {
                writer.println("MESSAGE " + username + ": " + message);
            }
        }
        private void handleFile(String command) throws IOException {
            if (username == null) {
                out.println("ERROR Not logged in");
                return;
            }
            String[] parts = command.substring(5).split(" ", 2);
            if (parts.length == 2) {
                String fileName = parts[0];
                int fileSize = Integer.parseInt(parts[1]);

                // Read file content
                String encodedContent = in.readLine();
                if (encodedContent != null) {
                    byte[] content = Base64.getDecoder().decode(encodedContent);

                    // Store file in database
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "INSERT INTO files (filename, content, sender) VALUES( ? , ? , ? )";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, fileName);
                    stmt.setBytes(2, content);
                    stmt.setString(3, username);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Error saving file: " +
                        e.getMessage());
                }
                // Store file in memory
                fileStore.put(fileName, content);

                // Broadcast file message to all clients
                for (PrintWriter writer: clients) {
                    writer.println("FILE " + username + " " + fileName + " " +
                        fileSize);
                    writer.println(encodedContent);
                }
            }
        }
    }
    private void handleImage(String command) throws IOException {
        if (username == null) {
            out.println("ERROR Not logged in");
            return;
        }
        String[] parts = command.substring(6).split(" ", 2);
        if (parts.length == 2) {
            String fileName = parts[0];
            int fileSize = Integer.parseInt(parts[1]);

            // Read image content
            String encodedContent = in.readLine();
            if (encodedContent != null) {
                byte[] content = Base64.getDecoder().decode(encodedContent);

                // Store image in database
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO files (filename, content,sender) VALUES( ? , ? , ? )";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, fileName);
                stmt.setBytes(2, content);
                stmt.setString(3, username);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error saving image: " +
                    e.getMessage());
            }
            // Store image in memory
            fileStore.put(fileName, content);

            // Broadcast image to all clients
            for (PrintWriter writer: clients) {
                writer.println("IMAGE " + username + " " + fileName + " " +
                    fileSize);
                writer.println(encodedContent);
            }
        }
    }
}
private void handleDownload(String command) throws IOException {
    if (username == null) {
        out.println("ERROR Not logged in");
        return;
    }
    String fileName = command.substring(9);
    byte[] content = fileStore.get(fileName);

    if (content != null) {
        String encodedContent =
            Base64.getEncoder().encodeToString(content);
        out.println(encodedContent);
    } else {
        out.println("ERROR File not found");
    }
}
private void handleTyping(String command) throws IOException {
    if (username == null) {
        return;
    }
    String typingUser = command.substring(command.indexOf(" ") + 1);
    for (PrintWriter writer: clients) {
        if (writer != out) { // Don't send to the typing user
            writer.println(command);
        }
    }
}
private void handleLoadHistory() throws IOException {
    if (username == null) {
        out.println("ERROR Not logged in");
        return;
    }
    try (Connection conn = DatabaseConnection.getConnection()) {
        String sql = "SELECT u.username, m.content, m.created_at " +
            "FROM messages m " +
            "JOIN users u ON m.sender_id = u.id " +
            "ORDER BY m.created_at ASC";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String sender = rs.getString("username");
            String message = rs.getString("content");
            String timestamp = rs.getTimestamp("created_at").toString();
            out.println("HISTORY " + sender + " " + timestamp + " " +
                message);
        }
    } catch (SQLException e) {
        System.out.println("Error loading chat history: " +
            e.getMessage());
        out.println("ERROR Failed to load chat history");
    }
}
}
}