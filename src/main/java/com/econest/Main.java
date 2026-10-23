package com.econest;

import com.econest.ui.MainFrame;
import com.econest.ui.Theme;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * <p>Everything Swing-related happens on the Event Dispatch Thread, including
 * installing the look and feel: setting it off the EDT can leave components
 * created before the switch with stale colours.</p>
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            new MainFrame().setVisible(true);
        });
    }

    /**
     * Installs FlatLaf and applies the handful of global defaults that are
     * easier to set once here than on every component. Colours and fonts
     * still come from {@link Theme}; this only seeds the defaults.
     */
    private static void installLookAndFeel() {
        try {
            FlatLightLaf.setup();

            // Global defaults so untouched components still look on-brand.
            UIManager.put("defaultFont", Theme.FONT_BODY);
            UIManager.put("Component.focusColor", Theme.ACCENT);
            UIManager.put("Component.focusedBorderColor", Theme.ACCENT);
            UIManager.put("Component.borderColor", Theme.BORDER);
            UIManager.put("Button.arc", Theme.CORNER_RADIUS);
            UIManager.put("Component.arc", Theme.CORNER_RADIUS);
            UIManager.put("TextComponent.arc", Theme.CORNER_RADIUS);
            UIManager.put("ScrollBar.thumbArc", Theme.CORNER_RADIUS);
            UIManager.put("Table.selectionBackground", Theme.ACCENT_SOFT);
            UIManager.put("Table.selectionForeground", Theme.TEXT_PRIMARY);
        } catch (Exception ex) {
            // Not fatal: Swing falls back to the default look and feel.
            System.err.println("Could not install FlatLaf: " + ex.getMessage());
        }
    }
}
