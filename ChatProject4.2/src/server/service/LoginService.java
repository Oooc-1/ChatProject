package server.service;

import server.ClientHandler;
import server.dao.DBHelper;
import server.dao.UserDao;
import server.OnlineUserManager;
import common.protocol.Message;

import java.util.List;
import java.util.Map;

/**
 * 登录服务类（对接UserDao，遵循协议规范）
 */
public class LoginService {
    private final UserDao userDao = new UserDao();

    /**
     * 处理登录请求
     */
    public void handleLogin(Message loginMsg, ClientHandler handler) {
        String account = loginMsg.getAccount();
        String password = loginMsg.getPassword();
        Message resultMsg = new Message("loginResult");

        // 1. 参数校验（账号8位，密码非空）
        if (account == null || account.length() != 8 || password == null || password.trim().isEmpty()) {
            resultMsg.setContent("failed");
            handler.send(resultMsg);
            return;
        }

        // 2. 查询用户
        Map<String, String> user = userDao.selectUserByAccount(account);
        if (user == null || !user.get("password").equals(password)) {
            resultMsg.setContent("failed");
            handler.send(resultMsg);
            return;
        }

        // 3. 检查重复登录
        if (OnlineUserManager.isUserOnline(account)) {
            resultMsg.setContent("duplicate");
            handler.send(resultMsg);
            return;
        }

        // 4. 更新在线状态
        userDao.updateUserStatus(account, 1);

        // 5. 绑定用户与连接
        handler.setUserId(account);
        OnlineUserManager.addUser(account, handler);

        // 6. 登录成功响应
        resultMsg.setContent("success");
        handler.send(resultMsg);

        // 7. 推送离线消息和好友列表
        sendOfflineMessages(account, handler);
        sendFriendList(account, handler);
    }

    /**
     * 处理用户下线
     */
    public void handleLogout(String account) {
        if (account == null || account.length() != 8) return;

        OnlineUserManager.removeUser(account);
        userDao.updateUserStatus(account, 0);
    }

    /**
     * 推送离线消息
     */
    private void sendOfflineMessages(String account, ClientHandler handler) {
        List<Message> offlineMsgs = userDao.getOfflineMessages(account);
        if (!offlineMsgs.isEmpty()) {
            for (Message msg : offlineMsgs) {
                handler.send(msg);
            }
            userDao.clearOfflineMessages(account);
        }
    }

    /**
     * 推送好友列表
     */
    private void sendFriendList(String account, ClientHandler handler) {
        List<String> friends = userDao.getFriendList(account);
        if (!friends.isEmpty()) {
            Message friendMsg = new Message("friendList");
            friendMsg.setContent(String.join(",", friends));
            handler.send(friendMsg);
        }
    }

}