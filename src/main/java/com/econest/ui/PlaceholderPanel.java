package com.econest.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Stand-in page for a module whose screen has not been written yet.
 *
 * <p>{@code MainFrame} registers one of these for every module that has no
 * real panel, so the sidebar is complete from day one. When you build your
 * screen, extend {@link BaseModulePanel} and swap your class into your single
 * registration line in {@code MainFrame} - nothing else changes.</p>
 *
 * <pre>
 *   new PlaceholderPanel("Clients", "Client module owner")
 * </pre>
 *
 * It is also still usable as a base class ({@code extends PlaceholderPanel})
 * for the early stub panels that already do that.
 */
public class PlaceholderPanel extends JPanel {

    /** Placeholder with the generic "add your panel here" message. */
    public PlaceholderPanel(String moduleName) {
        this(moduleName, "Owner");
    }

    /**
     * @param moduleName the module as it appears in the sidebar, e.g. "Orders"
     * @param owner      who is building it, shown in the hint line
     */
    public PlaceholderPanel(String moduleName, String owner) {
        super(new BorderLayout());
        setBackground(Theme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        JLabel lblTitle = new JLabel(moduleName);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);

        JLabel lblHint = new JLabel(owner + ": add your panel here");
        lblHint.setFont(Theme.FONT_HEADING);
        lblHint.setForeground(Theme.ACCENT);

        JLabel lblHow = new JLabel(
                "Extend BaseModulePanel, then register it on your line in MainFrame. "
                        + "See docs/UI_GUIDE.md.");
        lblHow.setFont(Theme.FONT_BODY);
        lblHow.setForeground(Theme.TEXT_MUTED);

        // Centre the three lines vertically in the content area.
        JPanel pnlCentre = new JPanel();
        pnlCentre.setLayout(new BoxLayout(pnlCentre, BoxLayout.Y_AXIS));
        pnlCentre.setOpaque(false);
        pnlCentre.add(Box.createVerticalGlue());
        pnlCentre.add(centred(lblTitle));
        pnlCentre.add(Box.createVerticalStrut(Theme.GAP));
        pnlCentre.add(centred(lblHint));
        pnlCentre.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        pnlCentre.add(centred(lblHow));
        pnlCentre.add(Box.createVerticalGlue());

        add(pnlCentre, BorderLayout.CENTER);
    }

    private static JLabel centred(JLabel label) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }
}
