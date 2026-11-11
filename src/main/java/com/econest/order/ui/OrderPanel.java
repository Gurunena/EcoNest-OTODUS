package com.econest.order.ui;

import com.econest.order.model.Money;
import com.econest.order.model.Order;
import com.econest.order.model.OrderStatus;
import com.econest.order.service.OrderService;
import com.econest.ui.BaseModulePanel;
import com.econest.ui.Theme;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Process Client Orders - the order list, and everything you can do to one.
 *
 * <p>Built on {@link BaseModulePanel}, so the title, search box, table and
 * button row are the shared ones. The template's four buttons are retuned
 * for orders:</p>
 * <ul>
 *   <li><b>New order</b> (Add) - opens {@link OrderEditorDialog}</li>
 *   <li><b>Edit</b> (Update) - the same dialog, PENDING orders only</li>
 *   <li><b>Delete</b> - refused once an order is dispatched or delivered</li>
 *   <li>plus <b>Advance</b>, <b>Cancel order</b> and <b>Summary</b></li>
 * </ul>
 *
 * <p>Every database call runs on a {@link SwingWorker} so the window stays
 * responsive, and no stack trace ever reaches the user. The status column
 * renders as a pill through the shared renderer, and every amount is
 * formatted by {@link Money} from a {@code BigDecimal} the service worked
 * out - this class never does arithmetic on money.</p>
 */
public class OrderPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {
            "Order no", "Client", "Order date", "Delivery date", "Total", "Status"
    };

    private static final int COL_ORDER_NO = 0;
    private static final int COL_TOTAL = 4;
    private static final int COL_STATUS = 5;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** The status filter's "everything" entry. */
    private static final String ANY_STATUS = "All statuses";

    // --- Form fields ------------------------------------------------------
    private JComboBox<Object> cmbStatusFilter;
    private JLabel lblSelected;
    private JLabel lblClient;
    private JLabel lblDates;
    private JLabel lblItems;
    private JLabel lblTotal;
    private JComboBox<OrderStatus> cmbMoveTo;

    // --- Extra actions beyond the template's four -------------------------
    private JButton btnAdvance;
    private JButton btnMove;
    private JButton btnCancelOrder;
    private JButton btnSummary;

    private final OrderService service;

    /** The orders currently on screen, so a selection maps back to one. */
    private final List<Order> rows = new ArrayList<>();

    /**
     * True until the first load finishes. Every page is built when the window
     * opens, so a database that is down would otherwise stack modal dialogs
     * over the dashboard before the user has touched anything. The first
     * failure goes to the status bar; anything the user asked for still gets
     * a dialog. Same reasoning as {@code InventoryPanel}.
     */
    private boolean initialLoad = true;

    public OrderPanel() {
        this(new OrderService());
    }

    /** Injectable constructor, so a test can supply a fake service. */
    public OrderPanel(OrderService service) {
        super("Client Orders", COLUMNS);
        this.service = service;

        TableStyler.colourStatusColumn(tblData, COL_STATUS);
        TableStyler.rightAlign(tblData, COL_TOTAL);
        setEmptyState("No orders found",
                "Try a different search or status, or press New order to take one.");

        // Retune the template's buttons for an order screen.
        btnAdd.setText("New order");
        btnAdd.setToolTipText("Take a new order from a client");
        btnUpdate.setText("Edit");
        btnUpdate.setToolTipText("Change a pending order's items or dates");
        btnDelete.setToolTipText("Remove an order that was never sent out");

        btnAdvance = Buttons.secondary("Advance");
        btnAdvance.setToolTipText("Move this order on to its next status");
        btnAdvance.addActionListener(e -> advance());

        btnMove = Buttons.secondary("Move to");
        btnMove.setToolTipText("Move this order to the status chosen in the form");
        btnMove.addActionListener(e -> moveToChosenStatus());

        btnCancelOrder = Buttons.danger("Cancel order");
        btnCancelOrder.setToolTipText("Call the order off and put its stock back");
        btnCancelOrder.addActionListener(e -> cancelOrder());

        btnSummary = Buttons.secondary("Summary");
        btnSummary.setToolTipText("View, print or copy this order's summary");
        btnSummary.addActionListener(e -> showSummary());

        // New order | Edit | Advance | Move to | Cancel order | Summary |
        // Delete | Clear - a 2-column grid, so they wrap inside the card.
        pnlActions.add(btnAdvance, 2);
        pnlActions.add(btnMove, 3);
        pnlActions.add(btnCancelOrder, 4);
        pnlActions.add(btnSummary, 5);

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelection();
            }
        });

        initPanel();
    }

    // =====================================================================
    // Hook 1: the form
    // =====================================================================

    @Override
    protected void buildForm(JPanel form) {
        Object[] filters = new Object[OrderStatus.values().length + 1];
        filters[0] = ANY_STATUS;
        System.arraycopy(OrderStatus.values(), 0, filters, 1, OrderStatus.values().length);

        cmbStatusFilter = FormFields.comboBox(filters);
        cmbStatusFilter.setToolTipText("Show only orders in one status");
        cmbStatusFilter.addActionListener(e -> reload());

        lblSelected = headingLabel("No order selected");
        lblClient = detailLabel();
        lblDates = detailLabel();
        lblItems = detailLabel();

        lblTotal = new JLabel(Money.format(Money.ZERO));
        lblTotal.setFont(Theme.FONT_HEADING);
        lblTotal.setForeground(Theme.ACCENT);
        lblTotal.setAlignmentX(LEFT_ALIGNMENT);

        cmbMoveTo = FormFields.comboBox(OrderStatus.values());
        cmbMoveTo.setToolTipText("Pick a status, then press Move to");

        form.add(FormFields.heading("Filter"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Status", cmbStatusFilter));
        form.add(Box.createVerticalStrut(Theme.GAP));

        form.add(FormFields.heading("Selected order"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(lblSelected);
        form.add(Box.createVerticalStrut(4));
        form.add(lblClient);
        form.add(lblDates);
        form.add(lblItems);
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(lblTotal);
        form.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        form.add(FormFields.heading("Change status"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Move to", cmbMoveTo));
    }

    // =====================================================================
    // Hook 2: loading the table
    // =====================================================================

    @Override
    protected void loadTableData() {
        reload();
    }

    /** Reads orders off the UI thread and repaints the table when they land. */
    private void reload() {
        final String query = txtSearch == null ? "" : txtSearch.getText().trim();
        final OrderStatus filter = selectedFilter();

        setBusy(true);
        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() throws Exception {
                return service.search(query, filter);
            }

            @Override
            protected void done() {
                setBusy(false);
                boolean quiet = initialLoad;
                initialLoad = false;
                try {
                    showRows(get(), query, filter);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    if (quiet) {
                        quietFail("load the order list", ex.getCause());
                    } else {
                        fail("load the order list", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void showRows(List<Order> orders, String query, OrderStatus filter) {
        rows.clear();
        rows.addAll(orders);

        Object[][] data = new Object[orders.size()][COLUMNS.length];
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            data[i] = new Object[]{
                    order.getOrderNo(),
                    order.getClientName(),
                    order.getOrderDate() == null ? "-" : DATE.format(order.getOrderDate()),
                    order.getDeliveryDate() == null ? "-" : order.getDeliveryDate().toString(),
                    Money.format(order.getTotalAmount()),
                    order.getStatus().label()};
        }
        setRows(data);
        showSelection();

        StringBuilder status = new StringBuilder(orders.size() + " order(s)");
        if (!query.isEmpty()) {
            status.append(" matching \"").append(query).append('"');
        }
        if (filter != null) {
            status.append(query.isEmpty() ? " " : ", ")
                    .append(filter.label().toLowerCase());
        }
        setStatus(status.append('.').toString());
    }

    // =====================================================================
    // Hook 3: New order / Edit / Delete
    // =====================================================================

    @Override
    protected void onAdd() {
        if (OrderEditorDialog.open(this, service, null)) {
            reload();
        }
    }

    @Override
    protected void onUpdate() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select the order you want to edit.");
            return;
        }
        if (!selected.getStatus().isEditable()) {
            Dialogs.warn(this, "Order " + selected.getOrderNo() + " is "
                    + selected.getStatus().label().toLowerCase()
                    + ".\n\nOnly a pending order can be edited. Cancel it if it is wrong.");
            return;
        }

        // Re-read with the items attached; the table only carries the header.
        withFullOrder(selected.getOrderId(), "open order " + selected.getOrderNo(),
                full -> {
                    if (OrderEditorDialog.open(this, service, full)) {
                        reload();
                    }
                });
    }

    @Override
    protected void onDelete() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select the order you want to delete.");
            return;
        }
        if (selected.getStatus().isOutOfOurHands()) {
            Dialogs.warn(this, "Order " + selected.getOrderNo() + " is "
                    + selected.getStatus().label().toLowerCase()
                    + " and cannot be deleted.\n\n"
                    + "Cancel it instead if it fell through - that keeps the record "
                    + "and puts the stock back.");
            return;
        }

        final int orderId = selected.getOrderId();
        final String orderNo = selected.getOrderNo();
        runWrite("delete order " + orderNo,
                () -> service.deleteOrder(orderId),
                () -> setStatus("Order " + orderNo + " deleted; its stock went back."));
    }

    // =====================================================================
    // Extra actions
    // =====================================================================

    /** Moves the order one step along the happy path. */
    private void advance() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select an order first.");
            return;
        }
        OrderStatus next = selected.getStatus().next();
        if (next == null) {
            Dialogs.warn(this, "Order " + selected.getOrderNo() + " is "
                    + selected.getStatus().label().toLowerCase()
                    + " and has nowhere further to go.");
            return;
        }

        final int orderId = selected.getOrderId();
        final String orderNo = selected.getOrderNo();
        runWrite("move order " + orderNo + " to " + next.label().toLowerCase(),
                () -> service.updateStatus(orderId, next),
                () -> setStatus("Order " + orderNo + " is now "
                        + next.label().toLowerCase() + "."));
    }

    /** Moves the order to whatever the form's combo is showing. */
    private void moveToChosenStatus() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select an order first.");
            return;
        }
        OrderStatus target = (OrderStatus) cmbMoveTo.getSelectedItem();
        if (target == null) {
            Dialogs.error(this, "Choose the status to move the order to.");
            return;
        }
        if (target == OrderStatus.CANCELLED) {
            // Cancelling has stock to give back and a question to ask, so it
            // goes down the same path as the Cancel order button.
            cancelOrder();
            return;
        }

        final int orderId = selected.getOrderId();
        final String orderNo = selected.getOrderNo();
        runWrite("move order " + orderNo + " to " + target.label().toLowerCase(),
                () -> service.updateStatus(orderId, target),
                () -> setStatus("Order " + orderNo + " is now "
                        + target.label().toLowerCase() + "."));
    }

    /** Cancels the order and puts its stock back, after asking. */
    private void cancelOrder() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select the order you want to cancel.");
            return;
        }
        if (selected.getStatus() == OrderStatus.CANCELLED) {
            Dialogs.warn(this, "Order " + selected.getOrderNo()
                    + " is already cancelled.");
            return;
        }
        if (selected.getStatus() == OrderStatus.DELIVERED) {
            Dialogs.warn(this, "Order " + selected.getOrderNo()
                    + " has already been delivered and cannot be cancelled.");
            return;
        }

        if (!Dialogs.confirm(this, "Cancel order " + selected.getOrderNo()
                + " for " + selected.getClientName() + "?\n\n"
                + "Every item on it goes back into stock. This cannot be undone.")) {
            return;
        }

        final int orderId = selected.getOrderId();
        final String orderNo = selected.getOrderNo();
        runWrite("cancel order " + orderNo,
                () -> service.cancelOrder(orderId),
                () -> setStatus("Order " + orderNo
                        + " cancelled; its stock went back on the shelf."));
    }

    /** Opens the read-only summary for the selected order. */
    private void showSummary() {
        Order selected = selectedOrder();
        if (selected == null) {
            Dialogs.error(this, "Select the order you want a summary of.");
            return;
        }
        withFullOrder(selected.getOrderId(),
                "build the summary for " + selected.getOrderNo(),
                full -> OrderSummaryDialog.show(this,
                        "Order summary - " + full.getOrderNo(),
                        OrderService.summaryOf(full, full.getItems())));
    }

    // =====================================================================
    // Clear and search
    // =====================================================================

    @Override
    protected void onClear() {
        cmbStatusFilter.setSelectedItem(ANY_STATUS);
        super.onClear();      // clears the selection and the search box
        reload();
    }

    @Override
    protected void onSearch(String query) {
        reload();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Mirrors the selected row into the form. */
    private void showSelection() {
        Order selected = selectedOrder();
        if (selected == null) {
            lblSelected.setText("No order selected");
            lblSelected.setForeground(Theme.TEXT_MUTED);
            lblClient.setText(" ");
            lblDates.setText(" ");
            lblItems.setText(" ");
            lblTotal.setText(Money.format(Money.ZERO));
            return;
        }

        lblSelected.setText(selected.getOrderNo() + "  -  "
                + selected.getStatus().label());
        lblSelected.setForeground(Theme.TEXT_PRIMARY);
        lblClient.setText(selected.getClientName());
        lblDates.setText("<html>Ordered " + DATE.format(selected.getOrderDate())
                + "<br>Delivery "
                + (selected.getDeliveryDate() == null
                ? "not specified" : selected.getDeliveryDate())
                + "</html>");
        lblItems.setText(selected.getStatus().isEditable()
                ? "Editable" : "Read-only once past pending");
        lblTotal.setText(Money.format(selected.getTotalAmount()));

        // Offer the move the order can actually make, so the combo is useful
        // without having to think about the transition rules.
        OrderStatus next = selected.getStatus().next();
        cmbMoveTo.setSelectedItem(next == null ? selected.getStatus() : next);
    }

    /** The Order behind the selected table row, or null. */
    private Order selectedOrder() {
        Object orderNo = selectedValue(COL_ORDER_NO);
        if (orderNo == null) {
            return null;
        }
        // Looked up by order number rather than row index, because a search
        // or a sort means the view row is not the list position.
        return rows.stream()
                .filter(order -> order.getOrderNo().equals(orderNo))
                .findFirst()
                .orElse(null);
    }

    private OrderStatus selectedFilter() {
        Object chosen = cmbStatusFilter == null ? null : cmbStatusFilter.getSelectedItem();
        return chosen instanceof OrderStatus status ? status : null;
    }

    /** Re-reads one order with its items, then hands it to {@code action}. */
    private void withFullOrder(int orderId, String action, OrderConsumer whenReady) {
        setBusy(true);
        new SwingWorker<Order, Void>() {
            @Override
            protected Order doInBackground() throws Exception {
                return service.findById(orderId);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    Order full = get();
                    if (full == null) {
                        Dialogs.error(OrderPanel.this,
                                "That order no longer exists. Refreshing the list.");
                        reload();
                        return;
                    }
                    whenReady.accept(full);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    /** A database write that runs off the UI thread; reloads on success. */
    private void runWrite(String action, DbCall call, Runnable onSuccess) {
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                call.run();
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    onSuccess.run();
                    reload();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    /** Something a service call can throw. */
    @FunctionalInterface
    private interface DbCall {
        void run() throws SQLException;
    }

    /** What to do once a full order has been re-read. */
    @FunctionalInterface
    private interface OrderConsumer {
        void accept(Order order);
    }

    /**
     * Turns any failure into one clear sentence. A validation message from
     * the service is already user-ready; anything else gets a generic lead-in
     * and the detail. The stack trace goes to the console only.
     */
    private void fail(String action, Throwable cause) {
        if (cause instanceof IllegalArgumentException) {
            Dialogs.error(this, cause.getMessage());
            setStatus("Nothing changed.");
            return;
        }
        String detail = cause == null || cause.getMessage() == null
                ? "Unknown error."
                : cause.getMessage();
        System.err.println("OrderPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("The order operation failed; nothing was changed.");
    }

    /**
     * Reports a failure the user did not ask for - the load that happens when
     * the window first opens - in the status bar rather than a pop-up.
     */
    private void quietFail(String action, Throwable cause) {
        System.err.println("OrderPanel: could not " + action + " - " + cause);
        setStatus("Could not " + action + ". Check db.properties and the MySQL server, "
                + "and that migration 002 has been run, then search or press Clear to retry.");
    }

    private void setBusy(boolean busy) {
        btnAdd.setEnabled(!busy);
        btnUpdate.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
        for (JButton button : new JButton[]{btnAdvance, btnMove, btnCancelOrder, btnSummary}) {
            if (button != null) {
                button.setEnabled(!busy);
            }
        }
    }

    private JLabel headingLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BODY_BOLD);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel detailLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }
}
