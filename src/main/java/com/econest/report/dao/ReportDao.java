package com.econest.report.dao;

import com.econest.report.model.LowStockAlert;
import com.econest.report.model.MonthlySalesSummary;

import java.time.YearMonth;
import java.util.List;

/** Read-only aggregate queries backing the reports screen. */
public interface ReportDao {

    MonthlySalesSummary getMonthlySalesSummary(YearMonth month);

    List<LowStockAlert> findLowStockItems();
}
