package com.econest.order.service;

import com.econest.order.model.ProductRef;

import java.sql.SQLException;
import java.util.List;

/**
 * How the order module finds products to put on an order line.
 *
 * <p>Unlike {@link ClientLookup}, the product module <i>is</i> ready:
 * {@code ProductCatalogAdapter} implements this over the real
 * {@code ProductService.getAllProducts()} and {@code search(keyword)}. The
 * interface is here so the pricing and stock rules in {@link OrderService}
 * can be unit tested without a database, and so the order module depends on
 * two methods rather than on the whole product model.</p>
 */
public interface ProductLookup {

    /**
     * Products whose name or category matches, for the product picker.
     *
     * @param query a name fragment; blank means the whole catalogue
     */
    List<ProductRef> search(String query) throws SQLException;

    /** One product by id, or null when there is no such product. */
    ProductRef findById(int productId) throws SQLException;
}
