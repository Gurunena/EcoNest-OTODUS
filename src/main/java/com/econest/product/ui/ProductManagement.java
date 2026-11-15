package com.econest.product.ui;

import com.econest.product.model.Product;
import com.econest.product.service.ProductService;
import com.econest.ui.BaseModulePanel;
import com.econest.ui.Theme;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.InputValidator;
import com.econest.ui.components.TableStyler;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Task 1 screen: add, update, remove and search plants, planters and
 * gardening kits.
 *
 * Built on the shared BaseModulePanel so it looks like every other screen:
 * the product list on the left, the selected product's details on the right.
 * Database calls run on a SwingWorker so the window never freezes.
 */
public class ProductManagement extends BaseModulePanel {

    private static final String[] COLUMNS = {"ID", "Name", "Category", "Price", "Eco rating", "In stock"};
    private static final int COL_PRICE = 3;
    private static final int COL_ECO = 4;
    private static final int COL_STOCK = 5;

    /** What the user sees in the Category box, in the same order as ProductService.CATEGORIES. */
    private static final String[] CATEGORY_LABELS = {"Plant", "Planter", "Gardening kit"};

    /** What the user sees in the Eco rating box; the rating is the position + 1. */
    private static final String[] ECO_LABELS = {
            "1 - Low", "2 - Fair", "3 - Good", "4 - Very good", "5 - Excellent"};

    // --- form fields ------------------------------------------------------
    private JTextField txtName;
    private JComboBox<String> cmbCategory;
    private JTextField txtPrice;
    private JComboBox<String> cmbEcoRating;
    private JTextField txtQuantity;
    private JTextField txtDescription;

    private final ProductService service = new ProductService();
    private final InputValidator validator = new InputValidator();

    /** The products currently shown in the table, so a selected row can be turned back into a Product. */
    private final List<Product> rows = new ArrayList<>();

    /** True until the first load finishes, so a missing database doesn't pop up a dialog at start-up. */
    private boolean initialLoad = true;

    public ProductManagement() {
        super("Products", COLUMNS);

        setEmptyState("No products found", "Try a different search, or add a product with the form.");
        btnAdd.setText("Add product");
        TableStyler.rightAlign(tblData, COL_PRICE, COL_ECO, COL_STOCK);

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedProduct();
            }
        });

        initPanel();   // must be the last line (see UI_GUIDE.md)
    }

    // =====================================================================
    // The form
    // =====================================================================

    @Override
    protected void buildForm(JPanel form) {
        txtName = FormFields.textField("e.g. Peace Lily");
        cmbCategory = FormFields.comboBox(CATEGORY_LABELS);
        txtPrice = FormFields.textField("e.g. 1850.00");
        cmbEcoRating = FormFields.comboBox(ECO_LABELS);
        txtQuantity = FormFields.textField("e.g. 25");
        txtDescription = FormFields.textField("e.g. Air-purifying indoor plant");

        cmbEcoRating.setSelectedIndex(2);   // 3 - Good, the database default

        form.add(FormFields.heading("Product details"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Product name", txtName));
        form.add(FormFields.row("Category", cmbCategory));
        form.add(FormFields.row("Price (Rs.)", txtPrice));
        form.add(FormFields.row("Eco rating", cmbEcoRating));
        form.add(FormFields.row("Quantity in stock", txtQuantity));
        form.add(FormFields.row("Description (optional)", txtDescription));
    }

    // =====================================================================
    // Loading the table
    // =====================================================================

    @Override
    protected void loadTableData() {
        loadProducts(txtSearch.getText().trim());
    }

    @Override
    protected void onSearch(String query) {
        loadProducts(query);
    }

    private void loadProducts(String query) {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                return service.search(query);
            }

            @Override
            protected void done() {
                boolean quiet = initialLoad;
                initialLoad = false;
                try {
                    showRows(get(), query);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    if (quiet) {
                        setStatus("Could not load products. Check db.properties and that MySQL is running.");
                        System.err.println("ProductManagement: could not load products - " + ex.getCause());
                    } else {
                        fail("load the products", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void showRows(List<Product> products, String query) {
        rows.clear();
        rows.addAll(products);

        Object[][] data = new Object[products.size()][];
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            data[i] = new Object[]{
                    p.getProductId(),
                    p.getProductName(),
                    categoryLabel(p.getCategory()),
                    rupees(p.getPrice()),
                    p.getEcoRating() + " / 5",
                    p.getQuantity()
            };
        }
        setRows(data);
        setStatus(query.isEmpty()
                ? products.size() + " product(s)."
                : products.size() + " product(s) match \"" + query + "\".");
    }

    // =====================================================================
    // Buttons
    // =====================================================================

    @Override
    protected void onAdd() {
        Product product = readForm(0);
        if (product == null) {
            return;   // the problems are already shown next to the fields
        }
        runWrite("add the product", () -> service.addProduct(product), () -> {
            setStatus("Added " + product.getProductName() + " as product #" + product.getProductId() + ".");
            clearForm();
        });
    }

    @Override
    protected void onUpdate() {
        Product selected = selectedProduct();
        if (selected == null) {
            Dialogs.error(this, "Select the product you want to update in the table first.");
            return;
        }
        Product product = readForm(selected.getProductId());
        if (product == null) {
            return;
        }
        runWrite("update the product", () -> {
            if (!service.updateProduct(product)) {
                throw new IllegalArgumentException("This product no longer exists. It may have been deleted.");
            }
        }, () -> setStatus("Updated product #" + product.getProductId() + "."));
    }

    @Override
    protected void onDelete() {
        // BaseModulePanel has already asked "are you sure?"
        Product selected = selectedProduct();
        if (selected == null) {
            return;
        }
        runWrite("delete the product", () -> service.deleteProduct(selected.getProductId()), () -> {
            setStatus("Deleted " + selected.getProductName() + ".");
            clearForm();
        });
    }

    @Override
    protected void onClear() {
        clearForm();
        super.onClear();   // also clears the search box and the table selection
    }

    // =====================================================================
    // Form <-> Product
    // =====================================================================

    /** Checks the fields (showing problems in red under them) and builds a Product. Null if something is wrong. */
    private Product readForm(int productId) {
        validator.reset();
        String name = validator.required(txtName, "Product name");
        Double price = validator.number(txtPrice, "Price");
        Integer quantity = validator.integer(txtQuantity, "Quantity");
        if (!validator.isValid()) {
            return null;
        }
        return new Product(productId,
                name,
                ProductService.CATEGORIES[cmbCategory.getSelectedIndex()],
                price,
                cmbEcoRating.getSelectedIndex() + 1,
                txtDescription.getText().trim(),
                quantity);
    }

    private Product selectedProduct() {
        int row = selectedModelRow();
        return row < 0 || row >= rows.size() ? null : rows.get(row);
    }

    private void showSelectedProduct() {
        Product p = selectedProduct();
        if (p == null) {
            return;
        }
        clearErrors();
        txtName.setText(p.getProductName());
        cmbCategory.setSelectedIndex(categoryIndex(p.getCategory()));
        txtPrice.setText(String.format(Locale.ENGLISH, "%.2f", p.getPrice()));
        cmbEcoRating.setSelectedIndex(Math.max(0, Math.min(4, p.getEcoRating() - 1)));
        txtQuantity.setText(String.valueOf(p.getQuantity()));
        txtDescription.setText(p.getDescription() == null ? "" : p.getDescription());
    }

    private void clearForm() {
        tblData.clearSelection();
        clearErrors();
        txtName.setText("");
        cmbCategory.setSelectedIndex(0);
        txtPrice.setText("");
        cmbEcoRating.setSelectedIndex(2);
        txtQuantity.setText("");
        txtDescription.setText("");
    }

    private void clearErrors() {
        validator.clear(txtName);
        validator.clear(txtPrice);
        validator.clear(txtQuantity);
    }

    // =====================================================================
    // Small helpers
    // =====================================================================

    private static int categoryIndex(String code) {
        for (int i = 0; i < ProductService.CATEGORIES.length; i++) {
            if (ProductService.CATEGORIES[i].equalsIgnoreCase(code)) {
                return i;
            }
        }
        return 0;
    }

    private static String categoryLabel(String code) {
        return CATEGORY_LABELS[categoryIndex(code)];
    }

    /** 1850 -> "Rs. 1,850.00" */
    private static String rupees(double amount) {
        return String.format(Locale.ENGLISH, "Rs. %,.2f", amount);
    }

    /** Something that talks to the database. */
    @FunctionalInterface
    private interface DbCall {
        void run() throws SQLException;
    }

    /** Runs a save/delete on a background thread, then reloads the table. */
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
                    loadProducts(txtSearch.getText().trim());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    /** Shows a problem to the user in plain words. */
    private void fail(String action, Throwable cause) {
        if (cause instanceof IllegalArgumentException) {
            Dialogs.error(this, cause.getMessage());
            setStatus("Nothing changed.");
            return;
        }
        if (cause instanceof SQLIntegrityConstraintViolationException) {
            Dialogs.error(this, "This product is used by an order or a stock record, so it can't be deleted.");
            setStatus("Nothing changed.");
            return;
        }
        System.err.println("ProductManagement: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        String detail = (cause == null || cause.getMessage() == null) ? "Unknown error." : cause.getMessage();
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Product operation failed.");
    }

    private void setBusy(boolean busy) {
        btnAdd.setEnabled(!busy);
        btnUpdate.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
    }
}
