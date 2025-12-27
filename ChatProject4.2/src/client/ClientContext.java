package client;
/**
 * 客户端全局上下文：存储全局状态（当前登录账号、昵称等）
 */
public class ClientContext {
    // 当前登录账号（登录成功后设置，退出登录时清空）
    private static String currentAccount;
    // 当前登录昵称
    private static String currentNickname;

    /**
     * 设置登录状态（登录成功后调用）
     * @param account 账号
     * @param nickname 昵称
     */
    public static void setLoginStatus(String account, String nickname) {
        currentAccount = account;
        currentNickname = nickname;
    }

    /**
     * 清空登录状态（退出登录时调用）
     */
    public static void clearLoginStatus() {
        currentAccount = null;
        currentNickname = null;
    }

    // Getter：供各模块获取全局状态
    public static String getCurrentAccount() {
        return currentAccount;
    }

    public static String getCurrentNickname() {
        return currentNickname;
    }

    /**
     * 判断是否已登录
     * @return 已登录返回true，未登录返回false
     */
    public static boolean isLoggedIn() {
        return currentAccount != null && !currentAccount.isEmpty();
    }
}
