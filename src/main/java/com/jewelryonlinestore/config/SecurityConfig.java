package com.jewelryonlinestore.config;

import com.jewelryonlinestore.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl  userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2SuccessHandler;

    // ── Public URLs ────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/", "/products/**", "/categories/**",
            "/search", "/blog/**",
            "/auth/**",                   // đăng ký, đăng nhập, quên mật khẩu
            "/oauth2/**", "/login/oauth2/**",
            "/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // ── Authorization ───────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart/**", "/orders/**", "/account/**",
                                "/wishlist/**", "/reviews/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )

                // ── Form Login (email/password) ──────────────────
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            if (auth.getAuthorities().stream()
                                    .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"))) {
                                res.sendRedirect("/admin/dashboard");
                            } else {
                                String redirect = req.getParameter("redirect");
                                res.sendRedirect(redirect != null && !redirect.isBlank() ? redirect : "/");
                            }
                        })
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )

                // ── OAuth2 Google Login ──────────────────────────
                .oauth2Login(oauth -> oauth
                        .loginPage("/auth/login")
                        .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("/auth/login?oauth_error=true")
                )

                // ── Logout ───────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/?logout=true")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )

                // ── Remember Me ──────────────────────────────────
                .rememberMe(rm -> rm
                        .key("jewelry-secret-key-2024")
                        .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 ngày
                        .userDetailsService(userDetailsService)
                )

                // ── Session ──────────────────────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
                )

                // ── CSRF (giữ nguyên cho form, disable cho REST endpoints) ──
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/**", "/payment/callback/**")
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
