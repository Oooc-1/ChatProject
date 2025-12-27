package server;

import common.protocol.Message;
import common.utils.JsonUtil;
import server.service.LoginService;
import server.service.RegisterService;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String userId;
    private long lastHeartbeatTime;
    private volatile boolean running = true;
    private final int clientId;
    private final String clientAddress;
    private static final long HEARTBEAT_TIMEOUT = 45000; // 45秒超时
    private static final long CHECK_INTERVAL = 15000; // 每15秒检查一次
    private final LoginService loginService = new LoginService();
    private final RegisterService registerService = new RegisterService();

//    构造函数，初始化I/O流
    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
        this.clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.lastHeartbeatTime = System.currentTimeMillis();

        try {
            // 设置字符编码为UTF-8
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            System.out.println("🔄 客户端 #" + clientId + " 已连接: " + clientAddress);

        } catch (Exception e) {
            System.err.println("❌ 初始化客户端 #" + clientId + " 失败: " + e.getMessage());
            running = false;
//             I/O 流初始化失败时，主动终止当前客户端处理器（ClientHandler）的后续逻辑，避免程序进入无效或错误状态。
        }
    }

//    主处理循环
    @Override
    public void run() {
        try {
            // 启动心跳检测线程
            Thread heartbeatThread = new Thread(this::heartbeatCheck);
            heartbeatThread.setDaemon(true);//设置为守护线程
            heartbeatThread.start();

            // 主消息处理循环
            String line;
            while (running && (line = reader.readLine()) != null) {
                processMessage(line);//处理每行Json消息
            }

        } catch (IOException e) {
            if (running) { // 只有非主动断开才输出错误
                System.out.println("⚠️  客户端 #" + clientId + " 连接异常: " +
                        (userId != null ? userId : "未登录用户"));
            }
        } finally {
            disconnect();// 确保清理资源
        }
    }

    private void processMessage(String jsonLine) {
        try {
            // 解析JSON消息
            Message msg = JsonUtil.fromJson(jsonLine);//JSon反序列化

            // 更新最后活跃时间
            lastHeartbeatTime = System.currentTimeMillis();

            // 记录接收到的消息
            System.out.println("📨 收到消息 #" + clientId +
                    (userId != null ? " (" + userId + ")" : "") +
                    ": " + msg.getType());

            // 根据消息类型路由处理（根据消息的类型或内容，将它分发（转发）给对应的处理模块。）
            // 解耦设计，降低系统各部分之间的依赖程度，使它们尽可能独立、互不影响。
            routeMessage(msg);

        } catch (Exception e) {
            System.err.println("❌ 处理消息失败 #" + clientId + ": " + e.getMessage());
            sendError("消息格式错误或处理失败");
        }
    }

    private void routeMessage(Message msg) {
        String type = msg.getType();

        if (type == null || type.isEmpty()) {
            sendError("消息类型不能为空");
            return;
        }

        try {
            switch (type) {
                case "login":
                    loginService.handleLogin(msg, this);
                    break;

                case "register":
                    registerService.handleRegister(msg, this);
                    break;

                case "text":
                    handleTextMessage(msg);
                    break;

                case "group":
                    handleGroupMessage(msg);
                    break;

                case "heartbeat":
                    handleHeartbeat();
                    break;

                case "getOnlineUsers":
                    handleGetOnlineUsers();
                    break;

                case "logout":
                    handleLogout();
                    break;

                default:
                    System.err.println("❓ 未知消息类型 #" + clientId + ": " + type);
                    sendError("未知消息类型: " + type);
            }
        } catch (Exception e) {
            System.err.println("❌ 路由消息失败 #" + clientId + ": " + e.getMessage());
            e.printStackTrace();
            sendError("服务器内部错误");
        }
    }

    private void handleTextMessage(Message msg) {
        String toUser = msg.getTo();
        if (toUser == null || toUser.isEmpty()) {
            sendError("接收者不能为空");
            return;
        }

        // 验证发送者是否已登录
        if (userId == null) {
            sendError("请先登录");
            return;
        }

        // 设置发送者
        msg.setFrom(userId);

        // 转发消息
        if (OnlineUserManager.forwardMessage(msg)) {
            System.out.println("💬 私聊消息 #" + clientId + ": " + userId + " → " + toUser);
        } else {
            // 用户不在线，返回错误
            sendError("用户 " + toUser + " 不在线");
            System.out.println("❌ 用户不在线 #" + clientId + ": " + userId + " → " + toUser);
        }
    }

    private void handleGroupMessage(Message msg) {
        // 验证发送者是否已登录
        if (userId == null) {
            sendError("请先登录");
            return;
        }

        msg.setFrom(userId);

        // 广播群聊消息
        OnlineUserManager.broadcastGroupMessage(msg, userId);
        System.out.println("📢 群聊消息 #" + clientId + ": " + userId + " 发送群消息");
    }

    private void handleHeartbeat() {
        Message heartbeatResp = new Message("heartbeat");
        heartbeatResp.setContent("pong");
        send(heartbeatResp);
    }

    private void handleGetOnlineUsers() {
        try {
            Message resp = new Message("onlineList");
            String[] users = OnlineUserManager.getAllOnlineUsers();
            resp.setContent(String.join(",", users));
            send(resp);
            System.out.println("📋 返回在线列表 #" + clientId + ": " + userId);
        } catch (Exception e) {
            System.err.println("❌ 获取在线用户列表失败 #" + clientId + ": " + e.getMessage());
        }
    }

    private void handleLogout() {
        System.out.println("👋 用户主动退出 #" + clientId + ": " + userId);
        disconnect();
    }


//心跳超时检测
    private void heartbeatCheck() {

        while (running) {
            try {
                TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL);//休眠15秒

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastHeartbeatTime > HEARTBEAT_TIMEOUT) {//检查是否超过45秒未收到消息
                    System.out.println("💔 心跳超时 #" + clientId +
                            (userId != null ? " (" + userId + ")" : ""));
                    disconnect();//断开连接
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("心跳检查异常 #" + clientId + ": " + e.getMessage());
            }
        }
    }

    private void sendError(String errorMsg) {
        Message error = new Message("error");
        error.setContent(errorMsg);
        send(error); // 直接调用，无需 try-catch
    }

    public void send(Message msg) {
        if (writer == null || !running) {
            return; // 静默失败或记录日志
        }

        try {
            synchronized (writer) {
                String json = JsonUtil.toJson(msg);
                writer.write(json);
                writer.write("\n");
                writer.flush();

                System.out.println("📤 发送消息 #" + clientId +
                        (userId != null ? " (" + userId + ")" : "") +
                        ": " + msg.getType());
            }
        } catch (IOException e) {
            System.err.println("❌ 发送消息失败 #" + clientId + ": " + e.getMessage());
            // 主动断开连接
            disconnect();
        }
    }

    public void disconnect() {
        if (!running) return;

        running = false;

        // 从在线用户中移除
        if (userId != null) {
            OnlineUserManager.removeUser(userId);
        }

        // 关闭资源
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            System.out.println("🔌 连接关闭 #" + clientId + ": " + clientAddress +
                    (userId != null ? " (" + userId + ")" : ""));

        } catch (IOException e) {
            System.err.println("关闭连接资源时出错 #" + clientId + ": " + e.getMessage());
        }
    }

    public void setUserId(String id) {
        this.userId = id;
        OnlineUserManager.addUser(id, this);
        lastHeartbeatTime = System.currentTimeMillis(); // 重置心跳时间

        System.out.println("✅ 用户登录成功 #" + clientId + ": " + id);
    }

    public String getUserId() {
        return userId;
    }

    public boolean isRunning() {
        return running;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getClientAddress() {
        return clientAddress;
    }
}