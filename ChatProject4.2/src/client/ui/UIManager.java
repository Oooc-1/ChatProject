package client.ui;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * UI管理器：角色3与角色4的协作桥梁，处理UI更新逻辑
 * 实际由角色3实现，此处为测试桩
 */
public class UIManager {
    // 模拟好友状态存储（账号→在线状态）
    private static final Map<String, Boolean> friendStatusMap = new HashMap<>();

    /**
     * 更新好友在线状态（角色4调用，角色3实现UI刷新）
     * @param account 好友账号
     * @param isOnline 在线状态
     */
    public static void updateFriendStatus(String account, boolean isOnline) {
        friendStatusMap.put(account, isOnline);
        // 模拟UI刷新（实际更新MainFrame中的好友列表图标）
        String status = isOnline ? "在线" : "离线";
        System.out.println("好友状态更新：" + account + " → " + status);
        JOptionPane.showMessageDialog(null, "好友" + account + "已" + status, "状态通知", JOptionPane.INFORMATION_MESSAGE);
    }

}