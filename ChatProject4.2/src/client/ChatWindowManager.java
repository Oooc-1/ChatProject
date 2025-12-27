package client;

import client.ui.ChatFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天窗口管理器：统一管理所有聊天窗口，避免重复打开
 */
public class ChatWindowManager {
    // 存储所有已打开的聊天窗口
    private static final List<ChatFrame> chatWindows = new ArrayList<>();

    /**
     * 添加聊天窗口到管理器
     * @param chatFrame 聊天窗口实例
     */
    public static void addChatWindow(ChatFrame chatFrame) {
        if (chatFrame == null) {
            return;
        }
        // 窗口关闭时从管理器移除
        chatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                chatWindows.remove(chatFrame);
            }
        });
        chatWindows.add(chatFrame);
    }

    /**
     * 根据聊天类型和目标标识获取已打开的窗口
     * @param chatType 聊天类型（single/group）
     * @param targetId 目标标识（对方账号/群ID）
     * @return 已打开的窗口实例，无则返回null
     */
    public static ChatFrame getChatWindow(String chatType, String targetId) {
        for (ChatFrame frame : chatWindows) {
            if (frame.getChatType().equals(chatType) && frame.getTargetId().equals(targetId)) {
                return frame;
            }
        }
        return null;
    }

    /**
     * 关闭所有聊天窗口（退出登录时调用）
     */
    public static void closeAllChatWindows() {
        for (ChatFrame frame : new ArrayList<>(chatWindows)) { // 遍历副本，避免并发修改
            frame.dispose(); // 关闭窗口（触发windowClosed事件，从管理器移除）
        }
    }
}