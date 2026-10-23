package com.econest.ui.components;

import com.econest.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;

/**
 * Gives every {@code JTable} in the app the same look: green header, taller
 * rows, striped backgrounds and an optional colour-coded status column.
 *
 * <pre>
 *   TableStyler.style(tblStock);                 // header + stripes
 *   TableStyler.colourStatusColumn(tblStock, 4); // colour the "Status" column
 * </pre>
 */
public final class TableStyler {

    private TableStyler() {
        // Utility class.
    }

    /**
     * Applies the EcoNest look to a table: themed header, row height, grid
     * colour, selection colour and striped rows.
     *
     * @param tbl the table to style (call this once, after you set its model)
     */
    public static void style(JTable tbl) {
        tbl.setFont(Theme.FONT_BODY);
        tbl.setForeground(Theme.TEXT_PRIMARY);
        tbl.setBackground(Theme.CONTENT_BG);
        tbl.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setFillsViewportHeight(true);
        tbl.setSelectionBackground(Theme.ACCENT_SOFT);
        tbl.setSelectionForeground(Theme.TEXT_PRIMARY);
        tbl.setAutoCreateRowSorter(true);

        JTableHeader header = tbl.getTableHeader();
        header.setFont(Theme.FONT_HEADING);
        header.setBackground(Theme.SIDEBAR_BG);
        header.setForeground(Theme.TEXT_ON_DARK);
        header.setPreferredSize(new Dimension(0, Theme.TABLE_HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        // Some look and feels ignore header colours unless a renderer sets them.
        header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer()));

        // Striped rows for everything that does not have its own renderer.
        tbl.setDefaultRenderer(Object.class, new StripedRenderer());
    }

    /**
     * Widens each column to fit its widest cell, so text is not cut off with
     * an ellipsis. {@code BaseModulePanel} calls this for you every time the
     * rows change; call it yourself if you fill the model some other way.
     *
     * <p>Columns are capped at {@value #MAX_AUTO_WIDTH}px so one long value
     * cannot push the rest of the table off screen.</p>
     */
    public static void fitColumns(JTable tbl) {
        int total = 0;
        for (int col = 0; col < tbl.getColumnCount(); col++) {
            TableColumn column = tbl.getColumnModel().getColumn(col);

            // Start from the header's own width.
            TableCellRenderer headerRenderer = column.getHeaderRenderer() != null
                    ? column.getHeaderRenderer()
                    : tbl.getTableHeader().getDefaultRenderer();
            Component headerCell = headerRenderer.getTableCellRendererComponent(
                    tbl, column.getHeaderValue(), false, false, -1, col);
            int width = headerCell.getPreferredSize().width;

            // Then grow to the widest cell in the column.
            for (int row = 0; row < tbl.getRowCount(); row++) {
                Component cell = tbl.prepareRenderer(tbl.getCellRenderer(row, col), row, col);
                width = Math.max(width, cell.getPreferredSize().width);
            }

            int fitted = Math.min(width + 2 * Theme.GAP, MAX_AUTO_WIDTH);
            column.setPreferredWidth(fitted);
            total += fitted;
        }

        // If the columns fit, let them stretch to fill the table. If they do
        // not - a wide table squeezed next to a form, say - stop resizing so
        // each column keeps its full width and the scroll pane gives the user
        // a horizontal scrollbar, rather than cutting every cell off with an
        // ellipsis.
        int available = tbl.getParent() == null ? 0 : tbl.getParent().getWidth();
        tbl.setAutoResizeMode(available > 0 && total > available
                ? JTable.AUTO_RESIZE_OFF
                : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    /** Upper bound for an auto-sized column, in pixels. */
    private static final int MAX_AUTO_WIDTH = 320;

    /**
     * Colours one column's text according to the EcoNest status colours.
     * Call it after {@link #style(JTable)}.
     *
     * @param tbl         the table
     * @param columnIndex the model/view index of the status column
     */
    public static void colourStatusColumn(JTable tbl, int columnIndex) {
        tbl.getColumnModel().getColumn(columnIndex).setCellRenderer(new StatusRenderer());
    }

    /**
     * Maps a status word to a Theme colour. Unknown values fall back to the
     * normal text colour, so a new status never breaks the table.
     *
     * <p>Green: delivered, completed, in stock, active, paid, success.<br>
     * Amber: pending, processing, in transit, assigned, low.<br>
     * Red: failed, cancelled, rejected, out of stock, low stock, inactive.<br>
     * Blue: new, info, draft, scheduled.</p>
     */
    public static Color colourFor(String status) {
        if (status == null) {
            return Theme.TEXT_PRIMARY;
        }
        String s = status.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "delivered": case "completed": case "complete": case "in stock":
            case "active": case "paid": case "success": case "approved":
                return Theme.STATUS_SUCCESS;
            case "pending": case "processing": case "in transit": case "assigned":
            case "on the way": case "awaiting": case "low":
                return Theme.STATUS_PENDING;
            case "failed": case "cancelled": case "canceled": case "rejected":
            case "out of stock": case "low stock": case "inactive": case "overdue":
                return Theme.STATUS_DANGER;
            case "new": case "info": case "draft": case "scheduled": case "open":
                return Theme.STATUS_INFO;
            default:
                return Theme.TEXT_PRIMARY;
        }
    }

    /** Renders alternating row backgrounds and consistent cell padding. */
    private static class StripedRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Theme.CONTENT_BG : Theme.ROW_ALT_BG);
                c.setForeground(Theme.TEXT_PRIMARY);
            }
            setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP_SMALL, 0, Theme.GAP_SMALL));
            return c;
        }
    }

    /** Striped rows plus status-coloured, bold text. */
    private static class StatusRenderer extends StripedRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            c.setFont(Theme.FONT_BODY_BOLD);
            c.setForeground(colourFor(value == null ? null : value.toString()));
            return c;
        }
    }

    /** Wraps the look and feel's header renderer so our colours survive. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {

        private final TableCellRenderer delegate;

        HeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = delegate.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            c.setBackground(Theme.SIDEBAR_BG);
            c.setForeground(Theme.TEXT_ON_DARK);
            c.setFont(Theme.FONT_HEADING);
            if (c instanceof JLabel label) {
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(
                        0, Theme.GAP_SMALL, 0, Theme.GAP_SMALL));
            }
            return c;
        }
    }
}
