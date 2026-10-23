package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;

/**
 * Builds labelled form rows so every screen's form looks the same.
 *
 * <p>A row is a small vertical stack:</p>
 * <pre>
 *   Product name            &lt;- label  (lbl)
 *   [___________________]   &lt;- input  (txt / cmb / dtp)
 *   Required                &lt;- inline error, hidden until validation fails
 * </pre>
 *
 * Typical use inside {@code buildForm()}:
 * <pre>
 *   txtName = FormFields.textField();
 *   pnlForm.add(FormFields.row("Product name", txtName));
 * </pre>
 */
public final class FormFields {

    /**
     * Client property under which a row stores its inline error label, so
     * {@link InputValidator} can find it from the input component alone.
     */
    static final String ERROR_LABEL_KEY = "econest.errorLabel";

    private FormFields() {
        // Utility class.
    }

    /** A themed single-line text field, named {@code txtSomething} by convention. */
    public static JTextField textField() {
        JTextField txt = new JTextField();
        txt.setFont(Theme.FONT_BODY);
        txt.setForeground(Theme.TEXT_PRIMARY);
        txt.setPreferredSize(new Dimension(Theme.FIELD_WIDTH, Theme.FIELD_HEIGHT));
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.FIELD_HEIGHT));
        txt.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        return txt;
    }

    /** A themed combo box, named {@code cmbSomething} by convention. */
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cmb = new JComboBox<>(items);
        cmb.setFont(Theme.FONT_BODY);
        cmb.setForeground(Theme.TEXT_PRIMARY);
        cmb.setBackground(Theme.CONTENT_BG);
        cmb.setPreferredSize(new Dimension(Theme.FIELD_WIDTH, Theme.FIELD_HEIGHT));
        cmb.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.FIELD_HEIGHT));
        return cmb;
    }

    /** A themed caption label, named {@code lblSomething} by convention. */
    public static JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BODY_BOLD);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        lbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return lbl;
    }

    /** A themed section heading for a group of fields. */
    public static JLabel heading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_HEADING);
        lbl.setForeground(Theme.SIDEBAR_BG);
        lbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return lbl;
    }

    /**
     * Wraps any input component in a labelled row with a hidden inline error
     * label underneath.
     *
     * @param labelText caption shown above the input
     * @param input     the field itself (text field, combo box, date picker…)
     * @return a panel to add straight into your form
     */
    public static JPanel row(String labelText, JComponent input) {
        JPanel pnlRow = new JPanel();
        pnlRow.setLayout(new BoxLayout(pnlRow, BoxLayout.Y_AXIS));
        pnlRow.setOpaque(false);
        pnlRow.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        pnlRow.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP_SMALL, 0));

        input.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        // The inline error label. Hidden (empty) until a validator fills it in.
        JLabel lblError = new JLabel(" ");
        lblError.setFont(Theme.FONT_SMALL);
        lblError.setForeground(Theme.STATUS_DANGER);
        lblError.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        // Remember the error label on the input so InputValidator can reach it.
        input.putClientProperty(ERROR_LABEL_KEY, lblError);

        pnlRow.add(label(labelText));
        pnlRow.add(Box.createVerticalStrut(2));
        pnlRow.add(input);
        pnlRow.add(lblError);
        return pnlRow;
    }
}
