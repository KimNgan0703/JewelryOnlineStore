package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.AddressRequest;
import com.jewelryonlinestore.dto.response.AddressResponse;
import com.jewelryonlinestore.entity.Address;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.AddressRepository;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(Authentication auth) {
        Customer customer = requireCustomer(auth);
        return addressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest req, Authentication auth) {
        Customer customer = requireCustomer(auth);
        if (req.isDefault()) {
            addressRepository.clearDefaultByCustomer(customer.getId());
        }

        Address saved = addressRepository.save(Address.builder()
                .customer(customer)
                .recipientName(req.getRecipientName())
                .phone(req.getPhone())
                .province(req.getProvince())
                .district(req.getDistrict())
                .ward(req.getWard())
                .streetAddress(req.getStreetAddress())
                .isDefault(req.isDefault())
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest req, Authentication auth) {
        Customer customer = requireCustomer(auth);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to current customer");
        }

        if (req.isDefault()) {
            addressRepository.clearDefaultByCustomer(customer.getId());
            address.setDefault(true);
        }

        address.setRecipientName(req.getRecipientName());
        address.setPhone(req.getPhone());
        address.setProvince(req.getProvince());
        address.setDistrict(req.getDistrict());
        address.setWard(req.getWard());
        address.setStreetAddress(req.getStreetAddress());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void setDefault(Long id, Authentication auth) {
        Customer customer = requireCustomer(auth);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to current customer");
        }
        addressRepository.clearDefaultByCustomer(customer.getId());
        address.setDefault(true);
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id, Authentication auth) {
        Customer customer = requireCustomer(auth);
        if (!addressRepository.existsByIdAndCustomerId(id, customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to current customer");
        }
        addressRepository.softDelete(id);
    }

    private Customer requireCustomer(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));
    }

    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .province(a.getProvince())
                .district(a.getDistrict())
                .ward(a.getWard())
                .streetAddress(a.getStreetAddress())
                .fullAddress(a.getFullAddress())
                .isDefault(a.isDefault())
                .build();
    }
}

