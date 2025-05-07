# Group Chat Application

A Java-based group chat application using Java Swing for the client interface, Socket programming for server-client communication, and MySQL for data storage.

## Features

- User registration and login
- Real-time group chat
- Group management
- Message history
- Modern Java Swing UI

## Prerequisites

- Java JDK 8 or higher
- MySQL Server 5.7 or higher
- MySQL Connector/J (JDBC driver)

## Setup

1. Clone the repository
2. Create the database and tables:
   - Open MySQL command line or MySQL Workbench
   - Run the SQL script in `src/database/schema.sql`

3. Configure the database connection:
   - Open `src/database/DatabaseConfig.java`
   - Update the database URL, username, and password according to your MySQL setup

4. Add the MySQL Connector/J to your project's classpath:
   - Download MySQL Connector/J from the official MySQL website
   - Add the JAR file to your project's libraries

## Running the Application

1. Start the server:
   ```bash
   javac src/server/ChatServer.java
   java -cp src server.ChatServer
   ```

2. Start the client:
   ```bash
   javac src/Main.java
   java -cp src Main
   ```

3. You can start multiple client instances to simulate different users.

## Project Structure

- `src/client/` - Client-side code
  - `LoginWindow.java` - Login and registration interface
  - `ChatWindow.java` - Main chat interface
- `src/server/` - Server-side code
  - `ChatServer.java` - Server implementation
- `src/database/` - Database-related code
  - `DatabaseConfig.java` - Database configuration
  - `DatabaseConnection.java` - Database connection utility
  - `schema.sql` - Database schema

## Security Notes

- Passwords are stored in plain text in this basic implementation
- For production use, implement proper password hashing
- Add SSL/TLS for secure communication
- Implement proper session management
- Add input validation and sanitization

## Contributing

Feel free to submit issues and enhancement requests! 