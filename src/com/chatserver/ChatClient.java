package com.chatserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private JFrame frame = new JFrame("Chat Client");
    private JTextArea textArea = new JTextArea(20, 40);
    private JTextField textField = new JTextField(40);
    private JButton sendButton = new JButton("Send");
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket socket;

    public ChatClient() {
        textField.setEditable(true);
        textArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void connectToServer() {
        String serverAddress = JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chat",
            JOptionPane.QUESTION_MESSAGE);

        String username = JOptionPane.showInputDialog(
            frame,
            "Enter your username:",
            "Username Required",
            JOptionPane.PLAIN_MESSAGE);

        String password = JOptionPane.showInputDialog(
            frame,
            "Enter your password:",
            "Password Required",
            JOptionPane.PLAIN_MESSAGE);

        try {
            socket = new Socket(serverAddress, 5190);
            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            // send usrname and pwd
            writer.println(username);
            writer.println(password);

            // response from server
            String serverResponse = reader.readLine();
            if ("200".equals(serverResponse)) {
                textArea.append("Connected successfully.\n");
            } else {
                textArea.append("Authentication failed.\n");
                throw new IOException("Authentication failed");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void sendMessage() {
        String message = textField.getText().trim();
        if (!message.isEmpty()) {
            writer.println(message); // msg to server
            textField.setText("");
        }
    }


    private void run() {
        connectToServer();

        String fromServer;
        try {
            while ((fromServer = reader.readLine()) != null) {
                textArea.append(fromServer + "\n");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Server connection lost.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.run();
    }
}
