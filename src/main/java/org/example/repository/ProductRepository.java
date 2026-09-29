package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.model.Category;
import org.example.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Product save(Product product) {
        if (product.getId() == null) {
            entityManager.persist(product);
            return product;
        }
        return entityManager.merge(product);
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }

    public Optional<Product> findBySku(String sku) {
        return entityManager.createQuery("select p from Product p where p.sku = :sku", Product.class)
                .setParameter("sku", sku)
                .getResultStream()
                .findFirst();
    }

    public List<Product> findByCategory(String categoryName) {
        return entityManager.createQuery(
                        "select p from Product p join p.categories c where c.name = :categoryName", Product.class)
                .setParameter("categoryName", categoryName)
                .getResultList();
    }


    public List<Product> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String category) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> product = query.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get("name")), pattern),
                    cb.like(cb.lower(product.get("sku")), pattern)));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), maxPrice));
        }
        if (category != null && !category.isBlank()) {
            Join<Product, Category> categories = product.join("categories");
            predicates.add(cb.equal(categories.get("name"), category));
        }

        query.select(product)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(product.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    /** Products whose stock is strictly below the threshold, lowest stock first. */
    public List<Product> findLowStock(int threshold) {
        return entityManager.createQuery(
                        "select p from Product p where p.stock < :threshold order by p.stock asc, p.id asc",
                        Product.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    public List<Product> findPage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size must be >= 1");
        }
        return entityManager.createQuery("select p from Product p order by p.id asc", Product.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countAll() {
        return entityManager.createQuery("select count(p) from Product p", Long.class)
                .getSingleResult();
    }
}
