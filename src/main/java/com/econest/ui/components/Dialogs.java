package com.econest.ui.components;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Thin wrappers around {@code JOptionPane} so every module shows the same
 * wording and the same button layout.
 *
 * <pre>
 *   Dialogs.info(this, "Stock item saved.");
 *   Dialogs.error(this, "Could not reach the database.");
 *   if (Dialogs.confirmDelete(this, "stock item SKU-1042")) { ... }
 * </pre>
 */
public final class Dialogs {

    private Dialogs() {
        // Utility class.
    }

    /** A plain "it worked" message. */
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "EcoNest", JOptionPane.INFORMATION_MESSAGE);
    }

    /** An error message. Use for failures the user needs to act on. */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "EcoNest - Error", JOptionPane.ERROR_MESSAGE);
    }

    /** A warning: something is off but the user can carry on. */
    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "EcoNest - Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Confirmation shown before anything is deleted. Always call this first -
     * never delete straight from the button handler.
     *
     * @param what a short description of the record, e.g. "client 12 (A. Perera)"
     * @return true only if the user chose Yes
     */
    public static boolean confirmDelete(Component parent, String what) {
        int choice = JOptionPane.showConfirmDialog(
                parent,
                "Delete " + what + "?\nThis cannot be undone.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /** A generic yes/no question for non-delete confirmations. */
    public static boolean confirm(Component parent, String question) {
        int choice = JOptionPane.showConfirmDialog(
                parent, question, "EcoNest", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Shows an error dialog and moves the keyboard focus to the offending
     * field, so the user lands exactly where the problem is.
     */
    public static void errorAt(Component parent, String message, JComponent field) {
        error(parent, message);
        if (field != null) {
            field.requestFocusInWindow();
        }
    }
}
