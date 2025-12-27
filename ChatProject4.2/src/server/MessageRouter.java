package server;

import common.protocol.Message;
import common.utils.JsonUtil;
import server.service.FindPwdService;
import server.service.LoginService;
import server.service.RegisterService;
import server.FileTransferHandler;

/**
 * 消息路由器 - 负责将消息分发到对应的处理器
 * 用于解耦消息分发逻辑，使ClientHandler更简洁
 */
public class MessageRouter {

    /**
     * 路由消息到对应的处理器
     * @param msg 消息对象
     * @param handler 客户端处理器
     */

//    route统一路由入口
    public static void route(Message msg, ClientHandler handler) {
        String type = msg.getType();

        if (type == null || type.isEmpty()) {
            sendError(handler, "消息类型不能为空");
            return;
        }

        // 记录消息类型
        ServerLogger.debug("路由消息: type=" + type + ", from=" +
                (handler.getUserId() != null ? handler.getUserId() : "未登录"));

        try {
            switch (type) {
                case "login":
                    handleLogin(msg, handler);
                    break;

                case "register":
                    handleRegister(msg, handler);
                    break;

                case "text":
                    handleTextMessage(msg, handler);
                    break;

                case "group":
                    handleGroupMessage(msg, handler);
                    break;

                case "heartbeat":
                    handleHeartbeat(handler);
                    break;

                case "getOnlineUsers":
                    handleGetOnlineUsers(handler);
                    break;

                case "logout":
                    handleLogout(handler);
                    break;

                case "ping":
                    handlePing(handler);
                    break;

                case "findPwd":
                    handleFindPassword(msg, handler);
                    break;

//如果有做文件传输和窗口抖动
                case "file":
                    // 文件消息转发：直接发给目标用户
                    forwardMessageToTarget(msg, handler);
                    break;

                case "shake":
                    // 窗口抖动：直接发给目标用户
                    forwardMessageToTarget(msg, handler);
                    break;

                case "screenshot":
                    // 截图（其实本质也是发图片/文件）：转发
                    forwardMessageToTarget(msg, handler);
                    break;

                default:
                    ServerLogger.warn("未知消息类型: " + type);
                    sendError(handler, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            ServerLogger.exception("路由消息失败: type=" + type, e);
            sendError(handler, "服务器处理消息时发生错误");
        }
    }

    /**
     * 处理登录请求
     */
    private static void handleLogin(Message msg, ClientHandler handler) {
        try {
            LoginService loginService = new LoginService();
            loginService.handleLogin(msg, handler);
            ServerLogger.log("处理登录请求: account=" + msg.getAccount());
        } catch (Exception e) {
            ServerLogger.exception("登录处理失败", e);
            sendError(handler, "登录处理失败");
        }
    }

    /**
     * 处理注册请求
     */
    private static void handleRegister(Message msg, ClientHandler handler) {
        try {
            RegisterService registerService = new RegisterService();
            registerService.handleRegister(msg, handler);
            ServerLogger.log("处理注册请求: nickname=" + msg.getNickname());
        } catch (Exception e) {
            ServerLogger.exception("注册处理失败", e);
            sendError(handler, "注册处理失败");
        }
    }

    /**
     * 处理私聊消息
     */
    private static void handleTextMessage(Message msg, ClientHandler handler) {
        String userId = handler.getUserId();
        if (userId == null) {
            sendError(handler, "请先登录");
            return;
        }

        String toUser = msg.getTo();
        if (toUser == null || toUser.isEmpty()) {
            sendError(handler, "接收者不能为空");
            return;
        }

        // 设置发送者
        msg.setFrom(userId);

        // 转发消息
        if (OnlineUserManager.forwardMessage(msg)) {
            ServerLogger.log("私聊消息转发成功: " + userId + " -> " + toUser);

            // 给发送者确认
            Message ack = new Message("ack");
            ack.setContent("消息已发送给 " + toUser);
            try {
                handler.send(ack);
            } catch (Exception e) {
                ServerLogger.error("发送确认消息失败");
            }
        } else {
            sendError(handler, "用户 " + toUser + " 不在线");
            ServerLogger.warn("用户不在线: " + userId + " -> " + toUser);
        }
    }

    /**
     * 处理群聊消息
     */
    private static void handleGroupMessage(Message msg, ClientHandler handler) {
        String userId = handler.getUserId();
        if (userId == null) {
            sendError(handler, "请先登录");
            return;
        }

        msg.setFrom(userId);

        // 广播群聊消息
        OnlineUserManager.broadcastGroupMessage(msg, userId);
        ServerLogger.log("群聊消息广播: " + userId);

        // 给发送者确认
        Message ack = new Message("ack");
        ack.setContent("群聊消息已发送");
        try {
            handler.send(ack);
        } catch (Exception e) {
            ServerLogger.error("发送群聊确认失败");
        }
    }

    /**
     * 处理心跳
     */
    private static void handleHeartbeat(ClientHandler handler) {
        try {
            Message heartbeatResp = new Message("heartbeat");
            heartbeatResp.setContent("pong");
            handler.send(heartbeatResp);
            ServerLogger.debug("心跳响应: " + handler.getUserId());
        } catch (Exception e) {
            ServerLogger.error("发送心跳响应失败");
        }
    }

    /**
     * 处理获取在线用户列表
     */
    private static void handleGetOnlineUsers(ClientHandler handler) {
        try {
            String[] users = OnlineUserManager.getAllOnlineUsers();
            String userList = String.join(",", users);

            Message resp = new Message("onlineList");
            resp.setContent(userList);

            // 可选：添加在线人数
            resp.putExtra("count", String.valueOf(users.length));

            handler.send(resp);
            ServerLogger.log("返回在线用户列表: count=" + users.length);
        } catch (Exception e) {
            ServerLogger.exception("获取在线用户列表失败", e);
            sendError(handler, "获取在线用户失败");
        }
    }

    /**
     * 处理Ping请求（简单测试）
     */
    private static void handlePing(ClientHandler handler) {
        try {
            Message pong = new Message("pong");
            pong.setContent("服务器正常");
            pong.putExtra("time", String.valueOf(System.currentTimeMillis()));
            handler.send(pong);
        } catch (Exception e) {
            ServerLogger.error("发送Pong响应失败");
        }
    }

    /**
     * 处理退出登录
     */
    private static void handleLogout(ClientHandler handler) {
        String userId = handler.getUserId();
        ServerLogger.log("用户退出: " + userId);

        // 发送退出确认
        try {
            Message logoutAck = new Message("logoutResult");
            logoutAck.setContent("success");
            handler.send(logoutAck);
        } catch (Exception e) {
            ServerLogger.error("发送退出确认失败");
        }

        // 断开连接
        handler.disconnect();
    }

    /**
     * 发送错误消息
     */
    private static void sendError(ClientHandler handler, String errorMsg) {
        try {
            Message error = new Message("error");
            error.setContent(errorMsg);
            handler.send(error);
            ServerLogger.warn("发送错误消息: " + errorMsg);
        } catch (Exception e) {
            ServerLogger.error("发送错误消息失败: " + errorMsg);
        }
    }

    /**
     * 发送成功消息
     */
    public static void sendSuccess(ClientHandler handler, String message) {
        try {
            Message success = new Message("success");
            success.setContent(message);
            handler.send(success);
        } catch (Exception e) {
            ServerLogger.error("发送成功消息失败: " + message);
        }
    }
    private static void handleFindPassword(Message msg, ClientHandler handler) {
        try {
            FindPwdService findPwdService = new FindPwdService();
            findPwdService.handleFindPassword(msg, handler);
            ServerLogger.log("处理找回密码请求: account=" + msg.getAccount() + ", nickname=" + msg.getNickname());
        } catch (Exception e) {
            ServerLogger.exception("找回密码处理失败", e);
            sendError(handler, "找回密码处理失败");
        }
    }

    /**
     * 通用消息转发：查找目标用户并转发消息
     */
    private static void forwardMessageToTarget(Message msg, ClientHandler senderHandler) {
        String targetUser = msg.getTo(); // 目标账号

        // 如果是群聊或者特殊的群发逻辑，这里要改。
        // 如果是点对点文件/抖动：
        if (targetUser != null) {
            ClientHandler targetHandler = OnlineUserManager.getUser(targetUser);
            if (targetHandler != null) {
                targetHandler.send(msg);
                ServerLogger.debug("转发消息 [" + msg.getType() + "] 从 " + msg.getFrom() + " 到 " + targetUser);
            } else {
                // 目标不在线，可选：存入离线消息或返回错误
                // 这里简单处理：告诉发送者对方不在线
                Message errorMsg = new Message("error");
                errorMsg.setContent("用户 " + targetUser + " 不在线，无法发送 " + msg.getType());
                senderHandler.send(errorMsg);
            }
        }
    }

}