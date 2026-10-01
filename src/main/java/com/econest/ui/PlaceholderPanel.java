package com.econest.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * Base for module screens that haven't been built yet. Replace by extending
 * JPanel directly once a module's UI is implemented.
 */
public abstract class PlaceholderPanel extends JPanel {

    protected PlaceholderPanel(String title) {
        super(new BorderLayout());
        add(new JLabel(title + " — coming soon", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
