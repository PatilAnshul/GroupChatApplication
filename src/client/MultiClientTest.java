package client;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiClientTest {
    public static void main(String[] args) {
        // Create a thread pool to manage multiple clients
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Launch multiple clients
        for (int i = 1; i <= 3; i++) {
            final int clientNumber = i;
            executor.submit(() -> {
                // Use SwingUtilities.invokeLater to ensure GUI creation happens on EDT
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Set a different look and feel for each client to distinguish them
                        if (clientNumber == 1) {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } else if (clientNumber == 2) {
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                        } else {
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                        }
                        
                        // Create and show the login window
                        LoginWindow loginWindow = new LoginWindow();
                        loginWindow.setTitle("Client " + clientNumber + " - Login");
                        
                        // Position windows in different locations
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        loginWindow.setLocation(
                            (clientNumber - 1) * 300,  // X position
                            100  // Y position
                        );
                        
                        loginWindow.setVisible(true);
                        
                        // Add a window listener to handle cleanup
                        loginWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                                System.out.println("Client " + clientNumber + " closed");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        }
        
        // Add a shutdown hook to clean up the executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            System.out.println("All clients closed. Shutting down...");
        }));
    }
} 