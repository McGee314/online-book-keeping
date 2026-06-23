-- ============================================================
-- Personal Online Bookkeeping System — Database Schema
-- MySQL 5.7 / 8.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS bookkeeping
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bookkeeping;

-- ============================================================
-- Table: users
-- Stores registered accounts. Passwords are BCrypt-hashed.
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `username`   VARCHAR(50)      NOT NULL                COMMENT 'Unique login name',
    `password`   VARCHAR(255)     NOT NULL                COMMENT 'BCrypt hashed password',
    `nickname`   VARCHAR(100)     DEFAULT NULL            COMMENT 'Display name',
    `email`      VARCHAR(100)     DEFAULT NULL            COMMENT 'Optional email',
    `avatar`     VARCHAR(255)     DEFAULT NULL            COMMENT 'Avatar image URL',
    `status`     TINYINT(1)       NOT NULL DEFAULT 1      COMMENT '1=active, 0=disabled',
    `created_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT(1)       NOT NULL DEFAULT 0      COMMENT 'Soft delete flag (MyBatis-Plus)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_users_username` (`username`),
    UNIQUE KEY `uq_users_email`    (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System users';


-- ============================================================
-- Table: categories
-- Income/expense categories, scoped per user.
-- Built-in defaults (user_id=NULL) can be provided via seed.sql.
-- ============================================================
CREATE TABLE IF NOT EXISTS `categories` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id`     BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'NULL = system default category',
    `name`        VARCHAR(50)      NOT NULL                COMMENT 'Category name, e.g. Food, Salary',
    `type`        TINYINT(1)       NOT NULL                COMMENT '1=income, 2=expense',
    `icon`        VARCHAR(100)     DEFAULT NULL            COMMENT 'Icon identifier or URL',
    `color`       VARCHAR(20)      DEFAULT NULL            COMMENT 'Hex color, e.g. #FF6B6B',
    `sort_order`  INT              NOT NULL DEFAULT 0      COMMENT 'Display order',
    `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_categories_user_id` (`user_id`),
    KEY `idx_categories_type`    (`type`),
    CONSTRAINT `fk_categories_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Income/expense categories';


-- ============================================================
-- Table: transactions
-- Core ledger. Each row is one income or expense record.
-- ============================================================
CREATE TABLE IF NOT EXISTS `transactions` (
    `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id`       BIGINT UNSIGNED  NOT NULL                COMMENT 'Owner',
    `category_id`   BIGINT UNSIGNED  NOT NULL                COMMENT 'Category reference',
    `type`          TINYINT(1)       NOT NULL                COMMENT '1=income, 2=expense',
    `amount`        DECIMAL(12, 2)   NOT NULL                COMMENT 'Positive monetary value',
    `transaction_date` DATE          NOT NULL                COMMENT 'Date the transaction occurred',
    `note`          VARCHAR(500)     DEFAULT NULL            COMMENT 'Optional description',
    `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_transactions_user_id`         (`user_id`),
    KEY `idx_transactions_category_id`     (`category_id`),
    KEY `idx_transactions_type`            (`type`),
    KEY `idx_transactions_date`            (`transaction_date`),
    -- Composite index for the most common query: user + date range
    KEY `idx_transactions_user_date`       (`user_id`, `transaction_date`),
    CONSTRAINT `fk_transactions_user`
        FOREIGN KEY (`user_id`)     REFERENCES `users`      (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transactions_category`
        FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Income and expense records';


-- ============================================================
-- Table: budgets
-- Monthly budget limits per category per user.
-- ============================================================
CREATE TABLE IF NOT EXISTS `budgets` (
    `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id`      BIGINT UNSIGNED  NOT NULL                COMMENT 'Owner',
    `category_id`  BIGINT UNSIGNED  NOT NULL                COMMENT 'Target category',
    `year_month`   CHAR(7)          NOT NULL                COMMENT 'Format: YYYY-MM',
    `amount`       DECIMAL(12, 2)   NOT NULL                COMMENT 'Budget limit',
    `created_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    -- A user can only have one budget per category per month
    UNIQUE KEY `uq_budget_user_category_month` (`user_id`, `category_id`, `year_month`),
    KEY `idx_budgets_user_id`      (`user_id`),
    KEY `idx_budgets_category_id`  (`category_id`),
    CONSTRAINT `fk_budgets_user`
        FOREIGN KEY (`user_id`)     REFERENCES `users`      (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_budgets_category`
        FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Monthly budgets per category';


-- ============================================================
-- Useful queries (reference, not DDL)
-- ============================================================

-- Monthly summary for dashboard:
-- SELECT
--   DATE_FORMAT(transaction_date, '%Y-%m') AS month,
--   SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS total_income,
--   SUM(CASE WHEN type = 2 THEN amount ELSE 0 END) AS total_expense
-- FROM transactions
-- WHERE user_id = ? AND deleted = 0
-- GROUP BY month
-- ORDER BY month DESC
-- LIMIT 6;

-- Category pie chart data:
-- SELECT c.name, c.color, SUM(t.amount) AS total
-- FROM transactions t
-- JOIN categories c ON t.category_id = c.id
-- WHERE t.user_id = ? AND t.type = 2 AND t.deleted = 0
--   AND t.transaction_date BETWEEN ? AND ?
-- GROUP BY c.id, c.name, c.color
-- ORDER BY total DESC;

-- Budget vs. actual:
-- SELECT b.category_id, c.name, b.amount AS budget,
--        COALESCE(SUM(t.amount), 0) AS actual
-- FROM budgets b
-- JOIN categories c ON b.category_id = c.id
-- LEFT JOIN transactions t
--   ON t.category_id = b.category_id
--  AND t.user_id = b.user_id
--  AND DATE_FORMAT(t.transaction_date, '%Y-%m') = b.year_month
--  AND t.deleted = 0
-- WHERE b.user_id = ? AND b.year_month = ?
-- GROUP BY b.id, c.name, b.amount;
