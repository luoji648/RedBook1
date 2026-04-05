-- 群聊：群主主页展示、加群方式、入群申请（私信通知）

CREATE TABLE IF NOT EXISTS tb_chat_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    join_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0 无需验证 1 需群主验证',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_msg_time DATETIME DEFAULT NULL,
    INDEX idx_chat_group_owner (owner_id),
    CONSTRAINT fk_chat_group_owner FOREIGN KEY (owner_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_chat_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0 成员 1 群主',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cgm_group_user (group_id, user_id),
    INDEX idx_cgm_user (user_id),
    CONSTRAINT fk_cgm_group FOREIGN KEY (group_id) REFERENCES tb_chat_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgm_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_chat_group_join_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 待处理 1 已同意 2 已拒绝',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cgj_group_applicant (group_id, applicant_id),
    INDEX idx_cgj_group_status (group_id, status),
    CONSTRAINT fk_cgj_group FOREIGN KEY (group_id) REFERENCES tb_chat_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgj_applicant FOREIGN KEY (applicant_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2048) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gmsg_group_time (group_id, create_time),
    CONSTRAINT fk_gmsg_group FOREIGN KEY (group_id) REFERENCES tb_chat_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmsg_sender FOREIGN KEY (sender_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;
