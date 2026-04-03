CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    body LONGTEXT NOT NULL,
    category_id BIGINT NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_content_author(author_id),
    INDEX idx_content_status(status),
    INDEX idx_content_category(category_id),
    FULLTEXT KEY ft_title_body(title, body)
);

CREATE TABLE IF NOT EXISTS content_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_content_tag(content_id, tag_id)
);

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    body TEXT NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    INDEX idx_comment_content(content_id)
);

CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_like(user_id, target_id, target_type)
);

CREATE TABLE IF NOT EXISTS favorite_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_favorite(user_id, content_id)
);

CREATE TABLE IF NOT EXISTS sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(64) NOT NULL UNIQUE,
    enabled TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    INDEX idx_audit_content(content_id)
);

CREATE TABLE IF NOT EXISTS qa_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    best_answer_id BIGINT NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_qa_question_status(status)
);

CREATE TABLE IF NOT EXISTS qa_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    is_best TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_qa_answer_question(question_id)
);

CREATE TABLE IF NOT EXISTS user_behavior_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    event_weight INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    INDEX idx_behavior_user(user_id),
    INDEX idx_behavior_target(target_type, target_id)
);

CREATE TABLE IF NOT EXISTS follow_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_follow(follower_user_id, target_user_id),
    INDEX idx_follow_target(target_user_id)
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(500) NULL,
    related_id BIGINT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    INDEX idx_notification_user(user_id, is_read)
);

CREATE TABLE IF NOT EXISTS point_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    level_no INT NOT NULL DEFAULT 1,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_point_account_user(user_id)
);

CREATE TABLE IF NOT EXISTS point_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    change_amount INT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    biz_key VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_point_log_biz(biz_key),
    INDEX idx_point_log_user(user_id, created_at)
);

CREATE TABLE IF NOT EXISTS badge_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    badge_code VARCHAR(64) NOT NULL,
    badge_name VARCHAR(128) NOT NULL,
    awarded_at DATETIME NOT NULL,
    UNIQUE KEY uk_badge_user(user_id, badge_code)
);
