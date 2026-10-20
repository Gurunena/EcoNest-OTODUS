package com.econest.product.ui;

import com.econest.product.model.Product;
import com.econest.product.service.ProductService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

/** Task 1 screen: add, update, remove and search plants, planters and gardening kits. */
public class ProductManagement extends JPanel {

    private final ProductService service = new ProductService();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JComboBox<String> cmbCategory = new JComboBox<>(ProductService.CATEGORIES);
    private final JTextField txtPrice = new JTextField();
    private final JSpinner spnEcoRating = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
    private final JTextArea txtDescription = new JTextArea(2, 20);
    private final JSpinner spnQuantity = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JTextField txtSearch = new JTextField(15);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Category", "Price (Rs.)", "Eco Rating", "Description", "Quantity"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ProductManagement() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtId.setEditable(false);
        txtId.setToolTipText("Assigned automatically when the product is added");
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 10));
        form.add(new JLabel("Product ID (auto):"));
        form.add(txtId);
        form.add(new JLabel("Product Name:"));
        form.add(txtName);
        form.add(new JLabel("Category:"));
        form.add(cmbCategory);
        form.add(new JLabel("Price (Rs.):"));
        form.add(txtPrice);
        form.add(new JLabel("Eco Rating (1-5):"));
        form.add(spnEcoRating);
        form.add(new JLabel("Description:"));
        form.add(new JScrollPane(txtDescription));
        form.add(new JLabel("Quantity:"));
        form.add(spnQuantity);

        JButton btnAdd = new JButton("Add Product");
        JButton btnUpdate = new JButton("Update Product");
        JButton btnDelete = new JButton("Delete Product");
        JButton btnClear = new JButton("Clear");
        JButton btnSearch = new JButton("Search");
        JButton btnLoad = new JButton("Load All");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.add(new JLabel("Search name / category:"));
        searchBar.add(txtSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnLoad);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.CENTER);
        top.add(searchBar, BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearFields());
        btnSearch.addActionListener(e -> loadProducts(txtSearch.getText()));
        btnLoad.addActionListener(e -> {
            txtSearch.setText("");
            loadProducts(null);
        });
        txtSearch.addActionListener(e -> loadProducts(txtSearch.getText()));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedRow();
            }
        });

        loadProducts(null);
    }

    // ---------------------------------------------------------------- actions

    private void addProduct() {
        try {
            Product product = readForm(0);
            service.addProduct(product);
            info("Product added (ID " + product.getProductId() + ").");
            clearFields();
            loadProducts(null);
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        } catch (SQLException ex) {
            error("Could not add the product.", ex);
        }
    }

    private void updateProduct() {
        if (txtId.getText().isBlank()) {
            warn("Select a product in the table first.");
            return;
        }
        try {
            Product product = readForm(Integer.parseInt(txtId.getText()));
            if (service.updateProduct(product)) {
                info("Product updated.");
                loadProducts(null);
            } else {
                warn("Product not found - it may have been removed.");
            }
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        } catch (SQLException ex) {
            error("Could not update the product.", ex);
        }
    }

    private void deleteProduct() {
        if (txtId.getText().isBlank()) {
            warn("Select a product in the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete '" + txtName.getText() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (service.deleteProduct(Integer.parseInt(txtId.getText()))) {
                info("Product deleted.");
                clearFields();
                loadProducts(null);
            } else {
                warn("Product not found.");
            }
        } catch (SQLException ex) {
            error("Could not delete the product. It may already be used by an order or a stock record.", ex);
        }
    }

    private void loadProducts(String keyword) {
        try {
            List<Product> products = service.search(keyword);
            tableModel.setRowCount(0);
            for (Product p : products) {
                tableModel.addRow(new Object[]{p.getProductId(), p.getProductName(), p.getCategory(),
                        p.getPrice(), p.getEcoRating(), p.getDescription(), p.getQuantity()});
            }
        } catch (SQLException ex) {
            error("Could not load products.", ex);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Product readForm(int id) {
        double price;
        try {
            price = Double.parseDouble(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a number, e.g. 1850.00");
        }
        return new Product(id,
                txtName.getText().trim(),
                (String) cmbCategory.getSelectedItem(),
                price,
                (Integer) spnEcoRating.getValue(),
                txtDescription.getText().trim(),
                (Integer) spnQuantity.getValue());
    }

    private void showSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        txtId.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        txtName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        cmbCategory.setSelectedItem(tableModel.getValueAt(row, 2));
        txtPrice.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        spnEcoRating.setValue(tableModel.getValueAt(row, 4));
        Object desc = tableModel.getValueAt(row, 5);
        txtDescription.setText(desc == null ? "" : desc.toString());
        spnQuantity.setValue(tableModel.getValueAt(row, 6));
    }

    private void clearFields() {
        table.clearSelection();
        txtId.setText("");
        txtName.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        cmbCategory.setSelectedIndex(0);
        spnEcoRating.setValue(3);
        spnQuantity.setValue(0);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg, SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + "\n" + ex.getMessage(), "Database error",
                JOptionPane.ERROR_MESSAGE);
    }
}
