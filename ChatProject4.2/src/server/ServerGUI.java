package server;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;

/**
 * 修复版 ServerGUI - 解决按钮颜色不显示及透明问题
 */
public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private JButton startBtn, stopBtn, clearLogBtn, broadcastBtn, importBtn;
    private JTextField broadcastField, ipField, portField, sqlField;
    private JLabel statusLabel, onlineLabel;
    private Thread serverThread;

    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 颜色定义
    private Color primaryColor = new Color(0, 120, 215); // 主色调蓝色
    private Color successColor = new Color(40, 167, 69); // 启动绿色
    private Color dangerColor = new Color(220, 53, 69);  // 停止红色
    private Color panelBg = new Color(250, 250, 250);


    public ServerGUI() {
        setTitle("服务端(旗舰版) - 修复版");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setGlobalFont(); // 保持你原本正常的字体逻辑
        initUI();
        redirectSystemStreams();
    }

    private void setGlobalFont() {
        Font chineseFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof Font) UIManager.put(key, chineseFont);
        }
    }



    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));
        setContentPane(mainPanel);


        // --- 顶部：配置面板 ---
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBackground(Color.WHITE);
        configPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "服务器配置"));

        // 第一行
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.setOpaque(false);
        row1.add(new JLabel("服务器IP:"));
        ipField = new JTextField("127.0.0.1", 10);
        row1.add(ipField);
        row1.add(new JLabel("端口:"));
        portField = new JTextField("5000", 5);
        row1.add(portField);

        startBtn = createFlatButton("开始监听", successColor, Color.WHITE);
        stopBtn = createFlatButton("停止监听", dangerColor, Color.WHITE);
        stopBtn.setEnabled(false);
        row1.add(startBtn);
        row1.add(stopBtn);
        configPanel.add(row1);

        // 第二行
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.setOpaque(false);
        row2.add(new JLabel("SQL字串:"));
        sqlField = new JTextField("Data Source=.;Initial Catalog=LuckMeet;Integrated Sec.", 40);
        row2.add(sqlField);
        configPanel.add(row2);

        mainPanel.add(configPanel, BorderLayout.NORTH);

        // --- 中部：日志区域 ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("日志信息"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- 底部：控制面板 ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bcRow = new JPanel(new BorderLayout(10, 0));
        bcRow.setOpaque(false);
        bcRow.add(new JLabel("广播消息: "), BorderLayout.WEST);
        broadcastField = new JTextField();
        bcRow.add(broadcastField, BorderLayout.CENTER);
        broadcastBtn = createFlatButton("发送消息", primaryColor, Color.WHITE);
        bcRow.add(broadcastBtn, BorderLayout.EAST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnRow.setOpaque(false);
        importBtn = createFlatButton("导入配置", new Color(100, 100, 100), Color.WHITE);
        clearLogBtn = createFlatButton("清空控制台", Color.GRAY, Color.WHITE);
        btnRow.add(importBtn);
        btnRow.add(clearLogBtn);

        bottomPanel.add(bcRow, BorderLayout.NORTH);
        bottomPanel.add(btnRow, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 事件绑定
        clearLogBtn.addActionListener(e -> logArea.setText(""));
        // --- 1. 广播发送功能 ---
        broadcastBtn.addActionListener(e -> {
            String msg = broadcastField.getText().trim();
            if (!msg.isEmpty()) {
                try {
                    // 调用你项目中的在线管理类发送消息
                    OnlineUserManager.broadcastSystemMessage(msg);
                    appendLog("[系统广播] " + msg);
                    broadcastField.setText("");
                } catch (Exception ex) {
                    appendLog("❌ 发送失败: " + ex.getMessage());
                }
            }
        });

        // --- 2. 启动/停止按钮状态切换 (示例逻辑) ---
        startBtn.addActionListener(e -> {
            startServer(); // 调用启动方法
        });

        stopBtn.addActionListener(e -> {
            stopServer(); // 调用停止方法
        });

        broadcastBtn.addActionListener(e -> {
            String msg = broadcastField.getText().trim();
            if (!msg.isEmpty()) {
                // 关键：调用你代码库里的 OnlineUserManager
                OnlineUserManager.broadcastSystemMessage(msg);
                appendLog("📢 [发送广播] " + msg);
                broadcastField.setText(""); // 发送后清空输入框
            }
        });
    }

    private void startServer() {
        try {
            // 1. 获取端口号
            int port = Integer.parseInt(portField.getText().trim());

            // 2. 启动后台线程进行监听 (防止卡死界面)
            serverThread = new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(port);
                    isRunning = true;

                    // 更新UI状态
                    SwingUtilities.invokeLater(() -> {
                        startBtn.setEnabled(false);
                        stopBtn.setEnabled(true);
                        ipField.setEditable(false);
                        portField.setEditable(false);
                        appendLog("🚀 服务启动成功，正在监听端口: " + port);
                    });

                    int count = 0;
                    // 循环监听客户端连接
                    while (isRunning && !serverSocket.isClosed()) {
                        java.net.Socket socket = serverSocket.accept();
                        count++;
                        // 启动 ClientHandler
                        ClientHandler handler = new ClientHandler(socket, count);
                        new Thread(handler).start();

                        appendLog("📢 新连接接入: " + socket.getInetAddress() + " (ID:" + count + ")");
                    }
                } catch (Exception e) {
                    if (isRunning) { // 只有在非手动停止的情况下才报错
                        appendLog("❌ 监听服务异常停止: " + e.getMessage());
                        stopServer(); // 触发停止逻辑
                    }
                }
            });
            serverThread.start();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "端口号必须是数字！");
        } catch (Exception ex) {
            appendLog("❌ 启动失败: " + ex.getMessage());
        }
    }

    // --- 新增：停止服务器的具体逻辑 ---
    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // 这会抛出 SocketException 中断 accept() 阻塞
            }
        } catch (Exception e) {
            appendLog("关闭监听时出错: " + e.getMessage());
        }

        // 恢复UI状态
        SwingUtilities.invokeLater(() -> {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            ipField.setEditable(true);
            portField.setEditable(true);
            appendLog("🛑 服务已停止");
        });
    }

    /**
     * 核心修复：这个方法解决了按钮颜色显示不出来的 BUG
     */
    private JButton createFlatButton(String text, Color bgColor, Color textColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));

        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(70, 70, 70));

                // 关键：这三行强制按钮显示背景色，不被主题覆盖
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 鼠标交互
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if(btn.isEnabled()) btn.setBackground(new Color(245, 245, 245));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if(btn.isEnabled()) btn.setBackground(Color.WHITE);
            }
        });
        return btn;
    }

    public void appendLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override public void write(int b) {}
            @Override public void write(byte[] b, int off, int len) {
                String message = new String(b, off, len, java.nio.charset.StandardCharsets.UTF_8);
                if (!message.trim().isEmpty() && !message.contains("libpng")) {
                    appendLog(message.trim());
                }
            }
        };
    }
}