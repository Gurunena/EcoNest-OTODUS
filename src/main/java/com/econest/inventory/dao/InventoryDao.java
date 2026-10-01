package com.econest.inventory.dao;

import com.econest.inventory.model.StockItem;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link StockItem}. */
public interface InventoryDao {

    List<StockItem> findAll();

    Optional<StockItem> findById(int id);

    StockItem save(StockItem entity);

    void delete(int id);
}
