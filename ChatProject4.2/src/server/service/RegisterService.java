package server.service;

import server.ClientHandler;
import server.dao.DBHelper;
import server.dao.UserDao;
import common.protocol.Message;

/**
 * 注册服务类（遵循协议，自动生成8位账号）
 */
public class RegisterService {
    private final UserDao userDao = new UserDao();

    /**
     * 处理注册请求
     */
    public void handleRegister(Message registerMsg, ClientHandler handler) {
        String nickname = registerMsg.getNickname();
        String password = registerMsg.getPassword();
        Message resultMsg = new Message("registerResult");

        // 1. 参数校验（昵称、密码非空）
        if (nickname == null || nickname.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resultMsg.setContent("failed");
            handler.send(resultMsg);
            return;
        }

        // 2. 生成唯一账号（最多尝试10次）
        String account = null;
        for (int i = 0; i < 10; i++) {
            account = userDao.generateAccount();
            if (userDao.selectUserByAccount(account) == null) {
                break;
            }
            account = null;
        }

        if (account == null) {
            resultMsg.setContent("failed");
            handler.send(resultMsg);
            return;
        }

        // 3. 插入数据库
        boolean success = userDao.insertUser(account, password, nickname);
        resultMsg.setContent(success ? account : "failed");
        handler.send(resultMsg);
    }

}