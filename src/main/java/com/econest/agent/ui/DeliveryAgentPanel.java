package com.econest.agent.ui;

import com.econest.agent.model.AgentStatus;
import com.econest.agent.model.DeliveryAgent;
import com.econest.agent.model.Vehicle;
import com.econest.agent.model.VehicleType;
import com.econest.agent.service.DeliveryAgentService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

/** Task 4 screen: add, update, remove and search delivery agents and their vehicles. */
public class DeliveryAgentPanel extends JPanel {

    private final DeliveryAgentService service = new DeliveryAgentService();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtNic = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtVehicleReg = new JTextField();
    private final JComboBox<VehicleType> cmbVehicleType = new JComboBox<>(VehicleType.values());
    private final JSpinner spnMaxLoad = new JSpinner(new SpinnerNumberModel(100, 1, 20_000, 10));
    private final JComboBox<AgentStatus> cmbStatus = new JComboBox<>(AgentStatus.values());
    private final JTextField txtSearch = new JTextField(15);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "NIC", "Phone", "Email", "Vehicle Reg.", "Vehicle Type", "Max Load (kg)", "Status"},
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public DeliveryAgentPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtId.setEditable(false);
        txtId.setToolTipText("Assigned automatically when the agent is added");

        JPanel form = new JPanel(new GridLayout(5, 4, 10, 10));
        form.add(new JLabel("Agent ID (auto):"));
        form.add(txtId);
        form.add(new JLabel("Full Name:"));
        form.add(txtName);
        form.add(new JLabel("NIC:"));
        form.add(txtNic);
        form.add(new JLabel("Phone:"));
        form.add(txtPhone);
        form.add(new JLabel("Email:"));
        form.add(txtEmail);
        form.add(new JLabel("Status:"));
        form.add(cmbStatus);
        form.add(new JLabel("Vehicle Reg. No.:"));
        form.add(txtVehicleReg);
        form.add(new JLabel("Vehicle Type:"));
        form.add(cmbVehicleType);
        form.add(new JLabel("Max Load (kg):"));
        form.add(spnMaxLoad);

        JButton btnAdd = new JButton("Add Agent");
        JButton btnUpdate = new JButton("Update Agent");
        JButton btnDelete = new JButton("Delete Agent");
        JButton btnClear = new JButton("Clear");
        JButton btnSearch = new JButton("Search");
        JButton btnLoad = new JButton("Load All");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.add(new JLabel("Search name / NIC / vehicle:"));
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

        btnAdd.addActionListener(e -> addAgent());
        btnUpdate.addActionListener(e -> updateAgent());
        btnDelete.addActionListener(e -> deleteAgent());
        btnClear.addActionListener(e -> clearFields());
        btnSearch.addActionListener(e -> loadAgents(txtSearch.getText()));
        btnLoad.addActionListener(e -> {
            txtSearch.setText("");
            loadAgents(null);
        });
        txtSearch.addActionListener(e -> loadAgents(txtSearch.getText()));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedRow();
            }
        });

        loadAgents(null);
    }

    // ---------------------------------------------------------------- actions

    private void addAgent() {
        try {
            DeliveryAgent agent = readForm(0);
            service.addAgent(agent);
            info("Agent added (ID " + agent.getAgentId() + ").");
            clearFields();
            loadAgents(null);
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        } catch (SQLException ex) {
            error("Could not add the agent. Is the NIC or vehicle registration already registered?", ex);
        }
    }

    private void updateAgent() {
        if (txtId.getText().isBlank()) {
            warn("Select an agent in the table first.");
            return;
        }
        try {
            DeliveryAgent agent = readForm(Integer.parseInt(txtId.getText()));
            if (service.updateAgent(agent)) {
                info("Agent updated.");
                loadAgents(null);
            } else {
                warn("Agent not found - they may have been removed.");
            }
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        } catch (SQLException ex) {
            error("Could not update the agent. Is the NIC or vehicle registration already registered?", ex);
        }
    }

    private void deleteAgent() {
        if (txtId.getText().isBlank()) {
            warn("Select an agent in the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete agent '" + txtName.getText() + "' and their vehicle?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (service.deleteAgent(Integer.parseInt(txtId.getText()))) {
                info("Agent deleted.");
                clearFields();
                loadAgents(null);
            } else {
                warn("Agent not found.");
            }
        } catch (SQLException ex) {
            error("Could not delete the agent. They may already have delivery assignments - "
                    + "set their status to Off duty instead.", ex);
        }
    }

    private void loadAgents(String keyword) {
        try {
            List<DeliveryAgent> agents = service.search(keyword);
            tableModel.setRowCount(0);
            for (DeliveryAgent a : agents) {
                Vehicle v = a.getVehicle();
                tableModel.addRow(new Object[]{a.getAgentId(), a.getName(), a.getNic(), a.getPhone(), a.getEmail(),
                        v == null ? "" : v.getRegistrationNumber(),
                        v == null ? "" : v.getType().getLabel(),
                        v == null ? "" : v.getMaxLoadKg(),
                        a.getStatus().getLabel()});
            }
        } catch (SQLException ex) {
            error("Could not load agents.", ex);
        }
    }

    // ---------------------------------------------------------------- helpers

    private DeliveryAgent readForm(int id) {
        Vehicle vehicle = new Vehicle(txtVehicleReg.getText().trim().toUpperCase(),
                (VehicleType) cmbVehicleType.getSelectedItem(),
                (Integer) spnMaxLoad.getValue());
        return new DeliveryAgent(id, txtName.getText().trim(), txtNic.getText().trim(),
                txtPhone.getText().trim(), txtEmail.getText().trim(), vehicle,
                (AgentStatus) cmbStatus.getSelectedItem());
    }

    private void showSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        txtId.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        txtName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        txtNic.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        txtPhone.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        Object email = tableModel.getValueAt(row, 4);
        txtEmail.setText(email == null ? "" : email.toString());
        txtVehicleReg.setText(String.valueOf(tableModel.getValueAt(row, 5)));
        String type = String.valueOf(tableModel.getValueAt(row, 6));
        if (!type.isEmpty()) {
            cmbVehicleType.setSelectedItem(VehicleType.fromLabel(type));
        }
        Object load = tableModel.getValueAt(row, 7);
        if (load instanceof Integer) {
            spnMaxLoad.setValue(load);
        }
        cmbStatus.setSelectedItem(AgentStatus.fromLabel(String.valueOf(tableModel.getValueAt(row, 8))));
    }

    private void clearFields() {
        table.clearSelection();
        txtId.setText("");
        txtName.setText("");
        txtNic.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtVehicleReg.setText("");
        cmbVehicleType.setSelectedIndex(0);
        cmbStatus.setSelectedItem(AgentStatus.AVAILABLE);
        spnMaxLoad.setValue(100);
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
