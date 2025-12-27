// src/client/Main.java
package client;

import client.ui.AppLookAndFeel;
import client.ui.LoginFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // ✅ 第一步：设置全局 UI 外观（必须在创建任何 JFrame 之前！）
        AppLookAndFeel.setLookAndFeel();

        // ✅ 第二步：在事件调度线程中运行 GUI 和连接逻辑
        SwingUtilities.invokeLater(() -> {
            // 尝试连接服务器
            Client.connectServer("127.0.0.1", 5000);

            if (!Client.isConnected()) {
                JOptionPane.showMessageDialog(
                        null,
                        "无法连接到服务器，请检查服务端是否已启动！",
                        "连接失败",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }

            // 显示登录界面
            new LoginFrame().setVisible(true);
        });
    }
}