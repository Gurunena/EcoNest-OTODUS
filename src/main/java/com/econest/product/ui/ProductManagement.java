
package com.econest.agent.ui;

import com.econest.agent.model.AgentStatus;
import com.econest.agent.model.DeliveryAgent;
import com.econest.agent.model.Vehicle;
import com.econest.agent.model.VehicleType;
import com.econest.agent.service.DeliveryAgentService;
import com.econest.ui.BaseModulePanel;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Task 4: Manage delivery agents and vehicles.
 */
public class DeliveryAgentPanel extends BaseModulePanel {

    private final DeliveryAgentService service =
            new DeliveryAgentService();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtNic = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtVehicleReg = new JTextField();

    private final JComboBox<VehicleType> cmbVehicleType =
            new JComboBox<>(VehicleType.values());

    private final JSpinner spnMaxLoad =
            new JSpinner(
                    new SpinnerNumberModel(100, 1, 20000, 10)
            );

    private final JComboBox<AgentStatus> cmbStatus =
            new JComboBox<>(AgentStatus.values());

    public DeliveryAgentPanel() {

        super("Delivery Agent Management",
                new String[]{
                        "ID", "Name", "NIC", "Phone", "Email",
                        "Vehicle Reg.", "Vehicle Type",
                        "Max Load (kg)", "Status"
                });

        txtId.setEditable(false);
        txtId.setToolTipText("Assigned automatically");

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

        addField(form, "Agent ID (auto):", txtId);
        addField(form, "Full Name:", txtName);
        addField(form, "NIC:", txtNic);
        addField(form, "Phone:", txtPhone);
        addField(form, "Email:", txtEmail);
        addField(form, "Status:", cmbStatus);
        addField(form, "Vehicle Registration:", txtVehicleReg);
        addField(form, "Vehicle Type:", cmbVehicleType);
        addField(form, "Max Load (kg):", spnMaxLoad);
    }

    private void addField(JPanel form, String label,
                          JComponent component) {

        JLabel lbl = new JLabel(label);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 65)
        );

        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lbl);
        form.add(Box.createVerticalStrut(5));
        form.add(component);
        form.add(Box.createVerticalStrut(10));
    }

    @Override
    protected void loadTableData() {
        loadAgents(null);
    }

    @Override
    protected void onAdd() {

        try {
            DeliveryAgent agent = readForm(0);

            service.addAgent(agent);

            JOptionPane.showMessageDialog(
                    this,
                    "Agent added successfully. ID: "
                            + agent.getAgentId()
            );

            onClear();
            loadTableData();

        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());

        } catch (SQLException ex) {
            showError("Could not add the agent.", ex);
        }
    }

    @Override
    protected void onUpdate() {

        Object idValue = selectedValue(0);

        if (idValue == null) {
            showWarning("Select an agent in the table first.");
            return;
        }

        try {
            int id = Integer.parseInt(idValue.toString());

            DeliveryAgent agent = readForm(id);

            if (service.updateAgent(agent)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Agent updated successfully."
                );

                onClear();
                loadTableData();

            } else {
                showWarning("Agent not found.");
            }

        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());

        } catch (SQLException ex) {
            showError("Could not update the agent.", ex);
        }
    }

    @Override
    protected void onDelete() {

        Object idValue = selectedValue(0);

        if (idValue == null) {
            showWarning("Select an agent first.");
            return;
        }

        try {
            int id = Integer.parseInt(idValue.toString());

            if (service.deleteAgent(id)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Agent deleted successfully."
                );

                onClear();
                loadTableData();

            } else {
                showWarning("Agent not found.");
            }

        } catch (SQLException ex) {
            showError(
                    "Could not delete the agent. They may have delivery assignments.",
                    ex
            );
        }
    }

    @Override
    protected void onClear() {

        txtId.setText("");
        txtName.setText("");
        txtNic.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtVehicleReg.setText("");

        cmbVehicleType.setSelectedIndex(0);
        cmbStatus.setSelectedItem(AgentStatus.AVAILABLE);
        spnMaxLoad.setValue(100);

        tblData.clearSelection();

        setStatus("Delivery agent form cleared.");
    }

    @Override
    protected void onSearch(String query) {
        loadAgents(query.isBlank() ? null : query);
    }

    private void loadAgents(String keyword) {

        try {
            List<DeliveryAgent> agents =
                    service.search(keyword);

            Object[][] rows = new Object[agents.size()][9];

            for (int i = 0; i < agents.size(); i++) {

                DeliveryAgent a = agents.get(i);
                Vehicle v = a.getVehicle();

                rows[i] = new Object[]{
                        a.getAgentId(),
                        a.getName(),
                        a.getNic(),
                        a.getPhone(),
                        a.getEmail(),
                        v == null ? "" : v.getRegistrationNumber(),
                        v == null ? "" : v.getType().getLabel(),
                        v == null ? "" : v.getMaxLoadKg(),
                        a.getStatus().getLabel()
                };
            }

            setRows(rows);

        } catch (SQLException ex) {
            showError("Could not load delivery agents.", ex);
        }
    }

    private DeliveryAgent readForm(int id) {

        Vehicle vehicle = new Vehicle(
                txtVehicleReg.getText().trim().toUpperCase(),
                (VehicleType) cmbVehicleType.getSelectedItem(),
                (Integer) spnMaxLoad.getValue()
        );

        return new DeliveryAgent(
                id,
                txtName.getText().trim(),
                txtNic.getText().trim(),
                txtPhone.getText().trim(),
                txtEmail.getText().trim(),
                vehicle,
                (AgentStatus) cmbStatus.getSelectedItem()
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

        txtNic.setText(
                String.valueOf(tableModel.getValueAt(row, 2))
        );

        txtPhone.setText(
                String.valueOf(tableModel.getValueAt(row, 3))
        );

        Object email = tableModel.getValueAt(row, 4);

        txtEmail.setText(
                email == null ? "" : email.toString()
        );

        txtVehicleReg.setText(
                String.valueOf(tableModel.getValueAt(row, 5))
        );

        String type =
                String.valueOf(tableModel.getValueAt(row, 6));

        if (!type.isEmpty()) {
            cmbVehicleType.setSelectedItem(
                    VehicleType.fromLabel(type)
            );
        }

        Object load = tableModel.getValueAt(row, 7);

        if (load instanceof Number) {
            spnMaxLoad.setValue(
                    ((Number) load).intValue()
            );
        }

        cmbStatus.setSelectedItem(
                AgentStatus.fromLabel(
                        String.valueOf(tableModel.getValueAt(row, 8))
                )
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
