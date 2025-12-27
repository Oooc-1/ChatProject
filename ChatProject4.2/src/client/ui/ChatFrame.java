package client.ui;

import client.MessageSender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.UIManager;

/**
 * 聊天窗口：新增添加群成员、弹窗式设置群昵称按钮
 * 核心改法：移除原昵称输入框，改为按钮弹窗编辑；新增添加群成员按钮
 */
public class ChatFrame extends JFrame {
    // 成员变量
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JComboBox<String> quickMsgCombo;
    private JComboBox<String> fontCombo;
    private JSpinner fontSizeSpinner;
    private JPanel memberPanel;
    private Color chatFontColor = Color.BLACK;
    private JButton colorBtn;
    private String targetId;
    private String windowTitle;
    private String currentAccount;
    private String chatType;
    private String[] groupMemberAccounts; // 纯账号数组

    // 群聊专属：成员昵称映射、禁言状态映射（账号为唯一键）
    private Map<String, String> memberNicknameMap; // 账号->昵称
    private Map<String, Boolean> memberMuteMap;    // 账号->禁言状态
    private Map<String, String> groupNoticeCache;  // 群ID->公告内容

    // 群聊专属组件
    private JButton removeMemberBtn, muteBtn, unmuteBtn, exitBtn;
    private JButton addMemberBtn; // 新增：添加群成员按钮
    private JButton setNicknameBtn; // 新增：设置群昵称按钮
    private JList<String> memberList;
    private DefaultListModel<String> memberListModel; // 独立维护列表模型
    private JButton groupNoticeBtn;

    // 构造方法：单聊
    public ChatFrame(String targetId, String currentAccount) {
        this.chatType = "single";
        this.targetId = targetId;
        this.currentAccount = currentAccount;
        this.windowTitle = "与 " + targetId + " 聊天";
        initData();
        initUI();
        initEvent();
    }

    // 构造方法：群聊（核心修改：接收纯账号数组和初始昵称映射）
    public ChatFrame(String groupId, String groupName, String currentAccount, String[] groupMemberAccounts, Map<String, String> initialNicknames) {
        this.chatType = "group";
        this.targetId = groupId;
        this.currentAccount = currentAccount;
        this.windowTitle = groupName;
        this.groupMemberAccounts = groupMemberAccounts; // 纯账号数组

        initData();

        // 初始化昵称映射（用传入的初始昵称）
        for (String account : groupMemberAccounts) {
            memberNicknameMap.put(account, initialNicknames.getOrDefault(account, account));
            memberMuteMap.put(account, false);
        }

        initUI();
        initEvent();
    }

    // 简化构造方法（测试用：自动生成初始昵称）
    public ChatFrame(String groupId, String groupName, String currentAccount, String[] groupMemberAccounts) {
        this(groupId, groupName, currentAccount, groupMemberAccounts, initDefaultNicknames(groupMemberAccounts));
    }

    // 生成默认昵称（如10000→用户10000）
    private static Map<String, String> initDefaultNicknames(String[] accounts) {
        Map<String, String> nicknames = new HashMap<>();
        for (String account : accounts) {
            nicknames.put(account, "用户" + account);
        }
        return nicknames;
    }

    // 初始化数据缓存
    private void initData() {
        memberNicknameMap = new HashMap<>();
        memberMuteMap = new HashMap<>();
        groupNoticeCache = new HashMap<>();
    }

    /**
     * 刷新群成员列表（核心：完全基于账号映射生成显示名）
     */
    private void refreshMemberList() {
        memberListModel.clear();
        for (String account : groupMemberAccounts) {
            String nickname = memberNicknameMap.getOrDefault(account, account);
            boolean isMuted = memberMuteMap.getOrDefault(account, false);

            // 构建显示名：昵称(账号) + （禁言中）
            StringBuilder displayName = new StringBuilder(nickname + "(" + account + ")");
            if (isMuted) {
                displayName.append("（禁言中）");
            }
            memberListModel.addElement(displayName.toString());
        }
    }

    /**
     * 从列表选中项中提取账号（兼容所有显示格式）
     */
    private String extractAccountFromSelected(String selected) {
        if (selected == null) return null;
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(selected);
        return matcher.find() ? matcher.group(1) : selected;
    }

    /**
     * 获取当前用户的群昵称
     */
    private String getCurrentGroupNickname() {
        return memberNicknameMap.getOrDefault(currentAccount, currentAccount);
    }

    /**
     * 初始化UI（重点：新增添加成员、设置昵称按钮，移除原昵称输入框）
     */
    private void initUI() {
        // 窗口基本设置
        setTitle(windowTitle);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // ========== 1. 顶部用户/群信息栏 ==========
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(BorderFactory.createEtchedBorder());

        // 头像加载
        ImageIcon avatarIcon;
        try {
            avatarIcon = new ImageIcon("src/resources/avatar.png");
        } catch (Exception e) {
            Object iconObj = UIManager.get("OptionPane.userIcon");
            Icon defaultIcon = (iconObj instanceof Icon) ? (Icon) iconObj : null;
            avatarIcon = (defaultIcon instanceof ImageIcon) ? (ImageIcon) defaultIcon : new ImageIcon();
        }
        Image scaledAvatar = avatarIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        JLabel avatarLabel = new JLabel(new ImageIcon(scaledAvatar));
        avatarLabel.setPreferredSize(new Dimension(50, 50));

        // 名称+ID
        JLabel nameLabel = new JLabel(windowTitle.contains("与 ")
                ? targetId + "(" + targetId + ")"
                : windowTitle + "(" + targetId + ")");
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        topPanel.add(avatarLabel);
        topPanel.add(nameLabel);
        add(topPanel, BorderLayout.NORTH);

        // ========== 2. 中间区域：聊天区 + 群管理面板 ==========
        JPanel centerPanel = new JPanel(new BorderLayout());

        // 聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        chatArea.setForeground(chatFontColor);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("聊天记录"));
        centerPanel.add(chatScroll, BorderLayout.CENTER);

        // 群管理面板（仅群聊显示）
        if ("group".equals(chatType)) {
            memberPanel = new JPanel(new BorderLayout(5, 5));
            memberPanel.setBorder(BorderFactory.createTitledBorder("群成员管理"));
            memberPanel.setPreferredSize(new Dimension(220, 0));

            // -------- 上部：按钮区域（新增添加成员、设置昵称按钮） --------
            JPanel topBtnPanel = new JPanel(new GridLayout(3, 2, 3, 3)); // 改为3行2列，容纳更多按钮
            topBtnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            groupNoticeBtn = new JButton("群公告");
            groupNoticeBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            groupNoticeBtn.setPreferredSize(new Dimension(80, 20));

            muteBtn = new JButton("禁言");
            muteBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            muteBtn.setPreferredSize(new Dimension(80, 20));

            unmuteBtn = new JButton("解除禁言");
            unmuteBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            unmuteBtn.setPreferredSize(new Dimension(80, 20));

            removeMemberBtn = new JButton("移除成员");
            removeMemberBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            removeMemberBtn.setPreferredSize(new Dimension(80, 20));

            addMemberBtn = new JButton("添加群成员"); // 新增：添加群成员按钮
            addMemberBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            addMemberBtn.setPreferredSize(new Dimension(80, 20));

            setNicknameBtn = new JButton("设置群昵称"); // 新增：设置群昵称按钮
            setNicknameBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            setNicknameBtn.setPreferredSize(new Dimension(80, 20));

            // 按钮布局：3行2列
            topBtnPanel.add(groupNoticeBtn);
            topBtnPanel.add(muteBtn);
            topBtnPanel.add(unmuteBtn);
            topBtnPanel.add(removeMemberBtn);
            topBtnPanel.add(addMemberBtn);
            topBtnPanel.add(setNicknameBtn);

            memberPanel.add(topBtnPanel, BorderLayout.NORTH);

            // -------- 下部：群成员列表 + 退出按钮 --------
            JPanel bottomPanel = new JPanel(new BorderLayout(3, 3));

            // 群成员列表（核心：独立维护ListModel）
            memberListModel = new DefaultListModel<>();
            memberList = new JList<>(memberListModel);
            memberList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            JScrollPane memberScroll = new JScrollPane(memberList);
            memberScroll.setBorder(BorderFactory.createTitledBorder("群成员"));
            bottomPanel.add(memberScroll, BorderLayout.CENTER);

            // 退出该群按钮
            exitBtn = new JButton("退出该群");
            exitBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            exitBtn.setPreferredSize(new Dimension(80, 20));
            bottomPanel.add(exitBtn, BorderLayout.SOUTH);
            memberPanel.add(bottomPanel, BorderLayout.CENTER); // 改为CENTER，适配布局

            centerPanel.add(memberPanel, BorderLayout.EAST);

            // 初始化成员列表
            refreshMemberList();
        }
        add(centerPanel, BorderLayout.CENTER);

        // ========== 3. 功能栏 ==========
        JPanel funcPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        funcPanel.setBorder(BorderFactory.createEtchedBorder());

        colorBtn = new JButton("字体颜色");
        colorBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        fontCombo = new JComboBox<>(new String[]{"微软雅黑", "宋体", "黑体"});
        fontCombo.setSelectedItem("微软雅黑");
        fontCombo.setPreferredSize(new Dimension(100, 25));

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 20, 1));
        fontSizeSpinner.setPreferredSize(new Dimension(50, 25));

        JButton emojiBtn = new JButton("😀 表情");

        funcPanel.add(colorBtn);
        funcPanel.add(new JLabel("字体："));
        funcPanel.add(fontCombo);
        funcPanel.add(new JLabel("字号："));
        funcPanel.add(fontSizeSpinner);
        funcPanel.add(emojiBtn);

        // ========== 4. 底部输入区 ==========
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        quickMsgCombo = new JComboBox<>(new String[]{"快速语", "你好呀！！！", "大家好呀！", "很高兴认识你"});
        quickMsgCombo.setPreferredSize(new Dimension(120, 25));

        inputField = new JTextField();
        inputField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        inputField.setPreferredSize(new Dimension(inputField.getPreferredSize().width, 30));

        sendButton = new JButton("发送");
        sendButton.setPreferredSize(new Dimension(80, 25));

        inputPanel.add(quickMsgCombo, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(funcPanel, BorderLayout.NORTH);
        southPanel.add(inputPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        // ========== 字体颜色按钮事件 ==========
        colorBtn.addActionListener(e -> {
            JPanel colorPanel = new JPanel(new GridLayout(8, 16));
            Color[] colors = {
                    Color.WHITE, Color.LIGHT_GRAY, Color.GRAY, Color.DARK_GRAY, Color.BLACK, Color.PINK, Color.RED, Color.ORANGE,
                    Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, new Color(128,0,0), new Color(128,128,0), new Color(0,128,0),
                    new Color(128,0,128), new Color(0,128,128), new Color(0,0,128), new Color(255,192,203), new Color(255,0,0), new Color(255,165,0),
                    new Color(255,255,0), new Color(0,255,0), new Color(0,255,255), new Color(0,0,255), new Color(128,0,128), new Color(255,255,255),
                    new Color(200,200,200), new Color(150,150,150), new Color(100,100,100), new Color(50,50,50), new Color(255,105,180), new Color(255,69,0),
                    new Color(255,215,0), new Color(154,205,50), new Color(64,224,208), new Color(30,144,255), new Color(138,43,226), new Color(240,248,255),
                    new Color(245,245,220), new Color(255,228,196), new Color(255,250,205), new Color(240,255,240), new Color(240,255,255), new Color(248,248,255),
                    new Color(255,222,173), new Color(255,240,245), new Color(250,235,215), new Color(255,250,240), new Color(245,255,250), new Color(255,255,240)
            };
            for (Color color : colors) {
                JButton colorBtn = new JButton();
                colorBtn.setBackground(color);
                colorBtn.setPreferredSize(new Dimension(20, 20));
                colorBtn.addActionListener(ce -> {
                    chatFontColor = ((JButton) ce.getSource()).getBackground();
                    chatArea.setForeground(chatFontColor);
                    inputField.setForeground(chatFontColor);
                    SwingUtilities.getWindowAncestor((Component) ce.getSource()).dispose();
                });
                colorPanel.add(colorBtn);
            }

            JDialog colorDialog = new JDialog(this, "选择聊天字体颜色", true);
            colorDialog.setLayout(new BorderLayout());
            colorDialog.add(new JScrollPane(colorPanel), BorderLayout.CENTER);
            colorDialog.setSize(350, 200);
            colorDialog.setLocationRelativeTo(this);
            colorDialog.setVisible(true);
        });
    }

    /**
     * 弹窗：设置群昵称
     */
    private void showSetNicknameDialog() {
        // 弹窗主体
        JDialog nicknameDialog = new JDialog(this, "设置我的群昵称", true);
        nicknameDialog.setSize(300, 150);
        nicknameDialog.setLocationRelativeTo(this);
        nicknameDialog.setLayout(new BorderLayout(10, 10));
        nicknameDialog.setResizable(false);

        // 输入面板
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel tipLabel = new JLabel("新昵称：");
        JTextField nicknameInput = new JTextField(getCurrentGroupNickname(), 15); // 回显当前昵称
        nicknameInput.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputPanel.add(tipLabel);
        inputPanel.add(nicknameInput);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton confirmBtn = new JButton("确认");
        JButton cancelBtn = new JButton("取消");
        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        // 组装弹窗
        nicknameDialog.add(inputPanel, BorderLayout.CENTER);
        nicknameDialog.add(btnPanel, BorderLayout.SOUTH);

        // 确认按钮事件
        confirmBtn.addActionListener(e -> {
            String newNickname = nicknameInput.getText().trim();
            if (newNickname.isEmpty()) {
                JOptionPane.showMessageDialog(nicknameDialog, "昵称不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 更新昵称映射
            memberNicknameMap.put(currentAccount, newNickname);
            MessageSender.setGroupNickname(currentAccount, targetId, currentAccount, newNickname);
            // 刷新列表
            refreshMemberList();
            JOptionPane.showMessageDialog(nicknameDialog, "群昵称设置成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            nicknameDialog.dispose();
        });

        // 取消按钮事件
        cancelBtn.addActionListener(e -> nicknameDialog.dispose());

        // 显示弹窗
        nicknameDialog.setVisible(true);
    }

    /**
     * 弹窗：添加群成员
     */
    private void showAddMemberDialog() {
        // 弹窗主体
        JDialog addMemberDialog = new JDialog(this, "添加群成员", true);
        addMemberDialog.setSize(300, 150);
        addMemberDialog.setLocationRelativeTo(this);
        addMemberDialog.setLayout(new BorderLayout(10, 10));
        addMemberDialog.setResizable(false);

        // 输入面板
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel tipLabel = new JLabel("成员账号：");
        JTextField accountInput = new JTextField("", 15);
        accountInput.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputPanel.add(tipLabel);
        inputPanel.add(accountInput);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton confirmBtn = new JButton("添加");
        JButton cancelBtn = new JButton("取消");
        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        // 组装弹窗
        addMemberDialog.add(inputPanel, BorderLayout.CENTER);
        addMemberDialog.add(btnPanel, BorderLayout.SOUTH);

        // 确认按钮事件
        confirmBtn.addActionListener(e -> {
            String newAccount = accountInput.getText().trim();
            if (newAccount.isEmpty()) {
                JOptionPane.showMessageDialog(addMemberDialog, "账号不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 检查是否已存在
            for (String account : groupMemberAccounts) {
                if (account.equals(newAccount)) {
                    JOptionPane.showMessageDialog(addMemberDialog, "该成员已在群中！", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            // 调用后端添加成员
            MessageSender.addGroupMember(currentAccount, targetId, newAccount);
            // 本地临时更新（实际项目中应从后端拉取最新列表）
            String[] newAccounts = new String[groupMemberAccounts.length + 1];
            System.arraycopy(groupMemberAccounts, 0, newAccounts, 0, groupMemberAccounts.length);
            newAccounts[groupMemberAccounts.length] = newAccount;
            groupMemberAccounts = newAccounts;
            // 初始化新成员的昵称和禁言状态
            memberNicknameMap.put(newAccount, "用户" + newAccount);
            memberMuteMap.put(newAccount, false);
            // 刷新列表
            refreshMemberList();
            JOptionPane.showMessageDialog(addMemberDialog, "成员添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            addMemberDialog.dispose();
        });

        // 取消按钮事件
        cancelBtn.addActionListener(e -> addMemberDialog.dispose());

        // 显示弹窗
        addMemberDialog.setVisible(true);
    }

    /**
     * 群公告弹窗（本地缓存实现保存）
     */
    private void showGroupNoticeDialog() {
        JDialog noticeDialog = new JDialog(this, windowTitle + " - 群公告", true);
        noticeDialog.setSize(400, 300);
        noticeDialog.setLocationRelativeTo(this);
        noticeDialog.setLayout(new BorderLayout(5, 5));

        String currentNotice = groupNoticeCache.getOrDefault(targetId, "暂无群公告，点击编辑按钮添加吧！");

        JTextArea noticeArea = new JTextArea(currentNotice);
        noticeArea.setLineWrap(true);
        noticeArea.setWrapStyleWord(true);
        noticeArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        noticeArea.setEditable(false);
        JScrollPane noticeScroll = new JScrollPane(noticeArea);
        noticeScroll.setBorder(BorderFactory.createTitledBorder("群公告内容"));
        noticeDialog.add(noticeScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton editBtn = new JButton("编辑");
        JButton saveBtn = new JButton("保存");
        JButton closeBtn = new JButton("关闭");

        editBtn.addActionListener(e -> {
            noticeArea.setEditable(true);
            noticeArea.requestFocus();
            editBtn.setEnabled(false);
            saveBtn.setEnabled(true);
        });

        saveBtn.addActionListener(e -> {
            String newNotice = noticeArea.getText().trim();
            if (newNotice.isEmpty()) {
                JOptionPane.showMessageDialog(noticeDialog, "公告内容不能为空！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            groupNoticeCache.put(targetId, newNotice);
            MessageSender.saveGroupNotice(currentAccount, targetId, newNotice);
            JOptionPane.showMessageDialog(noticeDialog, "群公告保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            noticeArea.setEditable(false);
            editBtn.setEnabled(true);
            saveBtn.setEnabled(false);
        });

        closeBtn.addActionListener(e -> noticeDialog.dispose());
        saveBtn.setEnabled(false);

        btnPanel.add(editBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(closeBtn);
        noticeDialog.add(btnPanel, BorderLayout.SOUTH);

        noticeDialog.setVisible(true);
    }

    /**
     * 初始化事件监听（核心：新增添加成员、设置昵称按钮事件）
     */
    private void initEvent() {
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        quickMsgCombo.addActionListener(e -> {
            String quickMsg = (String) quickMsgCombo.getSelectedItem();
            if (quickMsg != null && !"快速语".equals(quickMsg)) {
                inputField.setText(quickMsg);
            }
        });

        ActionListener fontListener = e -> {
            String fontName = (String) fontCombo.getSelectedItem();
            int fontSize = (int) fontSizeSpinner.getValue();
            chatArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
            inputField.setFont(new Font(fontName, Font.PLAIN, fontSize));
        };

        fontCombo.addActionListener(fontListener);
        fontSizeSpinner.addChangeListener(e -> fontListener.actionPerformed(null));

        if ("group".equals(chatType)) {
            groupNoticeBtn.addActionListener(e -> showGroupNoticeDialog());

            // 新增：设置群昵称按钮事件
            setNicknameBtn.addActionListener(e -> showSetNicknameDialog());

            // 新增：添加群成员按钮事件
            addMemberBtn.addActionListener(e -> showAddMemberDialog());

            // 禁言按钮事件
            muteBtn.addActionListener(e -> {
                String selected = memberList.getSelectedValue();
                String account = extractAccountFromSelected(selected);
                if (account == null) {
                    JOptionPane.showMessageDialog(this, "请选择要禁言的成员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // 更新禁言状态
                memberMuteMap.put(account, true);
                MessageSender.muteGroupMember(currentAccount, targetId, account);
                // 刷新列表
                refreshMemberList();
                JOptionPane.showMessageDialog(this, "成员禁言成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            });

            // 解除禁言按钮事件
            unmuteBtn.addActionListener(e -> {
                String selected = memberList.getSelectedValue();
                String account = extractAccountFromSelected(selected);
                if (account == null) {
                    JOptionPane.showMessageDialog(this, "请选择要解除禁言的成员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                memberMuteMap.put(account, false);
                MessageSender.unmuteGroupMember(currentAccount, targetId, account);
                refreshMemberList();
                JOptionPane.showMessageDialog(this, "成员解除禁言成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            });

            // 移除成员按钮事件
            removeMemberBtn.addActionListener(e -> {
                String selected = memberList.getSelectedValue();
                String account = extractAccountFromSelected(selected);
                if (account == null) {
                    JOptionPane.showMessageDialog(this, "请选择要移除的成员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this, "确定要移除成员：" + selected + "吗？", "确认", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    MessageSender.removeGroupMember(currentAccount, targetId, account);
                    // 从数组中移除（简化处理，实际项目中重新请求后端）
                    memberListModel.removeElement(selected);
                    memberNicknameMap.remove(account);
                    memberMuteMap.remove(account);
                    JOptionPane.showMessageDialog(this, "成员移除成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            // 退出群聊按钮事件
            exitBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "确定要退出该群吗？", "确认", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    this.dispose();
                }
            });
        }
    }

    /**
     * 发送消息逻辑（显示最新昵称）
     */
    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "消息内容不能为空！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String senderName = getCurrentGroupNickname();

        if ("single".equals(chatType)) {
            MessageSender.sendSingleChatMsg(currentAccount, targetId, content);
            chatArea.append("我：" + content + "\n");
        } else if ("group".equals(chatType)) {
            MessageSender.sendGroupChatMsg(currentAccount, targetId, content);
            chatArea.append(senderName + "：" + content + "\n");
        }

        inputField.setText("");
        chatArea.setCaretPosition(chatArea.getText().length());
    }

    /**
     * 接收消息（显示发送者的最新昵称）
     */
    public void receiveMessage(String senderAccount, String content) {
        SwingUtilities.invokeLater(() -> {
            String senderName = memberNicknameMap.getOrDefault(senderAccount, senderAccount);
            chatArea.append(senderName + "：" + content + "\n");
            chatArea.setCaretPosition(chatArea.getText().length());
        });
    }

    // 在 receiveMessage 方法附近添加
    public void appendReceivedMessage(common.protocol.Message msg) {
        // 提取消息内容和发送者
        String sender = msg.getFrom();
        String content = msg.getContent();

        // 调用现有的 UI 更新逻辑
        receiveMessage(sender, content);
    }



    // Getter方法
    public String getTargetId() {
        return targetId;
    }

    public String getChatType() {
        return chatType;
    }


}