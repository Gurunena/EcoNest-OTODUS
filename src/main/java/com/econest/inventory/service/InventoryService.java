package com.econest.inventory.service;

import com.econest.inventory.dao.InventoryDao;
import com.econest.inventory.dao.InventoryDaoImpl;
import com.econest.inventory.model.StockItem;
import com.econest.inventory.model.Supplier;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for the Stock / Inventory module.
 *
 * <p>Holds the rules the UI must not be trusted with: stock can never go
 * negative, a restock must be a positive quantity from a real supplier, and
 * an adjustment must say why. Each rule throws
 * {@link IllegalArgumentException} with a message written to be shown to the
 * user as-is.</p>
 */
public class InventoryService {

    /** Reorder level used for a product that has no stock_items row yet. */
    public static final int DEFAULT_REORDER_LEVEL = 0;

    private final InventoryDao dao;

    /** Production constructor: talks to MySQL. */
    public InventoryService() {
        this(new InventoryDaoImpl());
    }

    /** Injectable constructor, so a test can pass a fake DAO. */
    public InventoryService(InventoryDao dao) {
        this.dao = dao;
    }

    // -----------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------

    public List<StockItem> findAllStock() throws SQLException {
        return dao.findAll();
    }

    /** Stock filtered by product name or category; blank returns everything. */
    public List<StockItem> search(String query) throws SQLException {
        return dao.search(query);
    }

    public List<Supplier> findAllSuppliers() throws SQLException {
        return dao.findAllSuppliers();
    }

    /** True when the item is at or below its reorder level. */
    public static boolean isLowStock(StockItem item) {
        return item != null && item.getQuantityOnHand() <= item.getReorderLevel();
    }

    /** The word shown in the Status column, which TableStyler then colours. */
    public static String statusOf(StockItem item) {
        if (item == null) {
            return "";
        }
        if (item.getQuantityOnHand() <= 0) {
            return "Out of stock";
        }
        return isLowStock(item) ? "Low stock" : "In stock";
    }

    // -----------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------

    /**
     * Records stock received from a supplier.
     *
     * @throws IllegalArgumentException if the input is not usable
     * @throws SQLException             if the database rejected the write
     */
    public void restock(int productId, int supplierId, int quantity, LocalDate receivedDate)
            throws SQLException {

        if (productId <= 0) {
            throw new IllegalArgumentException("Select a product to restock.");
        }
        if (supplierId <= 0) {
            throw new IllegalArgumentException("Select the supplier the stock came from.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be at least 1.");
        }
        if (receivedDate == null) {
            throw new IllegalArgumentException("Enter the date the stock was received.");
        }
        if (receivedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The received date cannot be in the future.");
        }
        dao.restock(productId, supplierId, quantity, receivedDate);
    }

    /**
     * Applies a signed correction to a product's stock and records the reason.
     *
     * <p>The negative-stock rule is enforced three times over: here against
     * the count we last read, in the UPDATE's own {@code >= 0} guard, and by
     * the {@code chk_products_quantity} constraint. The first gives the user
     * a good message; the others stop a race.</p>
     *
     * @param quantityDelta signed change; negative for a write-off
     * @param reason        why; required
     */
    public void adjustStock(int productId, int quantityDelta, String reason) throws SQLException {
        if (productId <= 0) {
            throw new IllegalArgumentException("Select a product to adjust.");
        }
        if (quantityDelta == 0) {
            throw new IllegalArgumentException(
                    "Enter how much to add or remove. Use a minus sign to remove stock.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Enter a reason for the adjustment.");
        }

        StockItem current = dao.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("That product no longer exists."));

        int resulting = current.getQuantityOnHand() + quantityDelta;
        if (resulting < 0) {
            throw new IllegalArgumentException(
                    "There are only " + current.getQuantityOnHand() + " units of "
                            + current.getProduct().getProductName()
                            + " in stock, so you cannot remove " + Math.abs(quantityDelta) + ".");
        }
        dao.adjustStock(productId, quantityDelta, reason.trim());
    }

    /** Sets the reorder threshold that drives the low-stock highlight. */
    public void setReorderLevel(int productId, int reorderLevel) throws SQLException {
        if (productId <= 0) {
            throw new IllegalArgumentException("Select a product first.");
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("The reorder level cannot be negative.");
        }
        dao.setReorderLevel(productId, reorderLevel);
    }
}
