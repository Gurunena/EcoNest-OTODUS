package econest.ui;

import econest.dao.OrderDAO;
import econest.model.Order;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class OrderManagement extends JFrame {

    private JTextField txtId;
    private JTextField txtClientId;
    private JTextField txtProductId;
    private JTextField txtQuantity;
    private JTextField txtDate;
    private JTextField txtTotalAmount;
    private JTextField txtDeliveryAgentId;

    private JComboBox<String> cmbStatus;

    private JTable table;
    private DefaultTableModel tableModel;

    private OrderDAO orderDAO = new OrderDAO();


    // =========================
    // CONSTRUCTOR
    // =========================

    public OrderManagement() {

        setTitle("EcoNest - Order Management");

        setSize(1150, 750);

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        setLocationRelativeTo(null);

        createGUI();

        loadOrders();
    }


    // =========================
    // CREATE GUI
    // =========================

    private void createGUI() {

        JPanel mainPanel =
                new JPanel(new BorderLayout(5, 5));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 12, 12, 12
                )
        );


        // =========================
        // TITLE
        // =========================

        JLabel title =
                new JLabel(
                        "Order Management",
                        SwingConstants.CENTER
                );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        24
                )
        );

        mainPanel.add(
                title,
                BorderLayout.NORTH
        );


        // =========================
        // CENTER PANEL
        // =========================

        JPanel centerPanel =
                new JPanel(
                        new BorderLayout(5, 5)
                );


        // =========================
        // FORM PANEL
        // =========================

        JPanel formPanel =
                new JPanel(
                        new GridLayout(
                                4,
                                4,
                                8,
                                5
                        )
                );

        formPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Order Details"
                )
        );


        // Order ID

        formPanel.add(
                new JLabel("Order ID:")
        );

        txtId = new JTextField();

        formPanel.add(txtId);


        // Client ID

        formPanel.add(
                new JLabel("Client ID:")
        );

        txtClientId = new JTextField();

        formPanel.add(txtClientId);


        // Product ID

        formPanel.add(
                new JLabel("Product ID:")
        );

        txtProductId = new JTextField();

        formPanel.add(txtProductId);


        // Quantity

        formPanel.add(
                new JLabel("Quantity:")
        );

        txtQuantity = new JTextField();

        formPanel.add(txtQuantity);


        // Order Date

        formPanel.add(
                new JLabel("Order Date:")
        );

        txtDate = new JTextField();

        txtDate.setText(
                LocalDate.now().toString()
        );

        formPanel.add(txtDate);


        // Total Amount

        formPanel.add(
                new JLabel("Total Amount:")
        );

        txtTotalAmount = new JTextField();

        formPanel.add(txtTotalAmount);


        // Order Status

        formPanel.add(
                new JLabel("Order Status:")
        );

        cmbStatus =
                new JComboBox<>();

        cmbStatus.addItem("Pending");
        cmbStatus.addItem("Processing");
        cmbStatus.addItem("Shipped");
        cmbStatus.addItem("Delivered");
        cmbStatus.addItem("Cancelled");

        formPanel.add(cmbStatus);


        // Delivery Agent ID

        formPanel.add(
                new JLabel("Delivery Agent ID:")
        );

        txtDeliveryAgentId =
                new JTextField();

        formPanel.add(
                txtDeliveryAgentId
        );


        // =========================
        // BUTTON PANEL
        // =========================

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                6,
                                5
                        )
                );


        JButton btnSearch =
                new JButton("Search");

        JButton btnAdd =
                new JButton("Add Order");

        JButton btnUpdate =
                new JButton("Update");

        JButton btnDelete =
                new JButton("Delete");

        JButton btnLoad =
                new JButton("Load All");

        JButton btnClear =
                new JButton("Clear");


        buttonPanel.add(btnSearch);

        buttonPanel.add(btnAdd);

        buttonPanel.add(btnUpdate);

        buttonPanel.add(btnDelete);

        buttonPanel.add(btnLoad);

        buttonPanel.add(btnClear);


        // =========================
        // TOP AREA
        // =========================

        JPanel topPanel =
                new JPanel(
                        new BorderLayout(5, 5)
                );


        topPanel.add(
                formPanel,
                BorderLayout.CENTER
        );


        topPanel.add(
                buttonPanel,
                BorderLayout.SOUTH
        );


        centerPanel.add(
                topPanel,
                BorderLayout.NORTH
        );


        // =========================
        // TABLE
        // =========================

        String[] columns = {

                "ID",
                "Client ID",
                "Product ID",
                "Quantity",
                "Order Date",
                "Total Amount",
                "Status",
                "Agent ID"

        };


        tableModel =
                new DefaultTableModel(
                        columns,
                        0
                );


        table =
                new JTable(tableModel);


        table.setRowHeight(25);


        JScrollPane scrollPane =
                new JScrollPane(table);


        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        "Order List"
                )
        );


        centerPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );


        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );


        add(mainPanel);


        // =========================
        // BUTTON ACTIONS
        // =========================

        btnAdd.addActionListener(
                e -> addOrder()
        );


        btnSearch.addActionListener(
                e -> searchOrder()
        );


        btnUpdate.addActionListener(
                e -> updateOrder()
        );


        btnDelete.addActionListener(
                e -> deleteOrder()
        );


        btnLoad.addActionListener(
                e -> loadOrders()
        );


        btnClear.addActionListener(
                e -> clearFields()
        );


        // =========================
        // TABLE ROW CLICK
        // =========================

        table.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {

                        int row =
                                table.getSelectedRow();


                        if (row >= 0) {

                            txtId.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    0
                                            )
                                            .toString()
                            );


                            txtClientId.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    1
                                            )
                                            .toString()
                            );


                            txtProductId.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    2
                                            )
                                            .toString()
                            );


                            txtQuantity.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    3
                                            )
                                            .toString()
                            );


                            txtDate.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    4
                                            )
                                            .toString()
                            );


                            txtTotalAmount.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    5
                                            )
                                            .toString()
                            );


                            cmbStatus.setSelectedItem(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    6
                                            )
                                            .toString()
                            );


                            Object agentValue =
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    7
                                            );

                            if (agentValue != null) {

                                txtDeliveryAgentId
                                        .setText(
                                                agentValue
                                                        .toString()
                                        );

                            } else {

                                txtDeliveryAgentId
                                        .setText("");

                            }
                        }
                    }
                });
    }


    // =========================
    // ADD ORDER
    // =========================

    private void addOrder() {

        try {

            int clientId =
                    Integer.parseInt(
                            txtClientId
                                    .getText()
                                    .trim()
                    );


            int productId =
                    Integer.parseInt(
                            txtProductId
                                    .getText()
                                    .trim()
                    );


            int quantity =
                    Integer.parseInt(
                            txtQuantity
                                    .getText()
                                    .trim()
                    );


            LocalDate date =
                    LocalDate.parse(
                            txtDate
                                    .getText()
                                    .trim()
                    );


            double total =
                    Double.parseDouble(
                            txtTotalAmount
                                    .getText()
                                    .trim()
                    );


            String status =
                    cmbStatus
                            .getSelectedItem()
                            .toString();


            int agentId = 0;


            if (!txtDeliveryAgentId
                    .getText()
                    .trim()
                    .isEmpty()) {

                agentId =
                        Integer.parseInt(
                                txtDeliveryAgentId
                                        .getText()
                                        .trim()
                        );
            }


            Order order =
                    new Order(
                            0,
                            clientId,
                            productId,
                            quantity,
                            date,
                            total,
                            status,
                            agentId
                    );


            if (orderDAO.addOrder(order)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Order added successfully!"
                );

                clearFields();

                loadOrders();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Failed to add order."
                );
            }


        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numbers."
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid date. Use YYYY-MM-DD."
            );
        }
    }


    // =========================
    // SEARCH ORDER
    // =========================

    private void searchOrder() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Order ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId
                                    .getText()
                                    .trim()
                    );


            Order order =
                    orderDAO.getOrderById(id);


            if (order != null) {

                txtClientId.setText(
                        String.valueOf(
                                order.getClientId()
                        )
                );


                txtProductId.setText(
                        String.valueOf(
                                order.getProductId()
                        )
                );


                txtQuantity.setText(
                        String.valueOf(
                                order.getQuantity()
                        )
                );


                txtDate.setText(
                        order.getOrderDate()
                                .toString()
                );


                txtTotalAmount.setText(
                        String.valueOf(
                                order.getTotalAmount()
                        )
                );


                cmbStatus.setSelectedItem(
                        order.getOrderStatus()
                );


                if (order.getDeliveryAgentId()
                        == 0) {

                    txtDeliveryAgentId
                            .setText("");

                } else {

                    txtDeliveryAgentId.setText(
                            String.valueOf(
                                    order
                                            .getDeliveryAgentId()
                            )
                    );
                }

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Order not found."
                );
            }


        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Order ID must be a number."
            );
        }
    }


    // =========================
    // UPDATE ORDER
    // =========================

    private void updateOrder() {

        try {

            int id =
                    Integer.parseInt(
                            txtId
                                    .getText()
                                    .trim()
                    );


            int clientId =
                    Integer.parseInt(
                            txtClientId
                                    .getText()
                                    .trim()
                    );


            int productId =
                    Integer.parseInt(
                            txtProductId
                                    .getText()
                                    .trim()
                    );


            int quantity =
                    Integer.parseInt(
                            txtQuantity
                                    .getText()
                                    .trim()
                    );


            LocalDate date =
                    LocalDate.parse(
                            txtDate
                                    .getText()
                                    .trim()
                    );


            double total =
                    Double.parseDouble(
                            txtTotalAmount
                                    .getText()
                                    .trim()
                    );


            String status =
                    cmbStatus
                            .getSelectedItem()
                            .toString();


            int agentId = 0;


            if (!txtDeliveryAgentId
                    .getText()
                    .trim()
                    .isEmpty()) {

                agentId =
                        Integer.parseInt(
                                txtDeliveryAgentId
                                        .getText()
                                        .trim()
                        );
            }


            Order order =
                    new Order(
                            id,
                            clientId,
                            productId,
                            quantity,
                            date,
                            total,
                            status,
                            agentId
                    );


            if (orderDAO.updateOrder(order)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Order updated successfully!"
                );

                clearFields();

                loadOrders();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Order not found."
                );
            }


        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please check your input values."
            );
        }
    }


    // =========================
    // DELETE ORDER
    // =========================

    private void deleteOrder() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Order ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId
                                    .getText()
                                    .trim()
                    );


            int confirm =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to delete this order?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                    );


            if (confirm ==
                    JOptionPane.YES_OPTION) {


                if (orderDAO.deleteOrder(id)) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Order deleted successfully!"
                    );

                    clearFields();

                    loadOrders();

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Order not found."
                    );
                }
            }


        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Order ID."
            );
        }
    }


    // =========================
    // LOAD ALL ORDERS
    // =========================

    private void loadOrders() {

        tableModel.setRowCount(0);


        List<Order> orders =
                orderDAO.getAllOrders();


        for (Order order : orders) {

            tableModel.addRow(
                    new Object[]{

                            order.getOrderId(),

                            order.getClientId(),

                            order.getProductId(),

                            order.getQuantity(),

                            order.getOrderDate(),

                            order.getTotalAmount(),

                            order.getOrderStatus(),

                            order.getDeliveryAgentId() == 0
                                    ? ""
                                    : order.getDeliveryAgentId()

                    }
            );
        }
    }


    // =========================
    // CLEAR FIELDS
    // =========================

    private void clearFields() {

        txtId.setText("");

        txtClientId.setText("");

        txtProductId.setText("");

        txtQuantity.setText("");

        txtDate.setText(
                LocalDate.now().toString()
        );

        txtTotalAmount.setText("");

        cmbStatus.setSelectedIndex(0);

        txtDeliveryAgentId.setText("");

        table.clearSelection();
    }


    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            OrderManagement window =
                    new OrderManagement();

            window.setVisible(true);

        });
    }
}
