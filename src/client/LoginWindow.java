package client;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in ;
    public LoginWindow() {
        setTitle("Group Chat Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);
        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);
        // Buttons
        JPanel buttonPanel = new JPanel();
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        add(panel);
        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not connect to server: " +
                ex.getMessage());
        }
    }
    private void login() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password ");
                    return;
                }
                try {
                    out.println("LOGIN " + username + " " + password);
                    String response = in.readLine();

                    if (response.startsWith("VERIFICATION_CODE ")) {
                        String verificationCode = response.substring(17);
                        showVerificationDialog(username, verificationCode);
                    } else if (response.startsWith("LOGIN_FAILED")) {
                        JOptionPane.showMessageDialog(this, "Login failed: " +
                            response.substring(12));
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error during login: " +
                        ex.getMessage());
                }
            }
            private void showVerificationDialog(String username, String verificationCode)
                {
                    JDialog verificationDialog = new JDialog(this, "Two-Factor Authentication ", true);
                        verificationDialog.setSize(300, 200); verificationDialog.setLocationRelativeTo(this); verificationDialog.setLayout(new GridBagLayout()); verificationDialog.getContentPane().setBackground(Color.WHITE);

                        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5, 5, 5, 5);
                        // Verification code label
                        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; JLabel codeLabel = new JLabel("Enter verification code :"); codeLabel.setForeground(Color.BLACK); verificationDialog.add(codeLabel, gbc);
                        // Code field
                        gbc.gridy = 1; JTextField codeField = new JTextField(15); codeField.setBackground(Color.WHITE); codeField.setForeground(Color.BLACK); codeField.setOpaque(true); verificationDialog.add(codeField, gbc);
                        // Verify button
                        gbc.gridy = 2; JButton verifyButton = new JButton("Verify"); verifyButton.setBackground(new Color(0, 128, 105)); // WhatsApp green
                        verifyButton.setForeground(Color.WHITE); verifyButton.setFont(new Font("Arial", Font.BOLD, 12)); verifyButton.setBorderPainted(false); verifyButton.setFocusPainted(false); verifyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

                        // Add hover effect to verify button
                        verifyButton.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                verifyButton.setBackground(new Color(0, 150, 120));
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                                verifyButton.setBackground(new Color(0, 128, 105));
                            }
                        });

                        verificationDialog.add(verifyButton, gbc); verifyButton.addActionListener(e -> {
                            String enteredCode = codeField.getText();
                            try {
                                out.println("VERIFY " + username + " " + enteredCode);
                                String response = in.readLine();

                                if (response.startsWith("VERIFY_SUCCESS")) {
                                    new ChatWindow(username, socket, out, in).setVisible(true);
                                    verificationDialog.dispose();
                                    this.dispose();
                                } else {
                                    JOptionPane.showMessageDialog(verificationDialog,
                                        "Invalid verification code. Please try again.");
                                }
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(verificationDialog,
                                    "Error during verification: " + ex.getMessage());
                            }
                        }); verificationDialog.setVisible(true);
                    }
                    private void register() {
                            String username = usernameField.getText();
                            String password = new String(passwordField.getPassword());
                            if (username.isEmpty() || password.isEmpty()) {
                                JOptionPane.showMessageDialog(this, "Please enter both username and password ");
                                    return;
                                }
                                try {
                                    out.println("REGISTER " + username + " " + password);
                                    String response = in.readLine();

                                    if (response.startsWith("REGISTER_SUCCESS")) {
                                        JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
                                            passwordField.setText("");
                                        }
                                        else {
                                            JOptionPane.showMessageDialog(this, "Registration failed: " +
                                                response);
                                        }
                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(this, "Error during registration: " +
                                            ex.getMessage());
                                    }
                                }
                            }