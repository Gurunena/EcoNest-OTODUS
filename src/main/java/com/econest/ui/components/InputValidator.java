package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.util.regex.Pattern;

/**
 * Form validation that reports the problem <b>next to the field</b> instead of
 * in a pop-up: the field gets a red border and the small label created by
 * {@link FormFields#row(String, JComponent)} shows the message.
 *
 * <p>Typical use in {@code onAdd()}:</p>
 * <pre>
 *   InputValidator v = new InputValidator();
 *   String name  = v.required(txtName, "Product name");
 *   Integer qty  = v.integer(txtQuantity, "Quantity");
 *   String mail  = v.email(txtEmail, "Supplier e-mail");
 *   if (!v.isValid()) {
 *       setStatus("Please fix the highlighted fields.");
 *       return;
 *   }
 * </pre>
 *
 * Each check returns the parsed value, or {@code null} when it failed, and
 * records the failure so {@link #isValid()} tells you whether to carry on.
 */
public final class InputValidator {

    /** Deliberately simple: one @, a dot in the domain, no spaces. */
    private static final Pattern EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private boolean valid = true;

    /** True while every check run so far has passed. */
    public boolean isValid() {
        return valid;
    }

    /** Resets the validator so the same instance can be reused. */
    public void reset() {
        valid = true;
    }

    // ---------------------------------------------------------------------
    // Checks
    // ---------------------------------------------------------------------

    /**
     * The field must not be empty.
     *
     * @return the trimmed text, or null if it was blank
     */
    public String required(JTextComponent txt, String fieldName) {
        String value = txt.getText() == null ? "" : txt.getText().trim();
        if (value.isEmpty()) {
            fail(txt, fieldName + " is required");
            return null;
        }
        clear(txt);
        return value;
    }

    /** A combo box must have something selected. */
    public Object requiredSelection(JComboBox<?> cmb, String fieldName) {
        Object value = cmb.getSelectedItem();
        if (value == null || value.toString().isBlank()) {
            fail(cmb, "Choose a value for " + fieldName);
            return null;
        }
        clear(cmb);
        return value;
    }

    /**
     * The field must hold a whole number that is zero or greater.
     *
     * @return the parsed value, or null if it was missing or not a number
     */
    public Integer integer(JTextComponent txt, String fieldName) {
        String value = required(txt, fieldName);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                fail(txt, fieldName + " cannot be negative");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            fail(txt, fieldName + " must be a whole number");
            return null;
        }
    }

    /**
     * The field must hold a decimal number that is zero or greater
     * (prices, weights, totals).
     *
     * @return the parsed value, or null if it was missing or not a number
     */
    public Double number(JTextComponent txt, String fieldName) {
        String value = required(txt, fieldName);
        if (value == null) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                fail(txt, fieldName + " cannot be negative");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            fail(txt, fieldName + " must be a number");
            return null;
        }
    }

    /**
     * The field must hold something that looks like an e-mail address.
     *
     * @return the trimmed address, or null if it was missing or malformed
     */
    public String email(JTextComponent txt, String fieldName) {
        String value = required(txt, fieldName);
        if (value == null) {
            return null;
        }
        if (!EMAIL.matcher(value).matches()) {
            fail(txt, "Enter a valid e-mail address");
            return null;
        }
        return value;
    }

    // ---------------------------------------------------------------------
    // Showing and hiding the inline message
    // ---------------------------------------------------------------------

    /** Marks a field as bad: red border plus a message under it. */
    public void fail(JComponent field, String message) {
        valid = false;
        field.setBorder(BorderFactory.createLineBorder(Theme.STATUS_DANGER));
        errorLabelOf(field).setText(message);
    }

    /** Clears any error shown on a field. */
    public void clear(JComponent field) {
        field.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        errorLabelOf(field).setText(" ");
    }

    /** Clears the errors on a whole group of fields - handy in {@code onClear()}. */
    public void clearAll(JComponent... fields) {
        for (JComponent field : fields) {
            clear(field);
        }
        reset();
    }

    /**
     * Finds the inline error label that {@link FormFields#row} attached to the
     * field. Returns a throwaway label when the field was not built by
     * FormFields, so validation never blows up on a hand-made form.
     */
    private static JLabel errorLabelOf(JComponent field) {
        Object stored = field.getClientProperty(FormFields.ERROR_LABEL_KEY);
        return stored instanceof JLabel label ? label : new JLabel();
    }
}
