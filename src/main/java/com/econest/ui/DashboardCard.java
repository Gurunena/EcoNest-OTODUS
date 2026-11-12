package com.econest.ui;

import javax.swing.JComponent;

/**
 * One card on the {@link DashboardPanel}, contributed by one module.
 *
 * <p>The Dashboard does not know what any card contains. It asks
 * {@link DashboardCards#register()} for the list, lays the views out in a
 * responsive grid, and calls {@link #refresh()} whenever the Dashboard comes
 * on screen. That is the whole contract - a module owner adds a card without
 * {@code DashboardPanel} changing at all.</p>
 *
 * <h2>Threading - the one thing to get right</h2>
 * <p>{@link #refresh()} is called on a <b>background thread</b>, inside a
 * {@link javax.swing.SwingWorker}, so a slow database call cannot freeze the
 * window. That means:</p>
 * <ul>
 *   <li>Do your query <i>in</i> {@code refresh()}. That is what it is for.</li>
 *   <li>Do <b>not</b> touch Swing components from {@code refresh()} directly.
 *       Hand the result back with
 *       {@link javax.swing.SwingUtilities#invokeLater(Runnable)}.</li>
 *   <li>Never let {@code refresh()} throw. A card that fails should show a
 *       zero, not take the landing page down with it.</li>
 * </ul>
 *
 * <p>{@link StatCard} already does all three correctly. Extending it, or
 * passing it a value source, is the easy road; implement this interface
 * directly only when you need a card that is not a single big number.</p>
 *
 * @see DashboardCards
 * @see StatCard
 */
public interface DashboardCard {

    /**
     * The card's heading, e.g. "Pending orders". Also used as its tooltip
     * and in any log message about it, so make it read like a label rather
     * than a sentence.
     */
    String getTitle();

    /**
     * The component the Dashboard puts in the grid.
     *
     * <p>Called once, while the Dashboard is being built, so return the
     * <b>same</b> instance every time rather than building a new one per
     * call - otherwise {@link #refresh()} ends up updating a view that is
     * not on screen.</p>
     */
    JComponent getView();

    /**
     * Re-reads whatever the card shows.
     *
     * <p>Called on a background thread each time the Dashboard is shown, and
     * never on the Event Dispatch Thread. See the threading note on this
     * interface.</p>
     */
    void refresh();
}
