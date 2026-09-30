package org.example.service;

import org.example.exception.InsufficientStockException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Customer;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.repository.CustomerRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a NEW order in a single transaction. Any unchecked exception (e.g.
     * InsufficientStockException) rolls back the order and all stock decrements.
     */
    @Transactional
    public Order placeOrder(Long customerId, Map<Long, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        Order order = new Order(customer);

        for (Map.Entry<Long, Integer> line : productQuantities.entrySet()) {
            Long productId = line.getKey();
            int quantity = line.getValue() == null ? 0 : line.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

            if (quantity <= 0) {
                throw new InsufficientStockException(
                        "Quantity must be greater than 0 for product " + product.getSku());
            }
            if (product.getStock() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getSku()
                        + ": requested " + quantity + ", available " + product.getStock());
            }

            product.setStock(product.getStock() - quantity);
            order.addItem(new OrderItem(product, quantity)); // unit price copied from product here
        }

        return orderRepository.save(order);
    }
}
