package com.jewelryonlinestore.security;

import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
										HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {
		String email = authentication.getName();
		Object principal = authentication.getPrincipal();
		if (principal instanceof OAuth2User oauth2User) {
			Object emailAttr = oauth2User.getAttributes().get("email");
			if (emailAttr instanceof String emailFromAttr && !emailFromAttr.isBlank()) {
				email = emailFromAttr;
			}
		}

		if (email == null || email.isBlank()) {
			response.sendRedirect("/auth/login?oauth_error=true");
			return;
		}

		boolean needProfile = userRepository.findByEmail(email)
				.map(user -> {
					boolean missingPassword = user.getPassword() == null || user.getPassword().isBlank();
					boolean missingProfile = customerRepository.findByUserId(user.getId())
							.map(customer -> customer.getFullName() == null || customer.getFullName().isBlank())
							.orElse(true);
					return missingPassword || missingProfile;
				})
				.orElse(true);

		if (needProfile) {
			response.sendRedirect("/auth/complete-profile");
			return;
		}

		response.sendRedirect("/");
	}
}

