package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.AddressRequest;
import com.jewelryonlinestore.dto.response.AddressResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AddressService {
	List<AddressResponse> getMyAddresses(Authentication auth);
	AddressResponse addAddress(AddressRequest req, Authentication auth);
	AddressResponse updateAddress(Long id, AddressRequest req, Authentication auth);
	void setDefault(Long id, Authentication auth);
	void deleteAddress(Long id, Authentication auth);
}

