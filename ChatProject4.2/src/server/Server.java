package server;

import javax.swing.*;
import client.ui.AppLookAndFeel;

public class Server {
    private static ServerGUI gui;

    public static void main(String[] args) {
        // 1. 强制 JVM 环境使用 UTF-8
        try {
            System.setProperty("file.encoding", "UTF-8");
            java.lang.reflect.Field charset = java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            // 忽略
        }

        // 2. 启动 UI
        SwingUtilities.invokeLater(() -> {
            AppLookAndFeel.setLookAndFeel();
            gui = new ServerGUI(); // 只负责创建窗口
            gui.setVisible(true);
            gui.appendLog("C.Lucky 服务器旗舰版已就绪，请点击配置面板启动监听...");
        });
    }

    public static ServerGUI getServerGUI() {
        return gui;
    }
}