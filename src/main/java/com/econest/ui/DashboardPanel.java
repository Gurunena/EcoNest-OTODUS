package com.econest.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * The landing page shown when the app opens. It belongs to the shared UI
 * foundation, not to any one module.
 *
 * <p>The cards below are static for now. When a module is ready, its owner
 * can feed a real number in through {@link #setCardValue(int, String)} rather
 * than rebuilding this screen.</p>
 */
public class DashboardPanel extends JPanel {

    /** Captions of the summary cards, left to right. */
    private static final String[] CARD_TITLES = {
            "Products", "Clients", "Low stock items", "Pending deliveries"
    };

    /** The value labels inside the cards, so they can be updated later. */
    private final JLabel[] lblValues = new JLabel[CARD_TITLES.length];

    public DashboardPanel() {
        super(new BorderLayout(Theme.GAP, Theme.GAP));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        JLabel lblTitle = new JLabel("Dashboard");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);

        JLabel lblSubtitle = new JLabel("EcoNest at a glance");
        lblSubtitle.setFont(Theme.FONT_BODY);
        lblSubtitle.setForeground(Theme.TEXT_MUTED);

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(2));
        pnlHeader.add(lblSubtitle);

        JPanel pnlCards = new JPanel(new GridLayout(1, CARD_TITLES.length, Theme.GAP, Theme.GAP));
        pnlCards.setOpaque(false);
        pnlCards.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_LARGE, 0, 0, 0));
        for (int i = 0; i < CARD_TITLES.length; i++) {
            pnlCards.add(buildCard(i, CARD_TITLES[i]));
        }

        JLabel lblHint = new JLabel(
                "Pick a module on the left to get started. "
                        + "Building a screen? See docs/UI_GUIDE.md.");
        lblHint.setFont(Theme.FONT_BODY);
        lblHint.setForeground(Theme.TEXT_MUTED);
        lblHint.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_LARGE, 0, 0, 0));

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);
        pnlTop.add(pnlHeader, BorderLayout.NORTH);
        pnlTop.add(pnlCards, BorderLayout.CENTER);
        pnlTop.add(lblHint, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);
    }

    /** One summary card: a big value over a small caption. */
    private JPanel buildCard(int index, String caption) {
        JLabel lblValue = new JLabel("-");
        lblValue.setFont(Theme.FONT_TITLE);
        lblValue.setForeground(Theme.ACCENT);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblValues[index] = lblValue;

        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(Theme.FONT_BODY);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlCard = new JPanel();
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setBackground(Theme.PANEL_BG);
        pnlCard.setPreferredSize(new Dimension(160, 90));
        pnlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(
                        Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP)));
        pnlCard.add(lblValue);
        pnlCard.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        pnlCard.add(lblCaption);
        return pnlCard;
    }

    /**
     * Updates the number on one card once a module can supply it.
     *
     * @param index 0 = Products, 1 = Clients, 2 = Low stock, 3 = Pending deliveries
     * @param value the text to show, e.g. "128"
     */
    public void setCardValue(int index, String value) {
        if (index >= 0 && index < lblValues.length) {
            lblValues[index].setText(value);
        }
    }
}
