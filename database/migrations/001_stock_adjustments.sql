-- =====================================================================
-- Migration 001 - stock_adjustments
--
-- PROPOSED by the inventory module. NOT yet folded into schema.sql,
-- because schema.sql is shared by the whole team.
--
-- Why it is needed
-- ----------------
-- The inventory screen has to record a stock *adjustment* with a reason
-- (damage, recount, write-off, return). Nothing in schema.sql can hold
-- that today:
--   - products.quantity  is only the current count, with no history
--   - stock_in           records supplier receipts only: quantity > 0 and
--                        supplier_id is NOT NULL, so it cannot represent a
--                        write-off and has no reason column
--
-- This table is additive: it creates one new table and changes nothing
-- that another module reads or writes.
--
-- Run with:
--   mysql -u root -p econest < database/migrations/001_stock_adjustments.sql
--
-- Once everyone has run it, copy the CREATE TABLE below into schema.sql
-- (after stock_in) and delete this file.
-- =====================================================================

USE econest;

CREATE TABLE IF NOT EXISTS stock_adjustments (
    adjustment_id INT          NOT NULL AUTO_INCREMENT,
    product_id    INT          NOT NULL,
    -- Signed change applied to products.quantity: negative for a write-off,
    -- positive for a recount upwards. Never zero.
    quantity_delta INT         NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    adjusted_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (adjustment_id),
    KEY idx_stock_adjustments_product (product_id),
    KEY idx_stock_adjustments_date (adjusted_at),
    CONSTRAINT fk_stock_adjustments_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_stock_adjustments_delta CHECK (quantity_delta <> 0)
) ENGINE = InnoDB;
