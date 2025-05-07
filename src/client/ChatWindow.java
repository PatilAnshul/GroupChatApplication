package client;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.border.EmptyBorder;
//import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.BadLocationException;

import java.util.HashMap;
import java.util.Map;

public class ChatWindow extends JFrame {
 private JTextPane chatArea;
 private JTextField messageField;
 private JButton sendButton;
 private JButton loadHistoryButton;
 private String username;
 private PrintWriter out;
 private BufferedReader in;
 private Socket socket;
 private boolean isTyping = false;
 private Timer typingTimer;

 private String replyToMessage = null; // stores original message
 private JLabel replyLabel; // shows reply preview

 // Map to track message references (could be improved later)
 private Map<Integer, String> messageMap = new HashMap<>();
 private int messageCounter = 0;

 public ChatWindow(String username, Socket socket, PrintWriter out, BufferedReader in) {
     this.username = username;
     this.socket = socket;
     this.out = out;
     this.in = in;

     setTitle("Group Chat - " + username);
     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     setSize(800, 600);
     setLocationRelativeTo(null);

     JPanel mainPanel = new JPanel(new BorderLayout());
     mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

     // Header Panel
     JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
     headerPanel.setBackground(new Color(0, 128, 105));
     headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

     JLabel titleLabel = new JLabel("Group Chat");
     titleLabel.setForeground(Color.WHITE);
     titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
     headerPanel.add(titleLabel);

     JLabel userLabel = new JLabel("Logged in as: " + username);
     userLabel.setForeground(Color.WHITE);
     headerPanel.add(userLabel);

     loadHistoryButton = new JButton("Load History");
     loadHistoryButton.setBackground(Color.WHITE);
     loadHistoryButton.setForeground(new Color(0, 128, 105));
     headerPanel.add(loadHistoryButton);

     mainPanel.add(headerPanel, BorderLayout.NORTH);

     // Chat area
     chatArea = new JTextPane();
     chatArea.setEditable(false);
     chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
     JScrollPane scrollPane = new JScrollPane(chatArea);
     mainPanel.add(scrollPane, BorderLayout.CENTER);

     // Input panel
     JPanel inputPanel = new JPanel(new BorderLayout());
     inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

     // Reply preview panel
     replyLabel = new JLabel();
     replyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
     replyLabel.setForeground(new Color(0, 128, 105));
     replyLabel.setVisible(false);

     JButton cancelReply = new JButton("x");
     cancelReply.setForeground(Color.RED);
     cancelReply.setFocusPainted(false);
     cancelReply.setMargin(new Insets(0, 5, 0, 5));
     cancelReply.setBorder(null);
     cancelReply.setContentAreaFilled(false);
     cancelReply.setCursor(new Cursor(Cursor.HAND_CURSOR));
     cancelReply.addActionListener(e -> {
         replyToMessage = null;
         replyLabel.setVisible(false);
     });

     JPanel replyPanel = new JPanel(new BorderLayout());
     replyPanel.add(replyLabel, BorderLayout.CENTER);
     replyPanel.add(cancelReply, BorderLayout.EAST);
     replyPanel.setVisible(true);
     inputPanel.add(replyPanel, BorderLayout.NORTH);

     messageField = new JTextField();
     inputPanel.add(messageField, BorderLayout.CENTER);

     sendButton = new JButton("Send");
     inputPanel.add(sendButton, BorderLayout.EAST);

     mainPanel.add(inputPanel, BorderLayout.SOUTH);
     add(mainPanel);

     // Event Listeners
     sendButton.addActionListener(e -> sendMessage());
     messageField.addActionListener(e -> sendMessage());
     loadHistoryButton.addActionListener(e -> loadChatHistory());

     // Typing detection
     messageField.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) { handleTyping(); }
         public void removeUpdate(DocumentEvent e) { handleTyping(); }
         public void changedUpdate(DocumentEvent e) { handleTyping(); }
     });

     // Right click listener for reply
     chatArea.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
             if (SwingUtilities.isRightMouseButton(e)) {
                 int pos = chatArea.viewToModel2D(e.getPoint());
                 try {
                     int start = Utilities.getRowStart(chatArea, pos);
                     int end = Utilities.getRowEnd(chatArea, pos);
                     String selectedLine = chatArea.getDocument().getText(start, end - start);
                     replyToMessage = selectedLine.trim();
                     replyLabel.setText("Replying to: " + replyToMessage);
                     replyLabel.setVisible(true);
                 } catch (Exception ex) {
                     ex.printStackTrace();
                 }
             }
         }
     });

     // Start message listener thread
     new Thread(this::listenForMessages).start();
 }

 private void handleTyping() {
     if (!isTyping) {
         isTyping = true;
         out.println("TYPING_START " + username);
     }
     if (typingTimer != null) typingTimer.stop();
     typingTimer = new Timer(1000, e -> {
         isTyping = false;
         out.println("TYPING_STOP " + username);
     });
     typingTimer.setRepeats(false);
     typingTimer.start();
 }

 private void sendMessage() {
     String message = messageField.getText().trim();
     if (!message.isEmpty()) {
         if (replyToMessage != null) {
             message = "[Reply to: " + replyToMessage + "] " + message;
             replyToMessage = null;
             replyLabel.setVisible(false);
         }
         out.println("MESSAGE " + message);
         messageField.setText("");
     }
 }

 private void appendMessage(String sender, String message, boolean isUser) {
     StyledDocument doc = chatArea.getStyledDocument();
     SimpleAttributeSet style = new SimpleAttributeSet();
     StyleConstants.setAlignment(style, isUser ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
     StyleConstants.setForeground(style, isUser ? new Color(0, 128, 105) : Color.BLACK);
     StyleConstants.setBold(style, true);
     try {
         doc.setParagraphAttributes(doc.getLength(), 1, style, false);
         doc.insertString(doc.getLength(), sender + "\n", style);
         StyleConstants.setBold(style, false);
         doc.insertString(doc.getLength(), message + "\n\n", style);
         chatArea.setCaretPosition(doc.getLength());

         messageMap.put(messageCounter++, message); // store for reply tracking
     } catch (BadLocationException e) {
         e.printStackTrace();
     }
 }

 private void appendSystemMessage(String message) {
     StyledDocument doc = chatArea.getStyledDocument();
     SimpleAttributeSet style = new SimpleAttributeSet();
     StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
     StyleConstants.setForeground(style, Color.white);
     StyleConstants.setItalic(style, true);
     try {
         doc.insertString(doc.getLength(), message + "\n\n", style);
         chatArea.setCaretPosition(doc.getLength());
     } catch (BadLocationException e) {
         e.printStackTrace();
     }
 }

 private void appendHistoryMessage(String sender, String timestamp, String content) {
     StyledDocument doc = chatArea.getStyledDocument();
     SimpleAttributeSet style = new SimpleAttributeSet();
     StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
     StyleConstants.setForeground(style, Color.GRAY);
     try {
         doc.insertString(doc.getLength(), "[" + timestamp + "] " + sender + ": " + content + "\n", style);
         chatArea.setCaretPosition(doc.getLength());
     } catch (BadLocationException e) {
         e.printStackTrace();
     }
 }

 private void loadChatHistory() {
     out.println("LOAD_HISTORY");
 }

 private void listenForMessages() {
     try {
         String message;
         while ((message = in.readLine()) != null) {
             final String finalMessage = message;
             SwingUtilities.invokeLater(() -> {
                 if (finalMessage.startsWith("MESSAGE ")) {
                     String content = finalMessage.substring(8);
                     String[] parts = content.split(": ", 2);
                     if (parts.length == 2) {
                         String sender = parts[0];
                         String messageText = parts[1];
                         appendMessage(sender, messageText, sender.equals(username));
                     }
                 } else if (finalMessage.startsWith("TYPING_START ")) {
                     String typingUser = finalMessage.substring(13);
                     if (!typingUser.equals(username)) {
                         appendSystemMessage(typingUser + " is typing...");
                     }
                 } else if (finalMessage.startsWith("TYPING_STOP ")) {
                     String stoppedUser = finalMessage.substring(12);
                     if (!stoppedUser.equals(username)) {
                         clearTypingMessage(stoppedUser);
                     }
                 } else if (finalMessage.startsWith("HISTORY ")) {
                     String[] parts = finalMessage.substring(8).split(" ", 3);
                     if (parts.length == 3) {
                         String sender = parts[0];
                         String timestamp = parts[1];
                         String content = parts[2];
                         appendHistoryMessage(sender, timestamp, content);
                     }
                 }
             });
         }
     } catch (IOException e) {
         SwingUtilities.invokeLater(() -> {
             JOptionPane.showMessageDialog(this, "Connection lost: " + e.getMessage());
             System.exit(0);
         });
     }
 }

 private void clearTypingMessage(String username) {
     StyledDocument doc = chatArea.getStyledDocument();
     try {
         String text = doc.getText(0, doc.getLength());
         String typingMessage = username + " is typing...\n\n";
         int index = text.lastIndexOf(typingMessage);
         if (index != -1) {
             doc.remove(index, typingMessage.length());
         }
     } catch (BadLocationException e) {
         e.printStackTrace();
     }
 }
}

