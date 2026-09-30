package org.example.dto;

import java.math.BigDecimal;

public record OrderItemSummary(
        Long productId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
