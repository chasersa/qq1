package com.simplechat.server;

import com.simplechat.common.Message;
import com.simplechat.common.MessageType;
import com.simplechat.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String userId;
    private boolean isRunning = false;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            // 先创建输出流，再创建输入流
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            isRunning = true;
            System.out.println("Client handler initialized for: " + socket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Error initializing client handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void run() {
        try {
            while (isRunning && !socket.isClosed()) {
                try {
                    Message message = (Message) ois.readObject();
                    System.out.println("Received message from client: " + message);
                    handleMessage(message);
                } catch (SocketException e) {
                    System.out.println("Client disconnected: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Client disconnected: " + e.getMessage());
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid message format: " + e.getMessage());
                }
            }
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message message) {
        try {
            switch (message.getType()) {
                case LOGIN:
                    handleLogin(message);
                    break;
                case REGISTER:
                    handleRegister(message);
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;
                default:
                    System.out.println("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanup() {
        isRunning = false;
        if (userId != null) {
            server.removeClient(userId);
        }
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private void handleLogin(Message message) {
        try {
            String[] credentials = message.getContent().split(",");
            if (credentials.length != 2) {
                sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", message.getSenderId(), "Invalid credentials format."));
                return;
            }
            
            String id = credentials[0];
            String password = credentials[1];
            User user = server.getUserManager().login(id, password);

            if (user != null) {
                if (server.getOnlineClients().containsKey(id)) {
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "User already online."));
                    return;
                }
                this.userId = id;
                server.addOnlineClient(id, this);
                sendMessage(new Message(MessageType.LOGIN_SUCCESS, "Server", id, user.getUsername()));
            } else {
                sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", message.getSenderId(), "Invalid ID or password."));
            }
        } catch (Exception e) {
            System.err.println("Error handling login: " + e.getMessage());
            try {
                sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", message.getSenderId(), "Server error."));
            } catch (IOException ioException) {
                System.err.println("Failed to send error message: " + ioException.getMessage());
            }
        }
    }

    private void handleRegister(Message message) {
        try {
            String[] userInfo = message.getContent().split(",");
            if (userInfo.length != 3) {
                sendMessage(new Message(MessageType.REGISTER_FAIL, "Server", message.getSenderId(), "Invalid registration format."));
                return;
            }
            
            String id = userInfo[0];
            String username = userInfo[1];
            String password = userInfo[2];

            if (server.getUserManager().registerUser(id, username, password)) {
                sendMessage(new Message(MessageType.REGISTER_SUCCESS, "Server", id, "Registration successful."));
            } else {
                sendMessage(new Message(MessageType.REGISTER_FAIL, "Server", id, "ID already exists."));
            }
        } catch (Exception e) {
            System.err.println("Error handling registration: " + e.getMessage());
            try {
                sendMessage(new Message(MessageType.REGISTER_FAIL, "Server", message.getSenderId(), "Server error."));
            } catch (IOException ioException) {
                System.err.println("Failed to send error message: " + ioException.getMessage());
            }
        }
    }

    private void handleChatMessage(Message message) {
        // 广播聊天消息给所有在线用户（除了发送者）
        for (ClientHandler handler : server.getOnlineClients().values()) {
            if (handler != null && !handler.getUserId().equals(message.getSenderId())) {
                try {
                    handler.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Error forwarding message: " + e.getMessage());
                }
            }
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (oos != null && !socket.isClosed()) {
            oos.writeObject(message);
            oos.flush();
        }
    }
}