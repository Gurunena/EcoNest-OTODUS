-- =====================================================================
-- Migration 002 - orders.delivery_date
--
-- PROPOSED by the order module. NOT yet folded into schema.sql,
-- because schema.sql is shared by the whole team.
--
-- Why it is needed
-- ----------------
-- "Process client orders" has to record the delivery date the client
-- asked for, which is a different thing from when the order was placed.
-- Nothing in schema.sql can hold it today:
--   - orders.order_date   is when the order was taken, not when it is wanted
--   - orders.notes        is free text and cannot be sorted or filtered on
--   - delivery_assignments.assigned_at / delivered_at are written by the
--     delivery module once an agent is on the job, which is far too late
--
-- This migration is additive: one nullable column on one table. Existing
-- rows keep working, and nothing another module reads or writes changes.
--
-- Run with:
--   mysql -u root -p econest < database/migrations/002_orders_delivery_date.sql
--
-- Once everyone has run it, copy the column into the orders CREATE TABLE
-- in schema.sql (after order_date) and delete this file.
-- =====================================================================

USE econest;

ALTER TABLE orders
    -- The delivery date the client requested. NULL means "not specified",
    -- which is what every row written before this migration says.
    ADD COLUMN delivery_date DATE NULL AFTER order_date,
    ADD CONSTRAINT chk_orders_delivery_date
        CHECK (delivery_date IS NULL OR delivery_date >= DATE(order_date));
