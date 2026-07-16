-- Migration: Make budgets.category_id nullable and drop the old unique constraint
-- Run this SQL against the bookkeeping_db database

-- Step 1: Drop the foreign key on category_id
ALTER TABLE `budgets` DROP FOREIGN KEY IF EXISTS `fk_budgets_category`;

-- Step 2: Make category_id nullable
ALTER TABLE `budgets` MODIFY COLUMN `category_id` BIGINT UNSIGNED NULL;

-- Step 3: Drop the old unique constraint (if it uses NOT NULL category_id)
ALTER TABLE `budgets` DROP INDEX IF EXISTS `uq_budget_user_category_month`;

-- Step 4: Add a new unique constraint that allows NULL category_id
-- User can only have one overall budget (NULL category) per month, 
-- OR one budget per specific category per month
ALTER TABLE `budgets` ADD UNIQUE KEY `uq_budget_user_category_month` (`user_id`, `budget_month`, `category_id`);
