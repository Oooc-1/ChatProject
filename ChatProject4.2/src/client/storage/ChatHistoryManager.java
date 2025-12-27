package client.storage; // 包名（需与目录结构一致）


import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 聊天记录管理器：本地存储聊天记录（TXT文件），支持保存和加载
 * 存储路径：项目根目录/chatHistory/[type]_[targetId].txt（type=single/group，targetId=对方账号/群ID）
 */
public class ChatHistoryManager {
    private final String filePath; // 聊天记录存储文件路径

    /**
     * 初始化聊天记录管理器
     * @param chatType 聊天类型（single/group）
     * @param targetId 目标标识（单聊=对方账号，群聊=群ID）
     */
    public ChatHistoryManager(String chatType, String targetId) {
        // 构建存储目录（项目根目录/chatHistory，不存在则创建）
        File dir = new File("chatHistory");
        if (!dir.exists()) {
            dir.mkdirs(); // 创建多级目录
        }
        // 构建文件路径：chatHistory/[type]_[targetId].txt
        this.filePath = dir.getAbsolutePath() + File.separator + chatType + "_" + targetId + ".txt";
    }

    /**
     * 保存聊天记录到本地文件（覆盖式保存）
     * @param content 聊天记录内容（ChatFrame中消息显示区域的文本）
     */
    public void saveChatHistory(String content) {
        if (content == null || content.isEmpty()) {
            return; // 空内容不保存
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)
        )) {
            writer.write(content);
            System.out.println("聊天记录保存成功：" + filePath);
        } catch (IOException e) {
            System.err.println("聊天记录保存失败：" + e.getMessage() + "，路径：" + filePath);
            e.printStackTrace();
        }
    }

    /**
     * 从本地文件加载聊天记录
     * @return 聊天记录内容（无记录返回空字符串）
     */
    public String loadChatHistory() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("无历史聊天记录：" + filePath);
            return ""; // 文件不存在，返回空
        }

        StringBuilder history = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                history.append(line).append("\n"); // 保留换行符
            }
            System.out.println("聊天记录加载成功：" + filePath + "，长度：" + history.length() + "字符");
        } catch (IOException e) {
            System.err.println("聊天记录加载失败：" + e.getMessage() + "，路径：" + filePath);
            e.printStackTrace();
            return "";
        }

        return history.toString();
    }

    /**
     * 删除聊天记录（可选功能，供用户手动删除）
     * @return 删除成功返回true，失败返回false
     */
    public boolean deleteChatHistory() {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("聊天记录删除成功：" + filePath);
                return true;
            } else {
                System.err.println("聊天记录删除失败：" + filePath);
                return false;
            }
        }
        return true; // 文件不存在，视为删除成功
    }
}
