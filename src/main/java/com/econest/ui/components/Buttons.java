package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Factory for the three button styles used across EcoNest.
 *
 * <pre>
 *   btnAdd    = Buttons.primary("Add");      // the main action on a screen
 *   btnClear  = Buttons.secondary("Clear");  // a neutral / cancel action
 *   btnDelete = Buttons.danger("Delete");    // a destructive action
 * </pre>
 *
 * Name your variables with the {@code btn} prefix (see docs/UI_GUIDE.md).
 */
public final class Buttons {

    private Buttons() {
        // Utility class.
    }

    /** Filled green button for the main action of a screen (Add, Save, Assign). */
    public static JButton primary(String text) {
        return build(text, Theme.ACCENT, Theme.TEXT_ON_ACCENT, Theme.ACCENT_DARK, null);
    }

    /** Outlined button for neutral actions (Clear, Refresh, Cancel). */
    public static JButton secondary(String text) {
        return build(text, Theme.CONTENT_BG, Theme.TEXT_PRIMARY, Theme.ACCENT_SOFT, Theme.BORDER);
    }

    /** Red button for destructive actions (Delete, Remove). Always confirm first. */
    public static JButton danger(String text) {
        return build(text, Theme.STATUS_DANGER, Theme.TEXT_ON_ACCENT, Theme.STATUS_DANGER.darker(), null);
    }

    /**
     * Shared construction: flat colours, themed font, hand cursor and a simple
     * hover effect. {@code borderColour} may be null for a borderless button.
     */
    private static JButton build(String text, Color background, Color foreground,
                                 Color hover, Color borderColour) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BODY);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // FlatLaf reads these client properties to round the corners.
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        button.putClientProperty("JButton.arc", Theme.CORNER_RADIUS);

        if (borderColour != null) {
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColour),
                    BorderFactory.createEmptyBorder(Theme.GAP_SMALL, Theme.GAP, Theme.GAP_SMALL, Theme.GAP)));
        } else {
            button.setBorder(BorderFactory.createEmptyBorder(
                    Theme.GAP_SMALL + 1, Theme.GAP, Theme.GAP_SMALL + 1, Theme.GAP));
        }

        button.setPreferredSize(new Dimension(
                Math.max(96, button.getPreferredSize().width), Theme.FIELD_HEIGHT + 4));

        // Hover feedback. Swap the background on enter and restore it on exit.
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }
}
