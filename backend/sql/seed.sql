INSERT INTO category(parent_id, name, sort, enabled) VALUES
(0, '后端开发', 1, 1),
(0, '前端开发', 2, 1),
(0, '算法与数据结构', 3, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO tag(name) VALUES
('SpringBoot'),
('Vue3'),
('MySQL'),
('Redis'),
('Java')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sensitive_word(word, enabled) VALUES
('违禁词示例', 1),
('敏感词示例', 1)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);
