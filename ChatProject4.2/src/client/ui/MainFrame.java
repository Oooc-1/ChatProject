package client.ui;

import common.protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {
    private JList<FriendItem> friendList;
    private DefaultListModel<FriendItem> listModel;
    private final Map<String, ChatFrame> chatWindows = new HashMap<>();

    // 示例好友数据（仅用于开发预览）
    private List<FriendItem> sampleFriends = Arrays.asList(
            new FriendItem("低调", "10000002", "你若安好,便是晴天霹雳", true),
            new FriendItem("彻夜", "10000003", "八十烦恼风", false),
            new FriendItem("晓安", "10000004", "最美丽长发没留在我手", true)
    );

    public MainFrame() {
        setTitle("C.Lucky");
        setSize(420, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ========== 1. 顶部用户信息区 ==========
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.setBackground(new Color(245, 245, 245));

        JLabel avatarLabel = new JLabel("👤", JLabel.CENTER);
        avatarLabel.setFont(new Font("Dialog", Font.PLAIN, 24));
        avatarLabel.setPreferredSize(new Dimension(45, 45));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(Color.WHITE);
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        JPanel userInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("古风小生 (10000)");
        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0;
        userInfo.add(nameLabel, gbc);

        JLabel signatureLabel = new JLabel("明意");
        signatureLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        signatureLabel.setForeground(new Color(100, 100, 100));
        gbc.gridy = 1;
        userInfo.add(signatureLabel, gbc);

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);
        topRow.add(avatarLabel);
        topRow.add(userInfo);
        headerPanel.add(topRow, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // ========== 2. 标签页区域 ==========
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        // --- 好友标签 ---
        tabbedPane.addTab("好友", createFriendsPanel());

        // --- 群组标签（已增强）---
        tabbedPane.addTab("群组", createGroupsPanel());

        // --- 巧遇（占位）---
        tabbedPane.addTab("巧遇", new JLabel("附近的人", JLabel.CENTER));

        add(tabbedPane, BorderLayout.CENTER);


        // ========== 3. 菜单栏 ==========
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("操作");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        menu.add(exitItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    // ========== 好友面板 ==========
    private JPanel createFriendsPanel() {
        listModel = new DefaultListModel<>();
        for (FriendItem f : sampleFriends) {
            listModel.addElement(f);
        }

        friendList = new JList<>(listModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.setCellRenderer(new FriendListCellRenderer());
        friendList.setFocusable(false);

        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FriendItem selected = friendList.getSelectedValue();
                    if (selected != null) {
                        openChatWindow(selected.account);
                    }
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(friendList), BorderLayout.CENTER);
        return panel;
    }

    // ========== 群组面板（新增）==========
    private JPanel createGroupsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 按钮行
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        buttonPanel.setOpaque(false);

        JButton searchBtn = createStyledButton("查找一个群", "🔍");
        JButton createBtn = createStyledButton("创建一个新群", "➕");

        buttonPanel.add(searchBtn);
        buttonPanel.add(createBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // 内容区（暂无群组）
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel noGroupLabel = new JLabel("暂无群组", JLabel.CENTER);
        noGroupLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        noGroupLabel.setForeground(new Color(120, 120, 120));
        contentPanel.add(noGroupLabel);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createStyledButton(String text, String iconEmoji) {
        JButton btn = new JButton(iconEmoji + " " + text);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 32));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setBackground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 点击反馈
        btn.addActionListener(e -> {
            if (text.contains("查找")) {
                JOptionPane.showMessageDialog(this, "功能开发中：查找群", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else if (text.contains("创建")) {
                JOptionPane.showMessageDialog(this, "功能开发中：创建群", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return btn;
    }

    // ========== 聊天窗口管理 ==========
    private void openChatWindow(String account) {
        ChatFrame frame = chatWindows.get(account);
        if (frame == null || !frame.isVisible()) {
            frame = new ChatFrame("single", account);
            chatWindows.put(account, frame);
            frame.setVisible(true);
        } else {
            frame.toFront();
            frame.requestFocus();
        }
    }

    public void onPrivateMessage(Message msg) {
        String sender = msg.getFrom();
        ChatFrame frame = chatWindows.get(sender);
        if (frame == null || !frame.isVisible()) {
            frame = new ChatFrame("single", sender);
            chatWindows.put(sender, frame);
            frame.setVisible(true);
        }
        frame.appendReceivedMessage(msg);
    }

    public void updateFriendStatus(String account, boolean online) {
        for (int i = 0; i < listModel.size(); i++) {
            FriendItem item = listModel.getElementAt(i);
            if (item.account.equals(account)) {
                item.online = online;
                listModel.setElementAt(item, i);
                break;
            }
        }
    }

    // ==================== 内部类：好友项 ====================
    private static class FriendItem {
        String nickname;
        String account;
        String signature;
        boolean online;

        FriendItem(String nickname, String account, String signature, boolean online) {
            this.nickname = nickname;
            this.account = account;
            this.signature = signature;
            this.online = online;
        }

        @Override
        public String toString() {
            return nickname + " (" + account + ")";
        }
    }

    // ==================== 自定义渲染器 ====================
    private static class FriendListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (!(value instanceof FriendItem)) {
                return this;
            }

            FriendItem item = (FriendItem) value;

            if (isSelected) {
                setBackground(new Color(230, 240, 255));
                setForeground(Color.BLACK);
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }

            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            // 头像占位
            JLabel avatar = new JLabel("👤", JLabel.CENTER);
            avatar.setFont(new Font("Dialog", Font.PLAIN, 18));
            avatar.setPreferredSize(new Dimension(36, 36));
            avatar.setOpaque(true);
            avatar.setBackground(new Color(240, 240, 240));
            avatar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

            // 文字
            JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
            textPanel.setOpaque(false);

            String statusText = item.online ? "(在线)" : "(离线)";
            Color statusColor = item.online ? Color.GREEN : Color.GRAY;

            JLabel nameLabel = new JLabel(item.nickname + " (" + item.account + ") " + statusText);
            nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            nameLabel.setForeground(statusColor);
            textPanel.add(nameLabel);

            JLabel sigLabel = new JLabel(item.signature);
            sigLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            sigLabel.setForeground(new Color(100, 100, 100));
            textPanel.add(sigLabel);

            removeAll();
            add(avatar, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);

            return this;
        }
    }
}