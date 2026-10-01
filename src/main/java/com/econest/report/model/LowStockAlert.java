package com.econest.report.model;

import com.econest.inventory.model.StockItem;

import java.time.LocalDateTime;

public class LowStockAlert {

    private StockItem stockItem;
    private LocalDateTime raisedAt;

    public LowStockAlert() {
    }

    public LowStockAlert(StockItem stockItem, LocalDateTime raisedAt) {
        this.stockItem = stockItem;
        this.raisedAt = raisedAt;
    }

    public StockItem getStockItem() { return stockItem; }
    public void setStockItem(StockItem stockItem) { this.stockItem = stockItem; }

    public LocalDateTime getRaisedAt() { return raisedAt; }
    public void setRaisedAt(LocalDateTime raisedAt) { this.raisedAt = raisedAt; }
}
