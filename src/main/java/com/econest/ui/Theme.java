package com.econest.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Single source of truth for every colour, font and spacing value in the
 * EcoNest UI.
 *
 * <p><b>Rule for the whole team:</b> never write {@code new Color(...)} or
 * {@code new Font(...)} inside a module panel. Read the constant from here
 * instead. If you need a shade that does not exist yet, add it to this class
 * so the rest of the app can use it too.</p>
 *
 * <p>Everything is {@code static final} — there is nothing to instantiate.</p>
 */
public final class Theme {

    private Theme() {
        // Utility class: constants only.
    }

    // ---------------------------------------------------------------------
    // Brand colours
    // ---------------------------------------------------------------------

    /** Dark green used for the navigation sidebar background. */
    public static final Color SIDEBAR_BG = new Color(0x1B, 0x4D, 0x3E);

    /** Slightly lighter green for a hovered sidebar button. */
    public static final Color SIDEBAR_HOVER = new Color(0x24, 0x66, 0x52);

    /** Light green accent: active nav item, primary buttons, table header. */
    public static final Color ACCENT = new Color(0x4C, 0xAF, 0x50);

    /** A darker accent for the pressed state of a primary button. */
    public static final Color ACCENT_DARK = new Color(0x3B, 0x8C, 0x3F);

    /** Very pale green used for selected table rows and soft highlights. */
    public static final Color ACCENT_SOFT = new Color(0xE3, 0xF4, 0xE4);

    // ---------------------------------------------------------------------
    // Surfaces
    // ---------------------------------------------------------------------

    /** White background of the main content area. */
    public static final Color CONTENT_BG = Color.WHITE;

    /** Off-white background behind cards / form areas. */
    public static final Color PANEL_BG = new Color(0xF5, 0xF7, 0xF5);

    /** Alternate (striped) table row background. */
    public static final Color ROW_ALT_BG = new Color(0xFA, 0xFB, 0xFA);

    /** Thin separator / component border colour. */
    public static final Color BORDER = new Color(0xDD, 0xE2, 0xDD);

    /** Background of the status bar at the bottom of the window. */
    public static final Color STATUS_BAR_BG = new Color(0xEE, 0xF2, 0xEE);

    // ---------------------------------------------------------------------
    // Text colours
    // ---------------------------------------------------------------------

    /** Main body / table text. */
    public static final Color TEXT_PRIMARY = new Color(0x1F, 0x29, 0x24);

    /** Secondary labels, hints, placeholder text. */
    public static final Color TEXT_MUTED = new Color(0x6B, 0x77, 0x70);

    /** Text drawn on the dark sidebar. */
    public static final Color TEXT_ON_DARK = new Color(0xE8, 0xF1, 0xEC);

    /** Text drawn on an accent-coloured surface (primary button, header). */
    public static final Color TEXT_ON_ACCENT = Color.WHITE;

    // ---------------------------------------------------------------------
    // Status colours
    //
    // Used by the status-column table renderer and by validation messages.
    // ---------------------------------------------------------------------

    /** Green — good / delivered / in stock / active. */
    public static final Color STATUS_SUCCESS = new Color(0x2E, 0x7D, 0x32);

    /** Amber — pending / in progress / awaiting action. */
    public static final Color STATUS_PENDING = new Color(0xE0, 0x8A, 0x00);

    /** Red — low stock / failed / cancelled / validation error. */
    public static final Color STATUS_DANGER = new Color(0xC6, 0x28, 0x28);

    /** Blue — informational / neutral notices. */
    public static final Color STATUS_INFO = new Color(0x15, 0x65, 0xC0);

    // ---------------------------------------------------------------------
    // Fonts
    //
    // One family for the whole app, four sizes. "Segoe UI" is the Windows
    // default; Swing falls back to a sensible sans-serif elsewhere.
    // ---------------------------------------------------------------------

    /** The single font family used everywhere. */
    public static final String FONT_FAMILY = "Segoe UI";

    /** Window / screen titles (e.g. the sidebar logo, a panel heading). */
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);

    /** Section headings inside a screen, table headers. */
    public static final Font FONT_HEADING = new Font(FONT_FAMILY, Font.BOLD, 15);

    /** Default text: labels, fields, table cells, buttons. */
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);

    /** Bold body text, for emphasis inside a form or a status message. */
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);

    /** Small print: hints, inline validation errors, the status bar. */
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);

    // ---------------------------------------------------------------------
    // Spacing and sizing
    //
    // Use these instead of magic numbers so every screen lines up.
    // ---------------------------------------------------------------------

    /** Tight gap between closely related components (e.g. label and field). */
    public static final int GAP_SMALL = 6;

    /** Standard gap between components in a row or column. */
    public static final int GAP = 12;

    /** Generous gap between major sections of a screen. */
    public static final int GAP_LARGE = 20;

    /** Inner padding applied to the content area of a screen. */
    public static final int PADDING = 16;

    /** Height of one table row. */
    public static final int TABLE_ROW_HEIGHT = 28;

    /** Height of the table header. */
    public static final int TABLE_HEADER_HEIGHT = 32;

    /** Width of the navigation sidebar. */
    public static final int SIDEBAR_WIDTH = 220;

    /** Height of one sidebar navigation button. */
    public static final int NAV_BUTTON_HEIGHT = 42;

    /** Corner radius hint for FlatLaf-styled buttons and fields. */
    public static final int CORNER_RADIUS = 6;

    /** Default width of a text field in a form. */
    public static final int FIELD_WIDTH = 200;

    /** Default height of a text field / combo box in a form. */
    public static final int FIELD_HEIGHT = 28;
}
