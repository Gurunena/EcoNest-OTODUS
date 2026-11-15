package com.econest.client;

import com.econest.client.dao.ClientDao;
import com.econest.client.model.Client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pretend ClientDao that keeps clients in a Map instead of MySQL.
 * It lets the tests check ClientService's rules without needing a database.
 */
public class FakeClientDao implements ClientDao {

    private final Map<Integer, Client> clients = new LinkedHashMap<>();
    private final Map<Integer, Integer> orderCounts = new LinkedHashMap<>();
    private int nextId = 1;

    /** Pretend this client has some orders (for the delete rule). */
    public void setOrderCount(int clientId, int count) {
        orderCounts.put(clientId, count);
    }

    @Override
    public List<Client> findAll() {
        return new ArrayList<>(clients.values());
    }

    @Override
    public Optional<Client> findById(int clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }

    @Override
    public List<Client> search(String keyword) {
        List<Client> result = new ArrayList<>();
        for (Client c : clients.values()) {
            if (c.getName().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public boolean emailExists(String email, int excludeClientId) {
        for (Client c : clients.values()) {
            if (c.getEmail().equalsIgnoreCase(email) && c.getClientId() != excludeClientId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void add(Client client) {
        client.setClientId(nextId++);
        clients.put(client.getClientId(), client);
    }

    @Override
    public boolean update(Client client) {
        if (!clients.containsKey(client.getClientId())) {
            return false;
        }
        clients.put(client.getClientId(), client);
        return true;
    }

    @Override
    public boolean delete(int clientId) {
        return clients.remove(clientId) != null;
    }

    @Override
    public int countAll() {
        return clients.size();
    }

    @Override
    public int countOrders(int clientId) {
        return orderCounts.getOrDefault(clientId, 0);
    }
}
