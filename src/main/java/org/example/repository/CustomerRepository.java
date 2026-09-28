package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            entityManager.persist(customer);
            return customer;
        }
        return entityManager.merge(customer);
    }

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Customer.class, id));
    }

    public Optional<Customer> findByEmail(String email) {
        return entityManager.createQuery("select c from Customer c where c.email = :email", Customer.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public List<Customer> findAll() {
        return entityManager.createQuery("select c from Customer c", Customer.class)
                .getResultList();
    }
}
