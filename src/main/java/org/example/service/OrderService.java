package org.example.service;

import org.example.dto.OrderItemSummary;
import org.example.dto.OrderSummary;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Customer;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.OrderStatus;
import org.example.model.Payment;
import org.example.model.PaymentMethod;
import org.example.model.Product;
import org.example.repository.CustomerRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Order placeOrder(Long customerId, Map<Long, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        Order order = new Order(customer);

        try {
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
        } catch (InsufficientStockException e) {
            // the order rolls back, but the audit entry is committed in its own transaction
            auditService.record("PLACE_ORDER_FAILED", "customer " + customerId + ": " + e.getMessage());
            throw e;
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order pay(Long orderId, PaymentMethod method) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.NEW) {
            throw new InvalidOrderStateException(
                    "Only NEW orders can be paid; order " + orderId + " is " + order.getStatus());
        }

        Payment payment = new Payment(order.getTotal(), method);
        payment.setPaidAt(LocalDateTime.now());
        order.setPayment(payment); // cascades persist to Payment
        order.setStatus(OrderStatus.PAID);

        // no orderRepository.save(): the order is managed in this transaction, so
        // dirty checking flushes the status change (and cascades the Payment) on commit
        return order;
    }

    @Transactional
    public Order ship(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException(
                    "Only PAID orders can be shipped; order " + orderId + " is " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        return order;
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException(
                    "Only NEW or PAID orders can be cancelled; order " + orderId + " is " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    @Transactional(readOnly = true)
    public OrderSummary getOrderSummary(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        List<OrderItemSummary> items = order.getItems().stream()
                .map(item -> new OrderItemSummary(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();

        Payment payment = order.getPayment();
        return new OrderSummary(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getStatus(),
                order.getOrderedAt(),
                items,
                order.getTotal(),
                payment == null ? null : payment.getMethod(),
                payment == null ? null : payment.getPaidAt());
    }
}
