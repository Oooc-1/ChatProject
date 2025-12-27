package client.ui;

import client.Client;
import client.MessageReceiver;
import client.MessageSender;
import common.protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private MainFrame mainFrame; // 假设你有 MainFrame

    public LoginFrame() {
        setTitle("登录");
        setSize(400, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 主内容面板（整体居中）
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ==================== 登录表单 ====================
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField accountField = new JTextField(20);
        JPasswordField pwdField = new JPasswordField(20);

        // 账号标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("账号："), gbc);
        // 账号输入框
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(accountField, gbc);

        // 密码标签
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("密码："), gbc);
        // 密码输入框
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(pwdField, gbc);

        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(formPanel);

        // === 登录按钮 ===
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginBtn = new JButton("登录");
        loginBtn.setPreferredSize(new Dimension(100, 30));
        buttonWrapper.add(loginBtn);
        buttonWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(buttonWrapper);
        contentPanel.add(Box.createVerticalStrut(15));

        // ==================== 服务器设置 ====================
        JPanel serverPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        serverPanel.setBorder(BorderFactory.createTitledBorder("服务器设置"));
        serverPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField serverField = new JTextField("127.0.0.1", 12);
        JTextField portField = new JTextField("5000", 6);

        serverPanel.add(new JLabel("服务器："));
        serverPanel.add(serverField);
        serverPanel.add(new JCheckBox("记住IP", true));
        serverPanel.add(new JLabel(""));
        serverPanel.add(new JLabel("端口："));
        serverPanel.add(portField);
        serverPanel.add(new JCheckBox("记住端口", true));
        serverPanel.add(new JLabel(""));

        contentPanel.add(serverPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // ==================== 底部按钮 ====================
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        JButton registerBtn = new JButton("注册账号");   // ← 关键：保存引用
        JButton forgotPwdBtn = new JButton("找回密码");

        // 绑定注册按钮事件
        registerBtn.addActionListener(e -> {
            new RegisterFrame().setVisible(true); // 打开注册窗口
        });

        forgotPwdBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "找回密码功能暂未实现", "提示", JOptionPane.WARNING_MESSAGE)
        );

        bottomButtonPanel.add(registerBtn);
        bottomButtonPanel.add(forgotPwdBtn);
        bottomButtonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(bottomButtonPanel);

        add(contentPanel, BorderLayout.CENTER);

        // ==================== 登录事件 ====================
        loginBtn.addActionListener(e ->
                handleLogin(accountField, pwdField, serverField, portField)
        );
    }

    private void handleLogin(JTextField accountField, JPasswordField pwdField,
                             JTextField serverField, JTextField portField) {
        try {
            String ip = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            if (ip.isEmpty() || port <= 0 || port > 65535) {
                JOptionPane.showMessageDialog(this,
                        "请输入有效的服务器地址和端口（1~65535）",
                        "输入错误", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Client.connectServer(ip, port);
            if (!Client.isConnected()) {
                JOptionPane.showMessageDialog(this,
                        "无法连接到服务器，请检查网络或地址",
                        "连接失败", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                MessageSender.init(Client.getSocket());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // 登录成功：打开主界面
            mainFrame = new MainFrame();
            mainFrame.setVisible(true);
            this.dispose();

            // 启动消息接收线程
            MessageReceiver receiver = new MessageReceiver(Client.getSocket());
            receiver.start();

            // 发送登录消息
            Message msg = new Message("login");
            msg.setAccount(accountField.getText().trim());
            msg.setPassword(new String(pwdField.getPassword()));
            MessageSender.send(msg);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "端口号必须是数字", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "登录失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}