-- ============================================================
-- Migration: Make budgets.category_id nullable for global budgets
-- Run this on your server:
--   docker compose exec db mysql -u root -psecretpassword bookkeeping_db -e "SOURCE /docker-entrypoint-initdb.d/migration_budget.sql"
-- Or pipe it directly:
--   docker compose exec -T db mysql -u root -psecretpassword bookkeeping_db < db/migration_budget.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE budgets DROP FOREIGN KEY fk_budgets_category;
ALTER TABLE budgets MODIFY COLUMN category_id BIGINT UNSIGNED NULL COMMENT 'Target category (NULL = global budget)';
ALTER TABLE budgets DROP INDEX uq_budget_user_category_month;
ALTER TABLE budgets ADD UNIQUE KEY uq_budget_user_category_month (user_id, budget_month, category_id);
ALTER TABLE budgets ADD CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id);

SET FOREIGN_KEY_CHECKS = 1;