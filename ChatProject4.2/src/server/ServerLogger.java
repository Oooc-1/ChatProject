package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 简单日志工具类
 */
public class ServerLogger {
    private static final String LOG_FILE = "server_log.txt";
    private static final int MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter logWriter = null;
    private static boolean enabled = true;

    // 初始化日志
    private static void initLog() {
        try {
            // 检查日志文件大小
            File logFile = new File(LOG_FILE);
            // 10MB轮转（防磁盘占满）
            // 当日志文件达到一定大小（或时间），就自动关闭当前文件，创建一个新文件继续写入
            // 同时对旧日志文件进行归档、压缩或删除，防止日志无限增长、占满磁盘。
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                // 重命名旧日志
                String backupName = LOG_FILE + "." + System.currentTimeMillis();
                logFile.renameTo(new File(backupName));
            }
            // 以追加模式打开
            FileWriter fileWriter = new FileWriter(LOG_FILE, true);
            logWriter = new PrintWriter(fileWriter, true);
        } catch (IOException e) {
            System.err.println("❌ 初始化日志失败: " + e.getMessage());
            logWriter = null;
        }
    }

    /**
     * 记录普通日志
     */
    public static void log(String message) {
        logInternal(message, "INFO");
    }

    /**
     * 记录错误日志
     */
    public static void error(String message) {
        logInternal(message, "ERROR");
    }

    /**
     * 记录警告日志
     */
    public static void warn(String message) {
        logInternal(message, "WARN");
    }

    /**
     * 记录调试日志
     */
    public static void debug(String message) {
        logInternal(message, "DEBUG");
    }

    /**
     * 内部日志记录方法，核心日志方法
     */
    private static void logInternal(String message, String level) {
        if (!enabled) return;

        String timestamp = DATE_FORMAT.format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // 输出到控制台
        switch (level) {
            case "ERROR":
                System.err.println(logEntry);
                break;
            case "WARN":
                System.out.println("\u001B[33m" + logEntry + "\u001B[0m"); // 黄色
                break;
            default:
                System.out.println(logEntry);
        }

        // 写入文件（带轮转）
        if (logWriter != null) {
            logWriter.println(logEntry);
        }
    }

    /**
     * 记录异常堆栈
     */
    public static void exception(String message, Exception e) {
        error(message + ": " + e.getMessage());

        if (logWriter != null && e != null) {
            e.printStackTrace(logWriter);
        }
    }

    /**
     * 关闭日志
     */
    public static void close() {
        if (logWriter != null) {
            log("服务器关闭时间: " + new Date());
            logWriter.println("=".repeat(60));
            logWriter.close();
        }
    }

    /**
     * 启用/禁用日志
     */
    public static void setEnabled(boolean enabled) {
        ServerLogger.enabled = enabled;
    }
}