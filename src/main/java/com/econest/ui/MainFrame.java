
package com.econest.ui;

// Module panels
import com.econest.client.ui.ClientPanel;
import com.econest.delivery.ui.DeliveryAssignmentPanel;
import com.econest.inventory.ui.InventoryPanel;
import com.econest.notification.ui.NotificationPanel;
import com.econest.order.ui.OrderPanel;
import com.econest.report.ui.ReportPanel;

// Task 1 - Products
import com.econest.product.ui.ProductManagement;

// Task 4 - Delivery Agents
import com.econest.agent.ui.DeliveryAgentPanel;

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

public class MainFrame extends JFrame {

    public static final String PAGE_DASHBOARD = "Dashboard";
    public static final String PAGE_PRODUCTS = "Products";
    public static final String PAGE_CLIENTS = "Clients";
    public static final String PAGE_INVENTORY = "Inventory";
    public static final String PAGE_AGENTS = "Delivery Agents";
    public static final String PAGE_ORDERS = "Orders";
    public static final String PAGE_ASSIGNMENT = "Delivery Assignment";
    public static final String PAGE_REPORTS = "Reports";
    public static final String PAGE_NOTIFICATIONS = "Notifications";

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

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel pnlContent = new JPanel(contentLayout);
    private final JLabel lblStatus = new JLabel(" ");

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, String> navIcons = new LinkedHashMap<>();

    private String activePage;

    public MainFrame() {

        super("EcoNest - Home & Garden Retail");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
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

    // PAGE REGISTRATION

    private void registerPages() {

        // Dashboard
        addPage(PAGE_DASHBOARD, new DashboardPanel());

        // Task 1 - Products
        addPage(PAGE_PRODUCTS, new ProductManagement());

        // Clients
        addPage(PAGE_CLIENTS, new ClientPanel());

        // Inventory
        addPage(PAGE_INVENTORY, new InventoryPanel());

        // Task 4 - Delivery Agents
        addPage(PAGE_AGENTS, new DeliveryAgentPanel());

        // Orders
        addPage(PAGE_ORDERS, new OrderPanel());

        // Delivery Assignment
        addPage(PAGE_ASSIGNMENT, new DeliveryAssignmentPanel());

        // Reports
        addPage(PAGE_REPORTS, new ReportPanel());

        // Notifications module
        addPage(PAGE_NOTIFICATIONS, new NotificationPanel());
    }

    // ADD PAGE

    private void addPage(String pageName, JComponent page) {
        pnlContent.add(page, pageName);
    }

    // SHOW PAGE

    public void showPage(String pageName) {

        contentLayout.show(pnlContent, pageName);

        activePage = pageName;

        refreshNavHighlight();
    }

    public String getActivePage() {
        return activePage;
    }

    // STATUS BAR

    public void setStatus(String message) {

        lblStatus.setText(
                message == null || message.isBlank() ? " " : message
        );
    }

    // SIDEBAR

    private JPanel buildSidebar() {

        JPanel pnlSidebar = new JPanel();

        pnlSidebar.setLayout(
                new BoxLayout(pnlSidebar, BoxLayout.Y_AXIS)
        );

        pnlSidebar.setBackground(Theme.SIDEBAR_BG);

        pnlSidebar.setPreferredSize(
                new Dimension(Theme.SIDEBAR_WIDTH, 0)
        );

        pnlSidebar.setMinimumSize(
                new Dimension(Theme.SIDEBAR_WIDTH, 0)
        );

        pnlSidebar.setBorder(
                BorderFactory.createEmptyBorder(
                        Theme.GAP_LARGE, 0, Theme.GAP, 0
                )
        );

        pnlSidebar.add(buildLogoBlock());

        pnlSidebar.add(
                Box.createVerticalStrut(Theme.GAP_LARGE)
        );

        for (String[] item : NAV_ITEMS) {

            String pageName = item[0];
            String iconName = item[1];

            boolean startsGroup = "group".equals(item[2]);

            if (startsGroup && !navButtons.isEmpty()) {

                pnlSidebar.add(
                        Box.createVerticalStrut(Theme.GAP_SMALL)
                );

                pnlSidebar.add(sidebarDivider());

                pnlSidebar.add(
                        Box.createVerticalStrut(Theme.GAP_SMALL)
                );
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

    // LOGO

    private JPanel buildLogoBlock() {

        JPanel pnlLogo = new JPanel();

        pnlLogo.setLayout(
                new BoxLayout(pnlLogo, BoxLayout.Y_AXIS)
        );

        pnlLogo.setOpaque(false);

        pnlLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlLogo.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 140)
        );

        pnlLogo.setBorder(
                BorderFactory.createEmptyBorder(
                        0, Theme.PADDING, 0, Theme.PADDING
                )
        );

        Icon logo = Icons.image("logo.png", 56, 56);

        if (logo != null) {

            JLabel lblLogoImage = new JLabel(logo);

            lblLogoImage.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlLogo.add(lblLogoImage);

            pnlLogo.add(
                    Box.createVerticalStrut(Theme.GAP_SMALL + 2)
            );
        }

        JLabel lblWordmark = new JLabel("EcoNest");

        lblWordmark.setFont(Theme.FONT_TITLE);
        lblWordmark.setForeground(Color.WHITE);
        lblWordmark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTagline = new JLabel("Home & Garden Retail");

        lblTagline.setFont(Theme.FONT_SMALL);

        lblTagline.setForeground(
                Theme.alpha(Theme.TEXT_ON_DARK, 170)
        );

        lblTagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlLogo.add(lblWordmark);

        pnlLogo.add(Box.createVerticalStrut(2));

        pnlLogo.add(lblTagline);

        return pnlLogo;
    }

    // SIDEBAR DIVIDER

    private JComponent sidebarDivider() {

        JPanel divider = new JPanel();

        divider.setBackground(Theme.SIDEBAR_DIVIDER);

        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        divider.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1)
        );

        divider.setPreferredSize(
                new Dimension(Theme.SIDEBAR_WIDTH, 1)
        );

        return divider;
    }

    // NAVIGATION BUTTON

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

        btnNav.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        btnNav.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        Theme.NAV_PILL_INSET + Theme.GAP_SMALL + 4,
                        0,
                        Theme.GAP
                )
        );

        btnNav.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnNav.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Theme.NAV_BUTTON_HEIGHT
                )
        );

        btnNav.setPreferredSize(
                new Dimension(
                        Theme.SIDEBAR_WIDTH,
                        Theme.NAV_BUTTON_HEIGHT
                )
        );

        btnNav.setToolTipText(pageName);

        btnNav.addActionListener(
                e -> showPage(pageName)
        );

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

    // REFRESH NAVIGATION HIGHLIGHT

    private void refreshNavHighlight() {

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {

            String pageName = entry.getKey();

            boolean isActive = pageName.equals(activePage);

            JButton btnNav = entry.getValue();

            Color foreground = isActive
                    ? Color.WHITE
                    : Theme.TEXT_ON_DARK;

            btnNav.setForeground(foreground);

            btnNav.setFont(
                    isActive
                            ? Theme.FONT_BODY_BOLD
                            : Theme.FONT_BODY
            );

            btnNav.setIcon(
                    Icons.icon(
                            navIcons.get(pageName),
                            Theme.ICON_SIZE_NAV,
                            foreground
                    )
            );

            if (btnNav instanceof NavButton nav) {

                nav.active = isActive;
            }

            btnNav.repaint();
        }
    }

    // NAVIGATION BUTTON DESIGN

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

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(
                        active
                                ? Theme.SIDEBAR_SELECTED
                                : Theme.SIDEBAR_HOVER
                );

                g2.fillRoundRect(
                        Theme.NAV_PILL_INSET,
                        2,
                        getWidth() - 2 * Theme.NAV_PILL_INSET,
                        getHeight() - 4,
                        Theme.CORNER_RADIUS,
                        Theme.CORNER_RADIUS
                );

                g2.dispose();
            }

            super.paintComponent(g);
        }
    }

    // STATUS BAR

    private JPanel buildStatusBar() {

        lblStatus.setFont(Theme.FONT_SMALL);

        lblStatus.setForeground(Theme.TEXT_MUTED);

        JPanel pnlStatus = new JPanel(new BorderLayout());

        pnlStatus.setBackground(Theme.STATUS_BAR_BG);

        pnlStatus.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                1, 0, 0, 0, Theme.CARD_BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                Theme.GAP_SMALL,
                                Theme.PADDING,
                                Theme.GAP_SMALL,
                                Theme.PADDING
                        )
                )
        );

        pnlStatus.add(lblStatus, BorderLayout.CENTER);

        return pnlStatus;
    }
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
