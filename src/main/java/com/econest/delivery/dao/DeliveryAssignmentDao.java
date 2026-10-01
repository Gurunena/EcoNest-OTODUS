package com.econest.delivery.dao;

import com.econest.delivery.model.DeliveryAssignment;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link DeliveryAssignment}. */
public interface DeliveryAssignmentDao {

    List<DeliveryAssignment> findAll();

    Optional<DeliveryAssignment> findById(int id);

    DeliveryAssignment save(DeliveryAssignment entity);

    void delete(int id);
}
