package com.econest.ui.components;

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * A plain rounded rectangle border in one colour.
 *
 * <p>Swing has no rounded line border, and FlatLaf's arc properties only
 * apply to components it paints itself. This keeps the radius identical on
 * text fields, search boxes and anything else that needs it.</p>
 */
public class RoundedBorder extends AbstractBorder {

    private final Color colour;
    private final int radius;
    private final int thickness;

    public RoundedBorder(Color colour, int radius, int thickness) {
        this.colour = colour;
        this.radius = radius;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(colour);
        g2.setStroke(new java.awt.BasicStroke(thickness));
        int offset = thickness / 2;
        g2.drawRoundRect(x + offset, y + offset,
                width - thickness, height - thickness, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, thickness, thickness, thickness);
        return insets;
    }
}
