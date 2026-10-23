package com.econest.ui;

import com.econest.ui.components.Buttons;
import com.econest.ui.components.Dialogs;
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

/**
 * The shared template every EcoNest module screen is built from. It lays out
 * the approved mockup for you:
 *
 * <pre>
 *  +--------------------------------------------------------------+
 *  |  Screen title                       [ Search ............. ]  |
 *  +-----------------------------------------+--------------------+
 *  |                                         |  Form area         |
 *  |   JTable (tblData)                      |  (your fields,     |
 *  |                                         |   built by you)    |
 *  |                                         |                    |
 *  +-----------------------------------------+--------------------+
 *  |            [ Add ] [ Update ] [ Delete ] [ Clear ]            |
 *  +--------------------------------------------------------------+
 * </pre>
 *
 * <h2>What a subclass has to do</h2>
 * <ol>
 *   <li>Call {@code super("My screen", new String[]{"Col A", "Col B"})}.</li>
 *   <li>Implement {@link #buildForm(JPanel)} - add your fields to the panel.</li>
 *   <li>Implement {@link #loadTableData()} - fill the table from your DAO
 *       or service.</li>
 *   <li>Override {@link #onAdd()}, {@link #onUpdate()}, {@link #onDelete()},
 *       {@link #onClear()} and {@link #onSearch(String)} as needed. Each one
 *       has a harmless default, so you can implement them one at a time.</li>
 *   <li>Call {@link #initPanel()} at the end of your constructor, once your
 *       own fields are created. (The template cannot do this for you: your
 *       fields do not exist yet while {@code super(...)} is still running.)</li>
 * </ol>
 *
 * <h2>What you get for free</h2>
 * <ul>
 *   <li>A styled, striped {@link JTable} in {@link #tblData} with the model
 *       {@link #tableModel} - use {@link #setRows(Object[][])} to refill it.</li>
 *   <li>Add / Update / Delete / Clear buttons already wired to the hooks,
 *       with a confirm dialog in front of Delete.</li>
 *   <li>A search box that calls {@link #onSearch(String)} as the user types.</li>
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

    /** Row holding the action buttons; add extra buttons to it if you need more. */
    protected final JPanel pnlActions;

    private final String moduleTitle;

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

        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        // --- Top: title + search -----------------------------------------
        lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);

        txtSearch = new JTextField();
        txtSearch.setFont(Theme.FONT_BODY);
        txtSearch.setPreferredSize(new Dimension(240, Theme.FIELD_HEIGHT));
        txtSearch.putClientProperty("JTextField.placeholderText", "Search...");
        txtSearch.setToolTipText("Search this screen");

        JLabel lblSearch = new JLabel("Search");
        lblSearch.setFont(Theme.FONT_BODY);
        lblSearch.setForeground(Theme.TEXT_MUTED);

        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlSearch.setOpaque(false);
        pnlSearch.add(lblSearch);
        pnlSearch.add(txtSearch);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP, 0));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(pnlSearch, BorderLayout.EAST);

        // --- Centre: the table -------------------------------------------
        // Cells are not editable in place: edits go through the form.
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
        scrTable.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scrTable.getViewport().setBackground(Theme.CONTENT_BG);
        scrTable.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // --- Right: the form area ----------------------------------------
        pnlForm = new JPanel();
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
        pnlForm.setBackground(Theme.PANEL_BG);
        pnlForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(
                        Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP)));

        JScrollPane scrForm = new JScrollPane(pnlForm);
        scrForm.setBorder(BorderFactory.createEmptyBorder());
        scrForm.getVerticalScrollBar().setUnitIncrement(16);
        scrForm.setPreferredSize(new Dimension(320, 0));

        JPanel pnlBody = new JPanel(new BorderLayout(Theme.GAP, 0));
        pnlBody.setOpaque(false);
        pnlBody.add(scrTable, BorderLayout.CENTER);
        pnlBody.add(scrForm, BorderLayout.EAST);

        // --- Bottom: the action buttons ------------------------------------
        btnAdd = Buttons.primary("Add");
        btnUpdate = Buttons.secondary("Update");
        btnDelete = Buttons.danger("Delete");
        btnClear = Buttons.secondary("Clear");

        pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlActions.setOpaque(false);
        pnlActions.setBorder(BorderFactory.createEmptyBorder(Theme.GAP, 0, 0, 0));
        pnlActions.add(btnAdd);
        pnlActions.add(btnUpdate);
        pnlActions.add(btnDelete);
        pnlActions.add(btnClear);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlBody, BorderLayout.CENTER);
        add(pnlActions, BorderLayout.SOUTH);

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

        // Live search: fires on every keystroke in the search box.
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

    /**
     * Fill the table from your DAO or service. Clear it first with
     * {@link #setRows(Object[][])} or {@code tableModel.setRowCount(0)}.
     */
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
        // Default: no filtering. Override this to re-query your DAO, or to
        // filter the rows you already hold in memory.
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
     * the panel is not inside a {@link MainFrame} (in a unit test, say).
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
