package server.service;

import server.ClientHandler;
import server.dao.DBHelper;
import server.dao.UserDao;
import common.protocol.Message;

/**
 * 找回密码服务类（新增功能，遵循协议规范）
 */
public class FindPwdService {
    private final UserDao userDao = new UserDao();

    /**
     * 处理找回密码请求
     */
    public void handleFindPassword(Message findPwdMsg, ClientHandler handler) {
        String account = findPwdMsg.getAccount();
        String nickname = findPwdMsg.getNickname();
        Message resultMsg = new Message("findPwdResult");

        // 1. 参数校验（账号8位，昵称非空）
        if (account == null || account.length() != 8 || nickname == null || nickname.trim().isEmpty()) {
            resultMsg.setContent("failed");
            handler.send(resultMsg);
            return;
        }

        // 2. 查询密码
        String password = userDao.getPasswordByAccountAndNickname(account, nickname);

        // 3. 响应结果（遵循扩展协议）
        if (password != null) {
            resultMsg.setContent("success");
            resultMsg.putExtra("password", password);
        } else {
            resultMsg.setContent("failed");
        }
        handler.send(resultMsg);
    }

}