package com.econest.order.service;

import com.econest.order.model.Money;
import com.econest.order.model.ProductRef;
import com.econest.product.model.Product;
import com.econest.product.service.ProductService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ProductLookup} over the product module's own
 * {@link ProductService} - its existing public methods only, no SQL of its
 * own.
 *
 * <p>The one thing it does is convert {@code Product.getPrice()}, which is a
 * {@code double}, into a scaled {@link java.math.BigDecimal}. That conversion
 * lives here so it happens exactly once, at the module boundary.</p>
 */
public class ProductCatalogAdapter implements ProductLookup {

    private final ProductService products;

    public ProductCatalogAdapter() {
        this(new ProductService());
    }

    public ProductCatalogAdapter(ProductService products) {
        this.products = products;
    }

    @Override
    public List<ProductRef> search(String query) throws SQLException {
        List<Product> found = products.search(query);
        List<ProductRef> refs = new ArrayList<>(found.size());
        for (Product product : found) {
            refs.add(toRef(product));
        }
        return refs;
    }

    @Override
    public ProductRef findById(int productId) throws SQLException {
        // ProductService exposes no find-by-id, so this goes through the list
        // it does expose. The catalogue is small and this runs once per
        // added line, so a scan costs nothing worth a new method on someone
        // else's service.
        for (Product product : products.getAllProducts()) {
            if (product.getProductId() == productId) {
                return toRef(product);
            }
        }
        return null;
    }

    private static ProductRef toRef(Product product) {
        return new ProductRef(product.getProductId(), product.getProductName(),
                Money.of(product.getPrice()), product.getQuantity());
    }
}
