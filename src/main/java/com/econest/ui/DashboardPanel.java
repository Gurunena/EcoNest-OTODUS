package com.econest.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * The landing page shown when the app opens. It belongs to the shared UI
 * foundation, not to any one module.
 *
 * <pre>
 *   Dashboard
 *   EcoNest at a glance
 *
 *   +----------+  +----------+  +----------+  +----------+
 *   | !  12    |  | o  4     |  | =  7     |  | $ 48,000 |   &lt;- coloured cards
 *   | Low stock|  | Pending  |  | Deliv.   |  | Revenue  |
 *   +----------+  +----------+  +----------+  +----------+
 *
 *   +-----------------------------------------------------+
 *   | Welcome to EcoNest            [ dashboard-plants ]  |   &lt;- welcome card
 *   | sustainable-living message                          |
 *   +-----------------------------------------------------+
 * </pre>
 *
 * <p>Every colour, font and gap comes from {@link Theme}; the numbers come
 * from {@link DashboardStats}; the artwork comes from {@link Icons} and is
 * skipped silently when a file is missing.</p>
 */
public class DashboardPanel extends JPanel {

    /** The four cards, left to right. */
    private static final CardSpec[] CARDS = {
            new CardSpec("Low stock items", "alert", Theme.TINT_DANGER, Theme.STATUS_DANGER),
            new CardSpec("Pending orders", "clock", Theme.TINT_PENDING, Theme.STATUS_PENDING),
            new CardSpec("Deliveries today", "assignment", Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS),
            new CardSpec("Revenue this month", "revenue", Theme.TINT_INFO, Theme.STATUS_INFO)
    };

    /** The big number on each card, kept so {@link #refresh()} can update it. */
    private final JLabel[] lblValues = new JLabel[CARDS.length];

    /** Where the numbers come from. */
    private final DashboardStats stats;

    public DashboardPanel() {
        this(new DashboardStats());
    }

    /**
     * @param stats the source of the card numbers; pass your own subclass in
     *              a test to show fixed figures
     */
    public DashboardPanel(DashboardStats stats) {
        super(new BorderLayout(Theme.GAP, Theme.GAP));
        this.stats = stats;

        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        JPanel pnlTop = new JPanel();
        pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.Y_AXIS));
        pnlTop.setOpaque(false);
        pnlTop.add(buildHeader());
        pnlTop.add(Box.createVerticalStrut(Theme.GAP_LARGE));
        pnlTop.add(buildCards());
        pnlTop.add(Box.createVerticalStrut(Theme.GAP_LARGE));
        pnlTop.add(buildWelcomeCard());

        add(pnlTop, BorderLayout.NORTH);

        refresh();
    }

    /**
     * Re-reads every number from {@link DashboardStats}. Call it after a
     * module saves something, so the cards do not go stale.
     */
    public final void refresh() {
        lblValues[0].setText(stats.lowStockItems());
        lblValues[1].setText(stats.pendingOrders());
        lblValues[2].setText(stats.deliveriesToday());
        lblValues[3].setText(stats.revenueThisMonth());
    }

    // ---------------------------------------------------------------------
    // Sections
    // ---------------------------------------------------------------------

    /** Title and subtitle. */
    private JPanel buildHeader() {
        JLabel lblTitle = new JLabel("Dashboard");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("EcoNest at a glance");
        lblSubtitle.setFont(Theme.FONT_BODY);
        lblSubtitle.setForeground(Theme.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);
        pnlHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(2));
        pnlHeader.add(lblSubtitle);
        return pnlHeader;
    }

    /** The row of four coloured summary cards. */
    private JPanel buildCards() {
        JPanel pnlCards = new JPanel(new GridLayout(1, CARDS.length, Theme.GAP, Theme.GAP));
        pnlCards.setOpaque(false);
        pnlCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        for (int i = 0; i < CARDS.length; i++) {
            pnlCards.add(buildCard(i, CARDS[i]));
        }
        return pnlCards;
    }

    /** One card: icon on top, big number, caption underneath. */
    private JPanel buildCard(int index, CardSpec spec) {
        JPanel pnlCard = new JPanel();
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setBackground(spec.tint());
        pnlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, spec.accent()),
                BorderFactory.createEmptyBorder(
                        Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP)));

        // Icon, tinted to the card's accent colour. Skipped if the SVG is
        // not on the classpath.
        Icon icon = Icons.icon(spec.iconName(), Theme.ICON_SIZE_CARD, spec.accent());
        if (icon != null) {
            JLabel lblIcon = new JLabel(icon);
            lblIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlCard.add(lblIcon);
            pnlCard.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        }

        JLabel lblValue = new JLabel(DashboardStats.UNAVAILABLE);
        lblValue.setFont(Theme.FONT_DISPLAY);
        lblValue.setForeground(spec.accent());
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblValues[index] = lblValue;

        JLabel lblCaption = new JLabel(spec.caption());
        lblCaption.setFont(Theme.FONT_BODY);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlCard.add(lblValue);
        pnlCard.add(Box.createVerticalStrut(2));
        pnlCard.add(lblCaption);
        return pnlCard;
    }

    /** The welcome card: message on the left, artwork on the right. */
    private JPanel buildWelcomeCard() {
        JPanel pnlWelcome = new JPanel(new BorderLayout(Theme.GAP_LARGE, 0));
        pnlWelcome.setBackground(Theme.PANEL_BG);
        pnlWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlWelcome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(
                        Theme.GAP_LARGE, Theme.GAP_LARGE, Theme.GAP_LARGE, Theme.GAP_LARGE)));

        pnlWelcome.add(buildWelcomeText(), BorderLayout.CENTER);

        // Artwork on the right, left out entirely when the file is missing.
        Icon art = Icons.image("dashboard-plants.png", 340, 220);
        if (art != null) {
            JLabel lblArt = new JLabel(art);
            lblArt.setVerticalAlignment(JLabel.CENTER);
            pnlWelcome.add(lblArt, BorderLayout.EAST);
        }
        return pnlWelcome;
    }

    /** Heading plus the sustainable-living message. */
    private JPanel buildWelcomeText() {
        JLabel lblHeading = new JLabel("Welcome to EcoNest");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Theme.SIDEBAR_BG);
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        // HTML so the paragraph wraps. The width is deliberately narrower
        // than the space left beside the artwork, so the text never gets
        // clipped when the window is at its minimum size.
        JLabel lblMessage = new JLabel(
                "<html><body style='width:300px'>"
                        + "Every plant, planter and gardening kit you move through this "
                        + "system puts a little more green into a Sri Lankan home. "
                        + "Keep stock healthy, get deliveries out on time, and help our "
                        + "customers grow something that lasts."
                        + "</body></html>");
        lblMessage.setFont(Theme.FONT_BODY);
        lblMessage.setForeground(Theme.TEXT_PRIMARY);
        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlHeadingRow = new JPanel();
        pnlHeadingRow.setLayout(new BoxLayout(pnlHeadingRow, BoxLayout.X_AXIS));
        pnlHeadingRow.setOpaque(false);
        pnlHeadingRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        Icon leaf = Icons.icon("leaf", Theme.ICON_SIZE_CARD, Theme.ACCENT);
        if (leaf != null) {
            pnlHeadingRow.add(new JLabel(leaf));
            pnlHeadingRow.add(Box.createHorizontalStrut(Theme.GAP_SMALL));
        }
        pnlHeadingRow.add(lblHeading);
        pnlHeadingRow.add(Box.createHorizontalGlue());

        JLabel lblHint = new JLabel("Pick a module on the left to get started.");
        lblHint.setFont(Theme.FONT_SMALL);
        lblHint.setForeground(Theme.TEXT_MUTED);
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlText = new JPanel();
        pnlText.setLayout(new BoxLayout(pnlText, BoxLayout.Y_AXIS));
        pnlText.setOpaque(false);
        pnlText.add(pnlHeadingRow);
        pnlText.add(Box.createVerticalStrut(Theme.GAP));
        pnlText.add(lblMessage);
        pnlText.add(Box.createVerticalStrut(Theme.GAP));
        pnlText.add(lblHint);
        return pnlText;
    }

    /**
     * Static description of one summary card.
     *
     * @param caption  the label under the number
     * @param iconName the SVG in images/icons, without its extension
     * @param tint     the card's pale background
     * @param accent   the colour of its icon, number and left edge
     */
    private record CardSpec(String caption, String iconName, Color tint, Color accent) {
    }
}
