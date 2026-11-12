package com.econest.order.ui;

import com.econest.ui.Theme;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Card;
import com.econest.ui.components.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.datatransfer.StringSelection;
import java.awt.print.PrinterException;

/**
 * A read-only order summary: what is on the order, what it costs, and the
 * option to print it or copy it.
 *
 * <p>The text itself is built by
 * {@code OrderService.summaryOf(order, items)} - this window only shows it.
 * It is laid out in a monospaced font because the summary lines its columns
 * up with spaces, which only holds if every character is the same width.</p>
 */
public class OrderSummaryDialog extends JDialog {

    private final JTextArea txtSummary;

    private OrderSummaryDialog(Frame owner, String title, String summary) {
        super(owner, title, true);

        txtSummary = new JTextArea(summary);
        txtSummary.setEditable(false);
        txtSummary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Theme.FONT_SIZE_BASE));
        txtSummary.setForeground(Theme.TEXT_PRIMARY);
        txtSummary.setBackground(Theme.CARD_BG);
        txtSummary.setBorder(BorderFactory.createEmptyBorder(
                Theme.GAP_SMALL, Theme.GAP_SMALL, Theme.GAP_SMALL, Theme.GAP_SMALL));
        txtSummary.setCaretPosition(0);

        JScrollPane scrSummary = new JScrollPane(txtSummary);
        scrSummary.setBorder(BorderFactory.createEmptyBorder());
        scrSummary.getViewport().setBackground(Theme.CARD_BG);

        Card card = new Card(new BorderLayout()).withPadding();
        card.add(scrSummary, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, Theme.GAP_SMALL));
        root.setBackground(Theme.PAGE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        root.add(card, BorderLayout.CENTER);
        root.add(buildActions(), BorderLayout.SOUTH);

        setContentPane(root);
        setSize(640, 620);
        setMinimumSize(new Dimension(560, 480));
        setLocationRelativeTo(owner);
    }

    /** Shows the summary and blocks until it is closed. */
    public static void show(Component parent, String title, String summary) {
        Frame owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
        new OrderSummaryDialog(owner, title, summary).setVisible(true);
    }

    private JPanel buildActions() {
        JButton btnPrint = Buttons.primary("Print");
        btnPrint.addActionListener(e -> print());

        JButton btnCopy = Buttons.secondary("Copy");
        btnCopy.setToolTipText("Copy the summary to the clipboard");
        btnCopy.addActionListener(e -> copy());

        JButton btnClose = Buttons.text("Close");
        btnClose.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        actions.setOpaque(false);
        actions.add(btnCopy);
        actions.add(btnClose);
        actions.add(btnPrint);
        return actions;
    }

    /**
     * Hands the text to {@code JTextArea.print}, which opens the system print
     * dialog. Cancelling it is a normal outcome, not an error, so it is not
     * reported.
     */
    private void print() {
        try {
            txtSummary.print();
        } catch (PrinterException ex) {
            System.err.println("OrderSummaryDialog: printing failed - " + ex.getMessage());
            Dialogs.error(this, "Could not print the summary.\n\n"
                    + "Use Copy to paste it somewhere else instead.");
        }
    }

    private void copy() {
        getToolkit().getSystemClipboard()
                .setContents(new StringSelection(txtSummary.getText()), null);
        Dialogs.info(this, "The order summary has been copied to the clipboard.");
    }
}
