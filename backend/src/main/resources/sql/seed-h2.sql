MERGE INTO category (id, parent_id, name, sort, enabled) KEY(id) VALUES
(1, 0, '后端开发', 1, 1),
(2, 0, '前端开发', 2, 1),
(3, 0, '算法与数据结构', 3, 1);

MERGE INTO tag (id, name) KEY(id) VALUES
(1, 'SpringBoot'),
(2, 'Vue3'),
(3, 'MySQL'),
(4, 'Redis'),
(5, 'Java');

MERGE INTO sensitive_word (id, word, enabled) KEY(id) VALUES
(1, '色情', 1),
(2, '赌博', 1),
(3, '诈骗', 1),
(4, '刷单', 1),
(5, '毒品', 1),
(6, '枪支', 1),
(7, '炸药', 1),
(8, '代考', 1),
(9, '代写论文', 1),
(10, '出售银行卡', 1),
(11, 'porn', 1),
(12, 'gambling', 1),
(13, 'casino', 1),
(14, 'scam', 1),
(15, 'drug', 1),
(16, 'gun', 1),
(17, 'explosive', 1);
