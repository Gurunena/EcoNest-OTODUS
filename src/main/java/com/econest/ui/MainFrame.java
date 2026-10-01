package com.econest.ui;

import com.econest.agent.ui.DeliveryAgentPanel;
import com.econest.client.ui.ClientPanel;
import com.econest.delivery.ui.DeliveryAssignmentPanel;
import com.econest.inventory.ui.InventoryPanel;
import com.econest.notification.ui.NotificationPanel;
import com.econest.order.ui.OrderPanel;
import com.econest.product.ui.ProductPanel;
import com.econest.report.ui.ReportPanel;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("EcoNest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", new ProductPanel());
        tabs.addTab("Clients", new ClientPanel());
        tabs.addTab("Inventory", new InventoryPanel());
        tabs.addTab("Delivery Agents", new DeliveryAgentPanel());
        tabs.addTab("Orders", new OrderPanel());
        tabs.addTab("Deliveries", new DeliveryAssignmentPanel());
        tabs.addTab("Reports", new ReportPanel());
        tabs.addTab("Notifications", new NotificationPanel());
        setContentPane(tabs);
    }
}
