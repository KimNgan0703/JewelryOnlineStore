package com.jewelryonlinestore.repository;


import com.jewelryonlinestore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm theo email (dùng cho đăng nhập, kiểm tra trùng)
    Optional<User> findByEmail(String email);

    // Tìm theo Google ID (dùng cho OAuth2 login)
    Optional<User> findByGoogleId(String googleId);

    // Kiểm tra email đã tồn tại chưa (đăng ký)
    boolean existsByEmail(String email);

    // Tìm user theo email VÀ role (admin login)
    Optional<User> findByEmailAndRole(String email, String role);

    // Tìm tất cả user đang bị khóa
    List<User> findByStatus(String status);

    // Cập nhật trạng thái tài khoản (khóa / mở khóa - A06)
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // Cập nhật password (đặt lại mật khẩu)
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    // Liên kết Google ID vào tài khoản email có sẵn
    @Modifying
    @Query("UPDATE User u SET u.googleId = :googleId WHERE u.email = :email")
    int linkGoogleAccount(@Param("email") String email, @Param("googleId") String googleId);
}

