
package com.econest.product.ui;

import com.econest.product.model.Product;
import com.econest.product.service.ProductService;
import com.econest.ui.BaseModulePanel;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Task 1: Manage products.
 */
public class ProductManagement extends BaseModulePanel {

    private final ProductService service = new ProductService();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JComboBox<String> cmbCategory =
            new JComboBox<>(ProductService.CATEGORIES);
    private final JTextField txtPrice = new JTextField();
    private final JSpinner spnEcoRating =
            new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
    private final JTextArea txtDescription = new JTextArea(3, 20);
    private final JSpinner spnQuantity =
            new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));

    public ProductManagement() {
        super("Product Management",
                new String[]{
                        "ID", "Name", "Category", "Price (Rs.)",
                        "Eco Rating", "Description", "Quantity"
                });

        txtId.setEditable(false);
        txtId.setToolTipText("Assigned automatically");

        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);

        tblData.setAutoCreateRowSorter(true);

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedRow();
            }
        });

        initPanel();
    }

    @Override
    protected void buildForm(JPanel form) {

        addField(form, "Product ID (auto):", txtId);
        addField(form, "Product Name:", txtName);
        addField(form, "Category:", cmbCategory);
        addField(form, "Price (Rs.):", txtPrice);
        addField(form, "Eco Rating (1-5):", spnEcoRating);
        addField(form, "Description:", new JScrollPane(txtDescription));
        addField(form, "Quantity:", spnQuantity);
    }

    private void addField(JPanel form, String label, Component component) {
        JLabel lbl = new JLabel(label);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 65));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lbl);
        form.add(Box.createVerticalStrut(5));
        form.add(component);
        form.add(Box.createVerticalStrut(10));
    }

    @Override
    protected void loadTableData() {
        loadProducts(null);
    }

    @Override
    protected void onAdd() {
        try {
            Product product = readForm(0);

            service.addProduct(product);

            JOptionPane.showMessageDialog(
                    this,
                    "Product added successfully. ID: "
                            + product.getProductId()
            );

            onClear();
            loadTableData();

        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());

        } catch (SQLException ex) {
            showError("Could not add product.", ex);
        }
    }

    @Override
    protected void onUpdate() {

        Object idValue = selectedValue(0);

        if (idValue == null) {
            showWarning("Select a product in the table first.");
            return;
        }

        try {
            Product product = readForm(
                    Integer.parseInt(idValue.toString())
            );

            if (service.updateProduct(product)) {
                JOptionPane.showMessageDialog(
                        this, "Product updated successfully."
                );

                onClear();
                loadTableData();

            } else {
                showWarning("Product not found.");
            }

        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());

        } catch (SQLException ex) {
            showError("Could not update product.", ex);
        }
    }

    @Override
    protected void onDelete() {

        Object idValue = selectedValue(0);

        if (idValue == null) {
            showWarning("Select a product first.");
            return;
        }

        try {
            int id = Integer.parseInt(idValue.toString());

            if (service.deleteProduct(id)) {
                JOptionPane.showMessageDialog(
                        this, "Product deleted successfully."
                );

                onClear();
                loadTableData();

            } else {
                showWarning("Product not found.");
            }

        } catch (SQLException ex) {
            showError(
                    "Could not delete product. It may be used by an order or stock record.",
                    ex
            );
        }
    }

    @Override
    protected void onClear() {
        txtId.setText("");
        txtName.setText("");
        txtPrice.setText("");
        txtDescription.setText("");

        cmbCategory.setSelectedIndex(0);
        spnEcoRating.setValue(3);
        spnQuantity.setValue(0);

        tblData.clearSelection();
        setStatus("Product form cleared.");
    }

    @Override
    protected void onSearch(String query) {
        loadProducts(query.isBlank() ? null : query);
    }

    private void loadProducts(String keyword) {
        try {
            List<Product> products = service.search(keyword);

            Object[][] rows = new Object[products.size()][7];

            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);

                rows[i] = new Object[]{
                        p.getProductId(),
                        p.getProductName(),
                        p.getCategory(),
                        p.getPrice(),
                        p.getEcoRating(),
                        p.getDescription(),
                        p.getQuantity()
                };
            }

            setRows(rows);

        } catch (SQLException ex) {
            showError("Could not load products.", ex);
        }
    }

    private Product readForm(int id) {

        double price;

        try {
            price = Double.parseDouble(txtPrice.getText().trim());

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Price must be a valid number, e.g. 1850.00"
            );
        }

        return new Product(
                id,
                txtName.getText().trim(),
                (String) cmbCategory.getSelectedItem(),
                price,
                (Integer) spnEcoRating.getValue(),
                txtDescription.getText().trim(),
                (Integer) spnQuantity.getValue()
        );
    }

    private void showSelectedRow() {

        int row = selectedModelRow();

        if (row < 0) {
            return;
        }

        txtId.setText(
                String.valueOf(tableModel.getValueAt(row, 0))
        );

        txtName.setText(
                String.valueOf(tableModel.getValueAt(row, 1))
        );

        cmbCategory.setSelectedItem(
                tableModel.getValueAt(row, 2)
        );

        txtPrice.setText(
                String.valueOf(tableModel.getValueAt(row, 3))
        );

        spnEcoRating.setValue(
                tableModel.getValueAt(row, 4)
        );

        Object description = tableModel.getValueAt(row, 5);

        txtDescription.setText(
                description == null ? "" : description.toString()
        );

        spnQuantity.setValue(
                tableModel.getValueAt(row, 6)
        );
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Check your input",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(String message, SQLException ex) {
        ex.printStackTrace();

        JOptionPane.showMessageDialog(
                this,
                message + "\n" + ex.getMessage(),
                "Database error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
