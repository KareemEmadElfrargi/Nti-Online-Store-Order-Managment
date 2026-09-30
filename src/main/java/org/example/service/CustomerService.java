package org.example.service;

import org.example.exception.DuplicateCustomerException;
import org.example.model.Address;
import org.example.model.Customer;
import org.example.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer register(String name, String email, Address shippingAddress) {
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new DuplicateCustomerException(email);
        }
        return customerRepository.save(new Customer(name, email, shippingAddress));
    }
}
