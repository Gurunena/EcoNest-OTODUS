package com.econest.order.ui;

import com.econest.order.model.ClientRef;
import com.econest.order.model.Money;
import com.econest.order.model.Order;
import com.econest.order.model.OrderItem;
import com.econest.order.model.OrderTotals;
import com.econest.order.model.ProductRef;
import com.econest.order.service.OrderService;
import com.econest.ui.Theme;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Card;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The create / edit order window: who it is for, when it is wanted, and the
 * lines on it.
 *
 * <p>It is a dialog rather than part of the order screen's form card because
 * an order needs a table of its own. The module screen's form is 340px wide -
 * enough for the fields that describe one record, not for a product picker, a
 * four-column items table and a running total. The screen behind it keeps the
 * standard look: this window uses the same {@link Card}, {@link Theme} and
 * {@link TableStyler} as everything else, with its buttons grouped under the
 * fields they act on.</p>
 *
 * <p>The dialog collects and checks input. It does not add anything up -
 * every total on screen comes from
 * {@link OrderService#totalsOf(java.util.List)}, and the save goes through
 * {@link OrderService}, which puts it in one transaction.</p>
 *
 * <p>Editing is only ever reached for a PENDING order; the panel checks that
 * before opening this, and the service and DAO both check it again.</p>
 */
public class OrderEditorDialog extends JDialog {

    private static final String[] ITEM_COLUMNS =
            {"Product", "Qty", "Unit price", "Line total"};

    private static final int COL_QTY = 1;
    private static final int COL_UNIT_PRICE = 2;
    private static final int COL_LINE_TOTAL = 3;

    // --- Order-level fields ----------------------------------------------
    private JComboBox<ClientRef> cmbClient;
    private JTextField txtClientSearch;
    private JTextField dtpOrderDate;
    private JTextField dtpDeliveryDate;
    private JTextField txtDeliveryAddress;
    private JTextField txtNotes;

    // --- Line-level fields -----------------------------------------------
    private JComboBox<ProductRef> cmbProduct;
    private JTextField txtQuantity;
    private JLabel lblUnitPrice;
    private JLabel lblLineTotal;
    private JLabel lblStockHint;

    private JButton btnAddItem;
    private JButton btnUpdateItem;
    private JButton btnRemoveItem;

    // --- Items table ------------------------------------------------------
    private JTable tblItems;
    private DefaultTableModel itemsModel;

    // --- Totals -----------------------------------------------------------
    private JLabel lblItemCount;
    private JLabel lblSubtotal;
    private JLabel lblGrandTotal;

    private JButton btnSave;

    private final OrderService service;

    /** The order being built. Null id means a new one. */
    private final Order order;

    /** The lines on screen, in table order. */
    private final List<OrderItem> lines = new ArrayList<>();

    /**
     * Units of each product this order already holds, which were taken off
     * {@code products.quantity} when it was first saved.
     *
     * <p>This is what makes editing work. The catalogue's on-hand count no
     * longer includes them, so checking a raised quantity against the
     * catalogue alone would refuse to let 2 units become 3 on a product with
     * none left - even though this order is already holding the 2. The real
     * ceiling for a line is "what is on the shelf plus what this order
     * already took", which is what {@link #effectiveStock(ProductRef)}
     * returns. Empty for a new order.</p>
     */
    private final Map<Integer, Integer> heldByThisOrder = new HashMap<>();

    /** True once the user saved, so the caller knows to reload. */
    private boolean saved;

    private OrderEditorDialog(Frame owner, OrderService service, Order existing) {
        super(owner, existing == null ? "New order"
                : "Edit order " + existing.getOrderNo(), true);
        this.service = service;
        this.order = existing == null ? new Order() : existing;

        if (existing != null) {
            for (OrderItem item : existing.getItems()) {
                heldByThisOrder.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        buildUi();
        loadClients("");
        loadProducts("");
        if (existing != null) {
            showExisting(existing);
        }
        refreshTotals();
    }

    /**
     * Opens the dialog and blocks until it closes.
     *
     * @param existing the order to edit, with its items loaded, or null for a
     *                 new one
     * @return true if the user saved
     */
    public static boolean open(Component parent, OrderService service, Order existing) {
        Frame owner = (Frame) javax.swing.SwingUtilities.getAncestorOfClass(
                Frame.class, parent);
        OrderEditorDialog dialog = new OrderEditorDialog(owner, service, existing);
        dialog.setVisible(true);
        return dialog.saved;
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(Theme.GAP_SMALL, Theme.GAP_SMALL));
        root.setBackground(Theme.PAGE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        root.add(buildDetailsCard(), BorderLayout.WEST);
        root.add(buildItemsCard(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        setSize(1000, 660);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(getOwner());
    }

    /** Left card: who the order is for and when it is wanted. */
    private JComponent buildDetailsCard() {
        cmbClient = FormFields.comboBox(new ClientRef[0]);
        cmbClient.setToolTipText("The client this order is for");

        // A search box above the combo, for a client list too long to scroll.
        txtClientSearch = FormFields.textField("Type to narrow the list");
        txtClientSearch.addActionListener(e -> loadClients(txtClientSearch.getText().trim()));

        dtpOrderDate = FormFields.textField("yyyy-mm-dd");
        dtpOrderDate.setText(LocalDate.now().toString());
        dtpOrderDate.setToolTipText("When the order was taken (yyyy-mm-dd)");

        dtpDeliveryDate = FormFields.textField("yyyy-mm-dd");
        dtpDeliveryDate.setToolTipText(
                "The delivery date the client asked for. Leave blank if not specified.");

        txtDeliveryAddress = FormFields.textField("Defaults to the client's address");
        txtDeliveryAddress.setToolTipText(
                "Where this order goes. Left blank, the client's own address is used.");

        txtNotes = FormFields.textField("Anything the packer should know");

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(FormFields.heading("Order details"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Find client", txtClientSearch));
        form.add(FormFields.row("Client", cmbClient));
        form.add(FormFields.row("Order date", dtpOrderDate));
        form.add(FormFields.row("Requested delivery date", dtpDeliveryDate));
        form.add(Box.createVerticalStrut(Theme.GAP));
        form.add(FormFields.heading("Delivery"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Deliver to", txtDeliveryAddress));
        form.add(FormFields.row("Notes", txtNotes));
        form.add(Box.createVerticalGlue());

        JScrollPane scrForm = new JScrollPane(form);
        scrForm.setBorder(BorderFactory.createEmptyBorder());
        scrForm.setOpaque(false);
        scrForm.getViewport().setOpaque(false);
        scrForm.getVerticalScrollBar().setUnitIncrement(16);
        scrForm.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        Card card = new Card(new BorderLayout()).withPadding();
        card.add(scrForm, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(340, 0));
        card.setMinimumSize(new Dimension(320, 0));
        return card;
    }

    /** Centre card: the product picker, the lines and the running total. */
    private JComponent buildItemsCard() {
        Card card = new Card(new BorderLayout(0, Theme.GAP_SMALL)).withPadding();
        card.add(buildItemPicker(), BorderLayout.NORTH);
        card.add(buildItemsTable(), BorderLayout.CENTER);
        card.add(buildTotals(), BorderLayout.SOUTH);
        return card;
    }

    /** The one-line "add a product" strip above the items table. */
    private JComponent buildItemPicker() {
        cmbProduct = FormFields.comboBox(new ProductRef[0]);
        cmbProduct.addActionListener(e -> showPickedProduct());

        txtQuantity = FormFields.textField("0");
        txtQuantity.setPreferredSize(new Dimension(90, Theme.FIELD_HEIGHT));
        txtQuantity.setMaximumSize(new Dimension(90, Theme.FIELD_HEIGHT));
        // Live line total as the quantity is typed, so the number is never a
        // surprise once the line lands in the table.
        txtQuantity.getDocument().addDocumentListener(
                new SimpleDocumentListener(this::showPickedProduct));

        lblUnitPrice = valueLabel(Money.format(Money.ZERO));
        lblLineTotal = valueLabel(Money.format(Money.ZERO));

        lblStockHint = new JLabel(" ");
        lblStockHint.setFont(Theme.FONT_SMALL);
        lblStockHint.setForeground(Theme.TEXT_MUTED);

        btnAddItem = Buttons.primary("Add line");
        btnAddItem.addActionListener(e -> addLine());
        btnUpdateItem = Buttons.secondary("Update line");
        btnUpdateItem.setToolTipText("Change the quantity of the selected line");
        btnUpdateItem.addActionListener(e -> updateLine());
        btnRemoveItem = Buttons.danger("Remove line");
        btnRemoveItem.addActionListener(e -> removeLine());

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.GAP_SMALL, 0));
        fields.setOpaque(false);
        fields.add(captioned("Product", cmbProduct, 300));
        fields.add(captioned("Quantity", txtQuantity, 90));
        fields.add(captioned("Unit price", lblUnitPrice, 130));
        fields.add(captioned("Line total", lblLineTotal, 140));

        JPanel actions = new JPanel(new GridLayout(1, 0, Theme.GAP_SMALL, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_SMALL, 0, 0, 0));
        actions.add(btnAddItem);
        actions.add(btnUpdateItem);
        actions.add(btnRemoveItem);

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.add(leftAligned(FormFields.heading("Items")));
        wrap.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        wrap.add(leftAligned(fields));
        wrap.add(leftAligned(lblStockHint));
        wrap.add(leftAligned(actions));
        wrap.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        wrap.add(Card.separator());
        return wrap;
    }

    private JComponent buildItemsTable() {
        itemsModel = new DefaultTableModel(ITEM_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;      // edited through the fields, not in place
            }
        };
        tblItems = new JTable(itemsModel);
        tblItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblItems.setAutoCreateRowSorter(false);   // line order is the user's own
        TableStyler.style(tblItems);
        TableStyler.rightAlign(tblItems, COL_QTY, COL_UNIT_PRICE, COL_LINE_TOTAL);

        tblItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedLine();
            }
        });

        JScrollPane scrItems = new JScrollPane(tblItems);
        scrItems.setBorder(BorderFactory.createEmptyBorder());
        scrItems.getViewport().setBackground(Theme.CARD_BG);
        return scrItems;
    }

    /** The running total strip under the table. */
    private JComponent buildTotals() {
        lblItemCount = new JLabel("No items yet");
        lblItemCount.setFont(Theme.FONT_SMALL);
        lblItemCount.setForeground(Theme.TEXT_MUTED);

        lblSubtotal = valueLabel(Money.format(Money.ZERO));

        lblGrandTotal = new JLabel(Money.format(Money.ZERO));
        lblGrandTotal.setFont(Theme.FONT_HEADING);
        lblGrandTotal.setForeground(Theme.ACCENT);

        JPanel amounts = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP, 0));
        amounts.setOpaque(false);
        amounts.add(mutedLabel("Subtotal"));
        amounts.add(lblSubtotal);
        amounts.add(Box.createHorizontalStrut(Theme.GAP));
        amounts.add(mutedLabel("Grand total"));
        amounts.add(lblGrandTotal);

        JPanel strip = new JPanel(new BorderLayout());
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_SMALL, 0, 0, 0));
        strip.add(lblItemCount, BorderLayout.WEST);
        strip.add(amounts, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(Card.separator(), BorderLayout.NORTH);
        wrap.add(strip, BorderLayout.CENTER);
        return wrap;
    }

    /** Save / Close, under everything they act on. */
    private JComponent buildFooter() {
        btnSave = Buttons.primary(order.getOrderId() > 0 ? "Save changes" : "Save order");
        btnSave.addActionListener(e -> save());

        JButton btnClose = Buttons.text("Close");
        btnClose.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_SMALL, 0, 0, 0));
        footer.add(btnClose);
        footer.add(btnSave);
        return footer;
    }

    // =====================================================================
    // Loading
    // =====================================================================

    /**
     * Fills the client combo. Runs on the dialog's own thread rather than a
     * SwingWorker: the dialog is modal and empty until this lands, so there
     * is nothing for the user to interact with in the meantime, and a failure
     * has to stop the dialog rather than leave an unusable form open.
     */
    private void loadClients(String query) {
        try {
            List<ClientRef> found = service.findClients(query);
            cmbClient.removeAllItems();
            for (ClientRef client : found) {
                cmbClient.addItem(client);
            }
            if (found.isEmpty()) {
                Dialogs.warn(this, query.isBlank()
                        ? "There are no clients in the database yet. Add a client "
                        + "before taking an order."
                        : "No client matches \"" + query + "\".");
            }
        } catch (Exception ex) {
            Dialogs.error(this, "Could not load the client list.\n\n" + reason(ex));
        }
    }

    private void loadProducts(String query) {
        try {
            cmbProduct.removeAllItems();
            for (ProductRef product : service.findProducts(query)) {
                cmbProduct.addItem(product);
            }
            showPickedProduct();
        } catch (Exception ex) {
            Dialogs.error(this, "Could not load the product list.\n\n" + reason(ex));
        }
    }

    /** Puts an order being edited on screen. */
    private void showExisting(Order existing) {
        selectClient(existing.getClientId());
        dtpOrderDate.setText(existing.getOrderDate().toLocalDate().toString());
        dtpDeliveryDate.setText(existing.getDeliveryDate() == null
                ? "" : existing.getDeliveryDate().toString());
        txtDeliveryAddress.setText(nullSafe(existing.getDeliveryAddress()));
        txtNotes.setText(nullSafe(existing.getNotes()));

        lines.clear();
        for (OrderItem item : existing.getItems()) {
            OrderItem copy = new OrderItem(item.getProductId(), item.getProductName(),
                    item.getQuantity(), item.getUnitPrice());
            copy.setOrderItemId(item.getOrderItemId());
            copy.setOrderId(item.getOrderId());
            lines.add(copy);
        }
        redrawItems();
    }

    private void selectClient(int clientId) {
        for (int i = 0; i < cmbClient.getItemCount(); i++) {
            if (cmbClient.getItemAt(i).clientId() == clientId) {
                cmbClient.setSelectedIndex(i);
                return;
            }
        }
        // The client list was filtered or the client was removed; fetch the
        // one this order needs so the combo is never silently wrong.
        try {
            ClientRef owner = service.findClients("").stream()
                    .filter(c -> c.clientId() == clientId)
                    .findFirst().orElse(null);
            if (owner != null) {
                cmbClient.addItem(owner);
                cmbClient.setSelectedItem(owner);
            }
        } catch (Exception ex) {
            Dialogs.warn(this, "Could not confirm which client this order is for. "
                    + "Check the client before saving.");
        }
    }

    // =====================================================================
    // Line editing
    // =====================================================================

    /** Mirrors the picked product and typed quantity into the price labels. */
    private void showPickedProduct() {
        ProductRef product = (ProductRef) cmbProduct.getSelectedItem();
        if (product == null) {
            lblUnitPrice.setText(Money.format(Money.ZERO));
            lblLineTotal.setText(Money.format(Money.ZERO));
            lblStockHint.setText(" ");
            return;
        }

        lblUnitPrice.setText(Money.format(product.unitPrice()));

        int quantity = Math.max(0, parsedQuantity());
        lblLineTotal.setText(Money.format(Money.multiply(product.unitPrice(), quantity)));

        int available = effectiveStock(product);
        if (available <= 0) {
            lblStockHint.setText(product.productName() + " is out of stock.");
            lblStockHint.setForeground(Theme.STATUS_DANGER);
        } else if (quantity > available) {
            lblStockHint.setText("Only " + available + " in stock.");
            lblStockHint.setForeground(Theme.STATUS_DANGER);
        } else {
            lblStockHint.setText(available + " in stock.");
            lblStockHint.setForeground(Theme.TEXT_MUTED);
        }
    }

    private void addLine() {
        ProductRef product = (ProductRef) cmbProduct.getSelectedItem();
        if (product == null) {
            Dialogs.error(this, "Choose a product for this line.");
            return;
        }
        if (indexOfProduct(product.productId()) >= 0) {
            Dialogs.error(this, product.productName()
                    + " is already on this order.\n\n"
                    + "Select that line and use Update line to change its quantity.");
            return;
        }

        int quantity = parsedQuantity();
        try {
            // Checked against the shelf plus whatever this order already
            // holds, which is the real ceiling - see heldByThisOrder.
            OrderService.checkStock(withEffectiveStock(product), quantity);
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            txtQuantity.requestFocusInWindow();
            return;
        }

        lines.add(new OrderItem(product.productId(), product.productName(),
                quantity, product.unitPrice()));
        redrawItems();
        txtQuantity.setText("");
        tblItems.setRowSelectionInterval(lines.size() - 1, lines.size() - 1);
    }

    private void updateLine() {
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            Dialogs.error(this, "Select the line you want to change.");
            return;
        }
        OrderItem line = lines.get(row);
        ProductRef product = productFor(line);
        if (product == null) {
            Dialogs.error(this, "Could not look up " + line.getProductName()
                    + " in the catalogue. Reload the window and try again.");
            return;
        }

        int quantity = parsedQuantity();
        try {
            OrderService.checkStock(withEffectiveStock(product), quantity);
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            txtQuantity.requestFocusInWindow();
            return;
        }

        line.setQuantity(quantity);
        redrawItems();
        tblItems.setRowSelectionInterval(row, row);
    }

    private void removeLine() {
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            Dialogs.error(this, "Select the line you want to remove.");
            return;
        }
        lines.remove(row);
        redrawItems();
        txtQuantity.setText("");
    }

    /** Puts the selected line back into the fields, ready to be changed. */
    private void showSelectedLine() {
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            return;
        }
        OrderItem line = lines.get(row);
        txtQuantity.setText(String.valueOf(line.getQuantity()));

        for (int i = 0; i < cmbProduct.getItemCount(); i++) {
            if (cmbProduct.getItemAt(i).productId() == line.getProductId()) {
                cmbProduct.setSelectedIndex(i);
                break;
            }
        }
        showPickedProduct();
    }

    // =====================================================================
    // Totals and the table
    // =====================================================================

    private void redrawItems() {
        itemsModel.setRowCount(0);
        for (OrderItem line : lines) {
            itemsModel.addRow(new Object[]{
                    line.getProductName(),
                    line.getQuantity(),
                    Money.format(line.getUnitPrice()),
                    Money.format(line.getLineTotal())});
        }
        TableStyler.fitColumns(tblItems);
        refreshTotals();
        showPickedProduct();          // stock hint moves as lines come and go
    }

    /** Every amount on screen comes from the service, never from this class. */
    private void refreshTotals() {
        OrderTotals totals = OrderService.totalsOf(lines);

        lblItemCount.setText(totals.itemCount() == 0
                ? "No items yet - add at least one to save the order."
                : totals.itemCount() + " line(s), " + totals.unitCount() + " unit(s)");
        lblSubtotal.setText(Money.format(totals.subtotal()));
        lblGrandTotal.setText(Money.format(totals.grandTotal()));

        btnSave.setEnabled(totals.itemCount() > 0);
    }

    // =====================================================================
    // Saving
    // =====================================================================

    private void save() {
        ClientRef client = (ClientRef) cmbClient.getSelectedItem();
        if (client == null) {
            Dialogs.error(this, "Choose the client this order is for.");
            return;
        }

        final LocalDate placed;
        final LocalDate wanted;
        try {
            placed = requireDate(dtpOrderDate.getText(), "order date");
            wanted = optionalDate(dtpDeliveryDate.getText(), "delivery date");
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        order.setClientId(client.clientId());
        order.setClientName(client.name());
        // Keep the time of day on an order being edited; a new one is stamped
        // now, so two orders taken on the same day still sort in sequence.
        order.setOrderDate(order.getOrderId() > 0 && order.getOrderDate() != null
                ? placed.atTime(order.getOrderDate().toLocalTime())
                : placed.atTime(LocalTime.now().withNano(0)));
        order.setDeliveryDate(wanted);
        order.setDeliveryAddress(blankToNull(txtDeliveryAddress.getText()));
        order.setNotes(blankToNull(txtNotes.getText()));
        order.setItems(lines);

        try {
            if (order.getOrderId() > 0) {
                service.updateOrder(order);
            } else {
                service.createOrder(order);
            }
            saved = true;
            Dialogs.info(this, "Order " + order.getOrderNo() + " saved.\n\n"
                    + "Total: " + Money.format(order.getTotalAmount()) + "\n"
                    + "Stock has been deducted for every item.");
            dispose();
        } catch (IllegalArgumentException ex) {
            // A rule the user can fix: show exactly what it said.
            Dialogs.error(this, ex.getMessage());
        } catch (Exception ex) {
            // Anything else: one sentence here, the stack trace on the console.
            System.err.println("OrderEditorDialog: could not save the order");
            ex.printStackTrace();
            Dialogs.error(this, "Could not save the order. Nothing was written.\n\n"
                    + reason(ex));
        }
    }

    // =====================================================================
    // Small helpers
    // =====================================================================

    /**
     * What is really available for a line on this order: the catalogue's
     * on-hand count plus the units this order already took off the shelf.
     */
    private int effectiveStock(ProductRef product) {
        return product.quantityOnHand()
                + heldByThisOrder.getOrDefault(product.productId(), 0);
    }

    private ProductRef withEffectiveStock(ProductRef product) {
        return new ProductRef(product.productId(), product.productName(),
                product.unitPrice(), effectiveStock(product));
    }

    /** The catalogue entry behind a line, by id. */
    private ProductRef productFor(OrderItem line) {
        for (int i = 0; i < cmbProduct.getItemCount(); i++) {
            ProductRef candidate = cmbProduct.getItemAt(i);
            if (candidate.productId() == line.getProductId()) {
                return candidate;
            }
        }
        try {
            return service.findProduct(line.getProductId());
        } catch (Exception ex) {
            return null;
        }
    }

    private int indexOfProduct(int productId) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getProductId() == productId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The quantity box as a number, or -1 when it is blank or not a number.
     * A bad value is reported by {@code checkStock}, so this never throws -
     * it is called on every keystroke.
     */
    private int parsedQuantity() {
        try {
            return Integer.parseInt(txtQuantity.getText().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static LocalDate requireDate(String text, String what) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter the " + what + ".");
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter the " + what
                    + " as yyyy-mm-dd, for example " + LocalDate.now() + ".");
        }
    }

    private static LocalDate optionalDate(String text, String what) {
        return text == null || text.isBlank() ? null : requireDate(text, what);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String nullSafe(String text) {
        return text == null ? "" : text;
    }

    /** One readable sentence out of any exception. */
    private static String reason(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    /** A caption above a component, so the picker strip reads like the form. */
    private static JComponent captioned(String caption, JComponent field, int width) {
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(Theme.FONT_SMALL);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(width, Theme.FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(width, Theme.FIELD_HEIGHT));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.add(lblCaption);
        stack.add(Box.createVerticalStrut(4));
        stack.add(field);
        return stack;
    }

    private static JLabel valueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BODY_BOLD);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    private static JLabel mutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_FAINT);
        return lbl;
    }

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    /** A DocumentListener that runs the same action on any change. */
    private record SimpleDocumentListener(Runnable onChange)
            implements javax.swing.event.DocumentListener {

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            onChange.run();
        }
    }
}
