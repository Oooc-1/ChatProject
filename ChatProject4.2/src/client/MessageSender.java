package client;

import common.protocol.Message;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * 消息发送器：封装客户端向服务器发送消息的逻辑，遵循协议规范
 * 功能：
 * 1. 基础：发送登录/注册/私聊/群聊等消息
 * 2. 群管理：发送群公告、设置群元老、修改群昵称、移除成员、禁言/解除禁言、退出群聊等
 * 自动添加\n结束符和UTF-8编码
 */
public class MessageSender {
    // 唯一Socket实例（删除重复的socket变量）
    private static Socket clientSocket;
    private static OutputStream outputStream;
    private static boolean isInitialized;
    private static BufferedWriter writer;

    // 服务器配置（关键：IP前无空格！端口与服务器一致）
    private static final String SERVER_IP = "192.168.123.145"; // 无空格！
    private static final int SERVER_PORT = 5000;

    /**
     * 初始化消息发送器（登录成功后调用，绑定Socket）
     * @param socket 客户端与服务器的连接Socket
     */
    public static void init(Socket socket) throws Exception {
        if (socket == null || socket.isClosed()) {
            throw new Exception("Socket未连接或已关闭，无法初始化消息发送器");
        }
        clientSocket = socket;
        outputStream = socket.getOutputStream();
        writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        isInitialized = true;
    }

    /**
     * 内部方法：获取可用的Socket连接（自动处理重连/初始化）
     * @return 可用的Socket，null表示连接失败
     */
    private static Socket getAvailableSocket() {
        // 已有可用连接，直接返回
        if (clientSocket != null && !clientSocket.isClosed()) {
            return clientSocket;
        }

        // 重新创建连接（无静态代码块，按需创建）
        try {
            clientSocket = new Socket(SERVER_IP, SERVER_PORT); // 无空格的IP
            outputStream = clientSocket.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            isInitialized = true;
            return clientSocket;
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "服务器IP错误/不可达：" + SERVER_IP + "\n请检查IP是否正确、服务器是否开机", "网络错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "连接服务器失败：" + e.getMessage() + "\n请检查服务器是否启动、端口是否开放", "网络错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送JSON格式消息（核心方法，自动添加\n结束符和UTF-8编码）
     * @param jsonMsg 符合协议的JSON字符串
     */
    private static void sendJson(String jsonMsg) {
        // 校验初始化状态
        if (!isInitialized || clientSocket == null || clientSocket.isClosed() || outputStream == null) {
            System.err.println("消息发送失败：发送器未初始化或Socket已关闭");
            JOptionPane.showMessageDialog(null, "未连接到服务器，消息发送失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            byte[] msgBytes = (jsonMsg + "\n").getBytes(StandardCharsets.UTF_8);
            outputStream.write(msgBytes);
            outputStream.flush();
            System.out.println("发送消息：" + jsonMsg);
        } catch (Exception e) {
            System.err.println("消息发送失败：" + e.getMessage() + "，消息内容：" + jsonMsg);
            JOptionPane.showMessageDialog(null, "消息发送失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * 重载send方法：支持直接传入Message对象
     * @param message Message实体对象
     */
    public static void send(Message message) {
        if (message == null) {
            System.err.println("消息发送失败：Message对象不能为空");
            return;
        }
        String jsonMsg = "";
        switch (message.getType()) {
            case "login":
                jsonMsg = MessageParser.buildLoginMsg(message.getAccount(), message.getPassword());
                break;
            case "singleChat":
                jsonMsg = MessageParser.buildSingleChatMsg(message.getFrom(), message.getTo(), message.getContent());
                break;
            case "groupChat":
                String groupId = message.getExtra("groupId");
                jsonMsg = MessageParser.buildGroupChatMsg(message.getFrom(), groupId, message.getContent());
                break;
            case "register":
                jsonMsg = MessageParser.buildRegisterMsg(message.getNickname(), message.getPassword());
                break;
            case "groupNotice":
                jsonMsg = MessageParser.buildGroupNoticeMsg(message.getFrom(), message.getExtra("groupId"), message.getContent());
                break;
            case "groupElder":
                jsonMsg = MessageParser.buildGroupElderMsg(message.getFrom(), message.getExtra("groupId"), message.getExtra("memberId"));
                break;
            case "groupNickname":
                jsonMsg = MessageParser.buildGroupNicknameMsg(message.getFrom(), message.getExtra("groupId"), message.getExtra("memberId"), message.getExtra("nickname"));
                break;
            case "removeGroupMember":
                jsonMsg = MessageParser.buildRemoveGroupMemberMsg(message.getFrom(), message.getExtra("groupId"), message.getExtra("memberId"));
                break;
            case "groupMute":
                jsonMsg = MessageParser.buildGroupMuteMsg(message.getFrom(), message.getExtra("groupId"), message.getExtra("memberId"), Boolean.parseBoolean(message.getExtra("muteStatus")));
                break;
            case "quitGroup":
                jsonMsg = MessageParser.buildQuitGroupMsg(message.getFrom(), message.getExtra("groupId"));
                break;
            default:
                System.err.println("不支持的消息类型：" + message.getType());
                jsonMsg = "";
                break;
        }
        if (!jsonMsg.isEmpty()) {
            sendJson(jsonMsg);
        }
    }

    // ------------------- 基础功能快捷方法 -------------------
    public static void sendLoginRequest(String account, String password) {
        String jsonMsg = MessageParser.buildLoginMsg(account, password);
        sendJson(jsonMsg);
    }

    public static void sendRegisterRequest(String nickname, String password) {
        String jsonMsg = MessageParser.buildRegisterMsg(nickname, password);
        sendJson(jsonMsg);
    }

    public static void sendSingleChatMsg(String fromAccount, String toAccount, String content) {
        String jsonMsg = MessageParser.buildSingleChatMsg(fromAccount, toAccount, content);
        sendJson(jsonMsg);
    }

    public static void sendGroupChatMsg(String fromAccount, String groupId, String content) {
        String jsonMsg = MessageParser.buildGroupChatMsg(fromAccount, groupId, content);
        sendJson(jsonMsg);
    }

    // ------------------- 群管理功能快捷方法 -------------------
    public static void saveGroupNotice(String operator, String groupId, String notice) {
        if (operator == null || groupId == null || notice == null) {
            System.err.println("保存群公告失败：参数不能为空");
            JOptionPane.showMessageDialog(null, "保存群公告失败：参数不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String jsonMsg = MessageParser.buildGroupNoticeMsg(operator, groupId, notice);
        sendJson(jsonMsg);
    }

    public static void setGroupNickname(String operator, String groupId, String memberId, String nickname) {
        if (operator == null || groupId == null || memberId == null || nickname == null) {
            System.err.println("设置群昵称失败：参数不能为空");
            JOptionPane.showMessageDialog(null, "设置群昵称失败：参数不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String jsonMsg = MessageParser.buildGroupNicknameMsg(operator, groupId, memberId, nickname);
        sendJson(jsonMsg);
    }

    public static void removeGroupMember(String operator, String groupId, String memberId) {
        if (operator == null || groupId == null || memberId == null) {
            System.err.println("移除群成员失败：参数不能为空");
            JOptionPane.showMessageDialog(null, "移除群成员失败：参数不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String jsonMsg = MessageParser.buildRemoveGroupMemberMsg(operator, groupId, memberId);
        sendJson(jsonMsg);
    }

    /**
     * 禁言群成员（核心修复：先获取可用Socket，再操作）
     */
    public static void muteGroupMember(String operatorAccount, String groupId, String memberAccount) {
        // 1. 先获取可用Socket，避免NPE
        Socket socket = getAvailableSocket();
        if (socket == null) {
            JOptionPane.showMessageDialog(null, "禁言失败：未连接到服务器", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String jsonRequest = String.format(
                    "{\n" +
                            "  \"type\": \"groupMute\",\n" +
                            "  \"operatorAccount\": \"%s\",\n" +
                            "  \"groupId\": \"%s\",\n" +
                            "  \"memberAccount\": \"%s\",\n" +
                            "  \"muteDuration\": \"permanent\"\n" +
                            "}\n",
                    operatorAccount, groupId, memberAccount
            );

            OutputStream os = socket.getOutputStream();
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();
            JOptionPane.showMessageDialog(null, "成员禁言成功！", "成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "网络异常，禁言操作失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 解除禁言群成员（同理：先判空，再操作）
     */
    public static void unmuteGroupMember(String operatorAccount, String groupId, String memberAccount) {
        Socket socket = getAvailableSocket();
        if (socket == null) {
            JOptionPane.showMessageDialog(null, "解除禁言失败：未连接到服务器", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String jsonRequest = String.format(
                    "{\n" +
                            "  \"type\": \"groupUnmute\",\n" +
                            "  \"operatorAccount\": \"%s\",\n" +
                            "  \"groupId\": \"%s\",\n" +
                            "  \"memberAccount\": \"%s\"\n" +
                            "}\n",
                    operatorAccount, groupId, memberAccount
            );

            OutputStream os = socket.getOutputStream();
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();
            JOptionPane.showMessageDialog(null, "成员解除禁言成功！", "成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "网络异常，解除禁言操作失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void quitGroup(String operator, String groupId) {
        if (operator == null || groupId == null) {
            System.err.println("退出群聊失败：参数不能为空");
            JOptionPane.showMessageDialog(null, "退出群聊失败：参数不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String jsonMsg = MessageParser.buildQuitGroupMsg(operator, groupId);
        sendJson(jsonMsg);
    }

    public static void addGroupMember(String operatorAccount, String groupId, String newMemberAccount) {
        Socket socket = getAvailableSocket();
        if (socket == null) {
            JOptionPane.showMessageDialog(null, "添加成员失败：未连接到服务器", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String jsonRequest = String.format(
                    "{\n" +
                            "  \"type\": \"addGroupMember\",\n" +
                            "  \"operatorAccount\": \"%s\",\n" +
                            "  \"groupId\": \"%s\",\n" +
                            "  \"newMemberAccount\": \"%s\"\n" +
                            "}\n",
                    operatorAccount, groupId, newMemberAccount
            );
            OutputStream os = socket.getOutputStream();
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();
            JOptionPane.showMessageDialog(null, "添加群成员成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "网络异常，添加群成员失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------- 工具方法 -------------------
    public static void close() {
        isInitialized = false;
        try {
            if (writer != null) {
                writer.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getGroupNotice(String currentAccount, String groupId) {
        return "";
    }

    public static boolean isInitialized() {
        return isInitialized;
    }
}