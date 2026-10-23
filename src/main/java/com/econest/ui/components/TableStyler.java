package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

/**
 * Gives every {@code JTable} in the app the same modern look: a light header
 * with small uppercase labels, 44px rows, hairline separators, a soft green
 * hover and selection, right-aligned numbers and status pills.
 *
 * <pre>
 *   TableStyler.style(tblStock);                  // header, rows, hover
 *   TableStyler.colourStatusColumn(tblStock, 5);  // render column 5 as pills
 *   TableStyler.rightAlign(tblStock, 3, 4);       // numeric columns
 * </pre>
 */
public final class TableStyler {

    /** Upper bound for an auto-sized column, in pixels. */
    private static final int MAX_AUTO_WIDTH = 320;

    /** How far columns may overflow before the table scrolls instead. */
    private static final double SCROLL_THRESHOLD = 1.35;

    private TableStyler() {
        // Utility class.
    }

    // ---------------------------------------------------------------------
    // Styling
    // ---------------------------------------------------------------------

    /**
     * Applies the EcoNest look to a table. Call it once, after the model is
     * set and before any per-column renderer.
     */
    public static void style(JTable tbl) {
        tbl.setFont(Theme.FONT_BODY);
        tbl.setForeground(Theme.TEXT_PRIMARY);
        tbl.setBackground(Theme.CARD_BG);
        tbl.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tbl.setShowVerticalLines(false);
        tbl.setShowHorizontalLines(false);      // the renderer draws them
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setFillsViewportHeight(true);
        tbl.setSelectionBackground(Theme.ACCENT_SOFT);
        tbl.setSelectionForeground(Theme.TEXT_PRIMARY);
        tbl.setAutoCreateRowSorter(true);
        tbl.setGridColor(Theme.CARD_BORDER);

        JTableHeader header = tbl.getTableHeader();
        header.setFont(Theme.FONT_LABEL);
        header.setBackground(Theme.CARD_BG);
        header.setForeground(Theme.TEXT_FAINT);
        header.setPreferredSize(new Dimension(0, Theme.TABLE_HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tbl.setDefaultRenderer(Object.class, new BodyRenderer());

        installHover(tbl);
    }

    /**
     * Tracks the row under the pointer so {@link BodyRenderer} can tint it.
     * Stored as a client property, which keeps the renderer stateless.
     */
    private static void installHover(JTable tbl) {
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoverRow(tbl, tbl.rowAtPoint(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoverRow(tbl, -1);
            }
        };
        tbl.addMouseListener(hover);
        tbl.addMouseMotionListener(hover);
    }

    private static void setHoverRow(JTable tbl, int row) {
        Object previous = tbl.getClientProperty("econest.hoverRow");
        if (previous instanceof Integer prev && prev == row) {
            return;
        }
        tbl.putClientProperty("econest.hoverRow", row);
        tbl.repaint();
    }

    private static int hoverRow(JTable tbl) {
        Object value = tbl.getClientProperty("econest.hoverRow");
        return value instanceof Integer row ? row : -1;
    }

    /**
     * Renders a column as a rounded pill badge coloured by its value: green
     * for in stock / delivered, amber for low stock / pending, red for out of
     * stock / failed, blue for new / scheduled.
     */
    public static void colourStatusColumn(JTable tbl, int columnIndex) {
        if (columnIndex < 0 || columnIndex >= tbl.getColumnModel().getColumnCount()) {
            return;
        }
        tbl.getColumnModel().getColumn(columnIndex).setCellRenderer(new PillRenderer());
    }

    /** Right-aligns the given columns, which is how numbers should read. */
    public static void rightAlign(JTable tbl, int... columnIndexes) {
        for (int columnIndex : columnIndexes) {
            if (columnIndex < 0 || columnIndex >= tbl.getColumnModel().getColumnCount()) {
                continue;
            }
            BodyRenderer renderer = new BodyRenderer();
            renderer.setHorizontalAlignment(SwingConstants.RIGHT);
            tbl.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
        }
    }

    /**
     * Widens each column to fit its widest cell, so text is not cut off. If
     * the columns do not all fit, auto-resizing is switched off so the scroll
     * pane offers a horizontal scrollbar instead of truncating every cell.
     */
    public static void fitColumns(JTable tbl) {
        int total = 0;
        for (int col = 0; col < tbl.getColumnCount(); col++) {
            TableColumn column = tbl.getColumnModel().getColumn(col);

            TableCellRenderer headerRenderer = column.getHeaderRenderer() != null
                    ? column.getHeaderRenderer()
                    : tbl.getTableHeader().getDefaultRenderer();
            Component headerCell = headerRenderer.getTableCellRendererComponent(
                    tbl, column.getHeaderValue(), false, false, -1, col);
            int width = headerCell.getPreferredSize().width;

            for (int row = 0; row < tbl.getRowCount(); row++) {
                Component cell = tbl.prepareRenderer(tbl.getCellRenderer(row, col), row, col);
                width = Math.max(width, cell.getPreferredSize().width);
            }

            int fitted = Math.min(width + Theme.GAP, MAX_AUTO_WIDTH);
            column.setPreferredWidth(fitted);
            total += fitted;
        }

        // Squeeze a modest overflow rather than scrolling: losing a little
        // width per column reads far better than pushing the last column -
        // usually the status pill - off the right-hand edge. Only a big
        // overflow earns a horizontal scrollbar.
        int available = tbl.getParent() == null ? 0 : tbl.getParent().getWidth();
        boolean mustScroll = available > 0 && total > available * SCROLL_THRESHOLD;
        tbl.setAutoResizeMode(mustScroll
                ? JTable.AUTO_RESIZE_OFF
                : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    /**
     * Maps a status word to its colour. Unknown values fall back to the
     * normal text colour, so a new status never breaks a table.
     */
    public static Color colourFor(String status) {
        return palette(status).text();
    }

    // ---------------------------------------------------------------------
    // Renderers
    // ---------------------------------------------------------------------

    /**
     * Body cells: hover and selection tints, a hairline separator under each
     * row, and consistent padding.
     */
    private static class BodyRenderer extends DefaultTableCellRenderer {

        private boolean drawSeparator = true;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

            if (isSelected) {
                setBackground(Theme.ACCENT_SOFT);
            } else if (row == hoverRow(table)) {
                setBackground(Theme.ACCENT_FAINT);
            } else {
                setBackground(Theme.CARD_BG);
            }
            setForeground(Theme.TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP, 0, Theme.GAP));
            drawSeparator = row < table.getRowCount() - 1;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (drawSeparator) {
                g.setColor(Theme.CARD_BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        }
    }

    /** A rounded capsule badge, tinted by the status word it holds. */
    private static class PillRenderer extends DefaultTableCellRenderer {

        private Palette palette = palette(null);
        private boolean drawSeparator = true;
        private String text = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

            text = value == null ? "" : value.toString();
            palette = palette(text);

            if (isSelected) {
                setBackground(Theme.ACCENT_SOFT);
            } else if (row == hoverRow(table)) {
                setBackground(Theme.ACCENT_FAINT);
            } else {
                setBackground(Theme.CARD_BG);
            }
            setFont(Theme.FONT_PILL);
            setText("");                       // the pill is painted, not drawn as text
            setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP, 0, Theme.GAP));
            drawSeparator = row < table.getRowCount() - 1;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.FONT_PILL);
            return new Dimension(fm.stringWidth(text) + 4 * Theme.GAP_SMALL + 2 * Theme.GAP,
                    Theme.TABLE_ROW_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Row background and separator first.
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (drawSeparator) {
                g.setColor(Theme.CARD_BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
            if (text.isBlank()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.FONT_PILL);

            FontMetrics fm = g2.getFontMetrics();
            int padX = Theme.GAP_SMALL + 2;
            int padY = 5;
            int textWidth = fm.stringWidth(text);
            int pillW = textWidth + 2 * padX;
            int pillH = fm.getHeight() + 2 * padY;
            int x = Theme.GAP;
            int y = (getHeight() - pillH) / 2;

            g2.setColor(palette.tint());
            g2.fillRoundRect(x, y, pillW, pillH, pillH, pillH);

            g2.setColor(palette.text());
            g2.drawString(text, x + padX, y + padY + fm.getAscent());
            g2.dispose();
        }
    }

    /** Header cells: small uppercase labels with a bottom border only. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setText(value == null ? "" : value.toString().toUpperCase(Locale.ROOT));
            setFont(Theme.FONT_LABEL);
            setForeground(Theme.TEXT_FAINT);
            setBackground(Theme.CARD_BG);
            setOpaque(true);
            setHorizontalAlignment(alignmentOf(table, column));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.GAP, 0, Theme.GAP)));
            return this;
        }

        /** Headers follow their column's own alignment, so numbers line up. */
        private static int alignmentOf(JTable table, int column) {
            TableCellRenderer renderer =
                    table.getColumnModel().getColumn(column).getCellRenderer();
            if (renderer instanceof JLabel label) {
                return label.getHorizontalAlignment();
            }
            return SwingConstants.LEFT;
        }
    }

    // ---------------------------------------------------------------------
    // Status palette
    // ---------------------------------------------------------------------

    /** The two colours a status pill needs. */
    private record Palette(Color tint, Color text) {
    }

    private static Palette palette(String status) {
        if (status == null) {
            return new Palette(Theme.TINT_INFO, Theme.TEXT_PRIMARY);
        }
        switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "delivered": case "completed": case "complete": case "in stock":
            case "active": case "paid": case "success": case "approved":
                return new Palette(Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS);
            case "pending": case "processing": case "in transit": case "assigned":
            case "on the way": case "awaiting": case "low": case "low stock":
            case "picked up": case "dispatched": case "packed":
                return new Palette(Theme.TINT_PENDING, Theme.STATUS_PENDING);
            case "failed": case "cancelled": case "canceled": case "rejected":
            case "out of stock": case "inactive": case "overdue":
                return new Palette(Theme.TINT_DANGER, Theme.STATUS_DANGER);
            case "new": case "info": case "draft": case "scheduled":
            case "open": case "confirmed":
                return new Palette(Theme.TINT_INFO, Theme.STATUS_INFO);
            default:
                return new Palette(Theme.TINT_INFO, Theme.TEXT_PRIMARY);
        }
    }
}
