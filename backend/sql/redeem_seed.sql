-- Seed redeem items
INSERT IGNORE INTO redeem_item (name, description, point_cost, icon, enabled, created_at) VALUES
('内容推广', '让你的一篇内容在首页推荐位展示24小时', 50, 'Promotion', 1, NOW()),
('个性签名装饰', '个人主页签名获得特殊彩色样式', 30, 'Signature', 1, NOW()),
('金色头像框', '获得专属金色头像框装饰', 80, 'GoldFrame', 1, NOW()),
('内容置顶卡', '让你的一篇内容在个人主页置顶展示', 40, 'Pin', 1, NOW());
