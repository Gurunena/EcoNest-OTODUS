package com.econest.inventory.dao;

import com.econest.inventory.model.StockItem;
import com.econest.inventory.model.Supplier;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for stock levels.
 *
 * <p>Stock lives in two places, as schema.sql explains: the on-hand count is
 * {@code products.quantity} (owned by the product module) and the reorder
 * threshold is {@code stock_items.reorder_level}. A {@link StockItem} is the
 * two read together.</p>
 *
 * <p>Methods throw {@link SQLException}; the service decides what the user is
 * told.</p>
 */
public interface InventoryDao {

    /** Every product with its stock level, product name order. */
    List<StockItem> findAll() throws SQLException;

    /** One product's stock level. */
    Optional<StockItem> findById(int productId) throws SQLException;

    /**
     * Stock levels for products whose name or category matches.
     *
     * @param query free text; blank returns everything
     */
    List<StockItem> search(String query) throws SQLException;

    /** Suppliers, for the restock form. */
    List<Supplier> findAllSuppliers() throws SQLException;

    /**
     * Records stock received from a supplier: writes a {@code stock_in} row
     * and adds the quantity to {@code products.quantity}, in one transaction.
     *
     * @param quantity units received; must be positive
     */
    void restock(int productId, int supplierId, int quantity, LocalDate receivedDate)
            throws SQLException;

    /**
     * Applies a signed correction to the stock count and records why, in one
     * transaction.
     *
     * @param quantityDelta signed change; negative for a write-off
     * @param reason        why the correction was made
     * @throws SQLException if the adjustment would take stock below zero, or
     *                      the {@code stock_adjustments} table is missing
     */
    void adjustStock(int productId, int quantityDelta, String reason) throws SQLException;

    /** Sets the reorder threshold, inserting the stock_items row if needed. */
    void setReorderLevel(int productId, int reorderLevel) throws SQLException;
}
