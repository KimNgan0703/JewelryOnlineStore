package com.jewelryonlinestore.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens",
        indexes = {
                @Index(name = "idx_token",   columnList = "token"),
                @Index(name = "idx_expires", columnList = "expires_at")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** NULL = chưa dùng */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean isExpired()  { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isUsed()     { return usedAt != null; }
    public boolean isValid()    { return !isExpired() && !isUsed(); }

    public enum TokenType { EMAIL_VERIFICATION, PASSWORD_RESET }
}
