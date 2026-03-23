package com.jewelryonlinestore.entity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_email",  columnList = "email"),
                @Index(name = "idx_status", columnList = "status")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** NULL cho tài khoản đăng nhập bằng Google */
    @Column(length = 255)
    private String password;

    /** ID từ Google OAuth2 */
    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Customer customer;

    // ── Lifecycle ────────────────────────────────────────
    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Spring Security ──────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String   getUsername()              { return email; }
    @Override public boolean  isAccountNonExpired()      { return true; }
    @Override public boolean  isAccountNonLocked()       { return status != Status.LOCKED; }
    @Override public boolean  isCredentialsNonExpired()  { return true; }
    @Override public boolean  isEnabled()                { return status == Status.ACTIVE; }

    // ── Enums ────────────────────────────────────────────
    public enum Role   { CUSTOMER, ADMIN }
    public enum Status { PENDING, ACTIVE, LOCKED }
}
