package com.econest.order.model;

import java.math.BigDecimal;

/**
 * A product as an order line needs it: what it is called, what it costs and
 * how many are on the shelf.
 *
 * <p>{@code unitPrice} is a {@link BigDecimal} even though the product
 * module's {@code Product.getPrice()} is a {@code double} - the conversion
 * happens once, here at the boundary, through {@link Money#of(double)}. From
 * this point inward the order module never touches a floating-point amount.</p>
 *
 * @param productId       the {@code products.product_id}
 * @param productName     shown in the product combo and on the order lines
 * @param unitPrice       today's catalogue price, copied onto a new line
 * @param quantityOnHand  current stock, which caps the quantity that can be ordered
 */
public record ProductRef(int productId, String productName,
                         BigDecimal unitPrice, int quantityOnHand) {

    public ProductRef {
        unitPrice = Money.scale(unitPrice);
    }

    /** True when there is nothing left to sell. */
    public boolean isOutOfStock() {
        return quantityOnHand <= 0;
    }

    /** What the product combo box shows: name, price and what is left. */
    @Override
    public String toString() {
        return productName + "  -  " + Money.format(unitPrice)
                + (isOutOfStock() ? "  (out of stock)" : "  (" + quantityOnHand + " left)");
    }
}
