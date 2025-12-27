package common.utils;

import common.protocol.Message;

import java.util.Map;

public class JsonUtil {

    /**
     * 将Message对象转换为JSON字符串
     */
    public static String toJson(Message msg) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // 添加基础字段
        appendField(json, "type", msg.getType());
        appendField(json, "account", msg.getAccount());
        appendField(json, "password", msg.getPassword());
        appendField(json, "content", msg.getContent());
        appendField(json, "to", msg.getTo());
        appendField(json, "from", msg.getFrom());
        appendField(json, "nickname", msg.getNickname());

        // 添加额外字段
        Map<String, String> extraData = msg.getExtraData();
        if (extraData != null && !extraData.isEmpty()) {
            for (Map.Entry<String, String> entry : extraData.entrySet()) {
                appendField(json, entry.getKey(), entry.getValue());
            }
        }

        // 移除最后一个逗号
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 将JSON字符串解析为Message对象
     */
    public static Message fromJson(String jsonStr) {
        Message msg = new Message();

        // 移除首尾的{}
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
            jsonStr = jsonStr.substring(1, jsonStr.length() - 1).trim();
        }

        // 分割字段
        String[] pairs = jsonStr.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // 分割键值
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = removeQuotes(kv[0].trim());
                String value = removeQuotes(kv[1].trim());

                // 设置对应字段
                switch (key) {
                    case "type":
                        msg.setType(value);
                        break;
                    case "account":
                        msg.setAccount(value);
                        break;
                    case "password":
                        msg.setPassword(value);
                        break;
                    case "content":
                        msg.setContent(value);
                        break;
                    case "to":
                        msg.setTo(value);
                        break;
                    case "from":
                        msg.setFrom(value);
                        break;
                    case "nickname":
                        msg.setNickname(value);
                        break;
                    default:
                        msg.putExtra(key, value);
                }
            }
        }

        return msg;
    }

    /**
     * 为JSON添加字段
     */
    private static void appendField(StringBuilder json, String key, String value) {
        if (value != null) {
            if (json.length() > 1 && json.charAt(json.length() - 1) != '{') {
                json.append(",");
            }
            json.append("\"").append(key).append("\":");

            // 处理特殊字符
            if (value.startsWith("[") || value.startsWith("{")) {
                // 如果是数组或对象，直接添加
                json.append(value);
            } else {
                // 转义特殊字符
                String escapedValue = escapeJson(value);
                json.append("\"").append(escapedValue).append("\"");
            }
        }
    }

    /**
     * 移除字符串两端的引号
     */
    private static String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    /**
     * 转义JSON特殊字符
     */
    private static String escapeJson(String str) {
        if (str == null) return "";

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '/': sb.append("\\/"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}