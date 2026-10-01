package com.econest.report.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthlySalesSummary {

    private YearMonth month;
    private int orderCount;
    private int unitsSold;
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    public MonthlySalesSummary() {
    }

    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) { this.month = month; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public int getUnitsSold() { return unitsSold; }
    public void setUnitsSold(int unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}
