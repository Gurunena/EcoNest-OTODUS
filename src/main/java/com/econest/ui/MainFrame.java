package com.econest.ui;

import com.econest.delivery.ui.DeliveryAssignmentPanel;
import com.econest.inventory.ui.InventoryPanel;
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
        // TODO: each member adds their module's tab here once its panel is built
        tabs.addTab("Inventory", new InventoryPanel());
        tabs.addTab("Deliveries", new DeliveryAssignmentPanel());
        tabs.addTab("Reports", new ReportPanel());
        setContentPane(tabs);
    }
}
