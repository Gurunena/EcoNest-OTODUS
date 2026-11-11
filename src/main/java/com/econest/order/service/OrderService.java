package com.econest.order.service;

import com.econest.order.dao.ClientDirectoryDao;
import com.econest.order.dao.OrderDao;
import com.econest.order.model.ClientRef;
import com.econest.order.model.Money;
import com.econest.order.model.Order;
import com.econest.order.model.OrderItem;
import com.econest.order.model.OrderStatus;
import com.econest.order.model.OrderTotals;
import com.econest.order.model.ProductRef;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business rules for processing client orders. The panel talks to this; this
 * talks to {@link OrderDao}. No SQL above this line, and no arithmetic below
 * the UI.
 *
 * <h2>What other modules can call</h2>
 * <ul>
 *   <li>{@link #updateStatus(int, OrderStatus)} - move one order along,
 *       rejecting an invalid transition. This is the method the delivery
 *       module (task 6) should call instead of writing
 *       {@code orders.status} itself.</li>
 *   <li>{@link #findPendingUnassigned()} - orders still waiting for an agent</li>
 *   <li>{@link #findByMonth(int, int)} - one calendar month of orders</li>
 *   <li>{@link #countByStatus(OrderStatus)} - how many sit in a status</li>
 *   <li>{@link #revenueForMonth(int, int)} - month revenue, cancelled excluded</li>
 *   <li>{@link #getItems(int)} - the lines on one order</li>
 * </ul>
 *
 * <p>Everything that can fail for a reason the user could fix throws
 * {@link IllegalArgumentException} with a message that is safe to show as-is,
 * which is the convention {@code ProductService} and {@code InventoryService}
 * already use. Anything else is an {@link SQLException}.</p>
 */
public class OrderService {

    private final OrderDao dao;
    private final ClientLookup clients;
    private final ProductLookup products;

    public OrderService() {
        this(new OrderDao(), new ClientDirectoryDao(), new ProductCatalogAdapter());
    }

    /** Injectable constructor, so a test can supply fakes for all three. */
    public OrderService(OrderDao dao, ClientLookup clients, ProductLookup products) {
        this.dao = dao;
        this.clients = clients;
        this.products = products;
    }

    // =====================================================================
    // Totals - the one place order money is added up
    // =====================================================================

    /**
     * Adds up a set of order lines.
     *
     * <p>Static and free of any database, so the create-order form can call
     * it on every keystroke to show a live total, and a unit test can check
     * the arithmetic without a connection.</p>
     *
     * <p>Each line is rounded to 2dp and then the lines are summed, which is
     * the same order the stored {@code unit_price} values would be summed in
     * by the database. Rounding once at the end instead would let the screen
     * and {@code v_monthly_sales_summary} disagree by a cent.</p>
     */
    public static OrderTotals totalsOf(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return OrderTotals.EMPTY;
        }
        BigDecimal subtotal = Money.ZERO;
        int units = 0;
        for (OrderItem item : items) {
            subtotal = subtotal.add(item.getLineTotal());
            units += item.getQuantity();
        }
        subtotal = Money.scale(subtotal);

        // orders has no discount or delivery fee column, so the grand total
        // is the subtotal today. See OrderTotals for why they stay separate.
        return new OrderTotals(items.size(), units, subtotal, subtotal);
    }

    // =====================================================================
    // Validation
    // =====================================================================

    /**
     * Checks everything about a draft order that does not need the database:
     * a client, at least one line, every quantity above zero, no product
     * twice, and a delivery date that is not before the order date.
     *
     * @throws IllegalArgumentException with a message safe to show the user
     */
    public static void validate(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("There is no order to save.");
        }
        if (order.getClientId() <= 0) {
            throw new IllegalArgumentException("Choose the client this order is for.");
        }
        if (order.getOrderDate() == null) {
            throw new IllegalArgumentException("Enter the order date.");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one item to the order before saving it.");
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProductId() <= 0) {
                throw new IllegalArgumentException("Every line needs a product.");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity for "
                        + item.getProductName() + " must be more than zero.");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
                throw new IllegalArgumentException("Unit price for "
                        + item.getProductName() + " cannot be negative.");
            }
        }

        // order_items has UNIQUE (order_id, product_id), so two lines for the
        // same product would fail on insert with a constraint name the user
        // could do nothing with. Say it plainly instead.
        List<String> duplicates = duplicateProducts(order.getItems());
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", duplicates)
                    + " is on the order more than once. "
                    + "Edit that line's quantity instead of adding a second one.");
        }

        LocalDate wanted = order.getDeliveryDate();
        if (wanted != null && wanted.isBefore(order.getOrderDate().toLocalDate())) {
            throw new IllegalArgumentException(
                    "The delivery date cannot be before the order date ("
                            + order.getOrderDate().toLocalDate() + ").");
        }
    }

    /**
     * Checks one line against what is actually on the shelf.
     *
     * <p>This is the friendly check, run while the user is still typing. The
     * binding one is the {@code quantity >= ?} guard inside the DAO's
     * transaction, which is what holds when two people order the last unit at
     * the same moment.</p>
     *
     * @param alreadyOnOrder units of this product already on the draft, which
     *                       a new line has to share the shelf with
     * @throws IllegalArgumentException with a message safe to show the user
     */
    public static void checkStock(ProductRef product, int quantity, int alreadyOnOrder) {
        if (product == null) {
            throw new IllegalArgumentException("Choose a product for this line.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be more than zero.");
        }
        int available = product.quantityOnHand() - Math.max(0, alreadyOnOrder);
        if (available <= 0) {
            throw new IllegalArgumentException(product.productName()
                    + " is out of stock.");
        }
        if (quantity > available) {
            throw new IllegalArgumentException("Only " + available + " unit(s) of "
                    + product.productName() + " are in stock"
                    + (alreadyOnOrder > 0
                    ? " once the " + alreadyOnOrder + " already on this order are counted."
                    : ".")
                    + " You asked for " + quantity + ".");
        }
    }

    /** Convenience for a line that is not sharing stock with another line. */
    public static void checkStock(ProductRef product, int quantity) {
        checkStock(product, quantity, 0);
    }

    // =====================================================================
    // Reads
    // =====================================================================

    /**
     * Orders for the list, newest first.
     *
     * @param query  client name or order number; blank for all
     * @param status the only status to show, or null for every status
     */
    public List<Order> search(String query, OrderStatus status) throws SQLException {
        return dao.search(query, status);
    }

    /** One order with its items, or null when there is no such order. */
    public Order findById(int orderId) throws SQLException {
        return dao.findById(orderId);
    }

    /** The lines on one order. Other modules are welcome to call this. */
    public List<OrderItem> getItems(int orderId) throws SQLException {
        return dao.findItems(orderId);
    }

    /** Orders that are PENDING or CONFIRMED and have no delivery agent yet. */
    public List<Order> findPendingUnassigned() throws SQLException {
        return dao.findPendingUnassigned();
    }

    /** Every order placed in one calendar month, cancelled ones included. */
    public List<Order> findByMonth(int year, int month) throws SQLException {
        requireMonth(month);
        return dao.findByMonth(year, month);
    }

    /** How many orders sit in one status right now. */
    public int countByStatus(OrderStatus status) throws SQLException {
        if (status == null) {
            throw new IllegalArgumentException("Choose a status to count.");
        }
        return dao.countByStatus(status);
    }

    /** One month's revenue, cancelled orders excluded. */
    public BigDecimal revenueForMonth(int year, int month) throws SQLException {
        requireMonth(month);
        return dao.revenueForMonth(year, month);
    }

    /** Clients for the picker on the create-order form. */
    public List<ClientRef> findClients(String query) throws SQLException {
        return clients.search(query);
    }

    /** Products for the line picker. */
    public List<ProductRef> findProducts(String query) throws SQLException {
        return products.search(query);
    }

    /** One product, so a line can be priced and stock-checked. */
    public ProductRef findProduct(int productId) throws SQLException {
        return products.findById(productId);
    }

    // =====================================================================
    // Writes
    // =====================================================================

    /**
     * Validates a draft and saves it in one transaction: the order, its
     * items and the stock deduction.
     *
     * <p>The total is recomputed here from the lines rather than trusted from
     * the caller, so what is stored in {@code total_amount} can never drift
     * from what the lines add up to.</p>
     *
     * @return the new order id
     */
    public int createOrder(Order order) throws SQLException {
        validate(order);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalsOf(order.getItems()).grandTotal());

        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().isBlank()) {
            // Snapshot the client's address, so a later change to their
            // record never rewrites where a past order was sent.
            ClientRef client = clients.findById(order.getClientId());
            if (client != null) {
                order.setDeliveryAddress(client.address());
            }
        }
        return dao.insert(order);
    }

    /**
     * Saves changes to an order that is still PENDING. The status check is
     * repeated inside the DAO transaction; this one is only so the user gets
     * a clear sentence rather than a failed write.
     */
    public void updateOrder(Order order) throws SQLException {
        validate(order);
        if (order.getOrderId() <= 0) {
            throw new IllegalArgumentException("Select the order to update first.");
        }

        OrderStatus stored = dao.findStatus(order.getOrderId());
        if (stored == null) {
            throw new IllegalArgumentException("That order no longer exists.");
        }
        if (!stored.isEditable()) {
            throw new IllegalArgumentException("Order " + order.getOrderNo() + " is "
                    + stored.label().toLowerCase()
                    + ". Only a pending order can be edited.");
        }

        order.setTotalAmount(totalsOf(order.getItems()).grandTotal());
        dao.update(order);
    }

    /**
     * Moves an order to a new status, rejecting a move that is not allowed.
     *
     * <p><b>This is the method other modules should call.</b> The delivery
     * module (task 6) assigning an agent should call
     * {@code updateStatus(orderId, OrderStatus.DISPATCHED)} rather than
     * writing {@code orders.status} itself, so the transition rules and the
     * stock handling on a cancellation stay in one place.</p>
     *
     * <p>Cancelling is routed through {@link #cancelOrder(int)}, because it
     * is not just a status change - it has stock to give back.</p>
     *
     * @throws IllegalArgumentException if the move is not a legal one
     */
    public void updateStatus(int orderId, OrderStatus newStatus) throws SQLException {
        if (newStatus == null) {
            throw new IllegalArgumentException("Choose the status to move the order to.");
        }
        OrderStatus current = dao.findStatus(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Order #" + orderId + " no longer exists.");
        }
        if (current == newStatus) {
            throw new IllegalArgumentException("Order #" + orderId + " is already "
                    + newStatus.label().toLowerCase() + ".");
        }
        if (!current.canMoveTo(newStatus)) {
            throw new IllegalArgumentException("An order that is "
                    + current.label().toLowerCase() + " cannot be moved to "
                    + newStatus.label().toLowerCase() + ". "
                    + describeAllowed(current));
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancelOrder(orderId);
            return;
        }
        dao.updateStatus(orderId, current, newStatus);
    }

    /**
     * Moves an order one step along the happy path: pending to confirmed,
     * confirmed to packed, packed to dispatched, dispatched to delivered.
     */
    public void advanceStatus(int orderId) throws SQLException {
        OrderStatus current = dao.findStatus(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Order #" + orderId + " no longer exists.");
        }
        OrderStatus next = current.next();
        if (next == null) {
            throw new IllegalArgumentException("Order #" + orderId + " is "
                    + current.label().toLowerCase() + " and has nowhere further to go.");
        }
        dao.updateStatus(orderId, current, next);
    }

    /**
     * Cancels an order and returns its stock to the shelf, both in one
     * transaction. The confirmation dialog belongs to the panel; by the time
     * this is called the user has already said yes.
     */
    public void cancelOrder(int orderId) throws SQLException {
        OrderStatus current = dao.findStatus(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Order #" + orderId + " no longer exists.");
        }
        if (current == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order #" + orderId
                    + " is already cancelled.");
        }
        if (current == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Order #" + orderId
                    + " has already been delivered and cannot be cancelled.");
        }
        dao.cancel(orderId);
    }

    /**
     * Deletes an order, refusing once it is dispatched or delivered.
     */
    public void deleteOrder(int orderId) throws SQLException {
        OrderStatus current = dao.findStatus(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Order #" + orderId + " no longer exists.");
        }
        if (current.isOutOfOurHands()) {
            throw new IllegalArgumentException("Order #" + orderId + " is "
                    + current.label().toLowerCase()
                    + " and cannot be deleted. Cancel it instead if it fell through.");
        }
        dao.delete(orderId);
    }

    // =====================================================================
    // Order summary - the printable view
    // =====================================================================

    /**
     * A plain-text summary of one order, for the View / Print dialog.
     * Everything is already formatted, so the dialog only has to show it in
     * a monospaced font.
     */
    public static String summaryOf(Order order, List<OrderItem> items) {
        OrderTotals totals = totalsOf(items);
        DateTimeFormatter stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder out = new StringBuilder();
        out.append("EcoNest - Home & Garden Retail\n");
        out.append("Order summary\n");
        out.append("=".repeat(62)).append('\n');
        out.append(pad("Order no", 16)).append(order.getOrderNo()).append('\n');
        out.append(pad("Client", 16)).append(nullSafe(order.getClientName())).append('\n');
        out.append(pad("Order date", 16))
                .append(order.getOrderDate() == null ? "-" : stamp.format(order.getOrderDate()))
                .append('\n');
        out.append(pad("Delivery date", 16))
                .append(order.getDeliveryDate() == null
                        ? "not specified" : order.getDeliveryDate().toString())
                .append('\n');
        out.append(pad("Status", 16)).append(order.getStatus().label()).append('\n');
        out.append(pad("Deliver to", 16))
                .append(nullSafe(order.getDeliveryAddress())).append('\n');
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            out.append(pad("Notes", 16)).append(order.getNotes()).append('\n');
        }

        out.append('\n').append("-".repeat(62)).append('\n');
        out.append(pad("Qty", 6)).append(pad("Product", 28))
                .append(padLeft("Unit price", 13)).append(padLeft("Total", 15)).append('\n');
        out.append("-".repeat(62)).append('\n');

        for (OrderItem item : items) {
            out.append(pad(String.valueOf(item.getQuantity()), 6))
                    .append(pad(clip(item.getProductName(), 27), 28))
                    .append(padLeft(Money.format(item.getUnitPrice()), 13))
                    .append(padLeft(Money.format(item.getLineTotal()), 15))
                    .append('\n');
        }

        out.append("-".repeat(62)).append('\n');
        out.append(padLeft("Items:", 34)).append(padLeft(
                totals.itemCount() + " line(s), " + totals.unitCount() + " unit(s)", 28))
                .append('\n');
        out.append(padLeft("Subtotal:", 47))
                .append(padLeft(Money.format(totals.subtotal()), 15)).append('\n');
        out.append(padLeft("GRAND TOTAL:", 47))
                .append(padLeft(Money.format(totals.grandTotal()), 15)).append('\n');
        out.append("=".repeat(62)).append('\n');
        return out.toString();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Product names that appear on more than one line. */
    private static List<String> duplicateProducts(List<OrderItem> items) {
        Map<Integer, List<OrderItem>> byProduct = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId));

        List<String> names = new ArrayList<>();
        byProduct.forEach((productId, lines) -> {
            if (lines.size() > 1) {
                names.add(lines.get(0).getProductName() == null
                        ? "Product #" + productId
                        : lines.get(0).getProductName());
            }
        });
        return names;
    }

    /** "It can only be confirmed, dispatched or cancelled from here." */
    private static String describeAllowed(OrderStatus current) {
        List<String> allowed = current.allowedNext().stream()
                .sorted()
                .map(OrderStatus::label)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (allowed.isEmpty()) {
            return "It has reached the end of its life.";
        }
        if (allowed.size() == 1) {
            return "It can only be moved to " + allowed.get(0) + " from here.";
        }
        String last = allowed.remove(allowed.size() - 1);
        return "From here it can only be moved to "
                + String.join(", ", allowed) + " or " + last + ".";
    }

    private static void requireMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }

    private static String pad(String text, int width) {
        String safe = nullSafe(text);
        return safe.length() >= width ? safe + " " : safe + " ".repeat(width - safe.length());
    }

    private static String padLeft(String text, int width) {
        String safe = nullSafe(text);
        return safe.length() >= width ? safe : " ".repeat(width - safe.length()) + safe;
    }

    private static String clip(String text, int max) {
        String safe = nullSafe(text);
        return safe.length() <= max ? safe : safe.substring(0, max - 1) + "…";
    }

    private static String nullSafe(String text) {
        return text == null ? "-" : text;
    }
}
