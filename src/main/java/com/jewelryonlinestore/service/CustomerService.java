package com.jewelryonlinestore.service;

import org.springframework.data.domain.Page;

public interface CustomerService {
    Page<?>  searchCustomers(String keyword, String status, int page, int size);
    Object   getCustomerDetail(Long id);
    Page<?>  getCustomerOrders(Long customerId, int page, int size);
    String   toggleLock(Long id);
}