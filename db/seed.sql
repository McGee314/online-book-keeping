-- ============================================================
-- Seed Data — Default categories (system-wide, user_id = NULL)
-- Run this AFTER schema.sql
-- ============================================================

USE bookkeeping_db;

-- Income categories
INSERT INTO `categories` (`user_id`, `name`, `type`, `icon`, `color`, `sort_order`) VALUES
(NULL, 'Salary',      1, 'salary',     '#67C23A', 1),
(NULL, 'Freelance',   1, 'freelance',  '#409EFF', 2),
(NULL, 'Investment',  1, 'investment', '#E6A23C', 3),
(NULL, 'Gift',        1, 'gift',       '#F56C6C', 4),
(NULL, 'Other Income',1, 'other',      '#909399', 5);

-- Expense categories
INSERT INTO `categories` (`user_id`, `name`, `type`, `icon`, `color`, `sort_order`) VALUES
(NULL, 'Food & Dining',  2, 'food',      '#F56C6C', 1),
(NULL, 'Transport',      2, 'transport', '#E6A23C', 2),
(NULL, 'Shopping',       2, 'shopping',  '#409EFF', 3),
(NULL, 'Housing',        2, 'housing',   '#67C23A', 4),
(NULL, 'Entertainment',  2, 'entertain', '#9B59B6', 5),
(NULL, 'Healthcare',     2, 'health',    '#1ABC9C', 6),
(NULL, 'Education',      2, 'education', '#3498DB', 7),
(NULL, 'Other Expense',  2, 'other',     '#909399', 8);
