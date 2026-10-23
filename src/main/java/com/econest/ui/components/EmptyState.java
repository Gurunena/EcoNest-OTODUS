package com.econest.ui.components;

import com.econest.ui.Icons;
import com.econest.ui.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;

/**
 * The "there is nothing here" panel: a muted icon, a short line of text and
 * an optional hint.
 *
 * <pre>
 *   JComponent body = EmptyState.wrap(scrollPane, tbl,
 *                                     "No stock items found",
 *                                     "Try a different search.");
 *   card.add(body, BorderLayout.CENTER);
 * </pre>
 *
 * <p>{@link #wrap} returns a container that shows the table while it has
 * rows and the empty panel when it does not. The table never leaves its
 * scroll pane, so the column header stays put - swapping the viewport's view
 * instead would tear the header off.</p>
 */
public final class EmptyState {

    private EmptyState() {
        // Utility class.
    }

    /** Builds a standalone empty-state panel. */
    public static JPanel create(String message, String hint, String iconName) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        Icon icon = Icons.icon(iconName, 32, Theme.TEXT_FAINT);
        if (icon != null) {
            JLabel lblIcon = new JLabel(icon);
            lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(lblIcon);
            content.add(Box.createVerticalStrut(Theme.GAP_SMALL + 2));
        }

        JLabel lblMessage = new JLabel(message);
        lblMessage.setFont(Theme.FONT_BODY_BOLD);
        lblMessage.setForeground(Theme.TEXT_MUTED);
        lblMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblMessage);

        if (hint != null && !hint.isBlank()) {
            JLabel lblHint = new JLabel(hint);
            lblHint.setFont(Theme.FONT_SMALL);
            lblHint.setForeground(Theme.TEXT_FAINT);
            lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(Box.createVerticalStrut(4));
            content.add(lblHint);
        }

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CARD_BG);
        wrapper.add(content);
        return wrapper;
    }

    /** The standard empty state for a table, using the "search" icon. */
    public static JPanel create(String message, String hint) {
        return create(message, hint, "search");
    }

    /**
     * Wraps a table's scroll pane so an empty state shows whenever the table
     * has no rows.
     *
     * @param scroll  the scroll pane holding the table
     * @param tbl     the table to watch
     * @param message the headline, e.g. "No stock items found"
     * @param hint    a smaller second line, or null
     * @return the container to add to your layout, in place of {@code scroll}
     */
    public static Host wrap(JScrollPane scroll, JTable tbl, String message, String hint) {
        return new Host(scroll, tbl, message, hint);
    }

    /**
     * Holds a table and its empty state, showing whichever one belongs on
     * screen. Swap the wording later with {@link #setMessage}.
     */
    public static class Host extends JPanel {

        private static final String TABLE = "table";
        private static final String EMPTY = "empty";

        private final CardLayout layout = new CardLayout();
        private final JScrollPane scroll;
        private final JTable table;
        private JPanel empty;

        Host(JScrollPane scroll, JTable table, String message, String hint) {
            super();
            this.scroll = scroll;
            this.table = table;
            setLayout(layout);
            setOpaque(false);

            empty = create(message, hint);
            add(scroll, TABLE);
            add(empty, EMPTY);

            table.getModel().addTableModelListener(e -> sync());
            sync();
        }

        /** Changes the wording without rebuilding the host. */
        public void setMessage(String message, String hint) {
            remove(empty);
            empty = create(message, hint);
            add(empty, EMPTY);
            sync();
        }

        private void sync() {
            layout.show(this, table.getRowCount() == 0 ? EMPTY : TABLE);
            revalidate();
            repaint();
        }

        /** The scroll pane this host wraps, if a caller still needs it. */
        public JComponent getScrollPane() {
            return scroll;
        }
    }
}
