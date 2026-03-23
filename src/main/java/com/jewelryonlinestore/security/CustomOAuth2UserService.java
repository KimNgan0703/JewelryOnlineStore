package com.jewelryonlinestore.security;

import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = super.loadUser(userRequest);

		String email = oauth2User.getAttribute("email");
		String googleId = oauth2User.getAttribute("sub");
		String name = oauth2User.getAttribute("name");

		if (email == null || email.isBlank()) {
			throw new OAuth2AuthenticationException("Google account does not provide email");
		}

		User user = userRepository.findByEmail(email)
				.orElseGet(() -> userRepository.save(User.builder()
						.email(email)
						.googleId(googleId)
						.password(null)
						.role(User.Role.CUSTOMER)
						.status(User.Status.ACTIVE)
						.build()));

		if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
			user.setGoogleId(googleId);
			user = userRepository.save(user);
		}

		if (customerRepository.findByUserId(user.getId()).isEmpty()) {
			customerRepository.save(Customer.builder()
					.user(user)
					.fullName(name != null && !name.isBlank() ? name : email)
					.build());
		}


		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		oauth2User.getAuthorities().forEach(a -> authorities.add(new SimpleGrantedAuthority(a.getAuthority())));
		authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

		Map<String, Object> attributes = new java.util.HashMap<>(oauth2User.getAttributes());
		attributes.put("email", email);

		return new DefaultOAuth2User(authorities, attributes, "email");
	}
}

