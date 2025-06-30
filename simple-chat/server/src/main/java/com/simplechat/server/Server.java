package com.simplechat.server;

import com.simplechat.common.Message;
import com.simplechat.common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 8888;
    private UserManager userManager;
    private Map<String, ClientHandler> onlineClients;

    public Server() {
        userManager = new UserManager();
        onlineClients = new ConcurrentHashMap<>();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }

    public synchronized void addOnlineClient(String userId, ClientHandler handler) {
        onlineClients.put(userId, handler);
        System.out.println("User " + userId + " joined the chat room. Total online: " + onlineClients.size());
        
        // 通知所有用户有新用户加入
        broadcastMessage(new Message(MessageType.USER_JOIN, "Server", "ALL", userId + " joined the chat room"));
        
        // 发送在线用户列表给新用户
        sendUserList(handler);
        
        // 通知所有其他用户更新用户列表
        broadcastUserList();
    }

    public synchronized void removeClient(String userId) {
        onlineClients.remove(userId);
        System.out.println("User " + userId + " left the chat room. Total online: " + onlineClients.size());
        
        // 通知所有用户有用户离开
        broadcastMessage(new Message(MessageType.USER_LEAVE, "Server", "ALL", userId + " left the chat room"));
        
        // 通知所有用户更新用户列表
        broadcastUserList();
    }

    public void broadcastMessage(Message message) {
        for (ClientHandler handler : onlineClients.values()) {
            try {
                handler.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Error broadcasting message: " + e.getMessage());
            }
        }
    }

    public void broadcastUserList() {
        StringBuilder userList = new StringBuilder();
        for (String userId : onlineClients.keySet()) {
            userList.append(userId).append(";");
        }
        if (userList.length() > 0) {
            userList.setLength(userList.length() - 1); // Remove trailing semicolon
        }
        
        Message userListMessage = new Message(MessageType.USER_LIST, "Server", "ALL", userList.toString());
        broadcastMessage(userListMessage);
    }

    private void sendUserList(ClientHandler handler) {
        StringBuilder userList = new StringBuilder();
        for (String userId : onlineClients.keySet()) {
            userList.append(userId).append(";");
        }
        if (userList.length() > 0) {
            userList.setLength(userList.length() - 1); // Remove trailing semicolon
        }
        
        try {
            handler.sendMessage(new Message(MessageType.USER_LIST, "Server", handler.getUserId(), userList.toString()));
        } catch (IOException e) {
            System.err.println("Error sending user list: " + e.getMessage());
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}