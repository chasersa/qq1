package com.simplechat.server;

import com.simplechat.common.Message;
import com.simplechat.common.MessageType;
import com.simplechat.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String userId;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = (Message) ois.readObject();
                System.out.println("Received message from client: " + message);

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
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client " + userId + " disconnected.");
            if (userId != null) {
                server.removeClient(userId);
            }
        } finally {
            try {
                if (ois != null) ois.close();
                if (oos != null) oos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLogin(Message message) throws IOException {
        String[] credentials = message.getContent().split(",");
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
            sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "Invalid ID or password."));
        }
    }

    private void handleRegister(Message message) throws IOException {
        String[] userInfo = message.getContent().split(",");
        String id = userInfo[0];
        String username = userInfo[1];
        String password = userInfo[2];

        if (server.getUserManager().registerUser(id, username, password)) {
            sendMessage(new Message(MessageType.REGISTER_SUCCESS, "Server", id, "Registration successful."));
        } else {
            sendMessage(new Message(MessageType.REGISTER_FAIL, "Server", id, "ID already exists."));
        }
    }

    private void handleChatMessage(Message message) {
        // 广播聊天消息给所有在线用户（除了发送者）
        for (ClientHandler handler : server.getOnlineClients().values()) {
            if (!handler.getUserId().equals(message.getSenderId())) {
                try {
                    handler.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Error forwarding message to " + handler.getUserId() + ": " + e.getMessage());
                }
            }
        }
    }

    public void sendMessage(Message message) throws IOException {
        oos.writeObject(message);
        oos.flush();
    }
}