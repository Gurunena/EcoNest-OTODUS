package com.econest.delivery.ui;

import com.econest.delivery.model.AvailableAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.PendingOrder;
import com.econest.delivery.service.DeliveryAssignmentService;
import com.econest.ui.MainFrame;
import com.econest.ui.Theme;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Delivery Assignment screen.
 *
 * <p>Top table: orders that are PENDING or CONFIRMED with no agent yet.
 * Pick one, choose an agent from the combo box and press Assign - the
 * assignment is written, the order moves to the dispatched status and the
 * agent is marked busy, all in one transaction.</p>
 *
 * <p>Bottom table: every current assignment. Select one to reassign it to a
 * different agent, or unassign it and put the order back in the queue.</p>
 *
 * <p>Not built on {@code BaseModulePanel}: this screen needs two tables
 * side by side rather than one table and a form, so the template's single
 * table would be in the way. It uses the same {@link Theme} and helpers, so
 * it matches the rest of the app.</p>
 *
 * <p><b>On "out for delivery":</b> {@code orders.status} has no such value.
 * The module uses DISPATCHED, which is the existing status for that state -
 * see {@code DeliveryAssignmentDaoImpl.STATUS_OUT_FOR_DELIVERY}.</p>
 */
public class DeliveryAssignmentPanel extends JPanel {

    private static final String[] COLS_PENDING =
            {"Order", "Client", "Ordered", "Total", "Status", "Address"};

    private static final String[] COLS_ASSIGNMENTS =
            {"Order", "Client", "Agent", "Assigned", "Status"};

    private static final int COL_PENDING_ID = 0;
    private static final int COL_PENDING_STATUS = 4;
    private static final int COL_ASSIGNMENT_ID = 0;
    private static final int COL_ASSIGNMENT_STATUS = 4;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.ENGLISH);

    // --- Controls (tbl / cmb / btn / lbl prefixes) ------------------------
    private final DefaultTableModel pendingModel = readOnlyModel(COLS_PENDING);
    private final DefaultTableModel assignmentModel = readOnlyModel(COLS_ASSIGNMENTS);

    private final JTable tblPending = new JTable(pendingModel);
    private final JTable tblAssignments = new JTable(assignmentModel);

    private final JComboBox<AvailableAgent> cmbAgent = FormFields.comboBox(new AvailableAgent[0]);

    private final JButton btnAssign = Buttons.primary("Assign");
    private final JButton btnReassign = Buttons.secondary("Reassign");
    private final JButton btnUnassign = Buttons.danger("Unassign");
    private final JButton btnRefresh = Buttons.secondary("Refresh");

    private final JLabel lblAgentCount = new JLabel();

    private final DeliveryAssignmentService service;

    /** Rows currently on screen, so a selection maps back to a record. */
    private final List<PendingOrder> pendingOrders = new ArrayList<>();
    private final List<DeliveryAssignment> assignments = new ArrayList<>();

    /**
     * True until the first load finishes. Every page is constructed when the
     * window opens, so a database that is down would otherwise stack modal
     * error dialogs over the dashboard before the user has touched anything.
     * The first failure goes to the status bar instead; anything the user
     * actually asked for still gets a dialog.
     */
    private boolean initialLoad = true;

    public DeliveryAssignmentPanel() {
        this(new DeliveryAssignmentService());
    }

    /** Injectable constructor so a test can supply a fake service. */
    public DeliveryAssignmentPanel(DeliveryAssignmentService service) {
        super(new BorderLayout(Theme.GAP, Theme.GAP));
        this.service = service;

        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        tblPending.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAssignments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.style(tblPending);
        TableStyler.style(tblAssignments);
        TableStyler.colourStatusColumn(tblPending, COL_PENDING_STATUS);
        TableStyler.colourStatusColumn(tblAssignments, COL_ASSIGNMENT_STATUS);

        btnAssign.addActionListener(e -> assign());
        btnReassign.addActionListener(e -> reassign());
        btnUnassign.addActionListener(e -> unassign());
        btnRefresh.addActionListener(e -> reload());

        lblAgentCount.setFont(Theme.FONT_SMALL);
        lblAgentCount.setForeground(Theme.TEXT_MUTED);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        reload();
    }

    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    private JPanel buildHeader() {
        JLabel lblTitle = new JLabel("Delivery Assignment");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlRight.setOpaque(false);
        pnlRight.add(btnRefresh);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP, 0));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(pnlRight, BorderLayout.EAST);
        return pnlHeader;
    }

    private JPanel buildBody() {
        JPanel pnlTables = new JPanel(new GridLayout(2, 1, 0, Theme.GAP));
        pnlTables.setOpaque(false);
        pnlTables.add(tableSection("Orders waiting for an agent", tblPending, buildAssignBar()));
        pnlTables.add(tableSection("Current assignments", tblAssignments, buildManageBar()));
        return pnlTables;
    }

    /** Agent picker plus the Assign button, under the pending table. */
    private JPanel buildAssignBar() {
        JLabel lblAgent = new JLabel("Agent");
        lblAgent.setFont(Theme.FONT_BODY);
        lblAgent.setForeground(Theme.TEXT_MUTED);

        cmbAgent.setPreferredSize(new Dimension(220, Theme.FIELD_HEIGHT));

        JPanel pnlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlBar.setOpaque(false);
        pnlBar.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_SMALL, 0, 0, 0));
        pnlBar.add(lblAgentCount);
        pnlBar.add(Box.createHorizontalStrut(Theme.GAP));
        pnlBar.add(lblAgent);
        pnlBar.add(cmbAgent);
        pnlBar.add(btnAssign);
        return pnlBar;
    }

    /** Reassign / Unassign, under the assignments table. */
    private JPanel buildManageBar() {
        JPanel pnlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlBar.setOpaque(false);
        pnlBar.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_SMALL, 0, 0, 0));
        pnlBar.add(btnReassign);
        pnlBar.add(btnUnassign);
        return pnlBar;
    }

    private JPanel tableSection(String title, JTable tbl, JPanel actionBar) {
        JLabel lblHeading = new JLabel(title);
        lblHeading.setFont(Theme.FONT_HEADING);
        lblHeading.setForeground(Theme.SIDEBAR_BG);
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP_SMALL, 0));

        JScrollPane scrTable = new JScrollPane(tbl);
        scrTable.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scrTable.getViewport().setBackground(Theme.CONTENT_BG);

        JPanel pnlSection = new JPanel(new BorderLayout());
        pnlSection.setOpaque(false);
        pnlSection.add(lblHeading, BorderLayout.NORTH);
        pnlSection.add(scrTable, BorderLayout.CENTER);
        pnlSection.add(actionBar, BorderLayout.SOUTH);
        return pnlSection;
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    /** Reloads both tables and the agent list off the UI thread. */
    private void reload() {
        setBusy(true);
        setStatus("Loading deliveries...");

        new SwingWorker<Snapshot, Void>() {
            @Override
            protected Snapshot doInBackground() throws Exception {
                return new Snapshot(
                        service.findUnassignedOrders(),
                        service.findAvailableAgents(),
                        service.findAllAssignments());
            }

            @Override
            protected void done() {
                setBusy(false);
                boolean quiet = initialLoad;
                initialLoad = false;
                try {
                    show(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    if (quiet) {
                        quietFail(ex.getCause());
                    } else {
                        fail("load the delivery lists", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void show(Snapshot snapshot) {
        pendingOrders.clear();
        pendingOrders.addAll(snapshot.pending());
        pendingModel.setRowCount(0);
        for (PendingOrder order : snapshot.pending()) {
            pendingModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getClientName(),
                    stamp(order.getOrderDate()),
                    money(order.getTotalAmount()),
                    titleCase(order.getStatus()),
                    order.getDeliveryAddress()});
        }

        assignments.clear();
        assignments.addAll(snapshot.assignments());
        assignmentModel.setRowCount(0);
        for (DeliveryAssignment assignment : snapshot.assignments()) {
            assignmentModel.addRow(new Object[]{
                    assignment.getOrderId(),
                    assignment.getClientName(),
                    assignment.getAgentName(),
                    stamp(assignment.getAssignedAt()),
                    titleCase(assignment.getStatus().name())});
        }

        cmbAgent.removeAllItems();
        for (AvailableAgent agent : snapshot.agents()) {
            cmbAgent.addItem(agent);
        }

        TableStyler.fitColumns(tblPending);
        TableStyler.fitColumns(tblAssignments);

        int free = snapshot.agents().size();
        lblAgentCount.setText(free == 0
                ? "No agents free"
                : free + " agent" + (free == 1 ? "" : "s") + " free");
        btnAssign.setEnabled(free > 0);

        setStatus(snapshot.pending().size() + " order(s) waiting, "
                + snapshot.assignments().size() + " assignment(s), "
                + free + " agent(s) available.");
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    private void assign() {
        PendingOrder order = selectedPendingOrder();
        if (order == null) {
            Dialogs.error(this, "Select an order from the waiting list first.");
            return;
        }
        AvailableAgent agent = (AvailableAgent) cmbAgent.getSelectedItem();
        if (agent == null) {
            Dialogs.error(this, "Choose a delivery agent. "
                    + "If the list is empty, every agent is currently busy.");
            return;
        }

        final int orderId = order.getOrderId();
        final int agentId = agent.getAgentId();
        runWrite("assign order " + orderId + " to " + agent.getName(),
                () -> service.assign(orderId, agentId),
                "Order " + orderId + " assigned to " + agent.getName() + ".");
    }

    private void reassign() {
        DeliveryAssignment assignment = selectedAssignment();
        if (assignment == null) {
            Dialogs.error(this, "Select an assignment to move to another agent.");
            return;
        }
        AvailableAgent agent = (AvailableAgent) cmbAgent.getSelectedItem();
        if (agent == null) {
            Dialogs.error(this, "Choose the agent to move it to. "
                    + "If the list is empty, every other agent is currently busy.");
            return;
        }
        if (agent.getAgentId() == assignment.getAgentId()) {
            Dialogs.error(this, "Order " + assignment.getOrderId()
                    + " is already assigned to " + agent.getName() + ".");
            return;
        }

        if (!Dialogs.confirm(this, "Move order " + assignment.getOrderId()
                + " from " + assignment.getAgentName() + " to " + agent.getName() + "?")) {
            return;
        }

        final int orderId = assignment.getOrderId();
        final int agentId = agent.getAgentId();
        runWrite("reassign order " + orderId,
                () -> service.reassign(orderId, agentId),
                "Order " + orderId + " moved to " + agent.getName() + ".");
    }

    private void unassign() {
        DeliveryAssignment assignment = selectedAssignment();
        if (assignment == null) {
            Dialogs.error(this, "Select an assignment to remove.");
            return;
        }
        if (!Dialogs.confirmDelete(this, "the assignment of order "
                + assignment.getOrderId() + " to " + assignment.getAgentName())) {
            return;
        }

        final int orderId = assignment.getOrderId();
        runWrite("unassign order " + orderId,
                () -> service.unassign(orderId),
                "Order " + orderId + " is back in the waiting list.");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Everything the screen shows, fetched in one background pass. */
    private record Snapshot(List<PendingOrder> pending,
                            List<AvailableAgent> agents,
                            List<DeliveryAssignment> assignments) {
    }

    /** Something a service call can throw. */
    @FunctionalInterface
    private interface DbCall {
        void run() throws SQLException;
    }

    /** Runs a write off the UI thread, then reloads both tables. */
    private void runWrite(String action, DbCall call, String successMessage) {
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
                    setStatus(successMessage);
                    reload();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    private PendingOrder selectedPendingOrder() {
        int row = tblPending.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int orderId = ((Number) pendingModel.getValueAt(
                tblPending.convertRowIndexToModel(row), COL_PENDING_ID)).intValue();
        return pendingOrders.stream()
                .filter(order -> order.getOrderId() == orderId)
                .findFirst()
                .orElse(null);
    }

    private DeliveryAssignment selectedAssignment() {
        int row = tblAssignments.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int orderId = ((Number) assignmentModel.getValueAt(
                tblAssignments.convertRowIndexToModel(row), COL_ASSIGNMENT_ID)).intValue();
        return assignments.stream()
                .filter(assignment -> assignment.getOrderId() == orderId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Turns a failure into one clear sentence. A validation message from the
     * service is already user-ready; anything else gets a lead-in and the
     * detail. The stack trace goes to the console only.
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
        System.err.println("DeliveryAssignmentPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Delivery operation failed.");
    }

    /**
     * Reports a failure the user did not ask for - the load that happens when
     * the window first opens - in the status bar rather than a pop-up.
     */
    private void quietFail(Throwable cause) {
        System.err.println("DeliveryAssignmentPanel: could not load the delivery lists - " + cause);
        setStatus("Could not load the delivery lists. Check db.properties and the MySQL "
                + "server, then press Refresh.");
    }

    private void setBusy(boolean busy) {
        btnAssign.setEnabled(!busy && cmbAgent.getItemCount() > 0);
        btnReassign.setEnabled(!busy);
        btnUnassign.setEnabled(!busy);
        btnRefresh.setEnabled(!busy);
        cmbAgent.setEnabled(!busy);
    }

    private void setStatus(String message) {
        MainFrame frame = (MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, this);
        if (frame != null) {
            frame.setStatus(message);
        }
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String stamp(LocalDateTime when) {
        return when == null ? "" : when.format(STAMP);
    }

    private static String money(BigDecimal amount) {
        return "Rs. " + MONEY.format(amount == null ? BigDecimal.ZERO : amount);
    }

    /** "IN_TRANSIT" -> "In transit", so TableStyler can colour it. */
    private static String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String spaced = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
