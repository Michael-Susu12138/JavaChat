package com.chatserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatServer {
    private int port;
    private Set<UserThread> userThreads = new HashSet<>();
    Connection connection;

    public ChatServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            connectDatabase(); // Ensure the database connection

            while (true) {
                Socket socket = serverSocket.accept();
                UserThread newUser = new UserThread(socket, this);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException ex) {
            System.err.println("Error in the server: " + ex.getMessage());
        }
    }

    private void connectDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mariadb://localhost:8000/chat_db", "root", "password");
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(5190);
        server.execute();
    }

    void broadcast(String message, UserThread excludeUser) {
        for (UserThread aUser : userThreads) {
            aUser.sendMessage(message);
        }
    }

    boolean logUserConnection(String username, String ip) {
        String sql = "INSERT INTO logins(username, ip_address, login_time) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, ip);
            statement.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error logging user connection: " + e.getMessage());
            return false;
        }
    }
}

class UserThread extends Thread {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;

    public UserThread(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            String userName = reader.readLine();
            String password = reader.readLine();
            String clientIp = socket.getInetAddress().getHostAddress();

            if (authenticate(userName, password)) {
                server.logUserConnection(userName, clientIp);
                writer.println("200");  // success code

                String clientMessage;
                do {
                    clientMessage = reader.readLine();
                    if (clientMessage != null) {
                        String serverMessage = userName + ": " + clientMessage;
                        server.broadcast(serverMessage, this);
                    }
                } while (clientMessage != null && !clientMessage.equals("bye"));

                server.broadcast(userName + " has left.", this);
                socket.close();
            } else {
                writer.println("500");  // failed code
                socket.close();
            }
        } catch (IOException ex) {
            System.err.println("Error in UserThread: " + ex.getMessage());
        }
    }

    void sendMessage(String message) {
        writer.println(message);
    }

    private boolean authenticate(String userName, String password) {
        try (PreparedStatement statement = server.connection.prepareStatement(
            "SELECT * FROM users WHERE username = ? AND password = ?")) {
            statement.setString(1, userName);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Database authentication error: " + e.getMessage());
            return false;
        }
    }
}
