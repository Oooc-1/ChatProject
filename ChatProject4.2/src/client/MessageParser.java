package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 消息解析器：封装JSON消息的构建与解析（基于Gson 2.8.9，消除过时警告）
 */
public class MessageParser {
    // 全局Gson实例（复用提升性能，Gson是线程安全的）
    private static final Gson GSON = new Gson();

    // 构建登录消息
    public static String buildLoginMsg(String account, String password) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "login");
        jsonObject.addProperty("account", escapeJson(account));
        jsonObject.addProperty("password", escapeJson(password));
        return GSON.toJson(jsonObject);
    }

    // 构建注册消息
    public static String buildRegisterMsg(String nickname, String password) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "register");
        jsonObject.addProperty("nickname", escapeJson(nickname));
        jsonObject.addProperty("password", escapeJson(password));
        return GSON.toJson(jsonObject);
    }

    // 构建单聊消息（统一type为singleChat，与MessageSender匹配）
    public static String buildSingleChatMsg(String from, String to, String content) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "singleChat");
        jsonObject.addProperty("from", escapeJson(from));
        jsonObject.addProperty("to", escapeJson(to));
        jsonObject.addProperty("content", escapeJson(content));
        return GSON.toJson(jsonObject);
    }

    // 构建群聊消息（统一type为groupChat，与MessageSender匹配）
    public static String buildGroupChatMsg(String from, String groupId, String content) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "groupChat");
        jsonObject.addProperty("from", escapeJson(from));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("content", escapeJson(content));
        return GSON.toJson(jsonObject);
    }

    // 构建群公告消息
    public static String buildGroupNoticeMsg(String operator, String groupId, String notice) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "groupNotice");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("content", escapeJson(notice));
        return GSON.toJson(jsonObject);
    }

    // 构建设置群元老消息
    public static String buildGroupElderMsg(String operator, String groupId, String memberId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "groupElder");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("memberId", escapeJson(memberId));
        return GSON.toJson(jsonObject);
    }

    // 构建设置群昵称消息
    public static String buildGroupNicknameMsg(String operator, String groupId, String memberId, String nickname) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "groupNickname");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("memberId", escapeJson(memberId));
        jsonObject.addProperty("nickname", escapeJson(nickname));
        return GSON.toJson(jsonObject);
    }

    // 构建移除群成员消息
    public static String buildRemoveGroupMemberMsg(String operator, String groupId, String memberId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "removeGroupMember");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("memberId", escapeJson(memberId));
        return GSON.toJson(jsonObject);
    }

    // 构建禁言/解除禁言消息
    public static String buildGroupMuteMsg(String operator, String groupId, String memberId, boolean isMute) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "groupMute");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        jsonObject.addProperty("memberId", escapeJson(memberId));
        jsonObject.addProperty("muteStatus", isMute);
        return GSON.toJson(jsonObject);
    }

    // 构建退出群聊消息
    public static String buildQuitGroupMsg(String operator, String groupId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "quitGroup");
        jsonObject.addProperty("from", escapeJson(operator));
        jsonObject.addProperty("groupId", escapeJson(groupId));
        return GSON.toJson(jsonObject);
    }

    /**
     * 解析JSON消息的类型（替换过时的JsonParser）
     */
    public static String getMessageType(String jsonMsg) {
        try {
            JsonObject jsonObject = GSON.fromJson(jsonMsg, JsonObject.class);
            return jsonObject.get("type").getAsString();
        } catch (Exception e) {
            System.err.println("解析消息类型失败：" + e.getMessage() + "，消息内容：" + jsonMsg);
            return null;
        }
    }

    /**
     * 解析JSON消息中的指定字段值（替换过时的JsonParser）
     */
    public static String getField(String jsonMsg, String fieldName) {
        try {
            JsonObject jsonObject = GSON.fromJson(jsonMsg, JsonObject.class);
            if (jsonObject.has(fieldName)) {
                return jsonObject.get(fieldName).getAsString();
            } else {
                System.err.println("字段[" + fieldName + "]不存在，消息内容：" + jsonMsg);
                return null;
            }
        } catch (Exception e) {
            System.err.println("解析字段[" + fieldName + "]失败：" + e.getMessage() + "，消息内容：" + jsonMsg);
            return null;
        }
    }

    // 统一的JSON转义工具方法（移除重复定义）
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("/", "\\/")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}