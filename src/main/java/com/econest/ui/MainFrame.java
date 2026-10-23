package com.econest.ui;

// --- Module panels -------------------------------------------------------
// One import per module, in sidebar order. When you replace a placeholder
// with your real panel, add your import here and change your single line in
// registerPages() below.
import com.econest.delivery.ui.DeliveryAssignmentPanel;
import com.econest.inventory.ui.InventoryPanel;
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
     * The nine navigation items, in the order they appear in the sidebar,
     * each paired with its icon in {@code resources/images/icons}. The icon
     * name has no extension - {@link Icons} resolves it - and a missing file
     * just means that item shows without an icon.
     */
    private static final String[][] NAV_ITEMS = {
            {PAGE_DASHBOARD, "dashboard"},
            {PAGE_PRODUCTS, "products"},
            {PAGE_CLIENTS, "clients"},
            {PAGE_INVENTORY, "inventory"},
            {PAGE_AGENTS, "agents"},
            {PAGE_ORDERS, "orders"},
            {PAGE_ASSIGNMENT, "assignment"},
            {PAGE_REPORTS, "reports"},
            {PAGE_NOTIFICATIONS, "notifications"}
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
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        pnlContent.setBackground(Theme.CONTENT_BG);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(pnlContent, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        registerPages();

        // Open on the dashboard, with its sidebar item highlighted.
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
        addPage(PAGE_ORDERS, new PlaceholderPanel(PAGE_ORDERS, "Orders owner"));

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

    /**
     * Adds a page to the content area under the given name. The name must be
     * one of the {@code PAGE_*} constants so the sidebar can find it.
     */
    private void addPage(String pageName, JComponent page) {
        pnlContent.add(page, pageName);
    }

    /**
     * Brings a page to the front and moves the sidebar highlight with it, so
     * the highlighted item is always the page you are looking at.
     *
     * @param pageName one of the {@code PAGE_*} constants
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
     *
     * @param message the text to show; null or blank clears the bar
     */
    public void setStatus(String message) {
        lblStatus.setText(message == null || message.isBlank() ? " " : message);
    }

    /** Builds the dark green sidebar with the logo and the nav buttons. */
    private JPanel buildSidebar() {
        JPanel pnlSidebar = new JPanel();
        pnlSidebar.setLayout(new BoxLayout(pnlSidebar, BoxLayout.Y_AXIS));
        pnlSidebar.setBackground(Theme.SIDEBAR_BG);
        pnlSidebar.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        pnlSidebar.setBorder(BorderFactory.createEmptyBorder(
                Theme.GAP_LARGE, 0, Theme.GAP_LARGE, 0));

        // Logo block: the artwork when images/logo.png is present, otherwise
        // just the "EcoNest" wordmark. Either way the tagline sits under it.
        Icon logo = Icons.image("logo.png", 150, 52);
        if (logo != null) {
            JLabel lblLogoImage = new JLabel(logo);
            lblLogoImage.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblLogoImage.setBorder(BorderFactory.createEmptyBorder(
                    0, Theme.PADDING, Theme.GAP_SMALL, Theme.PADDING));
            pnlSidebar.add(lblLogoImage);
        }

        JLabel lblLogo = new JLabel("EcoNest");
        lblLogo.setFont(Theme.FONT_TITLE);
        lblLogo.setForeground(Theme.ACCENT);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblLogo.setBorder(BorderFactory.createEmptyBorder(0, Theme.PADDING, 0, Theme.PADDING));

        JLabel lblTagline = new JLabel("Home & Garden Retail");
        lblTagline.setFont(Theme.FONT_SMALL);
        lblTagline.setForeground(Theme.TEXT_ON_DARK);
        lblTagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTagline.setBorder(BorderFactory.createEmptyBorder(0, Theme.PADDING, 0, Theme.PADDING));

        pnlSidebar.add(lblLogo);
        pnlSidebar.add(Box.createVerticalStrut(2));
        pnlSidebar.add(lblTagline);
        pnlSidebar.add(Box.createVerticalStrut(Theme.GAP_LARGE));

        // One button per nav item, in order, each with its icon.
        for (String[] item : NAV_ITEMS) {
            String pageName = item[0];
            String iconName = item[1];
            JButton btnNav = buildNavButton(pageName, iconName);
            navButtons.put(pageName, btnNav);
            navIcons.put(pageName, iconName);
            pnlSidebar.add(btnNav);
            pnlSidebar.add(Box.createVerticalStrut(2));
        }

        pnlSidebar.add(Box.createVerticalGlue());
        return pnlSidebar;
    }

    /** A single flat sidebar button that switches to its page when clicked. */
    private JButton buildNavButton(String pageName, String iconName) {
        JButton btnNav = new JButton(pageName);
        btnNav.setFont(Theme.FONT_BODY);
        btnNav.setIconTextGap(Theme.GAP);
        btnNav.setForeground(Theme.TEXT_ON_DARK);
        btnNav.setBackground(Theme.SIDEBAR_BG);
        btnNav.setHorizontalAlignment(SwingConstants.LEFT);
        btnNav.setFocusPainted(false);
        btnNav.setBorderPainted(false);
        btnNav.setContentAreaFilled(true);
        btnNav.setOpaque(true);
        btnNav.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNav.setBorder(BorderFactory.createEmptyBorder(0, Theme.PADDING, 0, Theme.PADDING));
        btnNav.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnNav.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_BUTTON_HEIGHT));
        btnNav.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, Theme.NAV_BUTTON_HEIGHT));

        btnNav.addActionListener(e -> showPage(pageName));

        // Hover feedback, but never on the active item: its highlight wins.
        btnNav.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!pageName.equals(activePage)) {
                    btnNav.setBackground(Theme.SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!pageName.equals(activePage)) {
                    btnNav.setBackground(Theme.SIDEBAR_BG);
                }
            }
        });
        return btnNav;
    }

    /**
     * Repaints every nav button so exactly one - the active page - is
     * highlighted. Driven by {@link #activePage}, so the highlight can never
     * drift away from the page on screen.
     */
    private void refreshNavHighlight() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            String pageName = entry.getKey();
            boolean isActive = pageName.equals(activePage);
            JButton btnNav = entry.getValue();

            Color foreground = isActive ? Theme.TEXT_ON_ACCENT : Theme.TEXT_ON_DARK;
            btnNav.setBackground(isActive ? Theme.ACCENT : Theme.SIDEBAR_BG);
            btnNav.setForeground(foreground);
            btnNav.setFont(isActive ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY);

            // Re-tint the icon so it tracks the text colour. Null when the
            // SVG is missing, which just leaves the button text-only.
            btnNav.setIcon(Icons.icon(
                    navIcons.get(pageName), Theme.ICON_SIZE_NAV, foreground));
        }
    }

    /** The one-line status bar along the bottom of the window. */
    private JPanel buildStatusBar() {
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_MUTED);

        JPanel pnlStatus = new JPanel(new BorderLayout());
        pnlStatus.setBackground(Theme.STATUS_BAR_BG);
        pnlStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(
                        Theme.GAP_SMALL, Theme.PADDING, Theme.GAP_SMALL, Theme.PADDING)));
        pnlStatus.add(lblStatus, BorderLayout.WEST);
        return pnlStatus;
    }
}
