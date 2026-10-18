-- =====================================================================
-- EcoNest Management System - sample data
--
-- Run AFTER database/schema.sql:
--     mysql -u root -p < database/sample_data.sql
--
-- Safe to re-run: every table is cleared first, so everyone works with
-- the same ids. Only run this against a development database.
-- =====================================================================

USE econest;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE delivery_assignments;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE stock_in;
TRUNCATE TABLE stock_items;
TRUNCATE TABLE delivery_agents;
TRUNCATE TABLE vehicles;
TRUNCATE TABLE clients;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE products;
SET FOREIGN_KEY_CHECKS = 1;

-- --- products: 3 plants, 2 planters, 2 gardening kits ----------------
INSERT INTO products (product_id, sku, name, product_type, description, unit_price, eco_rating,
                      species, indoor, care_level, mature_height_cm)
VALUES (1, 'PLT-0001', 'Peace Lily', 'PLANT',
        'Air-purifying indoor plant with white blooms.', 1850.00, 'HIGH',
        'Spathiphyllum wallisii', TRUE, 'EASY', 60),
       (2, 'PLT-0002', 'Snake Plant', 'PLANT',
        'Very low maintenance, tolerates low light.', 2200.00, 'EXCELLENT',
        'Dracaena trifasciata', TRUE, 'EASY', 90),
       (3, 'PLT-0003', 'Curry Leaf Sapling', 'PLANT',
        'Outdoor kitchen-garden sapling.', 950.00, 'HIGH',
        'Murraya koenigii', FALSE, 'MODERATE', 250);

INSERT INTO products (product_id, sku, name, product_type, description, unit_price, eco_rating,
                      material, capacity_litres, diameter_cm, has_drainage)
VALUES (4, 'PNT-0001', 'Terracotta Pot 25cm', 'PLANTER',
        'Hand-thrown clay pot with saucer.', 1250.00, 'EXCELLENT',
        'Terracotta', 8.50, 25, TRUE),
       (5, 'PNT-0002', 'Recycled Plastic Trough', 'PLANTER',
        'Made from 100% post-consumer plastic.', 2950.00, 'HIGH',
        'Recycled plastic', 22.00, 60, TRUE);

INSERT INTO products (product_id, sku, name, product_type, description, unit_price, eco_rating,
                      piece_count, skill_level)
VALUES (6, 'KIT-0001', 'Balcony Starter Kit', 'GARDENING_KIT',
        'Trowel, fork, gloves, coco peat and seed packets.', 4500.00, 'MEDIUM',
        6, 'BEGINNER'),
       (7, 'KIT-0002', 'Compost Bin Kit', 'GARDENING_KIT',
        'Two-chamber home composting bin with aerator.', 7800.00, 'EXCELLENT',
        4, 'INTERMEDIATE');

-- --- suppliers -------------------------------------------------------
INSERT INTO suppliers (supplier_id, name, contact)
VALUES (1, 'Green Roots Nursery', 'Nimal Perera / 071-4455661'),
       (2, 'ClayCraft Ceramics', 'Sanduni Fernando / 077-2231190'),
       (3, 'EcoTools Lanka', 'orders@ecotools.lk / 011-2345678');

-- --- clients: 2 household, 2 office ----------------------------------
INSERT INTO clients (client_id, client_type, name, email, phone,
                     address_line1, address_line2, city, postal_code,
                     household_size, preferred_delivery_slot)
VALUES (1, 'HOUSEHOLD', 'Amara Silva', 'amara.silva@example.com', '0712345678',
        '42 Lake Road', 'Apt 3B', 'Colombo', '00500', 4, 'Weekday evening'),
       (2, 'HOUSEHOLD', 'Dinuka Jayawardena', 'dinuka.j@example.com', '0769876543',
        '17 Temple Lane', NULL, 'Kandy', '20000', 2, 'Saturday morning');

INSERT INTO clients (client_id, client_type, name, email, phone,
                     address_line1, address_line2, city, postal_code,
                     company_registration_no, contact_person, department)
VALUES (3, 'OFFICE', 'BlueSky Software (Pvt) Ltd', 'facilities@bluesky.example.com', '0112567890',
        '120 Union Place', 'Level 7', 'Colombo', '00200',
        'PV00123456', 'Rashmi Gunawardena', 'Facilities'),
       (4, 'OFFICE', 'Lagoon Hotels Ltd', 'procurement@lagoonhotels.example.com', '0912233445',
        '5 Beach Road', NULL, 'Galle', '80000',
        'PV00987654', 'Tharindu Wijesinghe', 'Procurement');

-- --- vehicles --------------------------------------------------------
INSERT INTO vehicles (vehicle_id, registration_number, vehicle_type, make_model, capacity_kg)
VALUES (1, 'WP-CAB-1234', 'VAN', 'Toyota HiAce', 950.00),
       (2, 'WP-BJK-5678', 'THREE_WHEELER', 'Bajaj RE', 300.00),
       (3, 'CP-MBK-9012', 'MOTORBIKE', 'Honda CD 125', 60.00);

-- --- delivery agents -------------------------------------------------
INSERT INTO delivery_agents (agent_id, name, phone, email, licence_number,
                             vehicle_id, area_covered, available, joined_on)
VALUES (1, 'Kasun Rathnayake', '0714567890', 'kasun.r@econest.example.com', 'B1234567',
        1, 'Colombo', TRUE, '2024-03-11'),
       (2, 'Pradeep Kumara', '0778901234', 'pradeep.k@econest.example.com', 'B2345678',
        2, 'Colombo suburbs', TRUE, '2024-07-01'),
       (3, 'Nadeesha Fonseka', '0723456789', 'nadeesha.f@econest.example.com', 'B3456789',
        3, 'Kandy', FALSE, '2025-01-20');

-- --- current stock (products 3 and 7 are at or below reorder level) ---
INSERT INTO stock_items (product_id, quantity_on_hand, reorder_level)
VALUES (1, 40, 10),
       (2, 25, 10),
       (3, 6, 8),
       (4, 60, 15),
       (5, 18, 5),
       (6, 12, 4),
       (7, 3, 3);

-- --- stock received from suppliers -----------------------------------
INSERT INTO stock_in (stock_in_id, product_id, supplier_id, quantity, received_date)
VALUES (1, 1, 1, 30, '2026-08-05'),
       (2, 2, 1, 20, '2026-08-05'),
       (3, 3, 1, 15, '2026-08-19'),
       (4, 4, 2, 50, '2026-09-02'),
       (5, 5, 2, 20, '2026-09-02'),
       (6, 6, 3, 15, '2026-09-18'),
       (7, 7, 3, 10, '2026-09-18');

-- --- orders ----------------------------------------------------------
INSERT INTO orders (order_id, client_id, order_date, status, total_amount, delivery_address, notes)
VALUES (1, 1, '2026-09-12 10:15:00', 'DELIVERED', 4950.00,
        '42 Lake Road, Apt 3B, Colombo 00500', 'Leave with security if not at home'),
       (2, 3, '2026-09-28 14:40:00', 'DISPATCHED', 17800.00,
        '120 Union Place, Level 7, Colombo 00200', 'Reception desk, office hours only'),
       (3, 2, '2026-10-05 09:05:00', 'CONFIRMED', 2200.00,
        '17 Temple Lane, Kandy 20000', NULL),
       (4, 4, '2026-10-14 16:20:00', 'PENDING', 11700.00,
        '5 Beach Road, Galle 80000', 'Quote requested for a repeat monthly order'),
       (5, 1, '2026-10-16 11:00:00', 'CANCELLED', 1250.00,
        '42 Lake Road, Apt 3B, Colombo 00500', 'Client cancelled the same day');

-- --- order items (totals above match these lines) --------------------
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
VALUES (1, 1, 1, 1850.00),   -- 1850
       (1, 4, 2, 1250.00),   -- 2500
       (1, 3, 1, 600.00),    --  600  (promotional price)
       (2, 7, 2, 7800.00),   -- 15600
       (2, 6, 1, 2200.00),   --  2200 (bundle price)
       (3, 2, 1, 2200.00),   --  2200
       (4, 5, 2, 2950.00),   --  5900
       (4, 6, 1, 4500.00),   --  4500
       (4, 4, 1, 1300.00),   --  1300
       (5, 4, 1, 1250.00);   --  1250 (cancelled order)

-- --- delivery assignments --------------------------------------------
INSERT INTO delivery_assignments (assignment_id, order_id, agent_id, status, assigned_at, delivered_at)
VALUES (1, 1, 1, 'DELIVERED', '2026-09-12 12:00:00', '2026-09-13 15:30:00'),
       (2, 2, 2, 'IN_TRANSIT', '2026-09-29 08:30:00', NULL),
       (3, 3, 3, 'ASSIGNED', '2026-10-05 10:00:00', NULL);

-- --- notifications ---------------------------------------------------
INSERT INTO notifications (notification_id, notification_type, recipient_email, subject, body,
                           status, client_id, order_id, sent_at, error_message)
VALUES (1, 'ORDER_CONFIRMATION', 'amara.silva@example.com',
        'EcoNest order #1 confirmed',
        'Thank you for your order. We will notify you when it is dispatched.',
        'SENT', 1, 1, '2026-09-12 10:16:00', NULL),
       (2, 'DELIVERY_COMPLETED', 'amara.silva@example.com',
        'EcoNest order #1 delivered',
        'Your order was delivered on 2026-09-13. We hope you enjoy your plants!',
        'SENT', 1, 1, '2026-09-13 15:35:00', NULL),
       (3, 'DELIVERY_ASSIGNED', 'facilities@bluesky.example.com',
        'EcoNest order #2 is on its way',
        'Your order has been assigned to Pradeep Kumara (WP-BJK-5678).',
        'SENT', 3, 2, '2026-09-29 08:31:00', NULL),
       (4, 'LOW_STOCK_ALERT', 'inventory@econest.example.com',
        'Low stock: Curry Leaf Sapling, Compost Bin Kit',
        'Two products have reached their reorder level.',
        'PENDING', NULL, NULL, NULL, NULL),
       (5, 'ORDER_STATUS_CHANGE', 'procurement@lagoonhotels.example.com',
        'EcoNest order #4 received',
        'We have received your order and will confirm pricing shortly.',
        'FAILED', 4, 4, NULL, 'SMTP connection timed out');
