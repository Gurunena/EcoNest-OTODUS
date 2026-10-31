package com.econest.client.ui;

import com.econest.client.model.Client;
import com.econest.client.model.ClientType;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import com.econest.client.service.ClientService;
import com.econest.ui.BaseModulePanel;
import com.econest.ui.Theme;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.InputValidator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Task 2 - Manage Clients screen (add, update, remove and search household
 * and office clients and their contact details).
 *
 * Built on the team's BaseModulePanel: the title, search box, table, form card
 * and Add / Update / Delete / Clear buttons come from there. This class fills
 * in the form, the table and what each button does.
 *
 * Database calls run on a SwingWorker (a background thread) so the window
 * never freezes while MySQL is answering.
 */
public class ClientPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {"ID", "Name", "Type", "Phone", "City", "Email"};
    private static final int COL_ID = 0;

    // --- form fields ------------------------------------------------------
    private JComboBox<ClientType> cmbType;
    private JLabel lblNameCaption;
    private JTextField txtName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JTextField txtAddressLine1;
    private JTextField txtAddressLine2;
    private JTextField txtCity;
    private JTextField txtPostalCode;

    // Household-only fields
    private JPanel pnlHousehold;
    private JTextField txtHouseholdSize;
    private JComboBox<String> cmbDeliverySlot;

    // Office-only fields
    private JPanel pnlOffice;
    private JTextField txtContactPerson;
    private JTextField txtCompanyRegNo;
    private JTextField txtDepartment;

    private final ClientService service;
    private final InputValidator validator = new InputValidator();

    /** The clients currently shown in the table, so a selected row can be turned back into a Client. */
    private final List<Client> rows = new ArrayList<>();

    /** True until the first load finishes, so a missing database doesn't pop up a dialog at start-up. */
    private boolean initialLoad = true;

    public ClientPanel() {
        this(new ClientService());
    }

    /** Lets a test pass in its own service. */
    public ClientPanel(ClientService service) {
        super("Clients", COLUMNS);
        this.service = service;

        setEmptyState("No clients found", "Try a different search, or add a client with the form.");
        btnAdd.setText("Add client");

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedClient();
            }
        });

        initPanel();   // must be the last line (see UI_GUIDE.md)
    }

    // =====================================================================
    // The form
    // =====================================================================

    @Override
    protected void buildForm(JPanel form) {
        cmbType = FormFields.comboBox(ClientType.values());
        txtName = FormFields.textField("e.g. Amara Silva");
        txtEmail = FormFields.textField("e.g. amara@gmail.com");
        txtPhone = FormFields.textField("e.g. 0712345678");
        txtAddressLine1 = FormFields.textField("e.g. 42 Lake Road");
        txtAddressLine2 = FormFields.textField("e.g. Apt 3B");
        txtCity = FormFields.textField("e.g. Colombo");
        txtPostalCode = FormFields.textField("e.g. 00500");

        txtHouseholdSize = FormFields.textField("e.g. 4");
        cmbDeliverySlot = FormFields.comboBox(HouseholdClient.DELIVERY_SLOTS);

        txtContactPerson = FormFields.textField("e.g. Rashmi Gunawardena");
        txtCompanyRegNo = FormFields.textField("e.g. PV00123456");
        txtDepartment = FormFields.textField("e.g. Facilities");

        JPanel pnlNameRow = FormFields.row("Full name", txtName);
        lblNameCaption = (JLabel) pnlNameRow.getComponent(0);   // the caption, so we can rename it for offices

        form.add(FormFields.heading("Client details"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Client type", cmbType));
        form.add(pnlNameRow);
        form.add(FormFields.row("Email", txtEmail));
        form.add(FormFields.row("Phone", txtPhone));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));

        form.add(FormFields.heading("Address"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        form.add(FormFields.row("Address line 1", txtAddressLine1));
        form.add(FormFields.row("Address line 2 (optional)", txtAddressLine2));
        form.add(FormFields.row("City", txtCity));
        form.add(FormFields.row("Postal code (optional)", txtPostalCode));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));

        // Only one of these two groups is visible at a time, depending on the client type
        pnlHousehold = group();
        pnlHousehold.add(FormFields.heading("Household details"));
        pnlHousehold.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        pnlHousehold.add(FormFields.row("Household size (optional)", txtHouseholdSize));
        pnlHousehold.add(FormFields.row("Preferred delivery slot", cmbDeliverySlot));

        pnlOffice = group();
        pnlOffice.add(FormFields.heading("Office details"));
        pnlOffice.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        pnlOffice.add(FormFields.row("Contact person", txtContactPerson));
        pnlOffice.add(FormFields.row("Company registration no. (optional)", txtCompanyRegNo));
        pnlOffice.add(FormFields.row("Department (optional)", txtDepartment));

        form.add(pnlHousehold);
        form.add(pnlOffice);

        cmbType.addActionListener(e -> showFieldsForType());
        showFieldsForType();
    }

    /** A transparent vertical panel for a group of form rows. */
    private JPanel group() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);
        pnl.setAlignmentX(LEFT_ALIGNMENT);
        return pnl;
    }

    /** Shows the household or office fields, and renames the name caption to match. */
    private void showFieldsForType() {
        boolean office = selectedType() == ClientType.OFFICE;
        lblNameCaption.setText(office ? "Company name" : "Full name");
        pnlHousehold.setVisible(!office);
        pnlOffice.setVisible(office);
        pnlForm.revalidate();
        pnlForm.repaint();
    }

    // =====================================================================
    // Loading the table
    // =====================================================================

    @Override
    protected void loadTableData() {
        loadClients(txtSearch.getText().trim());
    }

    @Override
    protected void onSearch(String query) {
        loadClients(query);
    }

    /** Reads clients on a background thread, then fills the table. */
    private void loadClients(String query) {
        new SwingWorker<List<Client>, Void>() {
            @Override
            protected List<Client> doInBackground() throws Exception {
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
                        setStatus("Could not load clients. Check db.properties and that MySQL is running.");
                        System.err.println("ClientPanel: could not load clients - " + ex.getCause());
                    } else {
                        fail("load the clients", ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void showRows(List<Client> clients, String query) {
        rows.clear();
        rows.addAll(clients);

        Object[][] data = new Object[clients.size()][];
        for (int i = 0; i < clients.size(); i++) {
            Client c = clients.get(i);
            data[i] = new Object[]{
                    c.getClientId(),
                    c.getName(),
                    c.getClientType().getLabel(),
                    c.getPhone(),
                    c.getContactDetails().getCity(),
                    c.getEmail()};
        }
        setRows(data);

        String scope = query.isEmpty() ? "" : " matching \"" + query + "\"";
        setStatus(clients.size() + " client(s)" + scope + ".");
    }

    // =====================================================================
    // Buttons
    // =====================================================================

    @Override
    protected void onAdd() {
        Client client = readForm(0);
        if (client == null) {
            return;   // the problems are already shown next to the fields
        }
        runWrite("add the client", () -> service.addClient(client), () -> {
            setStatus("Added " + client.getName() + " as client #" + client.getClientId() + ".");
            clearForm();
        });
    }

    @Override
    protected void onUpdate() {
        Client selected = selectedClient();
        if (selected == null) {
            Dialogs.error(this, "Select the client you want to update in the table first.");
            return;
        }
        Client client = readForm(selected.getClientId());
        if (client == null) {
            return;
        }
        runWrite("update the client", () -> service.updateClient(client),
                () -> setStatus("Updated client #" + client.getClientId() + "."));
    }

    @Override
    protected void onDelete() {
        // BaseModulePanel has already asked "are you sure?"
        Client selected = selectedClient();
        if (selected == null) {
            return;
        }
        runWrite("delete the client", () -> service.deleteClient(selected.getClientId()), () -> {
            setStatus("Deleted client #" + selected.getClientId() + ".");
            clearForm();
        });
    }

    @Override
    protected void onClear() {
        clearForm();
        super.onClear();   // also clears the search box and the table selection
    }

    // =====================================================================
    // Form <-> Client
    // =====================================================================

    /**
     * Checks the fields (showing any problem in red under the field) and builds a
     * HouseholdClient or OfficeClient from them. Returns null if something is wrong.
     */
    private Client readForm(int clientId) {
        validator.reset();
        String name = validator.required(txtName, lblNameCaption.getText());
        if (name != null && name.length() < 2) {
            validator.fail(txtName, "At least 2 letters");
        }
        String email = validator.email(txtEmail, "Email");
        String phone = validator.required(txtPhone, "Phone");
        if (phone != null && !phone.replaceAll("[\\s-]", "").matches("0\\d{9}")) {
            validator.fail(txtPhone, "10 digits starting with 0, e.g. 0712345678");
        }
        String addressLine1 = validator.required(txtAddressLine1, "Address line 1");
        String city = validator.required(txtCity, "City");
        String postalCode = txtPostalCode.getText().trim();
        if (!postalCode.isEmpty() && !postalCode.matches("\\d{5}")) {
            validator.fail(txtPostalCode, "5 digits, e.g. 00500");
        } else {
            validator.clear(txtPostalCode);
        }

        Integer householdSize = null;
        String contactPerson = null;
        if (selectedType() == ClientType.HOUSEHOLD) {
            householdSize = readOptionalNumber(txtHouseholdSize, "Household size");
        } else {
            contactPerson = validator.required(txtContactPerson, "Contact person");
        }

        if (!validator.isValid()) {
            setStatus("Please fix the highlighted fields.");
            return null;
        }

        ContactDetails contact = new ContactDetails(email, phone, addressLine1,
                txtAddressLine2.getText(), city, postalCode);
        if (selectedType() == ClientType.OFFICE) {
            return new OfficeClient(clientId, name, contact,
                    txtCompanyRegNo.getText(), contactPerson, txtDepartment.getText());
        }
        return new HouseholdClient(clientId, name, contact, householdSize,
                (String) cmbDeliverySlot.getSelectedItem());
    }

    /** An optional whole number: empty means null; anything else must be a number. */
    private Integer readOptionalNumber(JTextField txt, String fieldName) {
        String text = txt.getText().trim();
        if (text.isEmpty()) {
            validator.clear(txt);
            return null;
        }
        return validator.integer(txt, fieldName);
    }

    /** Copies the selected client into the form. */
    private void showSelectedClient() {
        int viewRow = tblData.getSelectedRow();
        if (viewRow < 0 || viewRow >= tblData.getRowCount()) {
            return;   // the table is being reloaded, so there's nothing to show yet
        }
        Client client = selectedClient();
        if (client == null) {
            return;
        }
        validator.clearAll(allFields());
        ContactDetails contact = client.getContactDetails();
        cmbType.setSelectedItem(client.getClientType());
        txtName.setText(client.getName());
        txtEmail.setText(contact.getEmail());
        txtPhone.setText(contact.getPhone());
        txtAddressLine1.setText(contact.getAddressLine1());
        txtAddressLine2.setText(nullToEmpty(contact.getAddressLine2()));
        txtCity.setText(contact.getCity());
        txtPostalCode.setText(nullToEmpty(contact.getPostalCode()));

        if (client instanceof HouseholdClient) {
            HouseholdClient household = (HouseholdClient) client;
            txtHouseholdSize.setText(household.getHouseholdSize() == null
                    ? "" : String.valueOf(household.getHouseholdSize()));
            cmbDeliverySlot.setSelectedItem(household.getPreferredDeliverySlot());
        } else {
            OfficeClient office = (OfficeClient) client;
            txtContactPerson.setText(office.getContactPerson());
            txtCompanyRegNo.setText(nullToEmpty(office.getCompanyRegistrationNo()));
            txtDepartment.setText(nullToEmpty(office.getDepartment()));
        }
        setStatus("Selected client #" + client.getClientId() + ". Edit the form and press Update.");
    }

    /** The Client behind the selected table row (found by id, so it works while searching). */
    private Client selectedClient() {
        Object id = selectedValue(COL_ID);
        if (id == null) {
            return null;
        }
        int clientId = ((Number) id).intValue();
        for (Client c : rows) {
            if (c.getClientId() == clientId) {
                return c;
            }
        }
        return null;
    }

    private void clearForm() {
        for (JTextField txt : new JTextField[]{txtName, txtEmail, txtPhone, txtAddressLine1,
                txtAddressLine2, txtCity, txtPostalCode, txtHouseholdSize,
                txtContactPerson, txtCompanyRegNo, txtDepartment}) {
            txt.setText("");
        }
        cmbType.setSelectedItem(ClientType.HOUSEHOLD);
        cmbDeliverySlot.setSelectedIndex(0);
        validator.clearAll(allFields());
        tblData.clearSelection();
    }

    private javax.swing.JComponent[] allFields() {
        return new javax.swing.JComponent[]{txtName, txtEmail, txtPhone, txtAddressLine1,
                txtAddressLine2, txtCity, txtPostalCode, txtHouseholdSize, txtContactPerson,
                txtCompanyRegNo, txtDepartment};
    }

    private ClientType selectedType() {
        return (ClientType) cmbType.getSelectedItem();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // =====================================================================
    // Running database writes in the background
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
                    loadClients(txtSearch.getText().trim());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    fail(action, ex.getCause());
                }
            }
        }.execute();
    }

    /**
     * Shows a problem to the user. A rule broken in ClientService already has a
     * friendly message; a database error gets a short explanation instead.
     */
    private void fail(String action, Throwable cause) {
        if (cause instanceof IllegalArgumentException) {
            Dialogs.error(this, cause.getMessage());
            setStatus("Nothing changed.");
            return;
        }
        System.err.println("ClientPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        String detail = (cause == null || cause.getMessage() == null) ? "Unknown error." : cause.getMessage();
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Client operation failed.");
    }

    private void setBusy(boolean busy) {
        btnAdd.setEnabled(!busy);
        btnUpdate.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
    }
}
