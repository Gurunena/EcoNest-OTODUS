package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * A white rounded panel with a 1px border and a soft shadow - the surface
 * every piece of content sits on.
 *
 * <pre>
 *   Card card = new Card(new BorderLayout());
 *   card.add(myTable, BorderLayout.CENTER);
 *
 *   // or with a heading built in
 *   JPanel section = Card.titled("Low stock items", myTable);
 * </pre>
 *
 * <p>The shadow is painted by the card itself rather than by a border, so it
 * stays crisp at any size and does not fight the rounded corners.</p>
 */
public class Card extends JPanel {

    /** How far the shadow spreads beyond the card, in pixels. */
    private static final int SHADOW_SIZE = 6;

    /** How far the shadow is offset downwards. */
    private static final int SHADOW_DROP = 2;

    private final int radius;

    public Card() {
        this(new BorderLayout());
    }

    public Card(LayoutManager layout) {
        this(layout, Theme.CARD_RADIUS);
    }

    public Card(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
        setBackground(Theme.CARD_BG);
        // Leave room for the shadow, then the card's own padding.
        setBorder(BorderFactory.createEmptyBorder(
                SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE + SHADOW_DROP, SHADOW_SIZE));
    }

    /** Adds the usual inner padding on top of the shadow inset. */
    public Card withPadding() {
        return withPadding(Theme.CARD_PADDING);
    }

    /** Adds a specific inner padding on top of the shadow inset. */
    public Card withPadding(int padding) {
        setBorder(BorderFactory.createEmptyBorder(
                SHADOW_SIZE + padding,
                SHADOW_SIZE + padding,
                SHADOW_SIZE + SHADOW_DROP + padding,
                SHADOW_SIZE + padding));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = SHADOW_SIZE;
        int y = SHADOW_SIZE;
        int w = getWidth() - 2 * SHADOW_SIZE;
        int h = getHeight() - 2 * SHADOW_SIZE - SHADOW_DROP;
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        // Soft shadow: a few translucent rounded rectangles, each one larger
        // and fainter than the last.
        for (int i = SHADOW_SIZE; i > 0; i--) {
            float strength = (float) (SHADOW_SIZE - i + 1) / SHADOW_SIZE;
            g2.setColor(Theme.alpha(Theme.CARD_SHADOW,
                    (int) (Theme.CARD_SHADOW.getAlpha() * strength * 0.5f)));
            g2.fillRoundRect(x - i, y - i + SHADOW_DROP, w + 2 * i, h + 2 * i,
                    radius + i, radius + i);
        }

        g2.setColor(getBackground());
        g2.fillRoundRect(x, y, w, h, radius, radius);

        g2.setColor(Theme.CARD_BORDER);
        g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);

        g2.dispose();
    }

    // ---------------------------------------------------------------------
    // Convenience builders
    // ---------------------------------------------------------------------

    /** A card with a heading above the given content. */
    public static Card titled(String title, JComponent content) {
        return titled(title, content, null);
    }

    /**
     * A card with a heading and an optional action on the right of it - a
     * "View all" link, for example.
     */
    public static Card titled(String title, JComponent content, JComponent headerAction) {
        Card card = new Card(new BorderLayout(0, Theme.GAP_SMALL)).withPadding();

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading(title), BorderLayout.WEST);
        if (headerAction != null) {
            header.add(headerAction, BorderLayout.EAST);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** The standard card heading label. */
    public static JLabel heading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_HEADING);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        return lbl;
    }

    /** A 1px horizontal rule in the card border colour. */
    public static JComponent separator() {
        JPanel line = new JPanel();
        line.setBackground(Theme.CARD_BORDER);
        line.setPreferredSize(new java.awt.Dimension(0, 1));
        line.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    /** Paints a filled circle of {@code colour} behind an icon. */
    public static JPanel iconCircle(JComponent icon, Color colour) {
        JPanel circle = new JPanel(new java.awt.GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colour);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(
                new java.awt.Dimension(Theme.ICON_CIRCLE_SIZE, Theme.ICON_CIRCLE_SIZE));
        circle.setMaximumSize(
                new java.awt.Dimension(Theme.ICON_CIRCLE_SIZE, Theme.ICON_CIRCLE_SIZE));
        circle.setMinimumSize(
                new java.awt.Dimension(Theme.ICON_CIRCLE_SIZE, Theme.ICON_CIRCLE_SIZE));
        if (icon != null) {
            circle.add(icon);
        }
        return circle;
    }
}
