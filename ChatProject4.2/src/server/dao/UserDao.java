package server.dao;

import common.protocol.Message;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户数据访问对象（对接MySQL 5.7）
 * 支持登录、注册、找回密码、离线消息、好友列表功能
 */
public class UserDao {
    // 账号生成器（8位数字）
    private static final AtomicLong accountSeq = new AtomicLong(10000000L);

    // 离线消息内存存储（简化实现）
    private static final Map<String, List<Message>> offlineMessages = new ConcurrentHashMap<>();

    // ------------------- 核心业务方法 -------------------
    /**
     * 根据账号查询用户（登录/验证用）
     */
    public Map<String, String> selectUserByAccount(String account) {
        String sql = "SELECT password, nickname, status FROM users WHERE account = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("password", rs.getString("password"));
                user.put("nickname", rs.getString("nickname"));
                user.put("status", String.valueOf(rs.getInt("status")));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("❌ 查询用户失败：" + e.getMessage());
        }
        return null;
    }

    /**
     * 插入新用户（注册用）
     */
    public boolean insertUser(String account, String password, String nickname) {
        String sql = "INSERT INTO users (account, password, nickname, status) VALUES (?, ?, ?, 0)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            pstmt.setString(2, password);
            pstmt.setString(3, nickname);
            pstmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            // 账号已存在（主键冲突）
            return false;
        } catch (SQLException e) {
            System.err.println("❌ 插入用户失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 更新用户在线状态
     */
    public boolean updateUserStatus(String account, int status) {
        String sql = "UPDATE users SET status = ? WHERE account = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, status);
            pstmt.setString(2, account);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新状态失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 生成唯一8位账号
     */
    public String generateAccount() {
        long next = accountSeq.incrementAndGet();
        if (next > 99999999L) {
            accountSeq.set(10000000L);
            next = accountSeq.incrementAndGet();
        }
        return String.valueOf(next);
    }

    /**
     * 找回密码：根据账号+昵称查询密码
     */
    public String getPasswordByAccountAndNickname(String account, String nickname) {
        String sql = "SELECT password FROM users WHERE account = ? AND nickname = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            pstmt.setString(2, nickname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        } catch (SQLException e) {
            System.err.println("❌ 查询密码失败：" + e.getMessage());
        }
        return null;
    }

    // ------------------- 离线消息相关 -------------------
    public List<Message> getOfflineMessages(String toAccount) {
        return offlineMessages.getOrDefault(toAccount, Collections.emptyList());
    }

    public void clearOfflineMessages(String toAccount) {
        offlineMessages.remove(toAccount);
    }

    public void saveOfflineMessage(String toAccount, Message message) {
        offlineMessages.computeIfAbsent(toAccount, k -> new ArrayList<>()).add(message);
    }

    // ------------------- 好友列表相关 -------------------
    public List<String> getFriendList(String userAccount) {
        List<String> friends = new ArrayList<>();
        String sql = "SELECT friend_account FROM friends WHERE user_account = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userAccount);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                friends.add(rs.getString("friend_account"));
            }
        } catch (SQLException e) {
            System.err.println("❌ 获取好友列表失败：" + e.getMessage());
            friends = Collections.emptyList();
        }
        return friends;
    }
}