package server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳管理器 - 统一管理所有客户端的心跳检测
 * 用于检测连接是否存活、防止“假死”连接占用资源的一种机制。
 * 它通过周期性地在客户端与服务器之间发送“心跳包”（一种特殊的空消息或轻量消息），
 * 来确认对方是否仍然在线。
 * 在网络编程中，TCP 连接可能处于“半开”状态：
 * 客户端突然断电、拔网线、App 崩溃 → 没有发送关闭连接的信号
 * 服务器不知道客户端已离线，仍认为连接有效
 * 结果：服务器持续维护无效连接，浪费内存、线程、文件描述符等资源
 */
public class HeartbeatManager {
    private static final long HEARTBEAT_CHECK_INTERVAL = 30; // 秒
    private static final long HEARTBEAT_TIMEOUT = 90; // 秒
    private static ScheduledExecutorService scheduler;

    public static void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                HeartbeatManager::checkAllClients,
                HEARTBEAT_CHECK_INTERVAL,
                HEARTBEAT_CHECK_INTERVAL,
                TimeUnit.SECONDS
        );

        System.out.println("💓 心跳管理器已启动");
    }

    private static void checkAllClients() {
        // 这里可以扩展为检查所有客户端连接状态
        // 当前实现在 ClientHandler 中各自检测
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            System.out.println("💔 心跳管理器已停止");
        }
    }
}