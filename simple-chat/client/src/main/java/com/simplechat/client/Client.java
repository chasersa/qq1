package com.simplechat.client;

import com.simplechat.common.Message;
import com.simplechat.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
            // 先创建输出流，再创建输入流
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            isConnected = true;
            
            // 启动消息接收线程
            new Thread(() -> {
                try {
                    while (isConnected && !socket.isClosed()) {
                        try {
                            Message message = (Message) ois.readObject();
                            System.out.println("Client received: " + message);
                            if (messageListener != null) {
                                messageListener.accept(message);
                            }
                        } catch (SocketException e) {
                            if (isConnected) {
                                System.out.println("Server disconnected");
                            }
                            break;
                        } catch (IOException e) {
                            if (isConnected) {
                                System.out.println("Connection error: " + e.getMessage());
                            }
                            break;
                        } catch (ClassNotFoundException e) {
                            System.err.println("Invalid message format: " + e.getMessage());
                        }
                    }
                } finally {
                    disconnect();
                }
            }).start();
            
        } catch (IOException e) {
            isConnected = false;
            cleanup();
            throw e;
        }
    }

    public void disconnect() {
        isConnected = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (oos != null) oos.close();
            if (ois != null) ois.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        if (!isConnected || socket == null || socket.isClosed()) {
            System.err.println("Not connected to server");
            return;
        }
        
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            disconnect();
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