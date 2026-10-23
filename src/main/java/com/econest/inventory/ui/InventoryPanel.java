package com.econest.inventory.ui;

import com.econest.ui.BaseModulePanel;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.InputValidator;
import com.econest.ui.components.TableStyler;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stock / Inventory screen - and the <b>reference example</b> for the whole
 * team. Every other module screen should look like this one: extend
 * {@link BaseModulePanel}, declare your fields, fill in the five hooks.
 *
 * <p>The rows below are held in memory so the screen runs without a database.
 * Each place that will later talk to {@code InventoryService} is marked
 * {@code TODO (DAO)} - swapping those four spots over is the only change
 * needed to make the screen live.</p>
 *
 * <p>Read {@code docs/UI_GUIDE.md} alongside this file.</p>
 */
public class InventoryPanel extends BaseModulePanel {

    // --- Table layout -----------------------------------------------------
    private static final String[] COLUMNS = {
            "Code", "Product", "Quantity", "Reorder level", "Status"
    };

    /** Index of the column the status renderer colours. */
    private static final int COL_STATUS = 4;

    private static final String[] LOCATIONS = {"Main store", "Back room", "Nursery"};

    // --- Form fields (txt / cmb prefixes, see the naming convention) -------
    private JTextField txtCode;
    private JTextField txtProduct;
    private JTextField txtQuantity;
    private JTextField txtReorderLevel;
    private JComboBox<String> cmbLocation;

    /** Validation helper shared by the Add and Update handlers. */
    private final InputValidator validator = new InputValidator();

    /** In-memory stand-in for the database. TODO (DAO): drop once wired up. */
    private final List<Object[]> rows = new ArrayList<>();

    public InventoryPanel() {
        super("Stock / Inventory", COLUMNS);

        // Colour the Status column with the shared status colours.
        TableStyler.colourStatusColumn(tblData, COL_STATUS);

        // Must be the last line: the base class builds the form and loads the
        // table here, and both need the fields created above to exist.
        initPanel();
    }

    // ---------------------------------------------------------------------
    // Hook 1: the form
    // ---------------------------------------------------------------------

    @Override
    protected void buildForm(JPanel form) {
        txtCode = FormFields.textField();
        txtProduct = FormFields.textField();
        txtQuantity = FormFields.textField();
        txtReorderLevel = FormFields.textField();
        cmbLocation = FormFields.comboBox(LOCATIONS);

        form.add(FormFields.heading("Stock item"));
        form.add(Box.createVerticalStrut(12));
        form.add(FormFields.row("Code", txtCode));
        form.add(FormFields.row("Product", txtProduct));
        form.add(FormFields.row("Quantity on hand", txtQuantity));
        form.add(FormFields.row("Reorder level", txtReorderLevel));
        form.add(FormFields.row("Location", cmbLocation));
    }

    // ---------------------------------------------------------------------
    // Hook 2: loading the table
    // ---------------------------------------------------------------------

    @Override
    protected void loadTableData() {
        if (rows.isEmpty()) {
            seedSampleRows();
        }
        // TODO (DAO): replace the two lines above with
        //   rows = inventoryService.findAllStockItems() mapped to Object[]
        setRows(rows.toArray(new Object[0][]));
        setStatus(rows.size() + " stock items loaded.");
    }

    // ---------------------------------------------------------------------
    // Hook 3: add
    // ---------------------------------------------------------------------

    @Override
    protected void onAdd() {
        Object[] row = readForm();
        if (row == null) {
            setStatus("Please fix the highlighted fields.");
            return;
        }
        if (findRowByCode((String) row[0]) >= 0) {
            validator.fail(txtCode, "That code is already in use");
            setStatus("Duplicate stock code.");
            return;
        }

        rows.add(row);          // TODO (DAO): inventoryService.save(stockItem)
        loadTableData();
        onClear();
        setStatus("Added stock item " + row[0] + ".");
    }

    // ---------------------------------------------------------------------
    // Hook 4: update
    // ---------------------------------------------------------------------

    @Override
    protected void onUpdate() {
        // Resolve through the code rather than the row index: while a search
        // filter is active, the table shows a subset and the indexes differ.
        int selected = selectedSourceRow();
        if (selected < 0) {
            setStatus("Select the row you want to update.");
            return;
        }
        Object[] row = readForm();
        if (row == null) {
            setStatus("Please fix the highlighted fields.");
            return;
        }

        rows.set(selected, row); // TODO (DAO): inventoryService.save(stockItem)
        loadTableData();
        setStatus("Updated stock item " + row[0] + ".");
    }

    // ---------------------------------------------------------------------
    // Hook 5: delete
    //
    // The base class has already shown the confirm dialog by the time this
    // runs, so there is nothing to ask here.
    // ---------------------------------------------------------------------

    @Override
    protected void onDelete() {
        int selected = selectedSourceRow();
        if (selected < 0) {
            setStatus("Select the row you want to delete.");
            return;
        }

        Object[] removed = rows.remove(selected); // TODO (DAO): service.delete(id)
        loadTableData();
        onClear();
        setStatus("Deleted stock item " + removed[0] + ".");
    }

    // ---------------------------------------------------------------------
    // Clear and search
    // ---------------------------------------------------------------------

    @Override
    protected void onClear() {
        txtCode.setText("");
        txtProduct.setText("");
        txtQuantity.setText("");
        txtReorderLevel.setText("");
        cmbLocation.setSelectedIndex(0);
        validator.clearAll(txtCode, txtProduct, txtQuantity, txtReorderLevel, cmbLocation);
        super.onClear();        // drops the table selection and the search box
    }

    @Override
    protected void onSearch(String query) {
        if (query.isEmpty()) {
            setRows(rows.toArray(new Object[0][]));
            setStatus(rows.size() + " stock items.");
            return;
        }
        String needle = query.toLowerCase(Locale.ROOT);

        // TODO (DAO): for a large table, search in SQL instead of in memory.
        List<Object[]> matches = new ArrayList<>();
        for (Object[] row : rows) {
            String code = String.valueOf(row[0]).toLowerCase(Locale.ROOT);
            String product = String.valueOf(row[1]).toLowerCase(Locale.ROOT);
            if (code.contains(needle) || product.contains(needle)) {
                matches.add(row);
            }
        }
        setRows(matches.toArray(new Object[0][]));
        setStatus(matches.size() + " of " + rows.size() + " stock items match \"" + query + "\".");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Validates the form and turns it into a table row.
     *
     * @return the row, or null when a field failed validation (the field
     *         itself already shows the reason)
     */
    private Object[] readForm() {
        validator.reset();
        String code = validator.required(txtCode, "Code");
        String product = validator.required(txtProduct, "Product");
        Integer quantity = validator.integer(txtQuantity, "Quantity");
        Integer reorderLevel = validator.integer(txtReorderLevel, "Reorder level");
        validator.requiredSelection(cmbLocation, "Location");

        if (!validator.isValid()) {
            return null;
        }
        return new Object[]{
                code, product, quantity, reorderLevel, stockStatus(quantity, reorderLevel)
        };
    }

    /** The word the Status column shows; TableStyler turns it into a colour. */
    private static String stockStatus(int quantity, int reorderLevel) {
        if (quantity <= 0) {
            return "Out of stock";
        }
        return quantity <= reorderLevel ? "Low stock" : "In stock";
    }

    /**
     * Index into {@link #rows} of the record the user selected in the table.
     * Goes through the Code column so it stays correct even when a search
     * filter means the table is showing only some of the rows.
     *
     * @return the index, or -1 when nothing is selected
     */
    private int selectedSourceRow() {
        Object code = selectedValue(0);
        return code == null ? -1 : findRowByCode(code.toString());
    }

    /** Index of the row with this code, or -1 when there is none. */
    private int findRowByCode(String code) {
        for (int i = 0; i < rows.size(); i++) {
            if (String.valueOf(rows.get(i)[0]).equalsIgnoreCase(code)) {
                return i;
            }
        }
        return -1;
    }

    /** A few rows so the screen is not empty before the DAO is connected. */
    private void seedSampleRows() {
        rows.add(new Object[]{"SKU-1001", "Areca Palm (medium)", 42, 10, stockStatus(42, 10)});
        rows.add(new Object[]{"SKU-1002", "Terracotta planter 10in", 8, 12, stockStatus(8, 12)});
        rows.add(new Object[]{"SKU-1003", "Herb gardening kit", 0, 5, stockStatus(0, 5)});
        rows.add(new Object[]{"SKU-1004", "Coco peat block 5kg", 120, 25, stockStatus(120, 25)});
    }

    /** Kept so a teammate can see how to raise an error from a panel. */
    @SuppressWarnings("unused")
    private void reportFailure(Exception ex) {
        Dialogs.error(this, "Could not reach the stock database:\n" + ex.getMessage());
        setStatus("Stock operation failed.");
    }
}
