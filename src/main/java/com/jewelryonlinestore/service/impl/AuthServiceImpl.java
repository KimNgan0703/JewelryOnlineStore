package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.entity.Address;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.entity.VerificationToken;
import com.jewelryonlinestore.repository.AddressRepository;
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
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public void register(RegisterRequest req) {
        // 1. Kiểm tra Email trùng
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký trong hệ thống!");
        }

        // 2. Kiểm tra Số điện thoại trùng
        if (req.getPhone() != null && !req.getPhone().trim().isEmpty()) {
            if (customerRepository.existsByPhone(req.getPhone().trim())) {
                throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi tài khoản khác!");
            }
        }

        // 3. Tạo tài khoản User
        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.CUSTOMER) // Hoặc Role.CUSTOMER tùy cấu hình import của bạn
                .status(User.Status.ACTIVE) // Hoặc Status.ACTIVE tùy cấu hình import của bạn
                .build();
        userRepository.save(user);

        // 4. Tạo Hồ sơ Khách hàng (Customer)
        Customer customer = Customer.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .build();
        customerRepository.save(customer);

        // 5. THÊM ĐIỀU KIỆN IF VÀO ĐÂY:
        // Chỉ lưu Address nếu form thực sự có gửi lên trường district (Quận/Huyện)
        if (req.getDistrict() != null && !req.getDistrict().trim().isEmpty()) {
            Address address = Address.builder()
                    .customer(customer)
                    .recipientName(req.getRecipientName() != null ? req.getRecipientName() : req.getFullName())
                    .phone(req.getRecipientPhone() != null ? req.getRecipientPhone() : req.getPhone())
                    .province(req.getProvince())
                    .district(req.getDistrict())
                    .ward(req.getWard())
                    .streetAddress(req.getStreetAddress())
                    .isDefault(true)
                    .isDeleted(false)
                    .build();
            addressRepository.save(address);
        }
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
        // Kiểm tra email, nếu không có sẽ ném ra lỗi để Controller bắt
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống!"));

        String token = UUID.randomUUID().toString();

        // Chú ý: Không có .name() ở tham số PASSWORD_RESET
        verificationTokenRepository.invalidateOldTokens(
                user.getId(), VerificationToken.TokenType.PASSWORD_RESET, LocalDateTime.now());

        verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .token(token)
                .type(VerificationToken.TokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build());

        emailService.sendPasswordResetEmail(email, token);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidResetToken(String token) {
        // Chú ý: Không có .name() ở tham số PASSWORD_RESET
        return verificationTokenRepository
                .findValidToken(token, VerificationToken.TokenType.PASSWORD_RESET, LocalDateTime.now())
                .isPresent();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        // Chú ý: Không có .name() ở tham số PASSWORD_RESET
        VerificationToken token = verificationTokenRepository
                .findValidToken(req.getToken(), VerificationToken.TokenType.PASSWORD_RESET, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Đường dẫn đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

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

