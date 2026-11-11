package com.econest.order.model;

/**
 * The little a new order needs to know about a client: who to put on it, and
 * where to send it.
 *
 * <p>Deliberately not {@code com.econest.client.model.Client}. That class is
 * abstract over HouseholdClient / OfficeClient and carries contact details
 * this module has no business reading, and there is no ClientDao to build one
 * with yet. A flat reference is what a combo box and an address snapshot
 * actually need, and it swaps for the real thing in one place when the client
 * module is ready - see {@code ClientLookup}.</p>
 *
 * @param clientId the {@code clients.client_id} to store on the order
 * @param name     person's name, or company name for an office client
 * @param address  one-line address, used as the order's delivery snapshot
 */
public record ClientRef(int clientId, String name, String address) {

    /** What a combo box shows. */
    @Override
    public String toString() {
        return name + "  (#" + clientId + ")";
    }
}
