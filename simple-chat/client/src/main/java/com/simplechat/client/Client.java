package com.simplechat.client;

import com.simplechat.common.Message;
import com.simplechat.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8888;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private User currentUser;
    private Consumer<Message> messageListener;
    private boolean isConnected = false;

    public Client() {
    }

    public void connect() throws IOException {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            isConnected = true;
            
            System.out.println("Connected to server successfully");

            new Thread(() -> {
                try {
                    while (isConnected && !socket.isClosed()) {
                        Message message = (Message) ois.readObject();
                        System.out.println("Client received: " + message);
                        if (messageListener != null) {
                            messageListener.accept(message);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (isConnected) {
                        System.out.println("Server disconnected or error: " + e.getMessage());
                        e.printStackTrace();
                    }
                    disconnect();
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            throw e;
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (oos != null) oos.close();
            if (ois != null) ois.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        if (!isConnected || socket == null || socket.isClosed()) {
            System.err.println("Not connected to server. Cannot send message.");
            return;
        }
        
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setMessageListener(Consumer<Message> listener) {
        this.messageListener = listener;
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}