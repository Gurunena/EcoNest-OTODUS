package com.econest.ui;

import com.econest.report.model.LowStockAlert;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Card;
import com.econest.ui.components.EmptyState;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * The landing page shown when the app opens.
 *
 * <pre>
 *   +------------------------------------------------------------+
 *   |  gradient welcome banner                  [ plants image ]  |
 *   +------------------------------------------------------------+
 *   | (!) 2    | (o) 1      | (=) 0        | ($) 13,900           |
 *   | Low stock| Pending    | Deliveries   | Revenue this month   |
 *   +------------------------------------------------------------+
 *   | Low stock items        View all | Recent orders   View all  |
 *   +------------------------------------------------------------+
 * </pre>
 *
 * <p>Numbers and lists come from {@link DashboardStats}, which goes through
 * the service layer. Everything is loaded on a {@link SwingWorker}, and a
 * database that is down leaves zeros and empty states rather than an error
 * dialog over the landing page.</p>
 */
public class DashboardPanel extends JPanel {

    /** How many rows each list card shows. */
    private static final int LIST_ROWS = 4;

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    /** The four KPI cards, left to right. */
    private static final CardSpec[] CARDS = {
            new CardSpec("Low stock items", "alert", Theme.TINT_DANGER, Theme.STATUS_DANGER),
            new CardSpec("Pending orders", "clock", Theme.TINT_PENDING, Theme.STATUS_PENDING),
            new CardSpec("Deliveries today", "assignment", Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS),
            new CardSpec("Revenue this month", "revenue", Theme.TINT_INFO, Theme.STATUS_INFO)
    };

    private final JLabel[] lblValues = new JLabel[CARDS.length];

    private final DefaultTableModel lowStockModel =
            readOnlyModel(new String[]{"Product", "Qty", "Min", "Status"});
    private final DefaultTableModel recentOrdersModel =
            readOnlyModel(new String[]{"#", "Client", "When", "Total", "Status"});

    private final JTable tblLowStock = new JTable(lowStockModel);
    private final JTable tblRecentOrders = new JTable(recentOrdersModel);

    private final DashboardStats stats;

    public DashboardPanel() {
        this(new DashboardStats());
    }

    /**
     * @param stats the source of the numbers; pass your own subclass in a
     *              test to show fixed figures
     */
    public DashboardPanel(DashboardStats stats) {
        super(new BorderLayout(0, Theme.GAP));
        this.stats = stats;

        setBackground(Theme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        JPanel pnlTop = new JPanel(new BorderLayout(0, Theme.GAP));
        pnlTop.setOpaque(false);
        pnlTop.add(buildBanner(), BorderLayout.NORTH);
        pnlTop.add(buildCards(), BorderLayout.CENTER);

        add(pnlTop, BorderLayout.NORTH);
        add(buildLists(), BorderLayout.CENTER);

        refresh();
    }

    /**
     * Re-reads every number and list. Call it after a module saves something,
     * so the cards do not go stale.
     */
    public final void refresh() {
        new SwingWorker<Snapshot, Void>() {
            @Override
            protected Snapshot doInBackground() {
                return new Snapshot(
                        stats.lowStockItems(),
                        stats.pendingOrders(),
                        stats.deliveriesToday(),
                        stats.revenueThisMonth(),
                        stats.topLowStock(LIST_ROWS),
                        stats.recentOrders(LIST_ROWS));
            }

            @Override
            protected void done() {
                try {
                    show(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    // DashboardStats already swallows its own failures, so
                    // reaching here means something unexpected. The cards
                    // keep their zeros either way.
                    System.err.println("Dashboard: refresh failed - " + ex.getCause());
                }
            }
        }.execute();
    }

    private void show(Snapshot data) {
        setCardValue(0, data.lowStock());
        setCardValue(1, data.pending());
        setCardValue(2, data.deliveries());
        setCardValue(3, data.revenue());

        lowStockModel.setRowCount(0);
        for (LowStockAlert alert : data.lowStockRows()) {
            int onHand = alert.getStockItem().getQuantityOnHand();
            lowStockModel.addRow(new Object[]{
                    alert.getStockItem().getProduct().getProductName(),
                    onHand,
                    alert.getStockItem().getReorderLevel(),
                    onHand <= 0 ? "Out of stock" : "Low stock"});
        }

        recentOrdersModel.setRowCount(0);
        for (DashboardStats.OrderSummary order : data.recentOrders()) {
            recentOrdersModel.addRow(new Object[]{
                    "#" + order.orderId(),
                    order.client(),
                    order.when() == null ? "" : order.when().format(WHEN),
                    DashboardStats.money(order.total()),
                    order.status()});
        }

        // Deliberately no fitColumns here: these summary tables are narrow
        // and must squeeze to the card rather than grow a horizontal
        // scrollbar that hides the status pill.
        sizeColumns(tblLowStock, 0.46, 0.13, 0.13, 0.28);
        sizeColumns(tblRecentOrders, 0.08, 0.28, 0.22, 0.18, 0.24);
    }

    // ---------------------------------------------------------------------
    // Banner
    // ---------------------------------------------------------------------

    /**
     * A rounded gradient card with the text on the left and the plants
     * artwork anchored to the right edge. The image is painted into the card
     * rather than placed in a label, so there is no visible box around it.
     */
    private JPanel buildBanner() {
        Icon art = Icons.image("dashboard-plants.png", 320, 150);
        Image artImage = art instanceof javax.swing.ImageIcon icon ? icon.getImage() : null;

        GradientBanner banner = new GradientBanner(artImage);
        banner.setLayout(new BorderLayout());
        banner.setBorder(BorderFactory.createEmptyBorder(
                Theme.GAP_LARGE, Theme.GAP_LARGE + 4, Theme.GAP_LARGE, Theme.GAP_LARGE));
        banner.setPreferredSize(new Dimension(0, 150));

        JLabel lblHeading = new JLabel("Welcome to EcoNest");
        lblHeading.setFont(Theme.FONT_TITLE);
        lblHeading.setForeground(Color.WHITE);
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMessage = new JLabel(
                "<html><body style='width:380px'>"
                        + "Every plant, planter and gardening kit you move through this system "
                        + "puts a little more green into a Sri Lankan home."
                        + "</body></html>");
        lblMessage.setFont(Theme.FONT_BODY);
        lblMessage.setForeground(Theme.alpha(Color.WHITE, 220));
        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlText = new JPanel();
        pnlText.setLayout(new BoxLayout(pnlText, BoxLayout.Y_AXIS));
        pnlText.setOpaque(false);
        pnlText.add(Box.createVerticalGlue());
        pnlText.add(lblHeading);
        pnlText.add(Box.createVerticalStrut(Theme.GAP_SMALL));
        pnlText.add(lblMessage);
        pnlText.add(Box.createVerticalGlue());

        banner.add(pnlText, BorderLayout.WEST);
        return banner;
    }

    /** A rounded green gradient panel that paints artwork on its right edge. */
    private static class GradientBanner extends JPanel {

        private final Image art;

        GradientBanner(Image art) {
            this.art = art;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            java.awt.Shape clip = new java.awt.geom.RoundRectangle2D.Float(
                    0, 0, w, h, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
            g2.setClip(clip);

            g2.setPaint(new GradientPaint(
                    0, 0, Theme.SIDEBAR_BG,
                    w, h, Theme.ACCENT_DARK));
            g2.fillRect(0, 0, w, h);

            if (art != null) {
                int artH = h;
                int artW = art.getWidth(null) * artH / Math.max(1, art.getHeight(null));
                int x = w - artW - Theme.GAP;
                // Fade the artwork's left edge into the gradient so it reads
                // as part of the banner, not as a pasted-in box.
                g2.drawImage(fadeLeftEdge(art, artW, artH), x, 0, null);
            }
            g2.dispose();
        }

        /** Returns the image with a transparent-to-opaque ramp on its left. */
        private static BufferedImage fadeLeftEdge(Image source, int width, int height) {
            if (width <= 0 || height <= 0) {
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
            g.setComposite(java.awt.AlphaComposite.DstIn);
            g.setPaint(new GradientPaint(
                    0, 0, new Color(0, 0, 0, 0),
                    Math.max(1, width / 3f), 0, new Color(0, 0, 0, 255)));
            g.fillRect(0, 0, width, height);
            g.dispose();
            return out;
        }
    }

    // ---------------------------------------------------------------------
    // KPI cards
    // ---------------------------------------------------------------------

    private JPanel buildCards() {
        JPanel pnlCards = new JPanel(new GridLayout(1, CARDS.length, Theme.GAP_SMALL, 0));
        pnlCards.setOpaque(false);
        pnlCards.setPreferredSize(new Dimension(0, 158));
        for (int i = 0; i < CARDS.length; i++) {
            pnlCards.add(buildCard(i, CARDS[i]));
        }
        return pnlCards;
    }

    /**
     * One KPI card: tinted icon circle on top, then the number, then the
     * label. Stacked rather than side by side so a long figure like
     * "Rs. 1,234,500" gets the card's full width instead of being cut off.
     */
    private Card buildCard(int index, CardSpec spec) {
        Card card = new Card(new BorderLayout()).withPadding(Theme.GAP);

        Icon icon = Icons.icon(spec.iconName(), Theme.ICON_SIZE_CARD, spec.accent());
        JPanel circle = Card.iconCircle(icon == null ? null : new JLabel(icon), spec.tint());
        circle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValue = new JLabel(DashboardStats.UNAVAILABLE);
        lblValue.setFont(Theme.FONT_DISPLAY);
        lblValue.setForeground(Theme.TEXT_PRIMARY);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblValues[index] = lblValue;

        JLabel lblCaption = new JLabel(spec.caption());
        lblCaption.setFont(Theme.FONT_SMALL);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCaption.setToolTipText(spec.caption());

        JPanel pnlStack = new JPanel();
        pnlStack.setLayout(new BoxLayout(pnlStack, BoxLayout.Y_AXIS));
        pnlStack.setOpaque(false);
        pnlStack.add(circle);
        pnlStack.add(Box.createVerticalStrut(Theme.GAP_SMALL + 2));
        pnlStack.add(lblValue);
        pnlStack.add(Box.createVerticalStrut(2));
        pnlStack.add(lblCaption);
        pnlStack.add(Box.createVerticalGlue());

        card.add(pnlStack, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------------------------------
    // List cards
    // ---------------------------------------------------------------------

    private JPanel buildLists() {
        JPanel pnlLists = new JPanel(new GridLayout(1, 2, Theme.GAP_SMALL, 0));
        pnlLists.setOpaque(false);

        pnlLists.add(listCard("Low stock items", tblLowStock, 3,
                new int[]{1, 2}, MainFrame.PAGE_INVENTORY,
                "Nothing is low", "Every product is above its reorder level."));

        pnlLists.add(listCard("Recent orders", tblRecentOrders, 4,
                new int[]{3}, MainFrame.PAGE_ORDERS,
                "No recent orders", "Orders will appear here as they come in."));

        return pnlLists;
    }

    /**
     * A card holding one small table, with a "View all" link that switches
     * the sidebar to the module the list belongs to.
     */
    private Card listCard(String title, JTable tbl, int statusColumn, int[] numericColumns,
                          String page, String emptyMessage, String emptyHint) {
        TableStyler.style(tbl);
        tbl.setRowHeight(Theme.TABLE_ROW_HEIGHT - 6);
        tbl.setAutoCreateRowSorter(false);
        tbl.setRowSelectionAllowed(false);
        TableStyler.rightAlign(tbl, numericColumns);
        TableStyler.colourStatusColumn(tbl, statusColumn);

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Theme.CARD_BG);
        EmptyState.Host host = EmptyState.wrap(scroll, tbl, emptyMessage, emptyHint);

        JButton btnViewAll = Buttons.link("View all");
        btnViewAll.setToolTipText("Open " + page);
        btnViewAll.addActionListener(e -> {
            MainFrame frame =
                    (MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, this);
            if (frame != null) {
                frame.showPage(page);
            }
        });

        return Card.titled(title, host, btnViewAll);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Everything the dashboard shows, fetched in one background pass. */
    private record Snapshot(String lowStock, String pending, String deliveries, String revenue,
                            List<LowStockAlert> lowStockRows,
                            List<DashboardStats.OrderSummary> recentOrders) {
    }

    /** Static description of one KPI card. */
    private record CardSpec(String caption, String iconName, Color tint, Color accent) {
    }

    /**
     * Gives a table proportional column widths. The weights are relative, so
     * the columns always fill the card exactly and never scroll sideways.
     */
    private static void sizeColumns(JTable tbl, double... weights) {
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int col = 0; col < tbl.getColumnCount() && col < weights.length; col++) {
            TableColumn column = tbl.getColumnModel().getColumn(col);
            // Relative, not absolute: AUTO_RESIZE_ALL_COLUMNS shares the real
            // width out in proportion to these, so the layout is correct at
            // any window size without re-measuring.
            column.setPreferredWidth((int) Math.round(weights[col] * 1000));
            column.setMinWidth(40);
        }
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Kept for compatibility: the cards now take their numbers from
     * {@link DashboardStats} through {@link #refresh()}.
     *
     * @param index 0 = low stock, 1 = pending orders, 2 = deliveries, 3 = revenue
     */
    public void setCardValue(int index, String value) {
        if (index < 0 || index >= lblValues.length) {
            return;
        }
        JLabel lbl = lblValues[index];
        lbl.setText(value);
        lbl.setToolTipText(value);
        // "Rs. 1,234,500" will not fit at 28px in a quarter-width card, so
        // step the size down with the length instead of letting Swing clip
        // it to an ellipsis.
        int length = value == null ? 0 : value.length();
        lbl.setFont(length <= 5 ? Theme.FONT_DISPLAY
                : length <= 9 ? Theme.FONT_TITLE
                : Theme.FONT_HEADING);
    }
}
