package econest.ui;

import econest.dao.ClientDAO;
import econest.model.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClientManagement extends JFrame {

    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JTextField txtAddress;

    private JComboBox<String> cmbType;

    private JTable table;
    private DefaultTableModel tableModel;

    private ClientDAO clientDAO = new ClientDAO();


    // =========================
    // CONSTRUCTOR
    // =========================

    public ClientManagement() {

        setTitle("EcoNest - Client Management");

        setSize(1100, 750);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLocationRelativeTo(null);

        createGUI();

        loadClients();
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
                        "Client Management",
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
                        "Client Details"
                )
        );


        // Client ID

        formPanel.add(
                new JLabel("Client ID:")
        );

        txtId = new JTextField();

        formPanel.add(txtId);


        // Client Name

        formPanel.add(
                new JLabel("Client Name:")
        );

        txtName = new JTextField();

        formPanel.add(txtName);


        // Email

        formPanel.add(
                new JLabel("Email:")
        );

        txtEmail = new JTextField();

        formPanel.add(txtEmail);


        // Phone

        formPanel.add(
                new JLabel("Phone:")
        );

        txtPhone = new JTextField();

        formPanel.add(txtPhone);


        // Address

        formPanel.add(
                new JLabel("Address:")
        );

        txtAddress = new JTextField();

        formPanel.add(txtAddress);


        // Client Type

        formPanel.add(
                new JLabel("Client Type:")
        );

        cmbType =
                new JComboBox<>();

        cmbType.addItem("Household");
        cmbType.addItem("Office");
        cmbType.addItem("Business");

        formPanel.add(cmbType);


        centerPanel.add(
                formPanel,
                BorderLayout.NORTH
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
                new JButton("Add Client");

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


        centerPanel.add(
                buttonPanel,
                BorderLayout.CENTER
        );


        // =========================
        // TABLE
        // =========================

        String[] columns = {

                "ID",
                "Name",
                "Email",
                "Phone",
                "Address",
                "Client Type"

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
                        "Client List"
                )
        );


        centerPanel.add(
                scrollPane,
                BorderLayout.SOUTH
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
                e -> addClient()
        );


        btnSearch.addActionListener(
                e -> searchClient()
        );


        btnUpdate.addActionListener(
                e -> updateClient()
        );


        btnDelete.addActionListener(
                e -> deleteClient()
        );


        btnLoad.addActionListener(
                e -> loadClients()
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


                            txtEmail.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    2
                                            )
                                            .toString()
                            );


                            txtPhone.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    3
                                            )
                                            .toString()
                            );


                            txtAddress.setText(
                                    tableModel
                                            .getValueAt(
                                                    row,
                                                    4
                                            )
                                            .toString()
                            );


                            cmbType.setSelectedItem(
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
    // ADD CLIENT
    // =========================

    private void addClient() {

        String name =
                txtName.getText().trim();

        String email =
                txtEmail.getText().trim();

        String phone =
                txtPhone.getText().trim();

        String address =
                txtAddress.getText().trim();

        String type =
                cmbType.getSelectedItem()
                        .toString();


        if (name.isEmpty() ||
                email.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter Client Name and Email."
            );

            return;
        }


        Client client =
                new Client(
                        0,
                        name,
                        email,
                        phone,
                        address,
                        type
                );


        if (clientDAO.addClient(client)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Client added successfully!"
            );

            clearFields();

            loadClients();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to add client."
            );
        }
    }


    // =========================
    // SEARCH CLIENT
    // =========================

    private void searchClient() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Client ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId.getText()
                                    .trim()
                    );


            Client client =
                    clientDAO.getClientById(id);


            if (client != null) {

                txtName.setText(
                        client.getClientName()
                );


                txtEmail.setText(
                        client.getEmail()
                );


                txtPhone.setText(
                        client.getPhone()
                );


                txtAddress.setText(
                        client.getAddress()
                );


                cmbType.setSelectedItem(
                        client.getClientType()
                );

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Client not found."
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Client ID must be a number."
            );
        }
    }


    // =========================
    // UPDATE CLIENT
    // =========================

    private void updateClient() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Client ID."
            );

            return;
        }


        try {

            int id =
                    Integer.parseInt(
                            txtId.getText()
                                    .trim()
                    );


            Client client =
                    new Client(
                            id,
                            txtName.getText().trim(),
                            txtEmail.getText().trim(),
                            txtPhone.getText().trim(),
                            txtAddress.getText().trim(),
                            cmbType.getSelectedItem()
                                    .toString()
                    );


            if (clientDAO.updateClient(client)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Client updated successfully!"
                );

                clearFields();

                loadClients();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Client not found."
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Client ID."
            );
        }
    }


    // =========================
    // DELETE CLIENT
    // =========================

    private void deleteClient() {

        if (txtId.getText()
                .trim()
                .isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter Client ID."
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
                            "Are you sure you want to delete this client?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                    );


            if (confirm ==
                    JOptionPane.YES_OPTION) {


                if (clientDAO.deleteClient(id)) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Client deleted successfully!"
                    );

                    clearFields();

                    loadClients();

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Client not found."
                    );
                }
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Client ID."
            );
        }
    }


    // =========================
    // LOAD ALL CLIENTS
    // =========================

    private void loadClients() {

        tableModel.setRowCount(0);


        List<Client> clients =
                clientDAO.getAllClients();


        for (Client client : clients) {

            tableModel.addRow(
                    new Object[]{

                            client.getClientId(),

                            client.getClientName(),

                            client.getEmail(),

                            client.getPhone(),

                            client.getAddress(),

                            client.getClientType()

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

        txtEmail.setText("");

        txtPhone.setText("");

        txtAddress.setText("");

        cmbType.setSelectedIndex(0);

        table.clearSelection();
    }


    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            ClientManagement window =
                    new ClientManagement();

            window.setVisible(true);

        });
    }
}
