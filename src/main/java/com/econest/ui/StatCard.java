package com.econest.ui;

import com.econest.ui.components.Card;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/**
 * The standard Dashboard card: a tinted icon circle, one big number and a
 * small label, on the shared rounded card surface.
 *
 * <pre>
 *   +---------------------+
 *   |  (o)                |   &lt;- icon in a soft tinted circle
 *   |                     |
 *   |  12                 |   &lt;- the number, large
 *   |  Pending orders     |   &lt;- the label, small and muted
 *   +---------------------+
 * </pre>
 *
 * <p>Most modules want exactly this, so it is worth not writing it nine
 * times. Give it a {@link ValueSource} and it handles the rest - including
 * the threading rules on {@link DashboardCard}, which is the part that is
 * easy to get wrong:</p>
 *
 * <pre>
 *   new StatCard("Products", "products", Theme.TINT_INFO, Theme.STATUS_INFO,
 *                () -&gt; String.valueOf(new ProductService().getAllProducts().size()))
 * </pre>
 *
 * <p>The value source runs on a background thread and the label is updated
 * on the Event Dispatch Thread. A source that throws leaves the card showing
 * a zero and logs one line - a database that is not running is a normal
 * state during development and must never break the landing page.</p>
 */
public class StatCard implements DashboardCard {

    /**
     * Where a card's number comes from. Allowed to throw: whatever goes
     * wrong becomes a zero on the card and one line on the console.
     */
    @FunctionalInterface
    public interface ValueSource {
        String get() throws Exception;
    }

    private final String title;
    private final ValueSource source;

    private final Card view;
    private final JLabel lblValue;

    /**
     * @param title    the small label under the number, e.g. "Pending orders"
     * @param iconName an SVG from {@code resources/images/icons}, without the
     *                 extension - "orders", "clients", "inventory"...
     * @param tint     the pale circle behind the icon, a {@code Theme.TINT_*}
     * @param accent   the icon colour, the {@code Theme.STATUS_*} that pairs
     *                 with {@code tint}
     * @param source   where the number comes from; runs off the UI thread
     */
    public StatCard(String title, String iconName, Color tint, Color accent,
                    ValueSource source) {
        this.title = title;
        this.source = source;

        Icon icon = Icons.icon(iconName, Theme.ICON_SIZE_CARD, accent);
        JPanel circle = Card.iconCircle(icon == null ? null : new JLabel(icon), tint);
        circle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblValue = new JLabel(DashboardStats.UNAVAILABLE);
        lblValue.setFont(Theme.FONT_DISPLAY);
        lblValue.setForeground(Theme.TEXT_PRIMARY);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCaption = new JLabel(title);
        lblCaption.setFont(Theme.FONT_SMALL);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCaption.setToolTipText(title);

        // Stacked rather than side by side, so a long figure like
        // "Rs. 1,234,500" gets the card's full width instead of being cut off.
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.add(circle);
        stack.add(Box.createVerticalStrut(Theme.GAP_SMALL + 2));
        stack.add(lblValue);
        stack.add(Box.createVerticalStrut(2));
        stack.add(lblCaption);
        stack.add(Box.createVerticalGlue());

        view = new Card(new BorderLayout()).withPadding(Theme.GAP);
        view.add(stack, BorderLayout.CENTER);
    }

    /**
     * A card that always shows zero, for a module whose owner has not wired
     * theirs up yet. It keeps the module's slot in the grid so the Dashboard
     * looks complete, and swapping it for a real card is a one-line change in
     * {@link DashboardCards#register()}.
     */
    public static StatCard placeholder(String title, String iconName,
                                       Color tint, Color accent) {
        return new StatCard(title, iconName, tint, accent,
                () -> DashboardStats.UNAVAILABLE);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public JComponent getView() {
        return view;
    }

    /**
     * Reads the value source on whatever thread called this - the Dashboard's
     * SwingWorker - then applies it to the label on the EDT.
     */
    @Override
    public void refresh() {
        String value;
        try {
            value = source.get();
        } catch (Exception ex) {
            System.err.println("Dashboard: could not load \"" + title + "\" (" + ex + ")");
            value = DashboardStats.UNAVAILABLE;
        }
        final String resolved = value == null ? DashboardStats.UNAVAILABLE : value;
        SwingUtilities.invokeLater(() -> setValue(resolved));
    }

    /** Puts a value on the card. Must be called on the EDT. */
    protected void setValue(String value) {
        lblValue.setText(value);
        lblValue.setToolTipText(value);
        // "Rs. 1,234,500" will not fit at 28px in a narrow card, so step the
        // size down with the length instead of letting Swing clip it.
        int length = value == null ? 0 : value.length();
        lblValue.setFont(length <= 5 ? Theme.FONT_DISPLAY
                : length <= 9 ? Theme.FONT_TITLE
                : Theme.FONT_HEADING);
    }
}
