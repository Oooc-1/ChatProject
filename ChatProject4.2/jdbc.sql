-- 1. 切换到jdbc数据库
USE jdbc;

-- 2. 先删除旧表（避免冲突，执行一次即可）
DROP TABLE IF EXISTS friends;
DROP TABLE IF EXISTS offline_messages;
DROP TABLE IF EXISTS users;

-- 3. 重新创建users表（统一字符集/存储引擎）
CREATE TABLE IF NOT EXISTS users (
                                     account VARCHAR(20) PRIMARY KEY COMMENT '8位用户账号',
                                     password VARCHAR(50) NOT NULL COMMENT '登录密码',
                                     nickname VARCHAR(50) NOT NULL COMMENT '用户昵称',
                                     status TINYINT DEFAULT 0 COMMENT '0-离线，1-在线',
                                     created_time DATETIME DEFAULT NOW() COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 4. 创建friends表（外键匹配users表）
CREATE TABLE IF NOT EXISTS friends (
                                       user_account VARCHAR(20) NOT NULL COMMENT '当前用户账号',
                                       friend_account VARCHAR(20) NOT NULL COMMENT '好友账号',
                                       PRIMARY KEY (user_account, friend_account),
                                       FOREIGN KEY (user_account) REFERENCES users(account) ON DELETE CASCADE,
                                       FOREIGN KEY (friend_account) REFERENCES users(account) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 5. 创建offline_messages表
CREATE TABLE IF NOT EXISTS offline_messages (
                                                id INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                                from_account VARCHAR(20) NOT NULL COMMENT '发送者账号',
                                                to_account VARCHAR(20) NOT NULL COMMENT '接收者账号',
                                                message_type VARCHAR(20) NOT NULL COMMENT '消息类型',
                                                content TEXT NOT NULL COMMENT '消息内容',
                                                send_time DATETIME DEFAULT NOW() COMMENT '发送时间',
                                                FOREIGN KEY (from_account) REFERENCES users(account) ON DELETE CASCADE,
                                                FOREIGN KEY (to_account) REFERENCES users(account) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 6. 插入测试数据
INSERT INTO users (account, password, nickname)
VALUES ('10000000', '123456', 'Alice'), ('10000001', '123456', 'Bob');

INSERT INTO friends (user_account, friend_account)
VALUES ('10000000', '10000001'), ('10000001', '10000000');