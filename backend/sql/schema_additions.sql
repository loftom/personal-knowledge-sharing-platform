CREATE TABLE IF NOT EXISTS redeem_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    point_cost INT NOT NULL,
    icon VARCHAR(64) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS redeem_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    point_cost INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL,
    INDEX idx_redeem_record_user(user_id)
);
