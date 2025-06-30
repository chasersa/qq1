package com.simplechat.client;

import com.simplechat.common.Message;
import com.simplechat.common.MessageType;
import com.simplechat.common.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginWindow extends JFrame {
    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private Client client;

    public LoginWindow(Client client) {
        this.client = client;
        setTitle("简易聊天室 - 登录");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(panel);

        panel.add(new JLabel("用户ID:"));
        idField = new JTextField(15);
        panel.add(idField);

        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        loginButton = new JButton("登录");
        panel.add(loginButton);

        registerButton = new JButton("注册");
        panel.add(registerButton);

        loginButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (id.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID和密码不能为空！");
                return;
            }
            
            // 禁用按钮防止重复点击
            loginButton.setEnabled(false);
            
            try {
                if (!client.isConnected()) {
                    client.connect();
                    // 等待一小段时间确保连接建立
                    Thread.sleep(100);
                }
                client.sendMessage(new Message(MessageType.LOGIN, id, "Server", id + "," + password));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败，请确保服务器已启动: " + ex.getMessage());
                loginButton.setEnabled(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                loginButton.setEnabled(true);
            }
        });

        registerButton.addActionListener(e -> {
            RegisterWindow registerWindow = new RegisterWindow(client, this);
            registerWindow.setVisible(true);
            this.setVisible(false);
        });

        // 回车键登录
        passwordField.addActionListener(e -> loginButton.doClick());

        client.setMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.LOGIN_SUCCESS) {
                    User loggedInUser = new User(message.getReceiverId(), message.getContent(), "");
                    loggedInUser.setOnline(true);
                    client.setCurrentUser(loggedInUser);
                    ChatWindow chatWindow = new ChatWindow(client);
                    chatWindow.setVisible(true);
                    this.dispose();
                } else if (message.getType() == MessageType.LOGIN_FAIL) {
                    JOptionPane.showMessageDialog(this, "登录失败: " + message.getContent());
                    loginButton.setEnabled(true);
                }
            });
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            Client client = new Client();
            new LoginWindow(client).setVisible(true);
        });
    }
}