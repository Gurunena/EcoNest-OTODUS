package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Factory for the button styles used across EcoNest.
 *
 * <pre>
 *   btnAdd    = Buttons.primary("Add");      // filled green, the main action
 *   btnUpdate = Buttons.secondary("Update"); // outlined, a neutral action
 *   btnDelete = Buttons.danger("Delete");    // outlined red, destructive
 *   btnClear  = Buttons.text("Clear");       // no chrome at all
 * </pre>
 *
 * Name your variables with the {@code btn} prefix (see docs/UI_GUIDE.md).
 * Buttons paint their own rounded background so the radius is identical
 * everywhere, whatever the look and feel does.
 */
public final class Buttons {

    private Buttons() {
        // Utility class.
    }

    /** Filled green button for the main action of a screen. */
    public static JButton primary(String text) {
        return new StyledButton(text,
                Theme.ACCENT, Theme.ACCENT_DARK, Theme.TEXT_ON_ACCENT, null);
    }

    /** Outlined button for neutral actions (Update, Refresh, Cancel). */
    public static JButton secondary(String text) {
        return new StyledButton(text,
                Theme.CARD_BG, Theme.ACCENT_SOFT, Theme.TEXT_PRIMARY, Theme.BORDER_STRONG);
    }

    /** Outlined red button for destructive actions. Always confirm first. */
    public static JButton danger(String text) {
        return new StyledButton(text,
                Theme.CARD_BG, Theme.TINT_DANGER, Theme.STATUS_DANGER, Theme.STATUS_DANGER);
    }

    /** A button with no chrome, for the least important action in a group. */
    public static JButton text(String text) {
        return new StyledButton(text,
                null, Theme.ACCENT_SOFT, Theme.TEXT_MUTED, null);
    }

    /** A borderless link, used for "View all" on a dashboard card. */
    public static JButton link(String text) {
        StyledButton button = new StyledButton(text, null, null, Theme.ACCENT, null);
        button.setFont(Theme.FONT_BODY_BOLD);
        button.setPreferredSize(null);
        button.setCompact(true);
        return button;
    }

    /**
     * A small square icon-only button.
     *
     * @param tooltip shown on hover - always supply one, because the button
     *                has no label to explain itself
     */
    public static JButton icon(Icon icon, String tooltip) {
        StyledButton button = new StyledButton("", null, Theme.ACCENT_SOFT, Theme.TEXT_MUTED, null);
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setPreferredSize(new Dimension(32, 32));
        button.setCompact(true);
        return button;
    }

    /**
     * A flat button that paints its own rounded background, border and hover
     * state. A null background means "paint nothing until hovered".
     */
    private static class StyledButton extends JButton {

        private final Color background;
        private final Color hover;
        private final Color borderColour;
        private boolean hovered;
        private boolean compact;

        StyledButton(String text, Color background, Color hover,
                     Color foreground, Color borderColour) {
            super(text);
            this.background = background;
            this.hover = hover;
            this.borderColour = borderColour;

            setFont(Theme.FONT_BODY);
            setForeground(foreground);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP, 0, Theme.GAP));
            setPreferredSize(new Dimension(
                    Math.max(96, getPreferredSize().width + 2 * Theme.GAP), Theme.FIELD_HEIGHT));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        /** Compact buttons skip the minimum width and sit tight to their text. */
        void setCompact(boolean compact) {
            this.compact = compact;
            if (compact) {
                setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                Dimension size = super.getPreferredSize();
                setPreferredSize(new Dimension(size.width, Math.max(24, size.height)));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = compact ? Theme.GAP_SMALL : Theme.CORNER_RADIUS;
            Color fill = hovered && hover != null ? hover : background;

            if (fill != null) {
                g2.setColor(isEnabled() ? fill : Theme.alpha(fill, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            }
            if (borderColour != null) {
                g2.setColor(isEnabled() ? borderColour : Theme.alpha(borderColour, 110));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
