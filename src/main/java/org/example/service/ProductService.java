package org.example.service;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product addProduct(String sku, String name, BigDecimal price, int stock) {
        requirePositivePrice(price);
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        return productRepository.save(new Product(sku, name, price, stock));
    }

    @Transactional
    public Product restock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than 0");
        }
        Product product = findProduct(productId);
        product.setStock(product.getStock() + quantity);
        return product;
    }

    @Transactional
    public Product changePrice(Long productId, BigDecimal newPrice) {
        requirePositivePrice(newPrice);
        Product product = findProduct(productId);
        product.setPrice(newPrice);
        return product;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private void requirePositivePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
    }
}
