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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<?> searchCustomers(String keyword, String status, int page, int size) {

        // Ép kiểu từ String sang Enum User.Status trước khi tìm kiếm
        User.Status statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = User.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // Bỏ qua nếu giá trị gửi lên không khớp với Enum (sẽ trả về null để lấy tất cả)
            }
        }

        Page<Customer> customers = customerRepository.searchCustomers(
                blankToNull(keyword), statusEnum, PageRequest.of(page, size));

        List<Map<String, Object>> content = customers.getContent().stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("userId", c.getUser().getId());
            row.put("fullName", c.getFullName());
            row.put("phone", c.getPhone());
            row.put("email", c.getUser().getEmail());
            row.put("status", c.getUser().getStatus().name());
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
        detail.put("email", customer.getUser().getEmail());
        detail.put("status", customer.getUser().getStatus().name());
        detail.put("createdAt", customer.getCreatedAt());
        
        // Add addresses
        List<Map<String, Object>> addresses = customer.getAddresses().stream()
                .filter(a -> !a.isDeleted())
                .map(a -> {
                    Map<String, Object> addr = new HashMap<>();
                    addr.put("id", a.getId());
                    addr.put("recipientName", a.getRecipientName());
                    addr.put("phone", a.getPhone());
                    addr.put("province", a.getProvince());
                    addr.put("district", a.getDistrict());
                    addr.put("ward", a.getWard());
                    addr.put("streetAddress", a.getStreetAddress());
                    addr.put("fullAddress", a.getFullAddress());
                    addr.put("isDefault", a.isDefault());
                    return addr;
                }).toList();
        detail.put("addresses", addresses);
        
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