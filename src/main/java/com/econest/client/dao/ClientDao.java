package com.econest.client.dao;

import com.econest.client.model.Client;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link Client}. */
public interface ClientDao {

    List<Client> findAll();

    Optional<Client> findById(int id);

    Client save(Client entity);

    void delete(int id);
}
