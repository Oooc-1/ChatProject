package server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL 5.7数据库连接工具类
 * 负责连接管理、表初始化，与数据库脚本完全对齐
 */
public class DBHelper {
    // MySQL 5.7连接配置（需替换为你的实际账号密码）
    private static final String DB_URL = "jdbc:mysql://localhost:3306/jdbc?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";      // 你的MySQL用户名
    private static final String DB_PASSWORD = "123456";// 你的MySQL密码
    private static Connection connection = null;

    /**
     * 单例模式获取数据库连接
     */
    public static synchronized Connection getConnection() {
        if (connection == null || isConnectionClosed()) {
            try {
                // 加载MySQL 5.7驱动
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("✅ MySQL 5.7数据库连接成功（jdbc库）");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ 找不到MySQL驱动类：" + e.getMessage());
                System.err.println("请导入mysql-connector-java-5.x版本驱动（如5.1.49）");
            } catch (SQLException e) {
                System.err.println("❌ MySQL连接失败：" + e.getMessage());
                System.err.println("排查：1.服务是否启动 2.账号密码正确 3.jdbc数据库已创建");
            }
        }
        return connection;
    }

    /**
     * 关闭数据库连接
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("✅ MySQL连接已关闭");
            } catch (SQLException e) {
                System.err.println("❌ 关闭MySQL连接失败：" + e.getMessage());
            }
        }
    }

    /**
     * 初始化数据库表（与脚本一致，首次启动调用）
     */
    public static void initDatabase() {
        // 建表SQL（与你的database.sql完全一致）
        String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                "account VARCHAR(20) PRIMARY KEY COMMENT '8位用户账号', " +
                "password VARCHAR(50) NOT NULL COMMENT '登录密码', " +
                "nickname VARCHAR(50) NOT NULL COMMENT '用户昵称', " +
                "status TINYINT DEFAULT 0 COMMENT '0-离线，1-在线', " +
                "created_time DATETIME DEFAULT NOW() COMMENT '创建时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String createFriendTable = "CREATE TABLE IF NOT EXISTS friends (" +
                "user_account VARCHAR(20) NOT NULL COMMENT '当前用户账号', " +
                "friend_account VARCHAR(20) NOT NULL COMMENT '好友账号', " +
                "PRIMARY KEY (user_account, friend_account), " +
                "FOREIGN KEY (user_account) REFERENCES users(account) ON DELETE CASCADE, " +
                "FOREIGN KEY (friend_account) REFERENCES users(account) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String createOfflineMsgTable = "CREATE TABLE IF NOT EXISTS offline_messages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID', " +
                "from_account VARCHAR(20) NOT NULL COMMENT '发送者账号', " +
                "to_account VARCHAR(20) NOT NULL COMMENT '接收者账号', " +
                "message_type VARCHAR(20) NOT NULL COMMENT '消息类型', " +
                "content TEXT NOT NULL COMMENT '消息内容', " +
                "send_time DATETIME DEFAULT NOW() COMMENT '发送时间', " +
                "FOREIGN KEY (from_account) REFERENCES users(account) ON DELETE CASCADE, " +
                "FOREIGN KEY (to_account) REFERENCES users(account) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUserTable);
            stmt.execute(createFriendTable);
            stmt.execute(createOfflineMsgTable);
            System.out.println("✅ 数据库表初始化完成");

            // 插入测试数据（避免重复插入）
            insertTestData(conn);
        } catch (SQLException e) {
            System.err.println("❌ 初始化表失败：" + e.getMessage());
        }
    }

    /**
     * 插入测试数据（与脚本一致）
     */
    private static void insertTestData(Connection conn) throws SQLException {
        // 检查测试用户是否已存在
        String checkSql = "SELECT account FROM users WHERE account IN ('10000000', '10000001')";
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(checkSql)) {
            if (!rs.next()) {
                // 插入测试用户
                String insertUserSql = "INSERT INTO users (account, password, nickname) " +
                        "VALUES ('10000000', '123456', 'Alice'), ('10000001', '123456', 'Bob')";
                stmt.executeUpdate(insertUserSql);

                // 插入好友关系
                String insertFriendSql = "INSERT INTO friends (user_account, friend_account) " +
                        "VALUES ('10000000', '10000001'), ('10000001', '10000000')";
                stmt.executeUpdate(insertFriendSql);

                System.out.println("✅ 测试数据插入完成");
            }
        }
    }

    /**
     * 检查连接是否有效
     */
    private static boolean isConnectionClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}