package client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RegisterFrame extends JFrame {

    private JTextField nicknameField, emailField, assignedAccountField, realNameField, collegeField, majorField;
    private JPasswordField passwordField, confirmPasswordField;
    private JLabel messageLabel;
    private String avatarPath = null; // 可用于后续上传

    // 性别
    private JRadioButton maleRadio, femaleRadio;

    // 生日
    private JComboBox<String> yearBox, monthBox, dayBox;

    // 选填面板
    private JPanel extraInfoPanel;
    private JButton showExtraBtn;

    public RegisterFrame() {
        setTitle("注册");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        // 主面板：使用 BoxLayout 垂直排列，但所有子组件居中
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 添加基本信息
        addBasicInfo(mainPanel);

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton registerBtn = new JButton("注册");
        showExtraBtn = new JButton("填写详细信息");
        JButton cancelBtn = new JButton("取消");

        buttonPanel.add(registerBtn);
        buttonPanel.add(showExtraBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel);

        // 选填信息面板
        extraInfoPanel = createExtraInfoPanel();
        extraInfoPanel.setVisible(false);
        extraInfoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(extraInfoPanel);

        // 提示消息
        messageLabel = new JLabel(" ");
        messageLabel.setForeground(Color.RED);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(messageLabel);

        // 滚动容器
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);

        setMinimumSize(new Dimension(400, 300));

        // 事件绑定
        registerBtn.addActionListener(e -> handleRegister());
        cancelBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        showExtraBtn.addActionListener(e -> {
            boolean visible = extraInfoPanel.isVisible();
            extraInfoPanel.setVisible(!visible);
            showExtraBtn.setText(visible ? "填写详细信息" : "收起");
            scrollPane.revalidate();
            scrollPane.repaint();
        });

        setVisible(true);
    }

    private void addBasicInfo(JPanel parent) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // 昵称
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("昵称:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        nicknameField = new JTextField(18);
        panel.add(nicknameField, gbc);

        // 密码
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        passwordField = new JPasswordField(18);
        panel.add(passwordField, gbc);

        // 重复密码
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("重复密码:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        confirmPasswordField = new JPasswordField(18);
        panel.add(confirmPasswordField, gbc);

        // 邮箱
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("邮箱:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        emailField = new JTextField(18);
        panel.add(emailField, gbc);



        // 性别
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("性别:"), gbc);
        maleRadio = new JRadioButton("男");
        femaleRadio = new JRadioButton("女");
        maleRadio.setSelected(true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(maleRadio);
        bg.add(femaleRadio);
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        genderPanel.add(maleRadio);
        genderPanel.add(femaleRadio);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(genderPanel, gbc);

        // 生日
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("生日:"), gbc);
        String[] years = new String[100];
        for (int i = 0; i < 100; i++) years[i] = String.valueOf(2025 - i);
        yearBox = new JComboBox<>(years);
        yearBox.setSelectedItem("2000");

        String[] months = new String[12];
        for (int i = 0; i < 12; i++) months[i] = String.valueOf(i + 1);
        monthBox = new JComboBox<>(months);
        monthBox.setSelectedItem("1");

        String[] days = new String[31];
        for (int i = 0; i < 31; i++) days[i] = String.valueOf(i + 1);
        dayBox = new JComboBox<>(days);
        dayBox.setSelectedItem("1");

        JPanel birthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        birthPanel.add(yearBox);
        birthPanel.add(new JLabel("年"));
        birthPanel.add(monthBox);
        birthPanel.add(new JLabel("月"));
        birthPanel.add(dayBox);
        birthPanel.add(new JLabel("日"));

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(birthPanel, gbc);

        // 账号（只读）
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("您的账号:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        assignedAccountField = new JTextField(18);
        assignedAccountField.setEditable(false);
        panel.add(assignedAccountField, gbc);

        parent.add(panel);
    }

    private JPanel createExtraInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("选填资料信息"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // === 头像区域（使用程序生成的默认图）===
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("头像:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // 生成默认头像（灰色背景 + 文字“头”）
        BufferedImage placeholder = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 40, 40);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "头像";
        int x = (40 - fm.stringWidth(text)) / 2;
        int y = (40 - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        g2d.dispose();

        JLabel avatarLabel = new JLabel(new ImageIcon(placeholder));
        avatarLabel.setPreferredSize(new Dimension(40, 40));
        avatarLabel.setVerticalAlignment(SwingConstants.TOP);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton changeBtn = new JButton("更改");
        changeBtn.setPreferredSize(new Dimension(60, 25)); // 更宽一些，适合中文
        changeBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif");
            chooser.setFileFilter(filter);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    BufferedImage img = ImageIO.read(file);
                    Image scaledImg = img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    avatarLabel.setIcon(new ImageIcon(scaledImg));
                    avatarPath = file.getAbsolutePath(); // 保存路径（可选）
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "无法加载图片：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        avatarPanel.add(avatarLabel);
        avatarPanel.add(changeBtn);
        panel.add(avatarPanel, gbc);
        row++;

        // 真实姓名
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("真实姓名:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        realNameField = new JTextField(18);
        panel.add(realNameField, gbc);
        row++;

        // 学院
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("学院:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        collegeField = new JTextField(18);
        panel.add(collegeField, gbc);
        row++;

        // 专业
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("专业:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        majorField = new JTextField(18);
        panel.add(majorField, gbc);
        row++;

        return panel;
    }

    private void handleRegister() {
        // 注册逻辑保持不变
    }

    private void showMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.repaint();
    }
}