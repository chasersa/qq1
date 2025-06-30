package com.simplechat.client;

import com.simplechat.common.Message;
import com.simplechat.common.MessageType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RegisterWindow extends JFrame {
    private JTextField idField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backButton;
    private Client client;
    private LoginWindow loginWindow;

    public RegisterWindow(Client client, LoginWindow loginWindow) {
        this.client = client;
        this.loginWindow = loginWindow;
        setTitle("简易聊天室 - 注册");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(panel);

        panel.add(new JLabel("用户ID:"));
        idField = new JTextField(15);
        panel.add(idField);

        panel.add(new JLabel("用户名:"));
        usernameField = new JTextField(15);
        panel.add(usernameField);

        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        panel.add(new JLabel("确认密码:"));
        confirmPasswordField = new JPasswordField(15);
        panel.add(confirmPasswordField);

        registerButton = new JButton("注册");
        panel.add(registerButton);

        backButton = new JButton("返回登录");
        panel.add(backButton);

        registerButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (id.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有字段都不能为空！");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "两次输入的密码不一致！");
                return;
            }

            // 禁用按钮防止重复点击
            registerButton.setEnabled(false);
            
            try {
                if (!client.isConnected()) {
                    client.connect();
                    // 等待一小段时间确保连接建立
                    Thread.sleep(100);
                }
                client.sendMessage(new Message(MessageType.REGISTER, id, "Server", id + "," + username + "," + password));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败，请确保服务器已启动: " + ex.getMessage());
                registerButton.setEnabled(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                registerButton.setEnabled(true);
            }
        });

        backButton.addActionListener(e -> {
            loginWindow.setVisible(true);
            this.dispose();
        });

        client.setMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.REGISTER_SUCCESS) {
                    JOptionPane.showMessageDialog(this, "注册成功！请登录。");
                    loginWindow.setVisible(true);
                    this.dispose();
                } else if (message.getType() == MessageType.REGISTER_FAIL) {
                    JOptionPane.showMessageDialog(this, "注册失败: " + message.getContent());
                    registerButton.setEnabled(true);
                }
            });
        });
    }
}