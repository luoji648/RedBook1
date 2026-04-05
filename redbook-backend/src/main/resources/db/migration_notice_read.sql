-- 互动通知已读游标：赞和收藏 / 关注 / 评论与转发 三类各一条记录

CREATE TABLE IF NOT EXISTS tb_notice_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL COMMENT 'like_collect | follow | comment',
    last_read_time DATETIME(3) NOT NULL,
    UNIQUE KEY uk_notice_read_user_cat (user_id, category),
    CONSTRAINT fk_notice_read_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;
