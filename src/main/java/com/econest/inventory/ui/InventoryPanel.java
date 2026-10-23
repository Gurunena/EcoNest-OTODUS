package com.econest.inventory.ui;

import com.econest.inventory.model.StockItem;
import com.econest.inventory.model.Supplier;
import com.econest.inventory.service.InventoryService;
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
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Stock / Inventory screen.
 *
 * <p>Shows the stock level of every product, highlights anything at or below
 * its reorder level in red, and offers three write actions:</p>
 * <ul>
 *   <li><b>Restock</b> - stock received from a supplier (writes stock_in)</li>
 *   <li><b>Adjust</b> - a signed correction with a reason (damage, recount)</li>
 *   <li><b>Reorder level</b> - the threshold that drives the highlight</li>
 * </ul>
 *
 * <p>Built on {@link BaseModulePanel}: the title, search box, table and
 * button row come from the template. Delete is removed because stock rows
 * are never deleted - a product leaving the catalogue is the product
 * module's job.</p>
 *
 * <p>Every database call runs on a {@link SwingWorker} so the window stays
 * responsive, and no stack trace ever reaches the user.</p>
 */
public class InventoryPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {
            "ID", "Product", "Category", "On hand", "Reorder level", "Status"
    };

    private static final int COL_ID = 0;
    private static final int COL_STATUS = 5;

    // --- Form fields (txt / cmb prefixes) ---------------------------------
    private JLabel lblSelected;
    private JTextField txtRestockQty;
    private JComboBox<SupplierChoice> cmbSupplier;
    private JTextField txtReceivedDate;
    private JTextField txtAdjustQty;
    private JTextField txtAdjustReason;
    private JTextField txtReorderLevel;

    /** Extra action beyond the template's four. */
    private JButton btnReorder;

    private final InventoryService service;

    /** The rows currently on screen, so a selection can be mapped back. */
    private final List<StockItem> rows = new ArrayList<>();

    /**
     * True until the first load finishes. Every page is constructed when the
     * window opens, so a database that is down would otherwise stack modal
     * error dialogs over the dashboard before the user has touched anything.
     * The first failure goes to the status bar instead; anything the user
     * actually asked for still gets a dialog.
     */
    private boolean initialLoad = true;

    public InventoryPanel() {
        this(new InventoryService());
    }

    /** Injectable constructor so a test can supply a fake service. */
    public InventoryPanel(InventoryService service) {
        super("Stock / Inventory", COLUMNS);
        this.service = service;

        // Low stock shows red through the shared status renderer.
        TableStyler.colourStatusColumn(tblData, COL_STATUS);

        // Retune the template's buttons for a stock screen.
        btnAdd.setText("Restock");
        btnAdd.setToolTipText("Record stock received from a supplier");
        btnUpdate.setText("Adjust");
        btnUpdate.setToolTipText("Correct the stock count and say why");
        pnlActions.remove(btnDelete);        // stock rows are never deleted

        btnReorder = Buttons.secondary("Reorder level");
        btnReorder.setToolTipText("Set the threshold that marks this product low");
        btnReorder.addActionListener(e -> setReorderLevel());
        pnlActions.add(btnReorder, 2);

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelection();
            }
        });

        initPanel();
    }

    // ---------------------------------------------------------------------
    // Hook 1: the form
    // ---------------------------------------------------------------------

    @Override
    protected void buildForm(JPanel form) {
        lblSelected = new JLabel("No product selected");
        lblSelected.setFont(Theme.FONT_BODY_BOLD);
        lblSelected.setForeground(Theme.TEXT_MUTED);
        lblSelected.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        txtRestockQty = FormFields.textField();
        cmbSupplier = FormFields.comboBox(new SupplierChoice[0]);
        txtReceivedDate = FormFields.textField();
        txtReceivedDate.setText(LocalDate.now().toString());
        txtReceivedDate.setToolTipText("yyyy-mm-dd");

        txtAdjustQty = FormFields.textField();
        txtAdjustQty.setToolTipText("Use a minus sign to remove stock, e.g. -3");
        txtAdjustReason = FormFields.textField();

        txtReorderLevel = FormFields.textField();

        form.add(FormFields.heading("Selected product"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(lblSelected);
        form.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        form.add(FormFields.heading("Restock"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Quantity received", txtRestockQty));
        form.add(FormFields.row("Supplier", cmbSupplier));
        form.add(FormFields.row("Received date (yyyy-mm-dd)", txtReceivedDate));
        form.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        form.add(FormFields.heading("Adjust"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Change (+ / -)", txtAdjustQty));
        form.add(FormFields.row("Reason", txtAdjustReason));
        form.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        form.add(FormFields.heading("Reorder level"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Threshold", txtReorderLevel));
    }

    // ---------------------------------------------------------------------
    // Hook 2: loading the table
    // ---------------------------------------------------------------------

    @Override
    protected void loadTableData() {
        loadStock(txtSearch == null ? "" : txtSearch.getText().trim());
        loadSuppliers();
    }

    /** Reads stock off the UI thread and repaints the table when it lands. */
    private void loadStock(String query) {
        setBusy(true);
        new SwingWorker<List<StockItem>, Void>() {
            @Override
            protected List<StockItem> doInBackground() throws Exception {
                return service.search(query);
            }

            @Override
            protected void done() {
                setBusy(false);
                boolean quiet = initialLoad;
                initialLoad = false;
                try {
                    showRows(get(), query);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    if (quiet) {
                        quietFail("load the stock list", ex.getCause());
                    } else {
                        fail("load the stock list", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    /** Fills the supplier combo; a failure here only disables restocking. */
    private void loadSuppliers() {
        new SwingWorker<List<Supplier>, Void>() {
            @Override
            protected List<Supplier> doInBackground() throws Exception {
                return service.findAllSuppliers();
            }

            @Override
            protected void done() {
                try {
                    cmbSupplier.removeAllItems();
                    for (Supplier supplier : get()) {
                        cmbSupplier.addItem(new SupplierChoice(supplier));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    // Not fatal: the stock table is still usable.
                    System.err.println("InventoryPanel: could not load suppliers - "
                            + ex.getCause());
                }
            }
        }.execute();
    }

    private void showRows(List<StockItem> items, String query) {
        rows.clear();
        rows.addAll(items);

        Object[][] data = new Object[items.size()][COLUMNS.length];
        for (int i = 0; i < items.size(); i++) {
            StockItem item = items.get(i);
            data[i] = new Object[]{
                    item.getProduct().getProductId(),
                    item.getProduct().getProductName(),
                    item.getProduct().getCategory(),
                    item.getQuantityOnHand(),
                    item.getReorderLevel(),
                    InventoryService.statusOf(item)};
        }
        setRows(data);

        long low = items.stream().filter(InventoryService::isLowStock).count();
        String scope = query.isEmpty() ? "" : " matching \"" + query + "\"";
        setStatus(items.size() + " product(s)" + scope
                + (low == 0 ? "." : ", " + low + " at or below reorder level."));
    }

    // ---------------------------------------------------------------------
    // Hook 3: Restock (the template's Add button)
    // ---------------------------------------------------------------------

    @Override
    protected void onAdd() {
        StockItem selected = selectedStockItem();
        if (selected == null) {
            Dialogs.error(this, "Select the product you are restocking.");
            return;
        }

        final int quantity;
        final LocalDate received;
        try {
            quantity = parseInt(txtRestockQty.getText(), "Quantity received");
            received = parseDate(txtReceivedDate.getText());
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        SupplierChoice supplier = (SupplierChoice) cmbSupplier.getSelectedItem();
        if (supplier == null) {
            Dialogs.error(this, "Select the supplier the stock came from. "
                    + "If the list is empty, add a supplier to the database first.");
            return;
        }

        final int productId = selected.getProduct().getProductId();
        final String productName = selected.getProduct().getProductName();

        runWrite("restock " + productName,
                () -> service.restock(productId, supplier.id(), quantity, received),
                () -> {
                    txtRestockQty.setText("");
                    setStatus("Added " + quantity + " to " + productName + ".");
                });
    }

    // ---------------------------------------------------------------------
    // Hook 4: Adjust (the template's Update button)
    // ---------------------------------------------------------------------

    @Override
    protected void onUpdate() {
        StockItem selected = selectedStockItem();
        if (selected == null) {
            Dialogs.error(this, "Select the product you are adjusting.");
            return;
        }

        final int delta;
        try {
            delta = parseSignedInt(txtAdjustQty.getText(), "Change");
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        final String reason = txtAdjustReason.getText().trim();
        if (reason.isEmpty()) {
            Dialogs.error(this, "Enter a reason for the adjustment.");
            txtAdjustReason.requestFocusInWindow();
            return;
        }

        final int productId = selected.getProduct().getProductId();
        final String productName = selected.getProduct().getProductName();

        // Removing stock is destructive enough to confirm.
        if (delta < 0 && !Dialogs.confirm(this,
                "Remove " + Math.abs(delta) + " unit(s) from " + productName + "?\nReason: " + reason)) {
            return;
        }

        runWrite("adjust " + productName,
                () -> service.adjustStock(productId, delta, reason),
                () -> {
                    txtAdjustQty.setText("");
                    txtAdjustReason.setText("");
                    setStatus("Adjusted " + productName + " by " + delta + ".");
                });
    }

    // ---------------------------------------------------------------------
    // Extra action: reorder level
    // ---------------------------------------------------------------------

    private void setReorderLevel() {
        StockItem selected = selectedStockItem();
        if (selected == null) {
            Dialogs.error(this, "Select a product first.");
            return;
        }

        final int level;
        try {
            level = parseInt(txtReorderLevel.getText(), "Reorder level");
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        final int productId = selected.getProduct().getProductId();
        final String productName = selected.getProduct().getProductName();

        runWrite("set the reorder level for " + productName,
                () -> service.setReorderLevel(productId, level),
                () -> setStatus("Reorder level for " + productName + " is now " + level + "."));
    }

    // ---------------------------------------------------------------------
    // Clear and search
    // ---------------------------------------------------------------------

    @Override
    protected void onClear() {
        txtRestockQty.setText("");
        txtReceivedDate.setText(LocalDate.now().toString());
        txtAdjustQty.setText("");
        txtAdjustReason.setText("");
        txtReorderLevel.setText("");
        lblSelected.setText("No product selected");
        super.onClear();
    }

    @Override
    protected void onSearch(String query) {
        loadStock(query);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Mirrors the selected row into the form. */
    private void showSelection() {
        StockItem selected = selectedStockItem();
        if (selected == null) {
            lblSelected.setText("No product selected");
            lblSelected.setForeground(Theme.TEXT_MUTED);
            return;
        }
        lblSelected.setText("<html>" + selected.getProduct().getProductName()
                + "<br>" + selected.getQuantityOnHand() + " on hand</html>");
        lblSelected.setForeground(InventoryService.isLowStock(selected)
                ? Theme.STATUS_DANGER : Theme.TEXT_PRIMARY);
        txtReorderLevel.setText(String.valueOf(selected.getReorderLevel()));
    }

    /** The StockItem behind the selected table row, or null. */
    private StockItem selectedStockItem() {
        Object id = selectedValue(COL_ID);
        if (id == null) {
            return null;
        }
        int productId = ((Number) id).intValue();
        return rows.stream()
                .filter(item -> item.getProduct().getProductId() == productId)
                .findFirst()
                .orElse(null);
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
                    loadStock(txtSearch.getText().trim());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    /** Something a DAO call can throw. */
    @FunctionalInterface
    private interface DbCall {
        void run() throws SQLException;
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
        System.err.println("InventoryPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Stock operation failed.");
    }

    /**
     * Reports a failure the user did not ask for - the load that happens when
     * the window first opens - in the status bar rather than a pop-up.
     */
    private void quietFail(String action, Throwable cause) {
        System.err.println("InventoryPanel: could not " + action + " - " + cause);
        setStatus("Could not " + action + ". Check db.properties and the MySQL server, "
                + "then search or press Clear to retry.");
    }

    private void setBusy(boolean busy) {
        btnAdd.setEnabled(!busy);
        btnUpdate.setEnabled(!busy);
        if (btnReorder != null) {
            btnReorder.setEnabled(!busy);
        }
    }

    private static int parseInt(String text, String fieldName) {
        int value = parseSignedInt(text, fieldName);
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private static int parseSignedInt(String text, String fieldName) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private static LocalDate parseDate(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter the date the stock was received.");
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Enter the received date as yyyy-mm-dd, for example "
                            + LocalDate.now() + ".");
        }
    }

    /** Supplier wrapper whose toString drives the combo box display. */
    private record SupplierChoice(int id, String name) {

        SupplierChoice(Supplier supplier) {
            this(supplier.getId(), supplier.getName());
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
