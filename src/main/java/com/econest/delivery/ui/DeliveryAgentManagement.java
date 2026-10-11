package econest.ui;

import econest.dao.DeliveryAgentDAO;
import econest.model.DeliveryAgent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DeliveryAgentManagement extends JFrame {

    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtVehicleNumber;

    private JComboBox<String> cmbVehicleType;

    private JTable table;
    private DefaultTableModel tableModel;

    private DeliveryAgentDAO agentDAO = new DeliveryAgentDAO();


    // =========================
    // CONSTRUCTOR
    // =========================

    public DeliveryAgentManagement() {

        setTitle("EcoNest - Delivery Agent Management");

        setSize(1100, 750);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLocationRelativeTo(null);

        createGUI();

        loadAgents();
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
                        "Delivery Agent Management",
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
                                3,
                                4,
                                8,
                                5
                        )
                );

        formPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Delivery Agent Details"
                )
        );


        // Agent ID

        formPanel.add(
                new JLabel("Agent ID:")
        );

        txtId = new JTextField();

        formPanel.add(txtId);


        // Agent Name

        formPanel.add(
                new JLabel("Agent Name:")
        );

        txtName = new JTextField();

        formPanel.add(txtName);


        // Phone

        formPanel.add(
                new JLabel("Phone:")
        );

        txtPhone = new JTextField();

        formPanel.add(txtPhone);


        // Email

        formPanel.add(
                new JLabel("Email:")
        );

        txtEmail = new JTextField();

        formPanel.add(txtEmail);


        // Vehicle Number

        formPanel.add(
                new JLabel("Vehicle Number:")
        );

        txtVehicleNumber =
                new JTextField();

        formPanel.add(txtVehicleNumber);


        // Vehicle Type

        formPanel.add(
                new JLabel("Vehicle Type:")
        );

        cmbVehicleType =
                new JComboBox<>();

        cmbVehicleType.addItem("Motorcycle");
        cmbVehicleType.addItem("Car");
        cmbVehicleType.addItem("Van");
        cmbVehicleType.addItem("Three Wheeler");
        cmbVehicleType.addItem("Truck");

        formPanel.add(cmbVehicleType);


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
                new JButton("Add Agent");

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
        // FORM + BUTTONS
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
                "Agent Name",
                "Phone",
                "Email",
                "Vehicle Number",
                "Vehicle Type"

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
                        "Delivery Agent List"
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
                e -> addAgent()
        );


        btnSearch.addActionListener(
                e -> searchAgent()
        );


        btnUpdate.addActionListener(
                e -> updateAgent()
        );


        btnDelete.addActionListener(
                e -> deleteAgent()
        );


        btnLoad.addActionListener(
                e -> loadAgents()
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


                            txtName.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    1
                                            )
                                            .toString()
                            );


                            txtPhone.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    2
                                            )
                                            .toString()
                            );


                            txtEmail.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    3
                                            )
                                            .toString()
                            );


                            txtVehicleNumber.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    4
                                            )
                                            .toString()
                            );


                            cmbVehicleType.setSelectedItem(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    5
                                            )
                                            .toString()
                            );
                        }
                    }
                });
    }


    // =========================
    // ADD AGENT
    // =========================

    private void addAgent() {

        String name =
                txtName.getText().trim();

        String phone =
                txtPhone.getText().trim();

        String email =
                txtEmail.getText().trim();

        String vehicleNumber =
                txtVehicleNumber.getText().trim();

        String vehicleType =
                cmbVehicleType
                        .getSelectedItem()
                        .toString();


        if (name.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter Agent Name."
            );

            return;
        }


        DeliveryAgent agent =
                new DeliveryAgent(
                        0,
                        name,
                        phone,
                        email,
                        vehicleNumber,
                        vehicleType
                );


        if (agentDAO.addAgent(agent)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Delivery Agent added successfully!"
            );

            clearFields();

            loadAgents();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to add Delivery Agent."
            );
        }
    }


    // =========================
    // SEARCH AGENT
    // =========================

    private void searchAgent() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Agent ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId.getText()
                                    .trim()
                    );


            DeliveryAgent agent =
                    agentDAO.getAgentById(id);


            if (agent != null) {

                txtName.setText(
                        agent.getAgentName()
                );


                txtPhone.setText(
                        agent.getPhone()
                );


                txtEmail.setText(
                        agent.getEmail()
                );


                txtVehicleNumber.setText(
                        agent.getVehicleNumber()
                );


                cmbVehicleType.setSelectedItem(
                        agent.getVehicleType()
                );

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Delivery Agent not found."
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Agent ID must be a number."
            );
        }
    }


    // =========================
    // UPDATE AGENT
    // =========================

    private void updateAgent() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Agent ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId.getText()
                                    .trim()
                    );


            DeliveryAgent agent =
                    new DeliveryAgent(
                            id,
                            txtName.getText().trim(),
                            txtPhone.getText().trim(),
                            txtEmail.getText().trim(),
                            txtVehicleNumber
                                    .getText()
                                    .trim(),
                            cmbVehicleType
                                    .getSelectedItem()
                                    .toString()
                    );


            if (agentDAO.updateAgent(agent)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Delivery Agent updated successfully!"
                );

                clearFields();

                loadAgents();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Delivery Agent not found."
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Agent ID."
            );
        }
    }


    // =========================
    // DELETE AGENT
    // =========================

    private void deleteAgent() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Agent ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId.getText()
                                    .trim()
                    );


            int confirm =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to delete this agent?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                    );


            if (confirm ==
                    JOptionPane.YES_OPTION) {


                if (agentDAO.deleteAgent(id)) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Delivery Agent deleted successfully!"
                    );

                    clearFields();

                    loadAgents();

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Delivery Agent not found."
                    );
                }
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Agent ID."
            );
        }
    }


    // =========================
    // LOAD ALL AGENTS
    // =========================

    private void loadAgents() {

        tableModel.setRowCount(0);


        List<DeliveryAgent> agents =
                agentDAO.getAllAgents();


        for (DeliveryAgent agent : agents) {

            tableModel.addRow(
                    new Object[]{

                            agent.getAgentId(),

                            agent.getAgentName(),

                            agent.getPhone(),

                            agent.getEmail(),

                            agent.getVehicleNumber(),

                            agent.getVehicleType()

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

        txtPhone.setText("");

        txtEmail.setText("");

        txtVehicleNumber.setText("");

        cmbVehicleType.setSelectedIndex(0);

        table.clearSelection();
    }


    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            DeliveryAgentManagement window =
                    new DeliveryAgentManagement();

            window.setVisible(true);

        });
    }
}