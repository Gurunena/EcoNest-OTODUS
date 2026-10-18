# EcoNest database setup

Shared MySQL layer. Everyone should run these steps once so all modules work
against the same schema and the same test rows.

## 1. Create the schema and load the sample data

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample_data.sql
```

`schema.sql` creates the `econest` database if it does not exist and then every
table in dependency order. `sample_data.sql` clears all tables and reloads a
fixed set of rows with fixed ids, so it is safe to re-run whenever your data
gets messy — only run it against a development database.

## 2. Point the app at your database

```bash
cp db.properties.example db.properties
```

Then edit `db.properties` with your own MySQL user and password:

```properties
url=jdbc:mysql://localhost:3306/econest
user=root
password=
```

`db.properties` is git-ignored, so your password stays on your machine.
`db.properties.example` is committed and is the file to update if a new key is
ever needed.

## 3. Test the connection

`com.econest.util.DBConnection` has a `main` method that opens a connection and
prints the server version:

```bash
mvn compile exec:java -Dexec.mainClass=com.econest.util.DBConnection
```

Or just right-click `DBConnection` in IntelliJ and choose **Run**. On success:

```
Connected to: jdbc:mysql://localhost:3306/econest
Server:       8.0.x
Catalog:      econest
Connection OK
```

Anything else prints `Connection FAILED` with the reason (file missing, server
down, wrong password, schema not created).

## 4. Use it from a DAO

Every call returns a fresh connection, so always close it:

```java
try (Connection conn = DBConnection.getConnection();
     PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE product_id = ?")) {
    ps.setInt(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        ...
    }
}
```

## Tables

| Table | Module | Notes |
| --- | --- | --- |
| `products` | product | Columns match `ProductDao` exactly: `product_id`, `product_name`, `category`, `price`, `eco_rating` (int 1–5), `description`, `quantity`. `quantity` is the on-hand stock count. Nullable Plant / Planter / GardeningKit columns sit at the end and `ProductDao` ignores them |
| `suppliers` | inventory | |
| `clients` | client | One table for HouseholdClient / OfficeClient; `ContactDetails` is flattened into the same row |
| `vehicles` | agent | |
| `delivery_agents` | agent | `vehicle_id` is unique — one vehicle per agent |
| `stock_items` | inventory | Reorder level only — see the note below |
| `stock_in` | inventory | Stock received from a supplier |
| `orders` | order | `total_amount` is the order total; `delivery_address` is a snapshot taken at order time |
| `order_items` | order | `unit_price` is the price charged then, not today's `products.price` |
| `delivery_assignments` | delivery | One assignment per order (`order_id` is unique) |
| `notifications` | notification | Outgoing email queue with `PENDING` / `SENT` / `FAILED` status |

Three read-only views:

- `v_stock_items` — on-hand count and reorder level together, matching the
  `StockItem` model (`product`, `quantityOnHand`, `reorderLevel`)
- `v_low_stock_items` — products at or below their reorder level (`LowStockAlert`)
- `v_monthly_sales_summary` — order count, units sold and revenue per month,
  cancelled orders excluded (`MonthlySalesSummary`)

### Where stock levels live

`products.quantity` is the single source of truth for on-hand stock, because
`ProductDao` already reads and writes it. `stock_items` therefore holds only
`reorder_level` — if it kept its own copy of the quantity the two would drift
apart as soon as a product was sold.

For the inventory module that means:

- read stock through `v_stock_items` (it maps 1:1 onto `StockItem`)
- write the count with `UPDATE products SET quantity = ? WHERE product_id = ?`
- write the threshold with `UPDATE stock_items SET reorder_level = ? WHERE product_id = ?`

### Open item: OrderDao

`OrderDao` currently inserts into `orders` as
`(client_id, product_id, quantity, order_date, total_amount, order_status,
delivery_agent_id)`, which does not match this schema and will fail. The
`orders` table here is normalized, so that SQL needs:

- `order_status` → `status`
- `product_id` and `quantity` → one row per product in `order_items`
  (`order_id`, `product_id`, `quantity`, `unit_price`)
- `delivery_agent_id` → a row in `delivery_assignments` (`order_id`, `agent_id`,
  `status`), which is also what the delivery and reports modules read

That change belongs to the order module owner, not to this layer.

## Notes for the module owners

If you add a field that is not in the table, or name an enum constant
differently, update `schema.sql` and `sample_data.sql` in the same commit so the
shared database stays in step.

Columns that still need confirming against the real model classes:

- **clients** — `Client` keeps contact details in a `ContactDetails` object with
  a single `address` string, while this table splits it into `address_line1`,
  `address_line2`, `city` and `postal_code`. Either concatenate on read or
  collapse the table to one `address` column; no `ClientDao` SQL exists yet, so
  it is cheap to change now.
- **delivery_agents** — `DeliveryAgent` has `nic` and an `AgentStatus` enum,
  where this table has `licence_number` and a boolean `available`. The extra
  `area_covered` and `joined_on` columns have no model field.
- **vehicles** — `Vehicle` has `maxLoadKg` (int) against `capacity_kg`
  (decimal), and no `make_model` field.

Enum / check values currently in the schema:

- `products.category` — `PLANT`, `PLANTER`, `GARDENING_KIT` (plain VARCHAR, so
  nothing stops other values; keep to these three)
- `products.eco_rating` — integer 1 (worst) to 5 (best), enforced by a CHECK
- `products.care_level` — `EASY`, `MODERATE`, `DIFFICULT`
- `products.skill_level` — `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- `clients.client_type` — `HOUSEHOLD`, `OFFICE`
- `vehicles.vehicle_type` — `BICYCLE`, `MOTORBIKE`, `THREE_WHEELER`, `VAN`, `TRUCK`
- `orders.status` — `PENDING`, `CONFIRMED`, `PACKED`, `DISPATCHED`, `DELIVERED`, `CANCELLED`
- `delivery_assignments.status` — `ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`, `FAILED`
  (matches the existing `DeliveryStatus` enum)
- `notifications.notification_type` — `ORDER_CONFIRMATION`, `ORDER_STATUS_CHANGE`,
  `DELIVERY_ASSIGNED`, `DELIVERY_COMPLETED`, `LOW_STOCK_ALERT`
- `notifications.status` — `PENDING`, `SENT`, `FAILED`
