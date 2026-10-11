package econest.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProductManagement extends JFrame {

    private JTextField txtId, txtName, txtPrice, txtEcoRating, txtQuantity;
    private JComboBox<String> cmbCategory;
    private JTextArea txtDescription;

    private JButton btnAdd, btnUpdate, btnDelete, btnSearch, btnLoad;

    private JTable productTable;
    private DefaultTableModel tableModel;

    public ProductManagement() {

        setTitle("EcoNest - Product Management");
        setSize(900, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // =========================
        // FORM PANEL
        // =========================

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));

        formPanel.add(new JLabel("Product ID:"));
        txtId = new JTextField();
        formPanel.add(txtId);

        formPanel.add(new JLabel("Product Name:"));
        txtName = new JTextField();
        formPanel.add(txtName);

        formPanel.add(new JLabel("Category:"));
        cmbCategory = new JComboBox<>(
                new String[]{
                        "Plant",
                        "Planter",
                        "Gardening Kit",
                        "Accessories"
                }
        );
        formPanel.add(cmbCategory);

        formPanel.add(new JLabel("Price:"));
        txtPrice = new JTextField();
        formPanel.add(txtPrice);

        formPanel.add(new JLabel("Eco Rating:"));
        txtEcoRating = new JTextField();
        formPanel.add(txtEcoRating);

        formPanel.add(new JLabel("Description:"));
        txtDescription = new JTextArea(2, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(txtDescription));

        formPanel.add(new JLabel("Quantity:"));
        txtQuantity = new JTextField();
        formPanel.add(txtQuantity);

        // =========================
        // BUTTON PANEL
        // =========================

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 10)
        );

        btnSearch = new JButton("Search");
        btnAdd = new JButton("Add Product");
        btnUpdate = new JButton("Update Product");
        btnDelete = new JButton("Delete Product");
        btnLoad = new JButton("Load All Products");

        buttonPanel.add(btnSearch);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnLoad);

        // =========================
        // TABLE
        // =========================

        String[] columns = {
                "ID",
                "Name",
                "Category",
                "Price",
                "Eco Rating",
                "Description",
                "Quantity"
        };

        tableModel = new DefaultTableModel(columns, 0);

        productTable = new JTable(tableModel);

        productTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        JScrollPane tableScrollPane =
                new JScrollPane(productTable);

        // =========================
        // BUTTON ACTIONS
        // =========================

        btnSearch.addActionListener(e -> searchProduct());

        btnAdd.addActionListener(e -> addProduct());

        btnUpdate.addActionListener(e -> updateProduct());

        btnDelete.addActionListener(e -> deleteProduct());

        btnLoad.addActionListener(e -> loadProducts());

        // Click table row
        productTable.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {

                        int row =
                                productTable.getSelectedRow();

                        if (row >= 0) {

                            txtId.setText(
                                    tableModel.getValueAt(
                                            row, 0
                                    ).toString()
                            );

                            txtName.setText(
                                    tableModel.getValueAt(
                                            row, 1
                                    ).toString()
                            );

                            cmbCategory.setSelectedItem(
                                    tableModel.getValueAt(
                                            row, 2
                                    ).toString()
                            );

                            txtPrice.setText(
                                    tableModel.getValueAt(
                                            row, 3
                                    ).toString()
                            );

                            txtEcoRating.setText(
                                    tableModel.getValueAt(
                                            row, 4
                                    ).toString()
                            );

                            txtDescription.setText(
                                    tableModel.getValueAt(
                                            row, 5
                                    ).toString()
                            );

                            txtQuantity.setText(
                                    tableModel.getValueAt(
                                            row, 6
                                    ).toString()
                            );
                        }
                    }
                });

        // =========================
        // ADD TO FRAME
        // =========================

        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(tableScrollPane, BorderLayout.SOUTH);

        // Better table height
        tableScrollPane.setPreferredSize(
                new Dimension(850, 300)
        );

        add(mainPanel);

        setVisible(true);

        // Load products automatically
        loadProducts();
    }

    // =========================
    // ADD PRODUCT
    // =========================

    private void addProduct() {

        try {

            int id = Integer.parseInt(txtId.getText());

            String name = txtName.getText();

            String category =
                    cmbCategory.getSelectedItem().toString();

            double price =
                    Double.parseDouble(txtPrice.getText());

            int ecoRating =
                    Integer.parseInt(txtEcoRating.getText());

            String description =
                    txtDescription.getText();

            int quantity =
                    Integer.parseInt(txtQuantity.getText());

            econest.model.Product product =
                    new econest.model.Product(
                            id,
                            name,
                            category,
                            price,
                            ecoRating,
                            description,
                            quantity
                    );

            econest.dao.ProductDAO dao =
                    new econest.dao.ProductDAO();

            if (dao.addProduct(product)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Product added successfully!"
                );

                clearFields();
                loadProducts();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Failed to add product!"
                );
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid details!"
            );
        }
    }

    // =========================
    // SEARCH PRODUCT
    // =========================

    private void searchProduct() {

        try {

            int id =
                    Integer.parseInt(txtId.getText());

            econest.dao.ProductDAO dao =
                    new econest.dao.ProductDAO();

            econest.model.Product product =
                    dao.getProductById(id);

            if (product != null) {

                txtName.setText(
                        product.getProductName()
                );

                cmbCategory.setSelectedItem(
                        product.getCategory()
                );

                txtPrice.setText(
                        String.valueOf(
                                product.getPrice()
                        )
                );

                txtEcoRating.setText(
                        String.valueOf(
                                product.getEcoRating()
                        )
                );

                txtDescription.setText(
                        product.getDescription()
                );

                txtQuantity.setText(
                        String.valueOf(
                                product.getQuantity()
                        )
                );

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Product not found!"
                );
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Product ID!"
            );
        }
    }

    // =========================
    // UPDATE PRODUCT
    // =========================

    private void updateProduct() {

        try {

            int id =
                    Integer.parseInt(txtId.getText());

            String name =
                    txtName.getText();

            String category =
                    cmbCategory.getSelectedItem().toString();

            double price =
                    Double.parseDouble(
                            txtPrice.getText()
                    );

            int ecoRating =
                    Integer.parseInt(
                            txtEcoRating.getText()
                    );

            String description =
                    txtDescription.getText();

            int quantity =
                    Integer.parseInt(
                            txtQuantity.getText()
                    );

            econest.model.Product product =
                    new econest.model.Product(
                            id,
                            name,
                            category,
                            price,
                            ecoRating,
                            description,
                            quantity
                    );

            econest.dao.ProductDAO dao =
                    new econest.dao.ProductDAO();

            if (dao.updateProduct(product)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Product updated successfully!"
                );

                loadProducts();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Product not found!"
                );
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid details!"
            );
        }
    }

    // =========================
    // DELETE PRODUCT
    // =========================

    private void deleteProduct() {

        try {

            int id =
                    Integer.parseInt(txtId.getText());

            int confirm =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to delete?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                    );

            if (confirm ==
                    JOptionPane.YES_OPTION) {

                econest.dao.ProductDAO dao =
                        new econest.dao.ProductDAO();

                if (dao.deleteProduct(id)) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Product deleted successfully!"
                    );

                    clearFields();
                    loadProducts();

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Product not found!"
                    );
                }
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Product ID!"
            );
        }
    }

    // =========================
    // LOAD ALL PRODUCTS
    // =========================

    private void loadProducts() {

        tableModel.setRowCount(0);

        econest.dao.ProductDAO dao =
                new econest.dao.ProductDAO();

        List<econest.model.Product> products =
                dao.getAllProducts();

        for (econest.model.Product product : products) {

            tableModel.addRow(
                    new Object[]{
                            product.getProductId(),
                            product.getProductName(),
                            product.getCategory(),
                            product.getPrice(),
                            product.getEcoRating(),
                            product.getDescription(),
                            product.getQuantity()
                    }
            );
        }
    }

    // =========================
    // CLEAR FIELDS
    // =========================

    private void clearFields() {

        txtId.setText("");
        txtName.setText("");
        txtPrice.setText("");
        txtEcoRating.setText("");
        txtDescription.setText("");
        txtQuantity.setText("");

        cmbCategory.setSelectedIndex(0);
    }

    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> new ProductManagement()
        );
    }
}
