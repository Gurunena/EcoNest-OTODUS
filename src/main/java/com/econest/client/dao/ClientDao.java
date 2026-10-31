package com.econest.client.dao;

import com.econest.client.model.Client;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Database operations for clients (table: clients).
 *
 * This is an interface: it only lists WHAT can be done. ClientDaoImpl says HOW,
 * using SQL. The service talks to this interface, so in unit tests it can be
 * given a fake version that doesn't need MySQL.
 *
 * Methods throw SQLException; the service decides what the user is told.
 */
public interface ClientDao {

    /** Every client, lowest id first. */
    List<Client> findAll() throws SQLException;

    /** One client, or Optional.empty() if there is no client with that id. */
    Optional<Client> findById(int clientId) throws SQLException;

    /** Clients whose name, email, phone, city or contact person contains the keyword. */
    List<Client> search(String keyword) throws SQLException;

    /** True if a client OTHER than excludeClientId already uses this email. */
    boolean emailExists(String email, int excludeClientId) throws SQLException;

    /** Saves a new client and puts the id the database gave it into the client object. */
    void add(Client client) throws SQLException;

    /** @return true if the client existed and was updated */
    boolean update(Client client) throws SQLException;

    /** @return true if the client existed and was deleted */
    boolean delete(int clientId) throws SQLException;

    /** How many orders this client has (a client with orders must not be deleted). */
    int countOrders(int clientId) throws SQLException;
}
