package server;

import common.protocol.Message;

/**
 * 文件传输及扩展消息处理器
 * 负责处理非文本类的特殊消息转发
 */
public class FileTransferHandler {

    /**
     * 处理文件消息
     */
    public static void handleFileTransfer(Message msg, ClientHandler sender) {
        // 服务端只做中转，不解析文件内容
        // 1. 检查接收者
        String toUser = msg.getTo();
        if (toUser == null || toUser.isEmpty()) {
            return; // 或发送错误回执
        }

        // 2. 补充发送者信息
        msg.setFrom(sender.getUserId());

        // 3. 转发
        boolean success = OnlineUserManager.forwardMessage(msg);

        if (success) {
            ServerLogger.log("📁 文件转发: " + sender.getUserId() + " -> " + toUser +
                    " (文件名: " + msg.getExtra("fileName") + ")");
        } else {
            // 对方不在线，可以提示发送者
            Message error = new Message("error");
            error.setContent("对方不在线，文件发送失败");
            sender.send(error);
        }
    }

    /**
     * 处理截图消息 (逻辑同文件，本质都是Base64转发)
     */
    public static void handleScreenshot(Message msg, ClientHandler sender) {
        String toUser = msg.getTo();
        msg.setFrom(sender.getUserId());

        if (OnlineUserManager.forwardMessage(msg)) {
            ServerLogger.log("🖼️ 截图转发: " + sender.getUserId() + " -> " + toUser);
        }
    }

    /**
     * 处理窗口抖动
     */
    public static void handleShake(Message msg, ClientHandler sender) {
        String toUser = msg.getTo();
        msg.setFrom(sender.getUserId());

        if (OnlineUserManager.forwardMessage(msg)) {
            ServerLogger.log("📳 窗口抖动: " + sender.getUserId() + " -> " + toUser);
        }
    }
}