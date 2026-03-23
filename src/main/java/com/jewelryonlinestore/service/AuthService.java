package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.entity.Customer;
import org.springframework.security.core.Authentication;

public interface AuthService {
    void     register(RegisterRequest req);
    void     completeOAuth2Profile(OAuth2CompleteProfileRequest req, Authentication auth);
    void     sendPasswordResetEmail(String email);
    boolean  isValidResetToken(String token);
    void     resetPassword(ResetPasswordRequest req);
    void     changePassword(ChangePasswordRequest req, Authentication auth);
    void     updateProfile(UpdateProfileRequest req, Authentication auth);
    Customer getCustomerProfile(Authentication auth);
}
