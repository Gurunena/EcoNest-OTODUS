package com.econest.inventory.model;

import com.econest.product.model.Product;

/** Current stock position for one product. */
public class StockItem {

    private Product product;
    private int quantityOnHand;
    private int reorderLevel;

    public StockItem() {
    }

    public StockItem(Product product, int quantityOnHand, int reorderLevel) {
        this.product = product;
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
