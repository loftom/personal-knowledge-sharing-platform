CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    body CLOB NOT NULL,
    category_id BIGINT NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_content_author ON content(author_id);
CREATE INDEX IF NOT EXISTS idx_content_status ON content(status);
CREATE INDEX IF NOT EXISTS idx_content_category ON content(category_id);

CREATE TABLE IF NOT EXISTS content_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT uk_content_tag UNIQUE (content_id, tag_id)
);

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    body CLOB NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comment_content ON comment(content_id);

CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_like UNIQUE (user_id, target_id, target_type)
);

CREATE TABLE IF NOT EXISTS favorite_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_favorite UNIQUE (user_id, content_id)
);

CREATE TABLE IF NOT EXISTS sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(64) NOT NULL UNIQUE,
    enabled TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_content ON audit_log(content_id);

CREATE TABLE IF NOT EXISTS qa_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    best_answer_id BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_qa_question UNIQUE (question_id)
);

CREATE INDEX IF NOT EXISTS idx_qa_question_status ON qa_question(status);

CREATE TABLE IF NOT EXISTS qa_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    body CLOB NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    is_best TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_qa_answer_question ON qa_answer(question_id);

CREATE TABLE IF NOT EXISTS user_behavior_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    event_weight INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_behavior_user ON user_behavior_event(user_id);
CREATE INDEX IF NOT EXISTS idx_behavior_target ON user_behavior_event(target_type, target_id);

CREATE TABLE IF NOT EXISTS follow_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_follow UNIQUE (follower_user_id, target_user_id)
);

CREATE INDEX IF NOT EXISTS idx_follow_target ON follow_relation(target_user_id);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(500) NULL,
    related_id BIGINT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id, is_read);

CREATE TABLE IF NOT EXISTS point_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    level_no INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_point_account_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS point_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    change_amount INT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    biz_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_point_log_biz UNIQUE (biz_key)
);

CREATE INDEX IF NOT EXISTS idx_point_log_user ON point_log(user_id, created_at);

CREATE TABLE IF NOT EXISTS badge_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_code VARCHAR(64) NOT NULL,
    badge_name VARCHAR(128) NOT NULL,
    awarded_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_badge_user UNIQUE (user_id, badge_code)
);
