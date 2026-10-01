package com.econest.product.dao;

import com.econest.product.model.Product;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link Product}. */
public interface ProductDao {

    List<Product> findAll();

    Optional<Product> findById(int id);

    Product save(Product entity);

    void delete(int id);
}
