-- =====================================================================
-- EcoNest Management System - database schema
-- MySQL 8.0+
--
-- Run with:   mysql -u root -p < database/schema.sql
-- Then load:  mysql -u root -p < database/sample_data.sql
--
-- Tables are created in dependency order, so the file can be run top to
-- bottom on an empty server. To rebuild from scratch, uncomment the DROP
-- block below.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS econest
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE econest;

-- --- clean reset (uncomment to wipe and rebuild) ---------------------
-- DROP VIEW  IF EXISTS v_monthly_sales_summary, v_low_stock_items;
-- DROP TABLE IF EXISTS notifications, delivery_assignments, order_items,
--                      orders, stock_in, stock_items, delivery_agents,
--                      vehicles, clients, suppliers, products;

-- =====================================================================
-- 1. products
--    Column names match com.econest.product.dao.ProductDao exactly:
--    (product_id, product_name, category, price, eco_rating, description,
--    quantity). Do not rename these without telling the product module owner.
--
--    'quantity' is the on-hand stock count and is the single source of
--    truth for stock levels - see stock_items below, which holds only the
--    reorder level.
--
--    The nullable columns at the bottom are for Plant / Planter /
--    GardeningKit; only the ones matching 'category' are filled in, and
--    ProductDao ignores them.
-- =====================================================================
CREATE TABLE IF NOT EXISTS products (
    product_id        INT             NOT NULL AUTO_INCREMENT,
    product_name      VARCHAR(120)    NOT NULL,
    category          VARCHAR(40)     NOT NULL,  -- PLANT | PLANTER | GARDENING_KIT
    price             DECIMAL(10, 2)  NOT NULL,
    eco_rating        TINYINT         NOT NULL DEFAULT 3,  -- 1 (worst) .. 5 (best)
    description       VARCHAR(500)        NULL,
    quantity          INT             NOT NULL DEFAULT 0,  -- units on hand
    sku               VARCHAR(32)         NULL,  -- optional; ProductDao does not set it
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PLANT only
    species           VARCHAR(80)         NULL,
    indoor            BOOLEAN             NULL,
    care_level        ENUM('EASY', 'MODERATE', 'DIFFICULT') NULL,
    mature_height_cm  INT                 NULL,

    -- PLANTER only
    material          VARCHAR(60)         NULL,
    capacity_litres   DECIMAL(6, 2)       NULL,
    diameter_cm       INT                 NULL,
    has_drainage      BOOLEAN             NULL,

    -- GARDENING_KIT only
    piece_count       INT                 NULL,
    skill_level       ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NULL,

    PRIMARY KEY (product_id),
    UNIQUE KEY uq_products_sku (sku),
    KEY idx_products_category (category),
    KEY idx_products_name (product_name),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_products_eco_rating CHECK (eco_rating BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- =====================================================================
-- 2. suppliers  (inventory.model.Supplier)
-- =====================================================================
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id   INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    contact       VARCHAR(120) NOT NULL,

    PRIMARY KEY (supplier_id),
    UNIQUE KEY uq_suppliers_name (name)
) ENGINE = InnoDB;

-- =====================================================================
-- 3. clients  (single table for Client / HouseholdClient / OfficeClient,
--    with ContactDetails flattened into the same row)
-- =====================================================================
CREATE TABLE IF NOT EXISTS clients (
    client_id               INT          NOT NULL AUTO_INCREMENT,
    client_type             ENUM('HOUSEHOLD', 'OFFICE') NOT NULL,
    name                    VARCHAR(120) NOT NULL,   -- person name, or company name for OFFICE

    -- ContactDetails
    email                   VARCHAR(120) NOT NULL,
    phone                   VARCHAR(20)  NOT NULL,
    address_line1           VARCHAR(150) NOT NULL,
    address_line2           VARCHAR(150)     NULL,
    city                    VARCHAR(60)  NOT NULL,
    postal_code             VARCHAR(10)      NULL,

    -- HOUSEHOLD only
    household_size          INT              NULL,
    preferred_delivery_slot VARCHAR(40)      NULL,

    -- OFFICE only
    company_registration_no VARCHAR(40)      NULL,
    contact_person          VARCHAR(120)     NULL,
    department              VARCHAR(80)      NULL,

    registered_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (client_id),
    UNIQUE KEY uq_clients_email (email),
    KEY idx_clients_type (client_type)
) ENGINE = InnoDB;

-- =====================================================================
-- 4. vehicles  (agent.model.Vehicle) - created before delivery_agents
-- =====================================================================
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id          INT           NOT NULL AUTO_INCREMENT,
    registration_number VARCHAR(20)   NOT NULL,
    vehicle_type        ENUM('BICYCLE', 'MOTORBIKE', 'THREE_WHEELER', 'VAN', 'TRUCK') NOT NULL,
    make_model          VARCHAR(80)       NULL,
    capacity_kg         DECIMAL(7, 2)     NULL,

    PRIMARY KEY (vehicle_id),
    UNIQUE KEY uq_vehicles_reg (registration_number),
    CONSTRAINT chk_vehicles_capacity CHECK (capacity_kg IS NULL OR capacity_kg > 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 5. delivery_agents  (agent.model.DeliveryAgent) - one vehicle per agent
-- =====================================================================
CREATE TABLE IF NOT EXISTS delivery_agents (
    agent_id       INT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(120) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    email          VARCHAR(120)     NULL,
    licence_number VARCHAR(30)  NOT NULL,
    vehicle_id     INT              NULL,
    area_covered   VARCHAR(60)      NULL,
    available      BOOLEAN      NOT NULL DEFAULT TRUE,
    joined_on      DATE             NULL,

    PRIMARY KEY (agent_id),
    UNIQUE KEY uq_agents_licence (licence_number),
    UNIQUE KEY uq_agents_vehicle (vehicle_id),
    CONSTRAINT fk_agents_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (vehicle_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
-- 6. stock_items  (inventory.model.StockItem)
--    Holds only the reorder threshold per product. The on-hand count
--    lives in products.quantity, because ProductDao already reads and
--    writes it - keeping a second copy here would let the two drift
--    apart. Read both together through the v_stock_items view below,
--    which lines up 1:1 with the StockItem model.
-- =====================================================================
CREATE TABLE IF NOT EXISTS stock_items (
    product_id    INT       NOT NULL,
    reorder_level INT       NOT NULL DEFAULT 0,
    last_updated  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id),
    CONSTRAINT fk_stock_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_stock_items_reorder CHECK (reorder_level >= 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 7. stock_in  (inventory.model.StockIn) - stock received from a supplier
-- =====================================================================
CREATE TABLE IF NOT EXISTS stock_in (
    stock_in_id   INT  NOT NULL AUTO_INCREMENT,
    product_id    INT  NOT NULL,
    supplier_id   INT  NOT NULL,
    quantity      INT  NOT NULL,
    received_date DATE NOT NULL,

    PRIMARY KEY (stock_in_id),
    KEY idx_stock_in_product (product_id),
    KEY idx_stock_in_supplier (supplier_id),
    KEY idx_stock_in_date (received_date),
    CONSTRAINT fk_stock_in_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_stock_in_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (supplier_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_stock_in_qty CHECK (quantity > 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 8. orders  (order.model.Order / OrderStatus)
-- =====================================================================
CREATE TABLE IF NOT EXISTS orders (
    order_id         INT            NOT NULL AUTO_INCREMENT,
    client_id        INT            NOT NULL,
    order_date       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status           ENUM('PENDING', 'CONFIRMED', 'PACKED', 'DISPATCHED', 'DELIVERED', 'CANCELLED')
                     NOT NULL DEFAULT 'PENDING',
    total_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    delivery_address VARCHAR(255)       NULL,  -- snapshot; may differ from the client address
    notes            VARCHAR(255)       NULL,

    PRIMARY KEY (order_id),
    KEY idx_orders_client (client_id),
    KEY idx_orders_date (order_date),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id)
        REFERENCES clients (client_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 9. order_items  (order.model.OrderItem) - unit_price is the price charged
--    at the time of the order, not the current products.price
-- =====================================================================
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT            NOT NULL AUTO_INCREMENT,
    order_id      INT            NOT NULL,
    product_id    INT            NOT NULL,
    quantity      INT            NOT NULL,
    unit_price    DECIMAL(10, 2) NOT NULL,

    PRIMARY KEY (order_item_id),
    UNIQUE KEY uq_order_items_order_product (order_id, product_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_items_qty CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 10. delivery_assignments  (delivery.model.DeliveryAssignment / DeliveryStatus)
--     One assignment per order.
-- =====================================================================
CREATE TABLE IF NOT EXISTS delivery_assignments (
    assignment_id INT      NOT NULL AUTO_INCREMENT,
    order_id      INT      NOT NULL,
    agent_id      INT      NOT NULL,
    status        ENUM('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED')
                  NOT NULL DEFAULT 'ASSIGNED',
    assigned_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at  DATETIME     NULL,

    PRIMARY KEY (assignment_id),
    UNIQUE KEY uq_assignments_order (order_id),
    KEY idx_assignments_agent (agent_id),
    KEY idx_assignments_status (status),
    CONSTRAINT fk_assignments_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assignments_agent FOREIGN KEY (agent_id)
        REFERENCES delivery_agents (agent_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
-- 11. notifications  (notification.model.EmailNotification / NotificationType)
-- =====================================================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id   INT          NOT NULL AUTO_INCREMENT,
    notification_type ENUM('ORDER_CONFIRMATION', 'ORDER_STATUS_CHANGE', 'DELIVERY_ASSIGNED',
                           'DELIVERY_COMPLETED', 'LOW_STOCK_ALERT') NOT NULL,
    recipient_email   VARCHAR(120) NOT NULL,
    subject           VARCHAR(200) NOT NULL,
    body              TEXT             NULL,
    status            ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    client_id         INT              NULL,
    order_id          INT              NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at           DATETIME         NULL,
    error_message     VARCHAR(255)     NULL,

    PRIMARY KEY (notification_id),
    KEY idx_notifications_status (status),
    KEY idx_notifications_client (client_id),
    KEY idx_notifications_order (order_id),
    CONSTRAINT fk_notifications_client FOREIGN KEY (client_id)
        REFERENCES clients (client_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
-- Report / inventory helper views
-- =====================================================================

-- StockItem: on-hand count (products.quantity) plus the reorder level,
-- matching the inventory.model.StockItem fields. Read stock through this
-- view; write products.quantity and stock_items.reorder_level directly.
CREATE OR REPLACE VIEW v_stock_items AS
SELECT p.product_id,
       p.product_name,
       p.quantity                       AS quantity_on_hand,
       COALESCE(s.reorder_level, 0)     AS reorder_level,
       s.last_updated
FROM products p
         LEFT JOIN stock_items s ON s.product_id = p.product_id;

-- LowStockAlert: products at or below their reorder level
CREATE OR REPLACE VIEW v_low_stock_items AS
SELECT p.product_id,
       p.sku,
       p.product_name,
       p.quantity    AS quantity_on_hand,
       s.reorder_level,
       s.last_updated
FROM stock_items s
         JOIN products p ON p.product_id = s.product_id
WHERE p.quantity <= s.reorder_level;

-- MonthlySalesSummary: one row per calendar month, cancelled orders excluded
CREATE OR REPLACE VIEW v_monthly_sales_summary AS
SELECT YEAR(o.order_date)                        AS sales_year,
       MONTH(o.order_date)                       AS sales_month,
       COUNT(DISTINCT o.order_id)                AS order_count,
       COALESCE(SUM(oi.quantity), 0)             AS units_sold,
       COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS total_revenue
FROM orders o
         LEFT JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY YEAR(o.order_date), MONTH(o.order_date);
