package com.jewelryonlinestore.repository;


import com.jewelryonlinestore.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // Tìm token hợp lệ (chưa hết hạn, chưa dùng)
    @Query("""
        SELECT t FROM VerificationToken t
        WHERE t.token = :token
          AND t.type  = :type
          AND t.expiresAt > :now
          AND t.usedAt IS NULL
    """)
    Optional<VerificationToken> findValidToken(
            @Param("token") String token,
            @Param("type")  String type,
            @Param("now")   LocalDateTime now
    );

    // Vô hiệu hóa tất cả token cũ của user cùng loại (trước khi tạo mới)
    @Modifying
    @Query("""
        UPDATE VerificationToken t SET t.usedAt = :now
        WHERE t.user.id = :userId AND t.type = :type AND t.usedAt IS NULL
    """)
    int invalidateOldTokens(
            @Param("userId") Long userId,
            @Param("type")   String type,
            @Param("now")    LocalDateTime now
    );

    // Xóa token hết hạn (chạy scheduled cleanup)
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}

