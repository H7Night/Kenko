-- Seed script for populating tag data in Kenko database
-- Run after database migration from version 7 to 8
-- Usage: sqlite3 /path/to/kenko_database.db < seed_tags.sql

-- Insert parent tags (body parts)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (1, '胸', NULL, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (2, '背', NULL, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (3, '腿', NULL, 3);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (4, '肩', NULL, 4);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (5, '手臂', NULL, 5);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (6, '腹', NULL, 6);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (7, '有氧', NULL, 7);

-- Insert child tags (specific muscles) under 胸 (id=1)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (8, '上胸', 1, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (9, '中胸', 1, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (10, '下胸', 1, 3);

-- Insert child tags under 背 (id=2)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (11, '背阔肌', 2, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (12, '斜方肌', 2, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (13, '竖脊肌', 2, 3);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (14, '菱形肌', 2, 4);

-- Insert child tags under 腿 (id=3)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (15, '股四头肌', 3, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (16, '腘绳肌', 3, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (17, '小腿', 3, 3);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (18, '臀肌', 3, 4);

-- Insert child tags under 肩 (id=4)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (19, '前束', 4, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (20, '中束', 4, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (21, '后束', 4, 3);

-- Insert child tags under 手臂 (id=5)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (22, '二头肌', 5, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (23, '三头肌', 5, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (24, '前臂', 5, 3);

-- Insert child tags under 腹 (id=6)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (25, '上腹', 6, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (26, '下腹', 6, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (27, '腹斜肌', 6, 3);

-- Insert child tags under 有氧 (id=7)
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (28, '跑步', 7, 1);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (29, '骑行', 7, 2);
INSERT OR IGNORE INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (30, '划船', 7, 3);

-- Verify the seeded data
SELECT 'Parents:' AS '';
SELECT id, name, sortOrder FROM tags WHERE parentId IS NULL ORDER BY sortOrder;
SELECT 'Children:' AS '';
SELECT t.id, t.name, p.name AS parent, t.sortOrder FROM tags t LEFT JOIN tags p ON t.parentId = p.id WHERE t.parentId IS NOT NULL ORDER BY t.parentId, t.sortOrder;
