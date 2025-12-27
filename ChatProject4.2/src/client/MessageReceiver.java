package client;

import client.ui.UIManager;
import client.ui.ChatFrame;
import client.ClientContext;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 消息接收线程：独立线程接收服务器消息，按类型分发处理
 * 遵循协议：解析JSON格式消息，处理text/group/online/offline等类型
 */
public class MessageReceiver extends Thread {
    private final Socket clientSocket; // 客户端与服务器的连接Socket
    private BufferedReader reader; // 读取服务器消息的输入流
    private boolean isRunning; // 线程运行状态标志

    /**
     * 初始化消息接收线程
     * @param socket 客户端与服务器的连接Socket（登录成功后创建）
     */
    public MessageReceiver(Socket socket) {
        this.clientSocket = socket;
        this.isRunning = true;
        try {
            // 初始化输入流（UTF-8编码，遵循协议要求）
            this.reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "消息接收线程初始化失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String jsonMsg;
        // 持续读取服务器消息（每条消息以\n结束，协议要求）
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // 读取一行消息（阻塞直到收到消息）
                jsonMsg = reader.readLine();
                if (jsonMsg == null || jsonMsg.isEmpty()) {
                    continue;
                }
                // 解析并处理消息
                parseAndDispatchMessage(jsonMsg);
            } catch (IOException e) {
                if (isRunning) { // 非主动关闭时提示异常
                    JOptionPane.showMessageDialog(null, "与服务器断开连接！", "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
                break;
            }
        }
        // 线程结束时释放资源
        closeResources();
    }

    /**
     * 解析JSON消息并按类型分发处理
     * @param jsonMsg 服务器发送的JSON格式消息
     */
    private void parseAndDispatchMessage(String jsonMsg) {
        // 1. 获取消息类型（协议必填字段）
        String msgType = MessageParser.getMessageType(jsonMsg);
        if (msgType == null) {
            JOptionPane.showMessageDialog(null, "收到非法消息（无类型）：" + jsonMsg, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. 按消息类型分发处理
        switch (msgType) {
            case "text": // 私聊消息
                handleSingleChatMsg(jsonMsg);
                break;
            case "group": // 群聊消息
                handleGroupChatMsg(jsonMsg);
                break;
            case "online": // 用户上线通知
            case "offline": // 用户下线通知
                handleUserStatusMsg(jsonMsg, msgType);
                break;
            case "system": // 系统消息
                handleSystemMsg(jsonMsg);
                break;
            case "error": // 错误消息
                handleErrorMsg(jsonMsg);
                break;
            case "offlineMessages": // 离线消息（可选功能）
                handleOfflineMessages(jsonMsg);
                break;
            default:
                JOptionPane.showMessageDialog(null, "收到未支持的消息类型：" + msgType, "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 处理私聊消息：找到对应聊天窗口，显示消息
     */
    private void handleSingleChatMsg(String jsonMsg) {
        // 解析消息字段（from=发送者账号，content=消息内容）
        String fromAccount = MessageParser.getField(jsonMsg, "from");
        String content = MessageParser.getField(jsonMsg, "content");
        String currentAccount = ClientContext.getCurrentAccount(); // 从全局上下文获取当前登录账号

        // 校验字段完整性
        if (fromAccount == null || content == null || currentAccount == null) {
            JOptionPane.showMessageDialog(null, "私聊消息格式错误：" + jsonMsg, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 查找已打开的单聊窗口，无则新建
        ChatFrame chatFrame = ChatWindowManager.getChatWindow("single", fromAccount);
        if (chatFrame == null) {
            chatFrame = new ChatFrame(fromAccount, currentAccount);
            ChatWindowManager.addChatWindow(chatFrame); // 加入窗口管理器（避免重复打开）
            chatFrame.setVisible(true);
        }

        // 显示收到的消息
        chatFrame.receiveMessage(fromAccount, content);
    }

    /**
     * 处理群聊消息：找到对应群聊窗口，显示消息
     */
    /**
     * 处理群聊消息：找到对应群聊窗口，显示消息
     */
    private void handleGroupChatMsg(String jsonMsg) {
        // 解析消息字段（from=发送者账号，groupId=群ID，content=消息内容）
        String fromAccount = MessageParser.getField(jsonMsg, "from");
        String groupId = MessageParser.getField(jsonMsg, "groupId");
        String content = MessageParser.getField(jsonMsg, "content");
        String currentAccount = ClientContext.getCurrentAccount();

        // 校验字段完整性
        if (fromAccount == null || groupId == null || content == null || currentAccount == null) {
            JOptionPane.showMessageDialog(null, "群聊消息格式错误：" + jsonMsg, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 查找已打开的群聊窗口，无则新建
        ChatFrame chatFrame = ChatWindowManager.getChatWindow("group", groupId);
        if (chatFrame == null) {
            // ========== 核心修改：补充群成员数组参数 ==========
            // 方式1：从服务端/本地缓存获取真实群成员（推荐，需对接业务逻辑）
            String[] groupMembers = getGroupMembersFromServer(groupId);
            // 方式2：临时构造示例群成员（测试用，上线前替换为真实数据）
            // String[] groupMembers = {"无极大生(10000)", "低(10005)", "畅(10004)", currentAccount + "(我)"};

            // 调用4参数的群聊构造方法（groupId, 群名称, 当前账号, 群成员数组）
            chatFrame = new ChatFrame(groupId, "群聊-" + groupId, currentAccount, groupMembers);
            ChatWindowManager.addChatWindow(chatFrame);
            chatFrame.setVisible(true);
        }

        // 显示收到的消息
        chatFrame.receiveMessage(fromAccount, content);
    }

    /**
     * 从服务端/本地缓存获取群成员列表（需对接实际业务逻辑）
     * @param groupId 群ID
     * @return 群成员数组（格式：昵称(ID)）
     */
    private String[] getGroupMembersFromServer(String groupId) {
        // 示例：模拟从服务端获取群成员（实际需替换为HTTP/Socket请求）
        try {
            // 调用服务端接口，根据群ID查询成员列表
            // 此处为模拟数据，上线前替换为真实逻辑
            return new String[]{
                    "无极大生(10000)",
                    "低(10005)",
                    "畅(10004)",
                    ClientContext.getCurrentAccount() + "(我)" // 当前登录用户
            };
        } catch (Exception e) {
            // 获取失败时返回默认示例数据
            return new String[]{"默认成员(00000)"};
        }
    }

    /**
     * 处理用户上下线通知：通知UI更新好友列表状态
     */
    private void handleUserStatusMsg(String jsonMsg, String statusType) {
        // 解析字段（content=用户账号）
        String account = MessageParser.getField(jsonMsg, "content");
        if (account == null) {
            JOptionPane.showMessageDialog(null, "上下线通知格式错误：" + jsonMsg, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 通知UI管理器更新好友状态（在线=true，离线=false）
        boolean isOnline = "online".equals(statusType);
        UIManager.updateFriendStatus(account, isOnline);
    }

    /**
     * 处理系统消息：弹窗显示
     */
    private void handleSystemMsg(String jsonMsg) {
        String content = MessageParser.getField(jsonMsg, "content");
        JOptionPane.showMessageDialog(null, content, "系统通知", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 处理错误消息：弹窗显示错误原因
     */
    private void handleErrorMsg(String jsonMsg) {
        String content = MessageParser.getField(jsonMsg, "content");
        JOptionPane.showMessageDialog(null, "服务器错误：" + content, "错误", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 处理离线消息（可选功能）：登录后接收离线期间的消息
     */
    private void handleOfflineMessages(String jsonMsg) {
        String offlineMsgJson = MessageParser.getField(jsonMsg, "content");
        if (offlineMsgJson == null) {
            JOptionPane.showMessageDialog(null, "离线消息格式错误：" + jsonMsg, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 解析离线消息数组（此处简化处理，实际需循环解析每条消息并分发）
        JOptionPane.showMessageDialog(null, "收到" + offlineMsgJson.split(",").length + "条离线消息", "提示", JOptionPane.INFORMATION_MESSAGE);
        // 扩展：调用handleSingleChatMsg/handleGroupChatMsg分发每条离线消息
    }

    /**
     * 停止消息接收线程
     */
    public void stopReceiver() {
        this.isRunning = false;
        this.interrupt(); // 中断阻塞的readLine()
    }

    /**
     * 关闭输入流和Socket资源
     */
    private void closeResources() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}