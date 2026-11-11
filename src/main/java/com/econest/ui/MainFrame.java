package com.econest.ui;

// --- Module panels -------------------------------------------------------
// One import per module, in sidebar order. When you replace a placeholder
// with your real panel, add your import here and change your single line in
// registerPages() below.
import com.econest.delivery.ui.DeliveryAssignmentPanel;
import com.econest.inventory.ui.InventoryPanel;
import com.econest.order.ui.OrderPanel;
import com.econest.report.ui.ReportPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The application shell: a dark green navigation sidebar on the left, a
 * CardLayout content area on the right and a status bar along the bottom.
 *
 * <p>Teammates touch exactly <b>one line</b> of this class - their own line in
 * {@link #registerPages()}. Everything else (sidebar, highlighting, status
 * bar) is shared plumbing.</p>
 */
public class MainFrame extends JFrame {

    // ---------------------------------------------------------------------
    // Page keys. The sidebar order is the order of these constants.
    // ---------------------------------------------------------------------

    public static final String PAGE_DASHBOARD = "Dashboard";
    public static final String PAGE_PRODUCTS = "Products";
    public static final String PAGE_CLIENTS = "Clients";
    public static final String PAGE_INVENTORY = "Inventory";
    public static final String PAGE_AGENTS = "Delivery Agents";
    public static final String PAGE_ORDERS = "Orders";
    public static final String PAGE_ASSIGNMENT = "Delivery Assignment";
    public static final String PAGE_REPORTS = "Reports";
    public static final String PAGE_NOTIFICATIONS = "Notifications";

    /**
     * The nine navigation items, in sidebar order, each with its icon in
     * {@code resources/images/icons}. "group" in the third slot draws a
     * divider above that item.
     */
    private static final String[][] NAV_ITEMS = {
            {PAGE_DASHBOARD, "dashboard", ""},
            {PAGE_PRODUCTS, "products", "group"},
            {PAGE_CLIENTS, "clients", ""},
            {PAGE_INVENTORY, "inventory", ""},
            {PAGE_AGENTS, "agents", "group"},
            {PAGE_ORDERS, "orders", ""},
            {PAGE_ASSIGNMENT, "assignment", ""},
            {PAGE_REPORTS, "reports", "group"},
            {PAGE_NOTIFICATIONS, "notifications", ""}
    };

    // ---------------------------------------------------------------------
    // Shell components
    // ---------------------------------------------------------------------

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel pnlContent = new JPanel(contentLayout);
    private final JLabel lblStatus = new JLabel(" ");

    /** Sidebar buttons by page name, so we can repaint the active one. */
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    /** Icon name per page, so the icon can be re-tinted with the highlight. */
    private final Map<String, String> navIcons = new LinkedHashMap<>();

    /** The page currently on screen; the sidebar highlight always matches it. */
    private String activePage;

    public MainFrame() {
        super("EcoNest - Home & Garden Retail");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        // Comfortably inside a 1366x768 screen, so the layout never clips.
        setMinimumSize(new Dimension(1100, 660));
        setLocationRelativeTo(null);

        pnlContent.setBackground(Theme.PAGE_BG);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(pnlContent, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        registerPages();

        showPage(PAGE_DASHBOARD);
        setStatus("Ready.");
    }

    // =====================================================================
    // PAGE REGISTRATION
    //
    // ONE LINE PER MODULE, each separated by a blank line so two people
    // editing different modules never collide in the same diff hunk.
    //
    // To plug in your screen: import your panel at the top of this file and
    // replace the PlaceholderPanel on YOUR line with "new YourPanel()".
    // Do not reorder or reformat anyone else's line.
    // =====================================================================
    private void registerPages() {

        // Dashboard - shared landing page (UI foundation owner)
        addPage(PAGE_DASHBOARD, new DashboardPanel());

        // Products module
        addPage(PAGE_PRODUCTS, new PlaceholderPanel(PAGE_PRODUCTS, "Products owner"));

        // Clients module
        addPage(PAGE_CLIENTS, new PlaceholderPanel(PAGE_CLIENTS, "Clients owner"));

        // Stock / Inventory module
        addPage(PAGE_INVENTORY, new InventoryPanel());

        // Delivery Agents module
        addPage(PAGE_AGENTS, new PlaceholderPanel(PAGE_AGENTS, "Delivery Agents owner"));

        // Orders module
        addPage(PAGE_ORDERS, new OrderPanel());

        // Delivery Assignment module
        addPage(PAGE_ASSIGNMENT, new DeliveryAssignmentPanel());

        // Reports module
        addPage(PAGE_REPORTS, new ReportPanel());

        // Notifications module
        addPage(PAGE_NOTIFICATIONS, new PlaceholderPanel(PAGE_NOTIFICATIONS, "Notifications owner"));
    }

    // ---------------------------------------------------------------------
    // Shell plumbing
    // ---------------------------------------------------------------------

    /** Adds a page to the content area under the given name. */
    private void addPage(String pageName, JComponent page) {
        pnlContent.add(page, pageName);
    }

    /**
     * Brings a page to the front and moves the sidebar highlight with it, so
     * the highlighted item is always the page you are looking at.
     */
    public void showPage(String pageName) {
        contentLayout.show(pnlContent, pageName);
        activePage = pageName;
        refreshNavHighlight();
    }

    /** The page currently showing. */
    public String getActivePage() {
        return activePage;
    }

    /**
     * Shows a short message in the status bar at the bottom of the window.
     * Any panel can reach this through {@code BaseModulePanel.setStatus(...)}.
     */
    public void setStatus(String message) {
        lblStatus.setText(message == null || message.isBlank() ? " " : message);
    }

    /** Builds the dark green sidebar with the logo and the nav items. */
    private JPanel buildSidebar() {
        JPanel pnlSidebar = new JPanel();
        pnlSidebar.setLayout(new BoxLayout(pnlSidebar, BoxLayout.Y_AXIS));
        pnlSidebar.setBackground(Theme.SIDEBAR_BG);
        pnlSidebar.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        pnlSidebar.setMinimumSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        pnlSidebar.setBorder(BorderFactory.createEmptyBorder(Theme.GAP_LARGE, 0, Theme.GAP, 0));

        pnlSidebar.add(buildLogoBlock());
        pnlSidebar.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        for (String[] item : NAV_ITEMS) {
            String pageName = item[0];
            String iconName = item[1];
            boolean startsGroup = "group".equals(item[2]);

            if (startsGroup && !navButtons.isEmpty()) {
                pnlSidebar.add(Box.createVerticalStrut(Theme.GAP_SMALL));
                pnlSidebar.add(sidebarDivider());
                pnlSidebar.add(Box.createVerticalStrut(Theme.GAP_SMALL));
            }

            JButton btnNav = buildNavButton(pageName, iconName);
            navButtons.put(pageName, btnNav);
            navIcons.put(pageName, iconName);
            pnlSidebar.add(btnNav);
            pnlSidebar.add(Box.createVerticalStrut(2));
        }

        pnlSidebar.add(Box.createVerticalGlue());
        return pnlSidebar;
    }

    /** Logo image (when present), wordmark and tagline, with room to breathe. */
    private JPanel buildLogoBlock() {
        JPanel pnlLogo = new JPanel();
        pnlLogo.setLayout(new BoxLayout(pnlLogo, BoxLayout.Y_AXIS));
        pnlLogo.setOpaque(false);
        pnlLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlLogo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        pnlLogo.setBorder(BorderFactory.createEmptyBorder(
                0, Theme.PADDING, 0, Theme.PADDING));

        Icon logo = Icons.image("logo.png", 56, 56);
        if (logo != null) {
            JLabel lblLogoImage = new JLabel(logo);
            lblLogoImage.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlLogo.add(lblLogoImage);
            pnlLogo.add(Box.createVerticalStrut(Theme.GAP_SMALL + 2));
        }

        JLabel lblWordmark = new JLabel("EcoNest");
        lblWordmark.setFont(Theme.FONT_TITLE);
        lblWordmark.setForeground(Color.WHITE);
        lblWordmark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTagline = new JLabel("Home & Garden Retail");
        lblTagline.setFont(Theme.FONT_SMALL);
        lblTagline.setForeground(Theme.alpha(Theme.TEXT_ON_DARK, 170));
        lblTagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlLogo.add(lblWordmark);
        pnlLogo.add(Box.createVerticalStrut(2));
        pnlLogo.add(lblTagline);
        return pnlLogo;
    }

    /** A hairline divider between sidebar groups. */
    private JComponent sidebarDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(Theme.SIDEBAR_DIVIDER);
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 1));
        return divider;
    }

    /**
     * A sidebar item. The selected state is a rounded pill inset from the
     * sidebar edges rather than a full-width block, so it reads as a chip.
     */
    private JButton buildNavButton(String pageName, String iconName) {
        NavButton btnNav = new NavButton(pageName);
        btnNav.setFont(Theme.FONT_BODY);
        btnNav.setForeground(Theme.TEXT_ON_DARK);
        btnNav.setIconTextGap(Theme.GAP_SMALL + 2);
        btnNav.setHorizontalAlignment(SwingConstants.LEFT);
        btnNav.setFocusPainted(false);
        btnNav.setBorderPainted(false);
        btnNav.setContentAreaFilled(false);
        btnNav.setOpaque(false);
        btnNav.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNav.setBorder(BorderFactory.createEmptyBorder(
                0, Theme.NAV_PILL_INSET + Theme.GAP_SMALL + 4, 0, Theme.GAP));
        btnNav.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnNav.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_BUTTON_HEIGHT));
        btnNav.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, Theme.NAV_BUTTON_HEIGHT));
        btnNav.setToolTipText(pageName);

        btnNav.addActionListener(e -> showPage(pageName));
        btnNav.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnNav.hovered = true;
                btnNav.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnNav.hovered = false;
                btnNav.repaint();
            }
        });
        return btnNav;
    }

    /**
     * Repaints every nav item so exactly one - the active page - wears the
     * pill. Driven by {@link #activePage}, so the highlight can never drift
     * away from the page on screen.
     */
    private void refreshNavHighlight() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            String pageName = entry.getKey();
            boolean isActive = pageName.equals(activePage);
            JButton btnNav = entry.getValue();

            Color foreground = isActive ? Color.WHITE : Theme.TEXT_ON_DARK;
            btnNav.setForeground(foreground);
            btnNav.setFont(isActive ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY);
            btnNav.setIcon(Icons.icon(
                    navIcons.get(pageName), Theme.ICON_SIZE_NAV, foreground));

            if (btnNav instanceof NavButton nav) {
                nav.active = isActive;
            }
            btnNav.repaint();
        }
    }

    /** A sidebar item that paints its own rounded pill. */
    private static class NavButton extends JButton {

        boolean active;
        boolean hovered;

        NavButton(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? Theme.SIDEBAR_SELECTED : Theme.SIDEBAR_HOVER);
                g2.fillRoundRect(Theme.NAV_PILL_INSET, 2,
                        getWidth() - 2 * Theme.NAV_PILL_INSET, getHeight() - 4,
                        Theme.CORNER_RADIUS, Theme.CORNER_RADIUS);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** The one-line status bar along the bottom of the window. */
    private JPanel buildStatusBar() {
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_MUTED);

        JPanel pnlStatus = new JPanel(new BorderLayout());
        pnlStatus.setBackground(Theme.STATUS_BAR_BG);
        pnlStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(
                        Theme.GAP_SMALL, Theme.PADDING, Theme.GAP_SMALL, Theme.PADDING)));
        pnlStatus.add(lblStatus, BorderLayout.CENTER);
        return pnlStatus;
    }
}
