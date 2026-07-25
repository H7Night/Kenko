-- Apply MIGRATION_7_8 to backup database
-- This transforms version 7 db to version 8 (adds tag system)

-- 1. Create tags table
CREATE TABLE IF NOT EXISTS `tags` (`name` TEXT NOT NULL, `parentId` INTEGER, `sortOrder` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);

-- 2. Create exercise_tags junction table
CREATE TABLE IF NOT EXISTS `exercise_tags` (`exerciseId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`, `tagId`), FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );

-- 3. Insert parent tags
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (1, '胸', NULL, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (2, '背', NULL, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (3, '腿', NULL, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (4, '肩', NULL, 4);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (5, '手臂', NULL, 5);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (6, '腹', NULL, 6);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (7, '有氧', NULL, 7);

-- 4. Insert child tags
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (8, '上胸', 1, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (9, '中胸', 1, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (10, '下胸', 1, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (11, '背阔肌', 2, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (12, '斜方肌', 2, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (13, '竖脊肌', 2, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (14, '菱形肌', 2, 4);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (15, '股四头肌', 3, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (16, '腘绳肌', 3, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (17, '小腿', 3, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (18, '臀肌', 3, 4);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (19, '前束', 4, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (20, '中束', 4, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (21, '后束', 4, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (22, '二头肌', 5, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (23, '三头肌', 5, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (24, '前臂', 5, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (25, '上腹', 6, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (26, '下腹', 6, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (27, '腹斜肌', 6, 3);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (28, '跑步', 7, 1);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (29, '骑行', 7, 2);
INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (30, '划船', 7, 3);

-- 5. Add countType column
ALTER TABLE `exercises` ADD COLUMN `countType` TEXT NOT NULL DEFAULT 'REPS';
UPDATE `exercises` SET `countType` = 'MINUTES' WHERE `target` = 'Cardio';

-- 6. Map existing exercises' target to tags
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 8 FROM `exercises` WHERE `target` = 'Chest';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 11 FROM `exercises` WHERE `target` = 'Lats';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 11 FROM `exercises` WHERE `target` = 'Traps';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 13 FROM `exercises` WHERE `target` = 'UpperBack';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 15 FROM `exercises` WHERE `target` = 'Quads';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 16 FROM `exercises` WHERE `target` = 'Hamstrings';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 17 FROM `exercises` WHERE `target` = 'Calves';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 18 FROM `exercises` WHERE `target` = 'Glutes';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 20 FROM `exercises` WHERE `target` = 'Shoulders';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 22 FROM `exercises` WHERE `target` = 'Biceps';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 23 FROM `exercises` WHERE `target` = 'Triceps';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 25 FROM `exercises` WHERE `target` = 'Core';
INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 28 FROM `exercises` WHERE `target` = 'Cardio';

-- 7. Rebuild exercises table: remove target column, reorder columns
CREATE TABLE IF NOT EXISTS `exercises_new` (`name` TEXT NOT NULL, `countType` TEXT NOT NULL, `reference` TEXT, `isIsometric` INTEGER NOT NULL, `isBodyweight` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);
INSERT INTO `exercises_new` (`name`, `countType`, `reference`, `isIsometric`, `isBodyweight`, `id`) SELECT `name`, `countType`, `reference`, `isIsometric`, `isBodyweight`, `id` FROM `exercises`;
DROP TABLE `exercises`;
ALTER TABLE `exercises_new` RENAME TO `exercises`;

-- 8. Update database version
PRAGMA user_version = 8;
