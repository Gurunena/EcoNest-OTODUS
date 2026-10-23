package com.econest.inventory.model;

import java.time.LocalDateTime;

/**
 * A manual correction to a product's stock count, with the reason it was
 * made: damage, a recount, a write-off, a customer return.
 *
 * <p>Distinct from {@link StockIn}, which records stock arriving from a
 * supplier. An adjustment has no supplier and its delta may be negative.</p>
 *
 * <p>Stored in the {@code stock_adjustments} table - see
 * {@code database/migrations/001_stock_adjustments.sql}.</p>
 */
public class StockAdjustment {

    private int id;
    private int productId;

    /** Signed change applied to the stock count; never zero. */
    private int quantityDelta;

    private String reason;
    private LocalDateTime adjustedAt;

    public StockAdjustment() {
    }

    public StockAdjustment(int productId, int quantityDelta, String reason) {
        this.productId = productId;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(int quantityDelta) { this.quantityDelta = quantityDelta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(LocalDateTime adjustedAt) { this.adjustedAt = adjustedAt; }
}
