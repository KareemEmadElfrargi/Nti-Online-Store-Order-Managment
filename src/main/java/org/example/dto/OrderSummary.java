package org.example.dto;

import org.example.model.OrderStatus;
import org.example.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderSummary(
        Long orderId,
        Long customerId,
        String customerName,
        OrderStatus status,
        LocalDateTime orderedAt,
        List<OrderItemSummary> items,
        BigDecimal total,
        PaymentMethod paymentMethod,
        LocalDateTime paidAt
) {
}
