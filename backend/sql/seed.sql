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

DELETE FROM category;
ALTER TABLE category AUTO_INCREMENT = 1;
INSERT INTO category(parent_id, name, sort, enabled) VALUES
(0, '后端开发', 1, 1),
(0, '前端开发', 2, 1),
(0, '算法与数据结构', 3, 1);

DELETE FROM sensitive_word;
ALTER TABLE sensitive_word AUTO_INCREMENT = 1;
INSERT INTO sensitive_word(word, enabled) VALUES
('色情', 1),
('赌博', 1),
('诈骗', 1),
('刷单', 1),
('毒品', 1),
('枪支', 1),
('炸药', 1),
('代考', 1),
('代写论文', 1),
('出售银行卡', 1),
('porn', 1),
('gambling', 1),
('casino', 1),
('scam', 1),
('drug', 1),
('gun', 1),
('explosive', 1);
