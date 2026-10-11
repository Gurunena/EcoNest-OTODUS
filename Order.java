
import java.util.ArrayList;

public class Order {
    private String orderId;
    private ArrayList<OrderItem> items;

    public Order(String orderId) {
        this.orderId = orderId;
        this.items = new ArrayList<>();
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public double calculateTotal() {
        double total = 0;

        for (OrderItem item : items) {
            total += item.calculateSubtotal();
        }

        return total;
    }

    public String getOrderId() {
        return orderId;
    }
}
