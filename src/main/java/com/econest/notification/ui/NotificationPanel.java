package com.econest.notification.ui;

import com.econest.notification.dao.NotificationDao;
import com.econest.notification.dao.NotificationDaoImpl;
import com.econest.notification.model.EmailNotification;
import com.econest.ui.BaseModulePanel;
import com.econest.ui.Theme;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * The Notifications screen: every email EcoNest has sent or tried to send,
 * newest first. This includes the agent emails (Task 9) and the client
 * dispatch emails (Task 8).
 *
 * It's read-only. Click a row to read the whole email on the right.
 * Database calls run on a SwingWorker so the window never freezes.
 */
public class NotificationPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {"ID", "Date", "Type", "Status", "To"};
    private static final int COL_STATUS = 3;

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    // --- details on the right (read-only) --------------------------------
    private JTextField txtType;
    private JTextField txtTo;
    private JTextField txtSubject;
    private JTextField txtStatus;
    private JTextField txtOrder;
    private JTextField txtSentAt;
    private JTextField txtError;
    private JTextArea txtBody;

    private final NotificationDao dao;

    /** The emails currently shown in the table, so a selected row can be turned back into an email. */
    private final List<EmailNotification> rows = new ArrayList<>();

    /** True until the first load finishes, so a missing database doesn't pop up a dialog at start-up. */
    private boolean initialLoad = true;

    public NotificationPanel() {
        this(new NotificationDaoImpl());
    }

    /** Lets a test pass in its own DAO. */
    public NotificationPanel(NotificationDao dao) {
        super("Notifications", COLUMNS);
        this.dao = dao;

        setEmptyState("No emails yet",
                "Emails appear here when an order is assigned to a delivery agent.");
        TableStyler.colourStatusColumn(tblData, COL_STATUS);

        // Nothing to add or delete here: the emails are a history.
        pnlActions.remove(btnAdd);
        pnlActions.remove(btnDelete);
        btnUpdate.setText("Refresh");

        tblData.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedEmail();
            }
        });

        initPanel();   // must be the last line (see UI_GUIDE.md)
    }

    // =====================================================================
    // The details card
    // =====================================================================

    @Override
    protected void buildForm(JPanel form) {
        form.add(FormFields.heading("Email details"));
        form.add(Box.createVerticalStrut(Theme.GAP_SMALL));

        txtType = readOnlyField();
        txtTo = readOnlyField();
        txtSubject = readOnlyField();
        txtStatus = readOnlyField();
        txtOrder = readOnlyField();
        txtSentAt = readOnlyField();
        txtError = readOnlyField();

        form.add(FormFields.row("Type", txtType));
        form.add(FormFields.row("To", txtTo));
        form.add(FormFields.row("Subject", txtSubject));
        form.add(FormFields.row("Status", txtStatus));
        form.add(FormFields.row("Order", txtOrder));
        form.add(FormFields.row("Sent at", txtSentAt));
        form.add(FormFields.row("Problem (if it failed)", txtError));

        txtBody = new JTextArea(12, 20);
        txtBody.setEditable(false);
        txtBody.setLineWrap(true);
        txtBody.setWrapStyleWord(true);
        txtBody.setFont(Theme.FONT_SMALL);
        txtBody.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scrBody = new JScrollPane(txtBody);
        scrBody.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrBody.setPreferredSize(new Dimension(280, 260));
        scrBody.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        form.add(FormFields.label("Message"));
        form.add(Box.createVerticalStrut(4));
        form.add(scrBody);
    }

    private static JTextField readOnlyField() {
        JTextField field = FormFields.textField();
        field.setEditable(false);
        return field;
    }

    // =====================================================================
    // Loading the table
    // =====================================================================

    @Override
    protected void loadTableData() {
        loadEmails(txtSearch.getText().trim());
    }

    @Override
    protected void onSearch(String query) {
        loadEmails(query);
    }

    /** The Refresh button (the base class calls it Update). */
    @Override
    protected void onUpdate() {
        loadEmails(txtSearch.getText().trim());
    }

    @Override
    protected void onClear() {
        super.onClear();   // clears the selection and the search box
        clearDetails();
    }

    private void loadEmails(String query) {
        new SwingWorker<List<EmailNotification>, Void>() {
            @Override
            protected List<EmailNotification> doInBackground() throws Exception {
                return query.isEmpty() ? dao.findAll() : dao.search(query);
            }

            @Override
            protected void done() {
                boolean quiet = initialLoad;
                initialLoad = false;
                try {
                    showRows(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    setStatus("Could not load the notifications. Check db.properties and that MySQL is running.");
                    if (!quiet) {
                        System.err.println("NotificationPanel: could not load notifications - " + ex.getCause());
                    }
                }
            }
        }.execute();
    }

    private void showRows(List<EmailNotification> emails) {
        rows.clear();
        rows.addAll(emails);

        Object[][] data = new Object[emails.size()][];
        int sent = 0;
        for (int i = 0; i < emails.size(); i++) {
            EmailNotification n = emails.get(i);
            data[i] = new Object[]{
                    n.getNotificationId(),
                    format(n.getSentAt() != null ? n.getSentAt() : n.getCreatedAt()),
                    n.getType().getLabel(),
                    n.getStatus(),
                    n.getRecipientEmail()
            };
            if (n.isSent()) {
                sent++;
            }
        }
        setRows(data);
        clearDetails();
        setStatus(emails.size() + " email(s): " + sent + " sent, " + (emails.size() - sent) + " not sent.");
    }

    // =====================================================================
    // Showing one email
    // =====================================================================

    private void showSelectedEmail() {
        int row = selectedModelRow();
        if (row < 0 || row >= rows.size()) {
            return;
        }
        EmailNotification n = rows.get(row);
        txtType.setText(n.getType().getLabel());
        txtTo.setText(n.getRecipientEmail());
        txtSubject.setText(n.getSubject());
        txtSubject.setCaretPosition(0);
        txtStatus.setText(n.getStatus());
        txtOrder.setText(n.getOrderId() == null ? "-" : "#" + n.getOrderId());
        txtSentAt.setText(n.getSentAt() == null ? "Not sent" : format(n.getSentAt()));
        txtError.setText(n.getErrorMessage() == null ? "" : n.getErrorMessage());
        txtBody.setText(n.getBody() == null ? "" : n.getBody());
        txtBody.setCaretPosition(0);   // scroll back to the top of the message
    }

    private void clearDetails() {
        for (JTextField field : new JTextField[]{txtType, txtTo, txtSubject, txtStatus, txtOrder, txtSentAt, txtError}) {
            field.setText("");
        }
        txtBody.setText("");
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME);
    }
}
