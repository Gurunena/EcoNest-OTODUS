package com.econest.agent.ui;

import com.econest.agent.model.AgentStatus;
import com.econest.agent.model.DeliveryAgent;
import com.econest.agent.model.Vehicle;
import com.econest.agent.model.VehicleType;
import com.econest.agent.service.DeliveryAgentService;
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
import java.util.concurrent.ExecutionException;

/**
 * Task 4 screen: add, update, remove and search delivery agents and their
 * vehicles.
 *
 * Built on the shared BaseModulePanel so it looks like every other screen:
 * the agent list on the left, the selected agent's details on the right.
 * Database calls run on a SwingWorker so the window never freezes.
 */
public class DeliveryAgentPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {"ID", "Name", "Phone", "Vehicle", "Type", "Status"};
    private static final int COL_STATUS = 5;

    // --- form fields ------------------------------------------------------
    private JTextField txtName;
    private JTextField txtNic;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JComboBox<AgentStatus> cmbStatus;
    private JTextField txtVehicleReg;
    private JComboBox<VehicleType> cmbVehicleType;
    private JTextField txtMaxLoad;

    private final DeliveryAgentService service = new DeliveryAgentService();
    private final InputValidator validator = new InputValidator();

    /** The agents currently shown in the table, so a selected row can be turned back into an agent. */
    private final List<DeliveryAgent> rows = new ArrayList<>();

    /** True until the first load finishes, so a missing database doesn't pop up a dialog at start-up. */
    private boolean initialLoad = true;

    public DeliveryAgentPanel() {
        super("Delivery Agents", COLUMNS);

        setEmptyState("No delivery agents found", "Try a different search, or add an agent with the form.");
        btnAdd.setText("Add agent");
        TableStyler.colourStatusColumn(tblData, COL_STATUS);

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedAgent();
            }
        });

        initPanel();   // must be the last line (see UI_GUIDE.md)
    }

    // =====================================================================
    // The form
    // =====================================================================

    @Override
    protected void buildForm(JPanel form) {
        txtName = FormFields.textField("e.g. Kasun Rathnayake");
        txtNic = FormFields.textField("e.g. 199012345678");
        txtPhone = FormFields.textField("e.g. 0714567890");
        txtEmail = FormFields.textField("e.g. kasun@econest.lk");
        cmbStatus = FormFields.comboBox(AgentStatus.values());
        txtVehicleReg = FormFields.textField("e.g. WP-CAB-1234");
        cmbVehicleType = FormFields.comboBox(VehicleType.values());
        txtMaxLoad = FormFields.textField("e.g. 300");

        form.add(FormFields.heading("Agent details"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Full name", txtName));
        form.add(FormFields.row("NIC", txtNic));
        form.add(FormFields.row("Phone", txtPhone));
        form.add(FormFields.row("Email", txtEmail));
        form.add(FormFields.row("Status", cmbStatus));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));

        form.add(FormFields.heading("Vehicle"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Registration no.", txtVehicleReg));
        form.add(FormFields.row("Vehicle type", cmbVehicleType));
        form.add(FormFields.row("Max load (kg)", txtMaxLoad));
    }

    // =====================================================================
    // Loading the table
    // =====================================================================

    @Override
    protected void loadTableData() {
        loadAgents(txtSearch.getText().trim());
    }

    @Override
    protected void onSearch(String query) {
        loadAgents(query);
    }

    private void loadAgents(String query) {
        new SwingWorker<List<DeliveryAgent>, Void>() {
            @Override
            protected List<DeliveryAgent> doInBackground() throws Exception {
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
                        setStatus("Could not load delivery agents. Check db.properties and that MySQL is running.");
                        System.err.println("DeliveryAgentPanel: could not load agents - " + ex.getCause());
                    } else {
                        fail("load the delivery agents", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void showRows(List<DeliveryAgent> agents, String query) {
        rows.clear();
        rows.addAll(agents);

        Object[][] data = new Object[agents.size()][];
        for (int i = 0; i < agents.size(); i++) {
            DeliveryAgent a = agents.get(i);
            Vehicle v = a.getVehicle();
            data[i] = new Object[]{
                    a.getAgentId(),
                    a.getName(),
                    a.getPhone(),
                    v == null ? "-" : v.getRegistrationNumber(),
                    v == null || v.getType() == null ? "-" : v.getType().getLabel(),
                    a.getStatus() == null ? "-" : a.getStatus().getLabel()
            };
        }
        setRows(data);
        setStatus(query.isEmpty()
                ? agents.size() + " delivery agent(s)."
                : agents.size() + " agent(s) match \"" + query + "\".");
    }

    // =====================================================================
    // Buttons
    // =====================================================================

    @Override
    protected void onAdd() {
        DeliveryAgent agent = readForm(0);
        if (agent == null) {
            return;   // the problems are already shown next to the fields
        }
        runWrite("add the agent", () -> service.addAgent(agent), () -> {
            setStatus("Added " + agent.getName() + " as agent #" + agent.getAgentId() + ".");
            clearForm();
        });
    }

    @Override
    protected void onUpdate() {
        DeliveryAgent selected = selectedAgent();
        if (selected == null) {
            Dialogs.error(this, "Select the agent you want to update in the table first.");
            return;
        }
        DeliveryAgent agent = readForm(selected.getAgentId());
        if (agent == null) {
            return;
        }
        runWrite("update the agent", () -> {
            if (!service.updateAgent(agent)) {
                throw new IllegalArgumentException("This agent no longer exists. They may have been deleted.");
            }
        }, () -> setStatus("Updated agent #" + agent.getAgentId() + "."));
    }

    @Override
    protected void onDelete() {
        // BaseModulePanel has already asked "are you sure?"
        DeliveryAgent selected = selectedAgent();
        if (selected == null) {
            return;
        }
        runWrite("delete the agent", () -> service.deleteAgent(selected.getAgentId()), () -> {
            setStatus("Deleted " + selected.getName() + " and their vehicle.");
            clearForm();
        });
    }

    @Override
    protected void onClear() {
        clearForm();
        super.onClear();   // also clears the search box and the table selection
    }

    // =====================================================================
    // Form <-> DeliveryAgent
    // =====================================================================

    /**
     * Checks the required fields (showing problems in red under them) and builds
     * a DeliveryAgent. DeliveryAgentService.validate() then checks the NIC, phone
     * and email formats. Returns null if something is wrong.
     */
    private DeliveryAgent readForm(int agentId) {
        validator.reset();
        String name = validator.required(txtName, "Full name");
        String nic = validator.required(txtNic, "NIC");
        String phone = validator.required(txtPhone, "Phone");
        String email = validator.email(txtEmail, "Email");
        String vehicleReg = validator.required(txtVehicleReg, "Registration no.");
        Integer maxLoad = validator.integer(txtMaxLoad, "Max load");
        if (!validator.isValid()) {
            return null;
        }
        Vehicle vehicle = new Vehicle(vehicleReg.toUpperCase(),
                (VehicleType) cmbVehicleType.getSelectedItem(), maxLoad);
        return new DeliveryAgent(agentId, name, nic, phone.replaceAll("[\\s-]", ""), email, vehicle,
                (AgentStatus) cmbStatus.getSelectedItem());
    }

    private DeliveryAgent selectedAgent() {
        int row = selectedModelRow();
        return row < 0 || row >= rows.size() ? null : rows.get(row);
    }

    private void showSelectedAgent() {
        DeliveryAgent a = selectedAgent();
        if (a == null) {
            return;
        }
        clearErrors();
        txtName.setText(a.getName());
        txtNic.setText(a.getNic() == null ? "" : a.getNic());
        txtPhone.setText(a.getPhone());
        txtEmail.setText(a.getEmail() == null ? "" : a.getEmail());
        if (a.getStatus() != null) {
            cmbStatus.setSelectedItem(a.getStatus());
        }
        Vehicle v = a.getVehicle();
        txtVehicleReg.setText(v == null ? "" : v.getRegistrationNumber());
        if (v != null && v.getType() != null) {
            cmbVehicleType.setSelectedItem(v.getType());
        }
        txtMaxLoad.setText(v == null ? "" : String.valueOf(v.getMaxLoadKg()));
    }

    private void clearForm() {
        tblData.clearSelection();
        clearErrors();
        txtName.setText("");
        txtNic.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        cmbStatus.setSelectedIndex(0);
        txtVehicleReg.setText("");
        cmbVehicleType.setSelectedIndex(0);
        txtMaxLoad.setText("");
    }

    private void clearErrors() {
        for (JTextField field : new JTextField[]{txtName, txtNic, txtPhone, txtEmail, txtVehicleReg, txtMaxLoad}) {
            validator.clear(field);
        }
    }

    // =====================================================================
    // Talking to the database
    // =====================================================================

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
                    loadAgents(txtSearch.getText().trim());
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
            Dialogs.error(this, action.startsWith("delete")
                    ? "This agent has deliveries on record, so they can't be deleted. Set them to Off duty instead."
                    : "Another agent already has this licence/NIC or vehicle registration number.");
            setStatus("Nothing changed.");
            return;
        }
        System.err.println("DeliveryAgentPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        String detail = (cause == null || cause.getMessage() == null) ? "Unknown error." : cause.getMessage();
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Delivery agent operation failed.");
    }

    private void setBusy(boolean busy) {
        btnAdd.setEnabled(!busy);
        btnUpdate.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
    }
}
