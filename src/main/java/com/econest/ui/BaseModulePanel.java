package com.econest.ui;

import com.econest.ui.components.Buttons;
import com.econest.ui.components.Card;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.EmptyState;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * The shared template every EcoNest module screen is built from.
 *
 * <pre>
 *  +--------------------------------------------------------------+
 *  |  Screen title                      [ (Q) Search ......  (x) ] |
 *  +-----------------------------------------+--------------------+
 *  |  +-----------------------------------+  |  +--------------+  |
 *  |  |  table, in a white rounded card   |  |  | form card    |  |
 *  |  |                                   |  |  |  fields...   |  |
 *  |  |                                   |  |  |  [Add][Upd]  |  |
 *  |  +-----------------------------------+  |  +--------------+  |
 *  +-----------------------------------------+--------------------+
 * </pre>
 *
 * <h2>What a subclass has to do</h2>
 * <ol>
 *   <li>Call {@code super("My screen", new String[]{"Col A", "Col B"})}.</li>
 *   <li>Implement {@link #buildForm(JPanel)} - add your fields to the panel.</li>
 *   <li>Implement {@link #loadTableData()} - fill the table from your DAO
 *       or service.</li>
 *   <li>Override {@link #onAdd()}, {@link #onUpdate()}, {@link #onDelete()},
 *       {@link #onClear()} and {@link #onSearch(String)} as needed.</li>
 *   <li>Call {@link #initPanel()} at the end of your constructor.</li>
 * </ol>
 *
 * <h2>What you get for free</h2>
 * <ul>
 *   <li>A styled table in {@link #tblData}, inside a rounded card, with an
 *       empty state when it has no rows.</li>
 *   <li>Add / Update / Delete / Clear directly under the form fields, with a
 *       confirm dialog in front of Delete.</li>
 *   <li>A search box with a magnifier and a clear button, calling
 *       {@link #onSearch(String)} as the user types.</li>
 *   <li>{@link #setStatus(String)}, which writes to the window's status bar.</li>
 * </ul>
 *
 * See {@code docs/UI_GUIDE.md} for a worked example.
 */
public abstract class BaseModulePanel extends JPanel {

    // ---------------------------------------------------------------------
    // Components a subclass may use directly
    // ---------------------------------------------------------------------

    /** Screen title shown top-left. */
    protected final JLabel lblTitle;

    /** Search box top-right; typing in it calls {@link #onSearch(String)}. */
    protected final JTextField txtSearch;

    /** The main table. Already styled by {@code TableStyler}. */
    protected final JTable tblData;

    /** Model behind {@link #tblData}. Cells are read-only to the user. */
    protected final DefaultTableModel tableModel;

    /** The right-hand form area. Fill it in {@link #buildForm(JPanel)}. */
    protected final JPanel pnlForm;

    /** Standard action buttons. */
    protected final JButton btnAdd;
    protected final JButton btnUpdate;
    protected final JButton btnDelete;
    protected final JButton btnClear;

    /** Row holding the action buttons, inside the form card. */
    protected final JPanel pnlActions;

    private final String moduleTitle;
    private final EmptyState.Host tableHost;

    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    /**
     * @param title       the screen title, e.g. "Stock / Inventory"
     * @param columnNames the table's column headers, left to right
     */
    protected BaseModulePanel(String title, String[] columnNames) {
        super(new BorderLayout(Theme.GAP, Theme.GAP));
        this.moduleTitle = title;

        setBackground(Theme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        // --- Top: title + search -----------------------------------------
        lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        txtSearch = new JTextField();
        txtSearch.setToolTipText("Search this screen");
        JPanel pnlSearch = FormFields.searchBox(txtSearch);

        JPanel pnlSearchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlSearchWrap.setOpaque(false);
        pnlSearchWrap.add(pnlSearch);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP_SMALL, 0));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(pnlSearchWrap, BorderLayout.EAST);

        // --- Centre: the table, in a card --------------------------------
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblData = new JTable(tableModel);
        tblData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.style(tblData);

        JScrollPane scrTable = new JScrollPane(tblData);
        scrTable.setBorder(BorderFactory.createEmptyBorder());
        scrTable.setOpaque(false);
        scrTable.getViewport().setBackground(Theme.CARD_BG);
        scrTable.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableHost = EmptyState.wrap(scrTable, tblData, "No items found",
                "Nothing matches the current search.");

        Card cardTable = new Card(new BorderLayout());
        cardTable.add(tableHost, BorderLayout.CENTER);

        // --- Right: the form card ----------------------------------------
        pnlForm = new JPanel();
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
        pnlForm.setOpaque(false);
        pnlForm.setAlignmentX(LEFT_ALIGNMENT);

        btnAdd = Buttons.primary("Add");
        btnUpdate = Buttons.secondary("Update");
        btnDelete = Buttons.danger("Delete");
        btnClear = Buttons.text("Clear");

        // The actions live inside the form card, directly under the fields,
        // because that is what they act on.
        // A grid, not a row: the form card is narrow, and four buttons laid
        // out in a single FlowLayout line simply fall off the right edge.
        // Two per row wraps cleanly however many a screen adds.
        pnlActions = new JPanel(new GridLayout(0, 2, Theme.GAP_SMALL, Theme.GAP_SMALL));
        pnlActions.setOpaque(false);
        pnlActions.setAlignmentX(LEFT_ALIGNMENT);
        pnlActions.setBorder(BorderFactory.createEmptyBorder(Theme.GAP, 0, 0, 0));
        pnlActions.add(btnAdd);
        pnlActions.add(btnUpdate);
        pnlActions.add(btnDelete);
        pnlActions.add(btnClear);

        JPanel pnlFormBody = new JPanel();
        pnlFormBody.setLayout(new BoxLayout(pnlFormBody, BoxLayout.Y_AXIS));
        pnlFormBody.setOpaque(false);
        pnlFormBody.add(pnlForm);

        JScrollPane scrForm = new JScrollPane(pnlFormBody);
        scrForm.setBorder(BorderFactory.createEmptyBorder());
        scrForm.setOpaque(false);
        scrForm.getViewport().setOpaque(false);
        scrForm.getVerticalScrollBar().setUnitIncrement(16);
        scrForm.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // The actions sit at the foot of the card rather than in the scrolling
        // area, so a long form can never push Add out of sight.
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.add(Card.separator(), BorderLayout.NORTH);
        pnlFooter.add(pnlActions, BorderLayout.CENTER);

        Card cardForm = new Card(new BorderLayout()).withPadding();
        cardForm.add(scrForm, BorderLayout.CENTER);
        cardForm.add(pnlFooter, BorderLayout.SOUTH);
        cardForm.setPreferredSize(new Dimension(340, 0));
        cardForm.setMinimumSize(new Dimension(300, 0));

        JPanel pnlBody = new JPanel(new BorderLayout(Theme.GAP_SMALL, 0));
        pnlBody.setOpaque(false);
        pnlBody.add(cardTable, BorderLayout.CENTER);
        pnlBody.add(cardForm, BorderLayout.EAST);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlBody, BorderLayout.CENTER);

        wireActions();
    }

    /** Connects the standard buttons and the search box to the hook methods. */
    private void wireActions() {
        btnAdd.addActionListener(e -> onAdd());
        btnUpdate.addActionListener(e -> onUpdate());
        btnClear.addActionListener(e -> onClear());

        // Delete always asks first, so a subclass only ever sees a confirmed
        // delete and never has to remember to put the dialog in itself.
        btnDelete.addActionListener(e -> {
            if (tblData.getSelectedRow() < 0) {
                setStatus("Select a row first.");
                return;
            }
            if (Dialogs.confirmDelete(this, "the selected record")) {
                onDelete();
            }
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fire();
            }

            private void fire() {
                onSearch(txtSearch.getText().trim());
            }
        });
    }

    /**
     * Builds the form and loads the first batch of rows. Call this as the
     * <b>last line of your constructor</b>, once your own fields exist.
     */
    protected final void initPanel() {
        buildForm(pnlForm);
        pnlForm.add(Box.createVerticalGlue());
        loadTableData();
    }

    // ---------------------------------------------------------------------
    // Hooks: what a subclass fills in
    // ---------------------------------------------------------------------

    /**
     * Add your input fields to {@code form}. It already uses a vertical
     * BoxLayout, so {@code form.add(FormFields.row("Name", txtName))} is all
     * you need per field.
     */
    protected abstract void buildForm(JPanel form);

    /** Fill the table from your DAO or service. */
    protected abstract void loadTableData();

    /** Called when Add is pressed. Validate, save, reload, clear the form. */
    protected void onAdd() {
        setStatus("Add is not implemented for " + moduleTitle + " yet.");
    }

    /** Called when Update is pressed. Usually acts on the selected row. */
    protected void onUpdate() {
        setStatus("Update is not implemented for " + moduleTitle + " yet.");
    }

    /** Called when Delete is pressed <b>and</b> the user confirmed. */
    protected void onDelete() {
        setStatus("Delete is not implemented for " + moduleTitle + " yet.");
    }

    /** Called when Clear is pressed. Empty your fields and drop the selection. */
    protected void onClear() {
        tblData.clearSelection();
        txtSearch.setText("");
        setStatus("Cleared.");
    }

    /**
     * Called on every keystroke in the search box.
     *
     * @param query the trimmed text, empty when the box was cleared
     */
    protected void onSearch(String query) {
        // Default: no filtering.
    }

    // ---------------------------------------------------------------------
    // Small conveniences for subclasses
    // ---------------------------------------------------------------------

    /** Replaces every row in the table in one go, then re-fits the columns. */
    protected void setRows(Object[][] rows) {
        tableModel.setRowCount(0);
        for (Object[] row : rows) {
            tableModel.addRow(row);
        }
        TableStyler.fitColumns(tblData);
    }

    /**
     * Changes the message shown when the table is empty, so a screen can say
     * something more useful than the default.
     */
    protected void setEmptyState(String message, String hint) {
        tableHost.setMessage(message, hint);
    }

    /** The selected row index in <b>model</b> coordinates, or -1 if none. */
    protected int selectedModelRow() {
        int viewRow = tblData.getSelectedRow();
        return viewRow < 0 ? -1 : tblData.convertRowIndexToModel(viewRow);
    }

    /** A cell value from the selected row, or null if nothing is selected. */
    protected Object selectedValue(int columnIndex) {
        int row = selectedModelRow();
        return row < 0 ? null : tableModel.getValueAt(row, columnIndex);
    }

    /**
     * Shows a short message in the window's status bar. Safe to call even when
     * the panel is not inside a {@link MainFrame}.
     */
    protected void setStatus(String message) {
        MainFrame frame = (MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, this);
        if (frame != null) {
            frame.setStatus(message);
        }
    }

    /** The screen title this panel was created with. */
    public String getModuleTitle() {
        return moduleTitle;
    }
}
