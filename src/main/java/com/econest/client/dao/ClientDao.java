package econest.dao;

import econest.db.DBConnection;
import econest.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    // ADD CLIENT
    public boolean addClient(Client client) {

        String sql = "INSERT INTO clients " +
                "(client_name, email, phone, address, client_type) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, client.getClientName());
            statement.setString(2, client.getEmail());
            statement.setString(3, client.getPhone());
            statement.setString(4, client.getAddress());
            statement.setString(5, client.getClientType());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // GET ALL CLIENTS
    public List<Client> getAllClients() {

        List<Client> clients = new ArrayList<>();

        String sql = "SELECT * FROM clients";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Client client = new Client(
                        resultSet.getInt("client_id"),
                        resultSet.getString("client_name"),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getString("address"),
                        resultSet.getString("client_type")
                );

                clients.add(client);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return clients;
    }


    // GET CLIENT BY ID
    public Client getClientById(int clientId) {

        String sql = "SELECT * FROM clients WHERE client_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, clientId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {

                return new Client(
                        resultSet.getInt("client_id"),
                        resultSet.getString("client_name"),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getString("address"),
                        resultSet.getString("client_type")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    // UPDATE CLIENT
    public boolean updateClient(Client client) {

        String sql = "UPDATE clients SET " +
                "client_name=?, email=?, phone=?, " +
                "address=?, client_type=? " +
                "WHERE client_id=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, client.getClientName());
            statement.setString(2, client.getEmail());
            statement.setString(3, client.getPhone());
            statement.setString(4, client.getAddress());
            statement.setString(5, client.getClientType());
            statement.setInt(6, client.getClientId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // DELETE CLIENT
    public boolean deleteClient(int clientId) {

        String sql = "DELETE FROM clients WHERE client_id=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, clientId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
