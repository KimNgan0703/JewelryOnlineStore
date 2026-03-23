package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<?> searchCustomers(String keyword, String status, int page, int size) {
        Page<Customer> customers = customerRepository.searchCustomers(
                blankToNull(keyword), blankToNull(status), PageRequest.of(page, size));

        List<Map<String, Object>> content = customers.getContent().stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("fullName", c.getFullName());
            row.put("phone", c.getPhone());
            row.put("email", c.getEmail());
            row.put("status", c.getStatus());
            return row;
        }).toList();

        return new PageImpl<>(content, customers.getPageable(), customers.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Object getCustomerDetail(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", customer.getId());
        detail.put("fullName", customer.getFullName());
        detail.put("phone", customer.getPhone());
        detail.put("email", customer.getEmail());
        detail.put("status", customer.getStatus());
        detail.put("createdAt", customer.getCreatedAt());
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<?> getCustomerOrders(Long customerId, int page, int size) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public String toggleLock(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        User user = customer.getUser();
        User.Status next = user.getStatus() == User.Status.LOCKED ? User.Status.ACTIVE : User.Status.LOCKED;
        user.setStatus(next);
        userRepository.save(user);
        return next.name();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

