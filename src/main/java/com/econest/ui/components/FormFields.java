package com.econest.ui.components;

import com.econest.ui.Icons;
import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Builds labelled form rows so every screen's form looks the same.
 *
 * <p>A row is a small vertical stack:</p>
 * <pre>
 *   Product name            &lt;- caption (turns green while the field has focus)
 *   [___________________]   &lt;- input, 40px tall, green focus ring
 *   Required                &lt;- inline error, hidden until validation fails
 * </pre>
 *
 * Typical use inside {@code buildForm()}:
 * <pre>
 *   txtName = FormFields.textField("e.g. Peace Lily");
 *   pnlForm.add(FormFields.row("Product name", txtName));
 * </pre>
 */
public final class FormFields {

    /**
     * Client property under which a row stores its inline error label, so
     * {@link InputValidator} can find it from the input component alone.
     */
    static final String ERROR_LABEL_KEY = "econest.errorLabel";

    /** Client property holding the row's caption label. */
    static final String CAPTION_LABEL_KEY = "econest.captionLabel";

    private FormFields() {
        // Utility class.
    }

    // ---------------------------------------------------------------------
    // Inputs
    // ---------------------------------------------------------------------

    /** A themed single-line text field. */
    public static JTextField textField() {
        return textField(null);
    }

    /**
     * A themed text field with placeholder text shown while it is empty.
     *
     * @param placeholder greyed hint text, or null for none
     */
    public static JTextField textField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(Theme.FONT_BODY);
        txt.setForeground(Theme.TEXT_PRIMARY);
        txt.setBackground(Theme.CARD_BG);
        txt.setPreferredSize(new Dimension(Theme.FIELD_WIDTH, Theme.FIELD_HEIGHT));
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.FIELD_HEIGHT));
        txt.setBorder(restingBorder());
        if (placeholder != null) {
            txt.putClientProperty("JTextField.placeholderText", placeholder);
        }
        installFocusRing(txt);
        return txt;
    }

    /** A themed combo box. */
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cmb = new JComboBox<>(items);
        cmb.setFont(Theme.FONT_BODY);
        cmb.setForeground(Theme.TEXT_PRIMARY);
        cmb.setBackground(Theme.CARD_BG);
        cmb.setPreferredSize(new Dimension(Theme.FIELD_WIDTH, Theme.FIELD_HEIGHT));
        cmb.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.FIELD_HEIGHT));
        installFocusRing(cmb);
        return cmb;
    }

    /** A themed caption label. */
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
        lbl.setForeground(Theme.TEXT_PRIMARY);
        lbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return lbl;
    }

    // ---------------------------------------------------------------------
    // Rows
    // ---------------------------------------------------------------------

    /**
     * Wraps an input in a labelled row with a hidden inline error label
     * underneath. The caption turns green while the field has focus, which
     * is what makes the group read as one control.
     *
     * @param labelText caption shown above the input
     * @param input     the field itself
     */
    public static JPanel row(String labelText, JComponent input) {
        JPanel pnlRow = new JPanel();
        pnlRow.setLayout(new BoxLayout(pnlRow, BoxLayout.Y_AXIS));
        pnlRow.setOpaque(false);
        pnlRow.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        pnlRow.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP_SMALL, 0));
        pnlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Theme.FIELD_HEIGHT + 44));

        input.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel lblCaption = caption(labelText);

        JLabel lblError = new JLabel(" ");
        lblError.setFont(Theme.FONT_SMALL);
        lblError.setForeground(Theme.STATUS_DANGER);
        lblError.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        input.putClientProperty(ERROR_LABEL_KEY, lblError);
        input.putClientProperty(CAPTION_LABEL_KEY, lblCaption);

        pnlRow.add(lblCaption);
        pnlRow.add(Box.createVerticalStrut(4));
        pnlRow.add(input);
        pnlRow.add(lblError);
        return pnlRow;
    }

    private static JLabel caption(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return lbl;
    }

    // ---------------------------------------------------------------------
    // Search box
    // ---------------------------------------------------------------------

    /**
     * Wraps a text field as a search box: magnifier on the left, a clear
     * button on the right that appears once something is typed.
     *
     * @param txtSearch the field to wrap - keep your own reference to it
     * @return the composed control, ready to add to a layout
     */
    public static JPanel searchBox(JTextField txtSearch) {
        txtSearch.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        txtSearch.setOpaque(false);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search...");

        JLabel lblIcon = new JLabel();
        Icon magnifier = Icons.icon("search", 16, Theme.TEXT_FAINT);
        if (magnifier != null) {
            lblIcon.setIcon(magnifier);
        }
        lblIcon.setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP_SMALL, 0, Theme.GAP_SMALL));

        JButton btnClear = Buttons.icon(Icons.icon("close", 14, Theme.TEXT_FAINT), "Clear search");
        btnClear.setVisible(false);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            txtSearch.requestFocusInWindow();
        });

        JPanel pnlSearch = new JPanel(new BorderLayout());
        pnlSearch.setBackground(Theme.CARD_BG);
        pnlSearch.setBorder(restingBorder());
        pnlSearch.setPreferredSize(new Dimension(280, Theme.FIELD_HEIGHT));
        pnlSearch.add(lblIcon, BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);
        pnlSearch.add(btnClear, BorderLayout.EAST);

        // The clear button only exists while there is something to clear.
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sync();
            }

            private void sync() {
                btnClear.setVisible(!txtSearch.getText().isEmpty());
                pnlSearch.revalidate();
                pnlSearch.repaint();
            }
        });

        // The ring belongs to the whole box, not the bare field inside it.
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pnlSearch.setBorder(focusBorder());
            }

            @Override
            public void focusLost(FocusEvent e) {
                pnlSearch.setBorder(restingBorder());
            }
        });
        return pnlSearch;
    }

    // ---------------------------------------------------------------------
    // Focus ring
    // ---------------------------------------------------------------------

    /**
     * Gives a component a green ring while it has keyboard focus, and turns
     * its caption green to match.
     */
    public static void installFocusRing(JComponent field) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Never fight a validation error that is already showing.
                if (!Boolean.TRUE.equals(field.getClientProperty(InputValidator.INVALID_KEY))) {
                    field.setBorder(focusBorder());
                }
                captionOf(field).ifPresent(lbl -> lbl.setForeground(Theme.ACCENT));
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!Boolean.TRUE.equals(field.getClientProperty(InputValidator.INVALID_KEY))) {
                    field.setBorder(restingBorder());
                }
                captionOf(field).ifPresent(lbl -> lbl.setForeground(Theme.TEXT_MUTED));
            }
        });
    }

    private static java.util.Optional<JLabel> captionOf(JComponent field) {
        Object stored = field.getClientProperty(CAPTION_LABEL_KEY);
        return stored instanceof JLabel label
                ? java.util.Optional.of(label)
                : java.util.Optional.empty();
    }

    /** The border an input wears at rest. */
    static Border restingBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.BORDER_STRONG, Theme.CORNER_RADIUS, 1),
                BorderFactory.createEmptyBorder(0, Theme.GAP_SMALL + 2, 0, Theme.GAP_SMALL));
    }

    /** The green ring an input wears while focused. */
    static Border focusBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.ACCENT, Theme.CORNER_RADIUS, 2),
                BorderFactory.createEmptyBorder(0, Theme.GAP_SMALL + 1, 0, Theme.GAP_SMALL - 1));
    }

    /** The red ring an input wears when validation failed. */
    static Border errorBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.STATUS_DANGER, Theme.CORNER_RADIUS, 2),
                BorderFactory.createEmptyBorder(0, Theme.GAP_SMALL + 1, 0, Theme.GAP_SMALL - 1));
    }
}
