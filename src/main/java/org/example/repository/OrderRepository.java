package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Order save(Order order) {
        if (order.getId() == null) {
            entityManager.persist(order);
            return order;
        }
        return entityManager.merge(order);
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Order.class, id));
    }

    public Optional<Order> findByIdWithItems(Long id) {
        return entityManager.createQuery(
                        "select distinct o from Order o left join fetch o.items where o.id = :id", Order.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    public List<Order> findByCustomer(Long customerId) {
        return entityManager.createQuery(
                        "select o from Order o where o.customer.id = :customerId order by o.orderedAt desc", Order.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return entityManager.createQuery(
                        "select o from Order o where o.status = :status order by o.orderedAt desc", Order.class)
                .setParameter("status", status)
                .getResultList();
    }
}
