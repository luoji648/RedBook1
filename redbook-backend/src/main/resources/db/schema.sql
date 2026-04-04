-- RedBook MySQL 8 schema (database: redbook)

CREATE DATABASE IF NOT EXISTS redbook DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE redbook;

-- ---------- user (existing shape + token_version) ----------
CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(128) DEFAULT NULL,
    nick_name VARCHAR(64) NOT NULL DEFAULT '',
    icon VARCHAR(512) NOT NULL DEFAULT '',
    token_version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_user_info (
    user_id BIGINT PRIMARY KEY,
    city VARCHAR(64) DEFAULT '',
    introduce VARCHAR(256) DEFAULT '',
    fans INT NOT NULL DEFAULT 0,
    followee INT NOT NULL DEFAULT 0,
    gender TINYINT(1) DEFAULT 0,
    birthday DATE DEFAULT NULL,
    credits INT NOT NULL DEFAULT 0,
    level TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_info_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- note ----------
CREATE TABLE IF NOT EXISTS tb_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) DEFAULT '',
    content TEXT,
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0 image 1 video',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 draft 1 published 2 offline',
    visibility TINYINT NOT NULL DEFAULT 0 COMMENT '0 public 1 followers 2 private',
    like_count INT NOT NULL DEFAULT 0,
    collect_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_note_user_time (user_id, create_time DESC),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_note_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    url VARCHAR(1024) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    media_type TINYINT NOT NULL DEFAULT 0 COMMENT '0 image 1 video',
    INDEX idx_media_note (note_id, sort_order),
    CONSTRAINT fk_media_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- like ----------
CREATE TABLE IF NOT EXISTS tb_note_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_like_user_note (user_id, note_id),
    INDEX idx_like_note (note_id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_note_like_agg (
    note_id BIGINT PRIMARY KEY,
    like_count BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agg_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_like_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    event_type TINYINT NOT NULL COMMENT '1 like 2 unlike',
    payload VARCHAR(512) DEFAULT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publish_time DATETIME DEFAULT NULL,
    INDEX idx_outbox_pub (published, id)
) ENGINE=InnoDB;

-- ---------- collect / follow ----------
CREATE TABLE IF NOT EXISTS tb_note_collect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collect_user_note (user_id, note_id),
    INDEX idx_collect_note (note_id),
    CONSTRAINT fk_collect_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_collect_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, followee_id),
    INDEX idx_followee (followee_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- comment ----------
CREATE TABLE IF NOT EXISTS tb_note_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    reply_to_user_id BIGINT DEFAULT NULL,
    content VARCHAR(1024) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 ok 1 deleted',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment_note_time (note_id, create_time),
    CONSTRAINT fk_comment_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- chat ----------
CREATE TABLE IF NOT EXISTS tb_chat_thread (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_low BIGINT NOT NULL,
    user_high BIGINT NOT NULL,
    last_msg_time DATETIME DEFAULT NULL,
    UNIQUE KEY uk_thread_users (user_low, user_high),
    CONSTRAINT fk_thread_low FOREIGN KEY (user_low) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_thread_high FOREIGN KEY (user_high) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2048) NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_msg_thread_time (thread_id, create_time),
    CONSTRAINT fk_msg_thread FOREIGN KEY (thread_id) REFERENCES tb_chat_thread(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- share ----------
CREATE TABLE IF NOT EXISTS tb_share_link (
    short_code VARCHAR(16) PRIMARY KEY,
    note_id BIGINT NOT NULL,
    creator_user_id BIGINT NOT NULL,
    expire_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_share_note (note_id),
    CONSTRAINT fk_share_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- commerce (optional) ----------
CREATE TABLE IF NOT EXISTS tb_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    cover VARCHAR(512) DEFAULT '',
    price_cent BIGINT NOT NULL DEFAULT 0,
    stock INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_note_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_note_product (note_id, product_id),
    CONSTRAINT fk_np_note FOREIGN KEY (note_id) REFERENCES tb_note(id) ON DELETE CASCADE,
    CONSTRAINT fk_np_product FOREIGN KEY (product_id) REFERENCES tb_product(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_user_prod (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES tb_product(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_cent BIGINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 created 1 paid 2 cancelled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_user (user_id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_cent BIGINT NOT NULL,
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES tb_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES tb_product(id)
) ENGINE=InnoDB;
