package com.econest.order.dao;

import com.econest.order.model.Order;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link Order}. */
public interface OrderDao {

    List<Order> findAll();

    Optional<Order> findById(int id);

    Order save(Order entity);

    void delete(int id);
}
