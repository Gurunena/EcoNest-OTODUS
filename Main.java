
public class Main {

    public static void main(String[] args) {

        // Create a new order
        Order order = new Order("ORD001");

        // Create order items
        OrderItem item1 =
                new OrderItem("Indoor Plant", 2, 1500.00);

        OrderItem item2 =
                new OrderItem("Planter", 1, 800.00);

        // Add items to the order
        order.addItem(item1);
        order.addItem(item2);

        // Display order details
        System.out.println("===== EcoNest Order =====");
        System.out.println("Order ID: " + order.getOrderId());

        System.out.println("\nOrder Items:");

        System.out.println(
                item1.getProductName()
                + " | Quantity: " + item1.getQuantity()
                + " | Subtotal: Rs. "
                + item1.calculateSubtotal()
        );

        System.out.println(
                item2.getProductName()
                + " | Quantity: " + item2.getQuantity()
                + " | Subtotal: Rs. "
                + item2.calculateSubtotal()
        );

        // Calculate and display total
        System.out.printf(
                "%nTotal Amount: Rs. %.2f%n",
                order.calculateTotal()
        );
    }
}
