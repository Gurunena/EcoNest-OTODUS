package com.econest.order.service;

import com.econest.order.model.ClientRef;

import java.sql.SQLException;
import java.util.List;

/**
 * How the order module finds clients to put on an order.
 *
 * <p><b>Why this exists:</b> {@code com.econest.client.service.ClientService}
 * is still an empty class and there is no ClientDao at all, so there is no
 * public method on the client module to call. Rather than wait, the order
 * module states the two things it needs as this interface and ships a
 * read-only implementation ({@code ClientDirectoryDao}) against the
 * {@code clients} table.</p>
 *
 * <p><b>When the client module is ready:</b> write one adapter class that
 * implements this interface over {@code ClientService} and pass it to
 * {@link OrderService}'s injectable constructor. Nothing else in the order
 * module changes, and {@code ClientDirectoryDao} can then be deleted.</p>
 */
public interface ClientLookup {

    /**
     * Clients whose name matches, for the picker on the create-order form.
     *
     * @param query a name fragment; blank means every client
     */
    List<ClientRef> search(String query) throws SQLException;

    /** One client by id, or null when there is no such client. */
    ClientRef findById(int clientId) throws SQLException;
}
