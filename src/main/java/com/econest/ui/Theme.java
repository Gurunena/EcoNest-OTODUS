package com.econest.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Single source of truth for every colour, font, radius and spacing value in
 * the EcoNest UI.
 *
 * <p><b>Rule for the whole team:</b> never write {@code new Color(...)} or
 * {@code new Font(...)} inside a module panel. Read the constant from here
 * instead. If you need a shade that does not exist yet, add it to this class
 * so the rest of the app can use it too.</p>
 *
 * <p>Everything is {@code static final} - there is nothing to instantiate.
 * Every constant that existed before the visual polish is still here with the
 * same name, so module code keeps compiling; only the values moved.</p>
 */
public final class Theme {

    private Theme() {
        // Utility class: constants only.
    }

    // ---------------------------------------------------------------------
    // Brand colours
    // ---------------------------------------------------------------------

    /** Dark green used for the navigation sidebar background. */
    public static final Color SIDEBAR_BG = new Color(0x15, 0x3C, 0x32);

    /** Slightly lighter green for a hovered sidebar item. */
    public static final Color SIDEBAR_HOVER = new Color(0x1E, 0x52, 0x44);

    /** The selected sidebar pill. */
    public static final Color SIDEBAR_SELECTED = new Color(0x2A, 0x7D, 0x5F);

    /** Divider drawn inside the sidebar. */
    public static final Color SIDEBAR_DIVIDER = new Color(0x24, 0x56, 0x49);

    /** Primary green: primary buttons, focus rings, highlights. */
    public static final Color ACCENT = new Color(0x2E, 0x9E, 0x6B);

    /** A darker accent for the pressed state of a primary button. */
    public static final Color ACCENT_DARK = new Color(0x24, 0x7E, 0x55);

    /** Very pale green for selected table rows and soft highlights. */
    public static final Color ACCENT_SOFT = new Color(0xE4, 0xF5, 0xEC);

    /** Even fainter green, for table row hover. */
    public static final Color ACCENT_FAINT = new Color(0xF1, 0xFA, 0xF5);

    // ---------------------------------------------------------------------
    // Surfaces
    // ---------------------------------------------------------------------

    /** The page background behind the cards. */
    public static final Color PAGE_BG = new Color(0xF6, 0xF8, 0xF7);

    /** White card surface. */
    public static final Color CARD_BG = Color.WHITE;

    /** Border around a card. */
    public static final Color CARD_BORDER = new Color(0xE5, 0xEA, 0xE7);

    /** The soft shadow painted under a card. */
    public static final Color CARD_SHADOW = new Color(0x0F, 0x1F, 0x1A, 18);

    /**
     * Background of the main content area. Same as {@link #PAGE_BG} now that
     * content sits on cards rather than directly on white.
     */
    public static final Color CONTENT_BG = PAGE_BG;

    /** Off-white background behind grouped form controls. */
    public static final Color PANEL_BG = new Color(0xFA, 0xFC, 0xFB);

    /** Alternate table row background. Rows are plain now, so this matches the card. */
    public static final Color ROW_ALT_BG = Color.WHITE;

    /** Thin separator / component border colour. */
    public static final Color BORDER = new Color(0xE5, 0xEA, 0xE7);

    /** A slightly stronger border, for inputs at rest. */
    public static final Color BORDER_STRONG = new Color(0xD3, 0xDC, 0xD7);

    /** Background of the status bar at the bottom of the window. */
    public static final Color STATUS_BAR_BG = new Color(0xF0, 0xF4, 0xF2);

    // ---------------------------------------------------------------------
    // Text colours
    // ---------------------------------------------------------------------

    /** Main body / table text. */
    public static final Color TEXT_PRIMARY = new Color(0x16, 0x23, 0x1E);

    /** Secondary labels, hints, placeholder text. */
    public static final Color TEXT_MUTED = new Color(0x6B, 0x7B, 0x74);

    /** Column headers and other small uppercase labels. */
    public static final Color TEXT_FAINT = new Color(0x8A, 0x99, 0x92);

    /** Text drawn on the dark sidebar. */
    public static final Color TEXT_ON_DARK = new Color(0xC9, 0xDE, 0xD5);

    /** Text drawn on an accent-coloured surface. */
    public static final Color TEXT_ON_ACCENT = Color.WHITE;

    // ---------------------------------------------------------------------
    // Status colours
    // ---------------------------------------------------------------------

    /** Green - good / delivered / in stock / active. */
    public static final Color STATUS_SUCCESS = new Color(0x1E, 0x7A, 0x4B);

    /** Amber - pending / in progress / awaiting action. */
    public static final Color STATUS_PENDING = new Color(0xB4, 0x72, 0x00);

    /** Red - low stock / failed / cancelled / validation error. */
    public static final Color STATUS_DANGER = new Color(0xC0, 0x29, 0x3C);

    /** Blue - informational / neutral notices. */
    public static final Color STATUS_INFO = new Color(0x1A, 0x5F, 0xB4);

    // ---------------------------------------------------------------------
    // Status tints
    //
    // Pale backgrounds for pill badges and KPI cards. Pair each tint with its
    // status colour for the text or icon drawn on top.
    // ---------------------------------------------------------------------

    /** Pale green, pairs with {@link #STATUS_SUCCESS}. */
    public static final Color TINT_SUCCESS = new Color(0xE3, 0xF6, 0xEA);

    /** Pale amber, pairs with {@link #STATUS_PENDING}. */
    public static final Color TINT_PENDING = new Color(0xFD, 0xF2, 0xDE);

    /** Pale red, pairs with {@link #STATUS_DANGER}. */
    public static final Color TINT_DANGER = new Color(0xFC, 0xE9, 0xEC);

    /** Pale blue, pairs with {@link #STATUS_INFO}. */
    public static final Color TINT_INFO = new Color(0xE6, 0xEF, 0xFB);

    // ---------------------------------------------------------------------
    // Fonts
    //
    // One family for the whole app. Inter when it is installed, otherwise
    // Segoe UI, otherwise whatever sans-serif the platform gives us.
    // ---------------------------------------------------------------------

    /** The single font family used everywhere. */
    public static final String FONT_FAMILY = pickFontFamily("Inter", "Segoe UI", "SansSerif");

    /** Base size. Everything else is derived from it. */
    public static final int FONT_SIZE_BASE = 14;

    /** Big numbers on the dashboard cards. */
    public static final Font FONT_DISPLAY = new Font(FONT_FAMILY, Font.BOLD, 28);

    /** Page titles. */
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);

    /** Card and section headings. */
    public static final Font FONT_HEADING = new Font(FONT_FAMILY, Font.BOLD, 15);

    /** Default text: labels, fields, table cells, buttons. */
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE_BASE);

    /** Bold body text. */
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE_BASE);

    /** Small print: hints, inline errors, the status bar. */
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);

    /** Uppercase table headers and other micro labels. */
    public static final Font FONT_LABEL = new Font(FONT_FAMILY, Font.BOLD, 12);

    /** Text inside a status pill. */
    public static final Font FONT_PILL = new Font(FONT_FAMILY, Font.BOLD, 11);

    // ---------------------------------------------------------------------
    // Spacing and sizing
    // ---------------------------------------------------------------------

    /** Tight gap between closely related components. */
    public static final int GAP_SMALL = 8;

    /** Standard gap between components. */
    public static final int GAP = 16;

    /** Generous gap between major sections. */
    public static final int GAP_LARGE = 24;

    /** Padding inside a page, outside the cards. */
    public static final int PADDING = 24;

    /** Padding inside a card. */
    public static final int CARD_PADDING = 20;

    /** Height of one table row. */
    public static final int TABLE_ROW_HEIGHT = 44;

    /** Height of the table header. */
    public static final int TABLE_HEADER_HEIGHT = 40;

    /** Width of the navigation sidebar. */
    public static final int SIDEBAR_WIDTH = 236;

    /** Height of one sidebar navigation item. */
    public static final int NAV_BUTTON_HEIGHT = 44;

    /** Horizontal inset of the sidebar's selected pill. */
    public static final int NAV_PILL_INSET = 12;

    /** Corner radius for buttons, fields and other small components. */
    public static final int CORNER_RADIUS = 12;

    /** Corner radius for cards. */
    public static final int CARD_RADIUS = 16;

    /** Corner radius for a status pill - large enough to be a capsule. */
    public static final int PILL_RADIUS = 999;

    /** Default width of a text field in a form. */
    public static final int FIELD_WIDTH = 220;

    /** Default height of a text field / combo box. */
    public static final int FIELD_HEIGHT = 40;

    /** Size of the small icons beside the sidebar navigation items. */
    public static final int ICON_SIZE_NAV = 18;

    /** Size of the icons on the dashboard cards. */
    public static final int ICON_SIZE_CARD = 20;

    /** Diameter of the tinted circle behind a dashboard card icon. */
    public static final int ICON_CIRCLE_SIZE = 44;

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Returns the first of these families that is actually installed, so the
     * app uses Inter where it exists and falls back cleanly where it does not.
     */
    private static String pickFontFamily(String... candidates) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            if (Arrays.stream(installed).anyMatch(name -> name.equalsIgnoreCase(candidate))) {
                return candidate;
            }
        }
        return "SansSerif";
    }

    /** The same colour at a different alpha - handy for hovers and shadows. */
    public static Color alpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }
}
