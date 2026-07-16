-- ============================================================
-- Migration: Make budgets.category_id nullable for global budgets
-- Run this MANUALLY on your production server:
--   docker compose exec db mysql -u root -psecretpassword bookkeeping_db < db/migration_budget.sql
-- ============================================================

-- Step 1: Drop the foreign key (if it exists — ignore error if not)
ALTER TABLE `budgets` DROP FOREIGN KEY `fk_budgets_category`;

-- Step 2: Make category_id nullable (allows NULL for global budgets)
ALTER TABLE `budgets` MODIFY COLUMN `category_id` BIGINT UNSIGNED NULL COMMENT 'Target category (NULL = global budget)';

-- Step 3: Drop the old unique constraint (ignore error if already dropped)
ALTER TABLE `budgets` DROP INDEX `uq_budget_user_category_month`;

-- Step 4: Re-add the foreign key (ignore error if already added)
ALTER TABLE `budgets` ADD CONSTRAINT `fk_budgets_category`
    FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`);

-- Step 5: Add the new unique constraint (ignore error if already added)
-- User can only have one overall (global) budget per month (category_id IS NULL),
-- OR one budget per specific category per month
ALTER TABLE `budgets` ADD UNIQUE KEY `uq_budget_user_category_month` (`user_id`, `budget_month`, `category_id`);