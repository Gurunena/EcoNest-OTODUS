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
| `products` | product | One table for Product / Plant / Planter / GardeningKit; `product_type` is the discriminator and the type-specific columns are null for other types |
| `suppliers` | inventory | |
| `clients` | client | One table for HouseholdClient / OfficeClient; `ContactDetails` is flattened into the same row |
| `vehicles` | agent | |
| `delivery_agents` | agent | `vehicle_id` is unique — one vehicle per agent |
| `stock_items` | inventory | Keyed by `product_id`; current quantity and reorder level |
| `stock_in` | inventory | Stock received from a supplier |
| `orders` | order | `total_amount` is the order total; `delivery_address` is a snapshot taken at order time |
| `order_items` | order | `unit_price` is the price charged then, not today's `products.unit_price` |
| `delivery_assignments` | delivery | One assignment per order (`order_id` is unique) |
| `notifications` | notification | Outgoing email queue with `PENDING` / `SENT` / `FAILED` status |

Two read-only views back the reports module:

- `v_low_stock_items` — products at or below their reorder level (`LowStockAlert`)
- `v_monthly_sales_summary` — order count, units sold and revenue per month,
  cancelled orders excluded (`MonthlySalesSummary`)

## Notes for the module owners

The model classes that are still empty skeletons (`Product`, `Plant`,
`Planter`, `GardeningKit`, `EcoRating`, `Client`, `HouseholdClient`,
`OfficeClient`, `ContactDetails`, `DeliveryAgent`, `Vehicle`, `Order`,
`OrderItem`, `OrderStatus`, `EmailNotification`, `NotificationType`) got their
columns from the project spec. If you add a field that is not in the table, or
name an enum constant differently, update `schema.sql` and `sample_data.sql` in
the same commit so the shared database stays in step.

Enum values currently in the schema:

- `products.product_type` — `PLANT`, `PLANTER`, `GARDENING_KIT`
- `products.eco_rating` — `LOW`, `MEDIUM`, `HIGH`, `EXCELLENT`
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
