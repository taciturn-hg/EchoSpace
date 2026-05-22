-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名(可用于登录)',
    nickname VARCHAR(50) NOT NULL UNIQUE COMMENT '昵称(展示用,注册时随机生成)',
    email VARCHAR(100) NOT NULL COMMENT '邮箱(可用于登录)',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号(可用于登录)',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    avatar VARCHAR(500) COMMENT '头像URL',
    bio VARCHAR(500) COMMENT '个人简介',
    status TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 帖子表
CREATE TABLE post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '发布者',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content_html LONGTEXT NOT NULL COMMENT '富文本HTML',
    content_text LONGTEXT NOT NULL COMMENT '纯文本(用于ES搜索)',
    cover_image VARCHAR(500) COMMENT '封面图(第一张图)',
    like_count INT DEFAULT 0 COMMENT '点赞数(缓存字段)',
    comment_count INT DEFAULT 0 COMMENT '评论数(缓存字段)',
    collect_count INT DEFAULT 0 COMMENT '收藏数(缓存字段)',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    status TINYINT DEFAULT 1 COMMENT '1发布 0草稿 -1删除',
    is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status_created (status, created_at),
    FULLTEXT INDEX ft_content (content_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL COMMENT '所属帖子',
    user_id BIGINT NOT NULL COMMENT '评论者',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID(0=一级评论)',
    reply_to_uid BIGINT COMMENT '回复的目标用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '1正常 -1删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点赞表(通用: 帖子点赞+评论点赞)
CREATE TABLE user_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '点赞人',
    target_type TINYINT NOT NULL COMMENT '1帖子 2评论',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏表
CREATE TABLE user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关注表
CREATE TABLE user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者',
    followed_id BIGINT NOT NULL COMMENT '被关注者',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_followed (follower_id, followed_id),
    INDEX idx_followed_id (followed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;