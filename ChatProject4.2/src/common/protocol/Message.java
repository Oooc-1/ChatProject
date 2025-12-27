package common.protocol;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String type;
    private String account;
    private String password;
    private String content;
    private String to;
    private String from;
    private String nickname;
    private Map<String, String> extraData; // 额外数据字段

    public Message() {
        extraData = new HashMap<>();
    }

    public Message(String type) {
        this.type = type;
        extraData = new HashMap<>();
    }

    // 生成所有 getter 和 setter 方法
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    // 额外数据操作
    public void putExtra(String key, String value) {
        extraData.put(key, value);
    }

    public String getExtra(String key) {
        return extraData.get(key);
    }

    public Map<String, String> getExtraData() {
        return extraData;
    }
}