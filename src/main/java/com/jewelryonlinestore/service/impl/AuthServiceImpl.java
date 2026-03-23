package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.entity.Address;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.entity.VerificationToken;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.repository.VerificationTokenRepository;
import com.jewelryonlinestore.service.AuthService;
import com.jewelryonlinestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.CUSTOMER)
                .status(User.Status.ACTIVE)
                .build());

        Customer customer = Customer.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .gender(parseGender(req.getGender()))
                .birthDate(req.getBirthDate())
                .build();

        Address defaultAddress = Address.builder()
                .customer(customer)
                .recipientName(req.getRecipientName())
                .phone(req.getRecipientPhone())
                .province(req.getProvince())
                .district(req.getDistrict())
                .ward(req.getWard())
                .streetAddress(req.getStreetAddress())
                .isDefault(true)
                .build();

        customer.getAddresses().add(defaultAddress);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void completeOAuth2Profile(OAuth2CompleteProfileRequest req, Authentication auth) {
        User user = requireUser(auth);
        Customer customer = requireCustomer(auth);
        customer.setFullName(req.getFullName());
        customer.setPhone(req.getPhone());
        customer.setGender(parseGender(req.getGender()));
        customer.setBirthDate(req.getBirthDate());
        customerRepository.save(customer);

        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            verificationTokenRepository.invalidateOldTokens(
                    user.getId(), VerificationToken.TokenType.PASSWORD_RESET.name(), LocalDateTime.now());
            verificationTokenRepository.save(VerificationToken.builder()
                    .user(user)
                    .token(token)
                    .type(VerificationToken.TokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusHours(2))
                    .build());
            emailService.sendPasswordResetEmail(email, token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidResetToken(String token) {
        return verificationTokenRepository
                .findValidToken(token, VerificationToken.TokenType.PASSWORD_RESET.name(), LocalDateTime.now())
                .isPresent();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        VerificationToken token = verificationTokenRepository
                .findValidToken(req.getToken(), VerificationToken.TokenType.PASSWORD_RESET.name(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest req, Authentication auth) {
        User user = requireUser(auth);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileRequest req, Authentication auth) {
        Customer customer = requireCustomer(auth);
        customer.setFullName(req.getFullName());
        customer.setPhone(req.getPhone());
        customer.setGender(parseGender(req.getGender()));
        customer.setBirthDate(req.getBirthDate());
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerProfile(Authentication auth) {
        return requireCustomer(auth);
    }

    private User requireUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Customer requireCustomer(Authentication auth) {
        User user = requireUser(auth);
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));
    }

    private Customer.Gender parseGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        return Customer.Gender.valueOf(gender.trim().toUpperCase());
    }
}

