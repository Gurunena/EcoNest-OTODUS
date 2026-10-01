package com.econest.agent.dao;

import com.econest.agent.model.DeliveryAgent;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link DeliveryAgent}. */
public interface DeliveryAgentDao {

    List<DeliveryAgent> findAll();

    Optional<DeliveryAgent> findById(int id);

    DeliveryAgent save(DeliveryAgent entity);

    void delete(int id);
}
