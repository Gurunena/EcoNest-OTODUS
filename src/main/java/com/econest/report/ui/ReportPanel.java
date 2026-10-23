package com.econest.report.ui;

import com.econest.report.model.AgentDeliveryCount;
import com.econest.report.model.LowStockAlert;
import com.econest.report.model.MonthlySalesSummary;
import com.econest.report.model.TopSellingProduct;
import com.econest.report.service.ReportService;
import com.econest.ui.MainFrame;
import com.econest.ui.Theme;
import com.econest.ui.components.Buttons;
import com.econest.ui.components.Dialogs;
import com.econest.ui.components.FormFields;
import com.econest.ui.components.TableStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Monthly Reports screen.
 *
 * <p>Pick a month and a year, press Run, and four tables fill in: total
 * orders and revenue at the top, then top-selling products, deliveries per
 * agent and the current low-stock list. "Export to CSV" writes the whole
 * month to one file.</p>
 *
 * <p>This screen is read-only, so it does not extend {@code BaseModulePanel}
 * - that template is built around Add / Update / Delete / Clear, none of
 * which a report has. It still uses the same {@link Theme}, {@link Buttons},
 * {@link TableStyler} and {@link Dialogs} helpers, so it looks identical to
 * the CRUD screens.</p>
 *
 * <p>Database work runs on a {@link SwingWorker} because four aggregate
 * queries over a full month are slow enough to freeze the window.</p>
 */
public class ReportPanel extends JPanel {

    private static final String[] COLS_TOP_PRODUCTS =
            {"Product", "Category", "Units sold", "Revenue"};

    private static final String[] COLS_AGENTS =
            {"Agent", "Assigned", "Delivered", "Failed", "Success rate"};

    private static final String[] COLS_LOW_STOCK =
            {"Product", "Category", "On hand", "Reorder level", "Status"};

    /** Index of the status column that TableStyler colours. */
    private static final int COL_LOW_STOCK_STATUS = 4;

    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.ENGLISH);

    // --- Controls (cmb / btn / lbl / tbl prefixes) ------------------------
    private final JComboBox<String> cmbMonth;
    private final JComboBox<Integer> cmbYear;
    private final JButton btnRun;
    private final JButton btnExport;

    private final JLabel lblOrders = statLabel();
    private final JLabel lblUnits = statLabel();
    private final JLabel lblRevenue = statLabel();

    private final DefaultTableModel topProductsModel = readOnlyModel(COLS_TOP_PRODUCTS);
    private final DefaultTableModel agentsModel = readOnlyModel(COLS_AGENTS);
    private final DefaultTableModel lowStockModel = readOnlyModel(COLS_LOW_STOCK);

    private final JTable tblTopProducts = new JTable(topProductsModel);
    private final JTable tblAgents = new JTable(agentsModel);
    private final JTable tblLowStock = new JTable(lowStockModel);

    private final ReportService service;

    /** The month currently on screen, used by the CSV export. */
    private YearMonth loadedMonth;

    public ReportPanel() {
        this(new ReportService());
    }

    /** Injectable constructor so a test can supply a fake service. */
    public ReportPanel(ReportService service) {
        super(new BorderLayout(Theme.GAP, Theme.GAP));
        this.service = service;

        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        YearMonth now = YearMonth.now();
        cmbMonth = FormFields.comboBox(monthNames());
        cmbMonth.setSelectedIndex(now.getMonthValue() - 1);
        cmbYear = FormFields.comboBox(yearChoices(now.getYear()));
        cmbYear.setSelectedItem(now.getYear());

        btnRun = Buttons.primary("Run report");
        btnExport = Buttons.secondary("Export to CSV");
        btnExport.setEnabled(false);          // nothing to export until a run succeeds

        btnRun.addActionListener(e -> runReport());
        btnExport.addActionListener(e -> exportCsv());

        TableStyler.style(tblTopProducts);
        TableStyler.style(tblAgents);
        TableStyler.style(tblLowStock);
        TableStyler.colourStatusColumn(tblLowStock, COL_LOW_STOCK_STATUS);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    /** Title on the left, month/year pickers and the buttons on the right. */
    private JPanel buildHeader() {
        JLabel lblTitle = new JLabel("Monthly Reports");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.SIDEBAR_BG);

        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_SMALL, 0));
        pnlControls.setOpaque(false);
        pnlControls.add(caption("Month"));
        pnlControls.add(cmbMonth);
        pnlControls.add(caption("Year"));
        pnlControls.add(cmbYear);
        pnlControls.add(btnRun);
        pnlControls.add(btnExport);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP, 0));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(pnlControls, BorderLayout.EAST);
        return pnlHeader;
    }

    /** Summary strip on top, then the three tables stacked. */
    private JPanel buildBody() {
        JPanel pnlBody = new JPanel(new BorderLayout(0, Theme.GAP));
        pnlBody.setOpaque(false);
        pnlBody.add(buildSummaryStrip(), BorderLayout.NORTH);

        JPanel pnlTables = new JPanel(new GridLayout(3, 1, 0, Theme.GAP));
        pnlTables.setOpaque(false);
        pnlTables.add(tableSection("Top selling products", tblTopProducts));
        pnlTables.add(tableSection("Deliveries per agent", tblAgents));
        pnlTables.add(tableSection("Low stock items (now)", tblLowStock));

        pnlBody.add(pnlTables, BorderLayout.CENTER);
        return pnlBody;
    }

    /** The three headline numbers for the chosen month. */
    private JPanel buildSummaryStrip() {
        JPanel pnlSummary = new JPanel(new GridLayout(1, 3, Theme.GAP, 0));
        pnlSummary.setOpaque(false);
        pnlSummary.setPreferredSize(new Dimension(0, 80));
        pnlSummary.add(summaryCard("Total orders", lblOrders, Theme.TINT_INFO, Theme.STATUS_INFO));
        pnlSummary.add(summaryCard("Units sold", lblUnits, Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS));
        pnlSummary.add(summaryCard("Total revenue", lblRevenue, Theme.TINT_PENDING, Theme.STATUS_PENDING));
        return pnlSummary;
    }

    private JPanel summaryCard(String caption, JLabel lblValue,
                               java.awt.Color tint, java.awt.Color accent) {
        lblValue.setForeground(accent);

        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(Theme.FONT_BODY);
        lblCaption.setForeground(Theme.TEXT_MUTED);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlCard = new JPanel();
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setBackground(tint);
        pnlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createEmptyBorder(
                        Theme.GAP_SMALL, Theme.GAP, Theme.GAP_SMALL, Theme.GAP)));
        pnlCard.add(lblValue);
        pnlCard.add(Box.createVerticalStrut(2));
        pnlCard.add(lblCaption);
        return pnlCard;
    }

    /** A titled, bordered box around one table. */
    private JPanel tableSection(String title, JTable tbl) {
        JLabel lblHeading = new JLabel(title);
        lblHeading.setFont(Theme.FONT_HEADING);
        lblHeading.setForeground(Theme.SIDEBAR_BG);
        lblHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP_SMALL, 0));

        JScrollPane scrTable = new JScrollPane(tbl);
        scrTable.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scrTable.getViewport().setBackground(Theme.CONTENT_BG);

        JPanel pnlSection = new JPanel(new BorderLayout());
        pnlSection.setOpaque(false);
        pnlSection.add(lblHeading, BorderLayout.NORTH);
        pnlSection.add(scrTable, BorderLayout.CENTER);
        return pnlSection;
    }

    // ---------------------------------------------------------------------
    // Running the report
    // ---------------------------------------------------------------------

    /**
     * Loads all four reports for the chosen month on a background thread and
     * drops the results into the tables on the UI thread.
     */
    private void runReport() {
        final YearMonth month;
        try {
            month = service.requireValid(selectedMonth());
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        setBusy(true);
        setStatus("Running the report for " + ReportService.label(month) + "...");

        new SwingWorker<ReportBundle, Void>() {
            @Override
            protected ReportBundle doInBackground() throws Exception {
                return new ReportBundle(
                        service.getMonthlySummary(month),
                        service.getTopSellingProducts(month),
                        service.getDeliveriesPerAgent(month),
                        service.getLowStockItems());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    show(month, get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    reportFailure("run the report", ex.getCause());
                }
            }
        }.execute();
    }

    /** Puts a loaded month on screen. */
    private void show(YearMonth month, ReportBundle data) {
        loadedMonth = month;

        MonthlySalesSummary summary = data.summary();
        lblOrders.setText(String.valueOf(summary.getOrderCount()));
        lblUnits.setText(String.valueOf(summary.getUnitsSold()));
        lblRevenue.setText(money(summary.getTotalRevenue()));

        topProductsModel.setRowCount(0);
        for (TopSellingProduct row : data.topProducts()) {
            topProductsModel.addRow(new Object[]{
                    row.getProductName(), row.getCategory(),
                    row.getUnitsSold(), money(row.getRevenue())});
        }

        agentsModel.setRowCount(0);
        for (AgentDeliveryCount row : data.agents()) {
            agentsModel.addRow(new Object[]{
                    row.getAgentName(), row.getAssigned(), row.getDelivered(),
                    row.getFailed(), String.format(Locale.ENGLISH, "%.0f%%", row.getSuccessRate())});
        }

        lowStockModel.setRowCount(0);
        for (LowStockAlert alert : data.lowStock()) {
            int onHand = alert.getStockItem().getQuantityOnHand();
            lowStockModel.addRow(new Object[]{
                    alert.getStockItem().getProduct().getProductName(),
                    alert.getStockItem().getProduct().getCategory(),
                    onHand,
                    alert.getStockItem().getReorderLevel(),
                    onHand <= 0 ? "Out of stock" : "Low stock"});
        }

        TableStyler.fitColumns(tblTopProducts);
        TableStyler.fitColumns(tblAgents);
        TableStyler.fitColumns(tblLowStock);

        btnExport.setEnabled(true);
        setStatus(ReportService.label(month) + ": " + summary.getOrderCount()
                + " orders, " + data.lowStock().size() + " low stock items.");
    }

    // ---------------------------------------------------------------------
    // CSV export
    // ---------------------------------------------------------------------

    /** Asks where to save, then writes the month off the UI thread. */
    private void exportCsv() {
        if (loadedMonth == null) {
            Dialogs.error(this, "Run a report first, then export it.");
            return;
        }
        final YearMonth month = loadedMonth;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export " + ReportService.label(month) + " to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(defaultFileName(month)));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final Path target = withCsvExtension(chooser.getSelectedFile()).toPath();

        if (java.nio.file.Files.exists(target)
                && !Dialogs.confirm(this, target.getFileName() + " already exists. Replace it?")) {
            return;
        }

        setBusy(true);
        setStatus("Exporting " + ReportService.label(month) + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.exportMonthToCsv(month, target);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    Dialogs.info(ReportPanel.this, "Report saved to:\n" + target);
                    setStatus("Exported to " + target.getFileName() + ".");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    reportFailure("export the report", ex.getCause());
                }
            }
        }.execute();
    }

    /** "econest-report-2026-10.csv" */
    private static String defaultFileName(YearMonth month) {
        return String.format("econest-report-%04d-%02d.csv",
                month.getYear(), month.getMonthValue());
    }

    private static File withCsvExtension(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
                ? file
                : new File(file.getParentFile(), file.getName() + ".csv");
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    /** Everything the screen needs for one month, fetched in one go. */
    private record ReportBundle(MonthlySalesSummary summary,
                                List<TopSellingProduct> topProducts,
                                List<AgentDeliveryCount> agents,
                                List<LowStockAlert> lowStock) {
    }

    private YearMonth selectedMonth() {
        Integer year = (Integer) cmbYear.getSelectedItem();
        int monthValue = cmbMonth.getSelectedIndex() + 1;
        return year == null ? null : YearMonth.of(year, monthValue);
    }

    /**
     * Turns a failure into one clear sentence. The cause is printed to the
     * console for us; the user never sees a stack trace.
     */
    private void reportFailure(String action, Throwable cause) {
        String detail = cause == null || cause.getMessage() == null
                ? "Unknown error."
                : cause.getMessage();
        System.err.println("ReportPanel: could not " + action);
        if (cause != null) {
            cause.printStackTrace();
        }
        Dialogs.error(this, "Could not " + action + ".\n\n" + detail);
        setStatus("Report failed.");
    }

    /** Disables the controls while a background job is running. */
    private void setBusy(boolean busy) {
        btnRun.setEnabled(!busy);
        cmbMonth.setEnabled(!busy);
        cmbYear.setEnabled(!busy);
        if (busy) {
            btnExport.setEnabled(false);
        } else {
            btnExport.setEnabled(loadedMonth != null);
        }
    }

    /** Writes to the window's status bar when the panel is inside MainFrame. */
    private void setStatus(String message) {
        MainFrame frame = (MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, this);
        if (frame != null) {
            frame.setStatus(message);
        }
    }

    private static JLabel statLabel() {
        JLabel lbl = new JLabel("-");
        lbl.setFont(Theme.FONT_DISPLAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JLabel caption(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BODY);
        lbl.setForeground(Theme.TEXT_MUTED);
        return lbl;
    }

    private static String money(BigDecimal amount) {
        return "Rs. " + MONEY.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String[] monthNames() {
        String[] names = new String[12];
        for (int i = 0; i < 12; i++) {
            names[i] = Month.of(i + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        return names;
    }

    /** The current year and the nine before it, newest first. */
    private static Integer[] yearChoices(int currentYear) {
        Integer[] years = new Integer[10];
        for (int i = 0; i < years.length; i++) {
            years[i] = currentYear - i;
        }
        return years;
    }
}
