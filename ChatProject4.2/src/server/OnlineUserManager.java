package server;

import common.protocol.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OnlineUserManager {
    // 在线用户映射：用户ID -> ClientHandler。
    // 用 用户ID 快速找到对应的 客户端连接对象（ClientHandler），实现精准通信。
    // ConcurrentHashMap：线程安全的哈希表
    private static final ConcurrentMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // 添加用户到在线列表
    public static void addUser(String userId, ClientHandler handler) {
        if (userId == null || userId.isEmpty() || handler == null) {
            System.err.println("❌ 添加在线用户失败：参数无效");
            return;
        }

        // 检查用户是否已在线，处理重复登录
        if (onlineUsers.containsKey(userId)) {
            ClientHandler oldHandler = onlineUsers.get(userId);
            oldHandler.disconnect(); // 断开旧连接，保证同一账号只能单点登录
            System.out.println("⚠️ 用户 " + userId + " 已在线，已断开旧连接");
        }

        onlineUsers.put(userId, handler);
        System.out.println("✅ 用户上线: " + userId);

        // 广播上线通知给其他在线用户
        broadcastOnlineStatus(userId, "online");
    }

    // 移除用户
    public static void removeUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        ClientHandler handler = onlineUsers.remove(userId);
        if (handler != null) {
            System.out.println("📤 用户下线: " + userId);

            // 广播下线通知给其他在线用户
            broadcastOnlineStatus(userId, "offline");
        }
    }

    // 转发私聊消息
    public static boolean forwardMessage(Message msg) {
        String toUserId = msg.getTo();
        if (toUserId == null || toUserId.isEmpty()) {
            return false;
        }

        ClientHandler target = onlineUsers.get(toUserId);

        if (target != null && target.isRunning()) {
            try {
                target.send(msg);//直接发送
                return true;
            } catch (Exception e) {
                ServerLogger.error("转发消息失败 [" + msg.getFrom() + " → " + toUserId + "]: " + e.getMessage());
                return false;
            }
        }
        return false; // 用户不在线或发送失败
    }

    // 广播群聊消息
    public static void broadcastGroupMessage(Message msg, String excludeUserId) {
        if (msg == null) return;

        String fromUser = msg.getFrom();
        System.out.println("📢 广播群聊消息，发送者: " + fromUser);

        int successCount = 0;
        int totalCount = onlineUsers.size() - (excludeUserId != null ? 1 : 0);

        for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
            String userId = entry.getKey();
            ClientHandler handler = entry.getValue();

            // 不发送给自己
            if (userId.equals(excludeUserId)) {
                continue;
            }

            if (handler.isRunning()) {
                try {
                    handler.send(msg);
                    successCount++;
                } catch (Exception e) {
//                    记录失败但不中断广播
                    System.err.println("❌ 广播消息给 " + userId + " 失败: " + e.getMessage());
                }
            }
        }

        System.out.println("  成功发送: " + successCount + "/" + totalCount + " 人");
    }

    // 广播系统消息
    public static void broadcastSystemMessage(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        Message systemMsg = new Message("system");
        systemMsg.setContent(content);
        systemMsg.setFrom("系统");

        // 在服务器日志中记录
        ServerGUI gui = Server.getServerGUI();
        if (gui != null) {
            gui.appendLog("📢 广播系统消息: " + content);
        }

        System.out.println("📢 广播系统消息: " + content);

        int successCount = 0;
        int totalCount = onlineUsers.size();

        for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
            ClientHandler handler = entry.getValue();

            if (handler.isRunning()) {
                try {
                    handler.send(systemMsg);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("❌ 发送系统消息给 " + entry.getKey() + " 失败");
                }
            }
        }

        System.out.println("  成功发送: " + successCount + "/" + totalCount + " 人");
    }

    // 广播用户上线/下线状态
    private static void broadcastOnlineStatus(String userId, String status) {
        if (userId == null || status == null) {
            return;
        }

        Message statusMsg = new Message(status);
        statusMsg.setContent(userId);

        int broadcastCount = 0;

        for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
            String targetUserId = entry.getKey();
            ClientHandler handler = entry.getValue();

            // 不通知自己
            if (targetUserId.equals(userId)) {
                continue;
            }

            if (handler.isRunning()) {
                try {
                    handler.send(statusMsg);
                    broadcastCount++;
                } catch (Exception e) {
                    System.err.println("❌ 广播状态给 " + targetUserId + " 失败");
                }
            }
        }

        System.out.println("  状态通知已发送给 " + broadcastCount + " 人");
    }

    /**
     * 根据用户账号获取对应的 ClientHandler 实例
     * @param userId 用户账号
     * @return 对应的 ClientHandler，如果用户不在线则返回 null
     */
    public static ClientHandler getUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return onlineUsers.get(userId);
    }

    // 获取所有在线用户ID
    public static String[] getAllOnlineUsers() {
        return onlineUsers.keySet().toArray(new String[0]);
    }

    // 获取在线用户数量
    public static int getOnlineCount() {
        return onlineUsers.size();
    }

    // 检查用户是否在线
    public static boolean isUserOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }

    // 踢出用户
    public static boolean kickUser(String userId) {
        ClientHandler handler = onlineUsers.get(userId);
        if (handler != null) {
            try {
                // 发送被踢通知
                Message kickMsg = new Message("kick");
                kickMsg.setContent("您已被管理员踢出");
                kickMsg.setFrom("系统");
                handler.send(kickMsg);

                // 短暂延迟确保消息发送
                Thread.sleep(100);

                // 断开连接
                handler.disconnect();
                return true;

            } catch (Exception e) {
                System.err.println("❌ 踢出用户失败 " + userId + ": " + e.getMessage());
            }
        }
        return false;
    }

    // 获取在线用户信息（用于控制台显示）
    public static List<String> getOnlineUsersInfo() {
        List<String> infoList = new ArrayList<>();

        for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
            String userId = entry.getKey();
            ClientHandler handler = entry.getValue();

            try {
                String ip = handler.getSocket().getInetAddress().getHostAddress();
                int port = handler.getSocket().getPort();
                String status = handler.isRunning() ? "在线" : "断开中";

                String info = String.format("%s [%s:%d] - %s",
                        userId, ip, port, status);
                infoList.add(info);

            } catch (Exception e) {
                infoList.add(userId + " [连接信息获取失败]");
            }
        }

        return infoList;
    }

    // 获取所有在线用户的ClientHandler
    public static List<ClientHandler> getAllClientHandlers() {
        return new ArrayList<>(onlineUsers.values());
    }
}