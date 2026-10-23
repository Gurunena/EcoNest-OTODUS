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
     * Installs FlatLaf and seeds the global defaults - rounded corners, the
     * base font, the brand focus colour - so components nobody has styled by
     * hand still look right. Specific colours still come from {@link Theme}.
     */
    private static void installLookAndFeel() {
        try {
            FlatLightLaf.setup();

            // Typography
            UIManager.put("defaultFont", Theme.FONT_BODY);

            // Rounded everything
            UIManager.put("Component.arc", Theme.CORNER_RADIUS);
            UIManager.put("Button.arc", Theme.CORNER_RADIUS);
            UIManager.put("TextComponent.arc", Theme.CORNER_RADIUS);
            UIManager.put("ScrollBar.thumbArc", Theme.PILL_RADIUS);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track", Theme.PAGE_BG);
            UIManager.put("ScrollBar.thumb", Theme.BORDER_STRONG);
            UIManager.put("ScrollBar.width", 10);

            // Brand accents
            UIManager.put("Component.focusColor", Theme.ACCENT);
            UIManager.put("Component.focusedBorderColor", Theme.ACCENT);
            UIManager.put("Component.borderColor", Theme.BORDER_STRONG);
            UIManager.put("Component.focusWidth", 1);

            // Surfaces
            UIManager.put("Panel.background", Theme.PAGE_BG);
            UIManager.put("TextField.background", Theme.CARD_BG);
            UIManager.put("ComboBox.background", Theme.CARD_BG);
            UIManager.put("Table.background", Theme.CARD_BG);
            UIManager.put("Table.selectionBackground", Theme.ACCENT_SOFT);
            UIManager.put("Table.selectionForeground", Theme.TEXT_PRIMARY);
            UIManager.put("TableHeader.background", Theme.CARD_BG);
            UIManager.put("TableHeader.separatorColor", Theme.CARD_BORDER);

            // Tooltips, which the icon-only buttons rely on
            UIManager.put("ToolTip.background", Theme.TEXT_PRIMARY);
            UIManager.put("ToolTip.foreground", Theme.TEXT_ON_ACCENT);
            UIManager.put("ToolTip.font", Theme.FONT_SMALL);
            javax.swing.ToolTipManager.sharedInstance().setInitialDelay(400);
        } catch (Exception ex) {
            // Not fatal: Swing falls back to the default look and feel.
            System.err.println("Could not install FlatLaf: " + ex.getMessage());
        }
    }
}
