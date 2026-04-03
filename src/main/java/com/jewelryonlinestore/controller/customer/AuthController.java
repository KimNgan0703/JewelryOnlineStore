package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.service.AuthService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * C01 — Đăng ký / Đăng nhập / Quên mật khẩu / Google OAuth2 (complete-profile).
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService           authService;
    private final AuthenticationManager authenticationManager;

    // ── Trang đăng nhập ──────────────────────────────────
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String expired,
                            @RequestParam(required = false) String oauth_error,
                            @RequestParam(required = false) String redirect,
                            Model model) {
        if (isAuthenticated()) return "redirect:/";
        model.addAttribute("error",      error      != null);
        model.addAttribute("expired",    expired    != null);
        model.addAttribute("oauthError", oauth_error != null);
        model.addAttribute("redirect",   redirect);
        model.addAttribute("pageTitle",  "Đăng Nhập");
        return "customer/login";
    }

    // ── Trang đăng ký — bước 1 ───────────────────────────
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (isAuthenticated()) return "redirect:/";
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("pageTitle", "Đăng Ký");
        return "customer/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult result, Model model,
                           HttpServletRequest request, HttpServletResponse response) {

        // 1. Kiểm tra mật khẩu xác nhận
        if (registerRequest.getPassword() != null && !registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "match", "Mật khẩu xác nhận không khớp!");
        }

        // 2. Trả về form nếu có lỗi Validation (như mật khẩu không đủ mạnh)
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Đăng Ký");
            return "customer/register";
        }

        try {
            // 3. Đăng ký (sẽ ném lỗi nếu trùng email/sđt)
            authService.register(registerRequest);

            // 4. Đăng nhập tự động
            autoLogin(registerRequest.getEmail(), registerRequest.getPassword(), request, response);

            // 5. CHUYỂN TRANG VỀ TRANG CHỦ KHI THÀNH CÔNG
            return "redirect:/";

        } catch (IllegalArgumentException e) {
            // BẮT LỖI TRÙNG EMAIL HOẶC SỐ ĐIỆN THOẠI
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Đăng Ký");
            return "customer/register";

        } catch (Exception e) {
            log.error("Lỗi đăng ký tài khoản: ", e);
            model.addAttribute("errorMessage", "Hệ thống đang bận, vui lòng thử lại sau.");
            model.addAttribute("pageTitle", "Đăng Ký");
            return "customer/register";
        }
    }

    // ── Bổ sung thông tin sau Google OAuth2 ─────────────
    @GetMapping("/complete-profile")
    public String completeProfilePage(Model model) {
        model.addAttribute("completeProfileRequest", new OAuth2CompleteProfileRequest());
        model.addAttribute("pageTitle", "Hoàn tất hồ sơ");
        return "customer/complete-profile";
    }

    @PostMapping("/complete-profile")
    public String completeProfile(@Valid @ModelAttribute("completeProfileRequest") OAuth2CompleteProfileRequest req,
                                  BindingResult result,
                                  Authentication auth,
                                  Model model,
                                  RedirectAttributes redirectAttr) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "match", "Mật khẩu xác nhận không khớp");
        }
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Hoàn tất hồ sơ");
            return "customer/complete-profile";
        }
        authService.completeOAuth2Profile(req, auth);
        redirectAttr.addFlashAttribute("toast_success", "Hồ sơ đã được cập nhật!");
        return "redirect:/";
    }

    // ── Quên mật khẩu ────────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        model.addAttribute("pageTitle", "Quên Mật Khẩu");
        return "customer/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest req,
                                 BindingResult result, Model model) {

        log.info("=== BẮT ĐẦU XỬ LÝ QUÊN MẬT KHẨU ===");
        log.info("Email nhận được từ Form: '{}'", req.getEmail());

        // 1. Kiểm tra Validation
        if (result.hasErrors()) {
            log.warn("Dữ liệu form bị lỗi Validation: {}", result.getAllErrors());
            model.addAttribute("pageTitle", "Quên Mật Khẩu");
            return "customer/forgot-password";
        }

        try {
            // 2. Gọi Service để gửi email
            log.info("Tiến hành gọi authService.sendPasswordResetEmail()...");
            authService.sendPasswordResetEmail(req.getEmail());
            log.info("Gọi Service gửi email thành công!");

            // 3. Báo thành công
            model.addAttribute("successMessage", "Vui lòng kiểm tra email của bạn để nhận liên kết đặt lại mật khẩu.");

        } catch (IllegalArgumentException e) {
            log.warn("Lỗi logic (ví dụ email không tồn tại): {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            log.error("Lỗi Exception hệ thống khi gửi email: ", e);
            model.addAttribute("errorMessage", "Hệ thống đang bận, không thể gửi email lúc này.");
        }

        model.addAttribute("pageTitle", "Quên Mật Khẩu");
        return "customer/forgot-password";
    }
    // ── Đặt lại mật khẩu ─────────────────────────────────
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
        // Kiểm tra token có hợp lệ không
        boolean isValid = authService.isValidResetToken(token);

        // LUÔN LUÔN gửi biến tokenExpired ra màn hình (true hoặc false), không bao giờ để rỗng
        model.addAttribute("tokenExpired", !isValid);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        model.addAttribute("resetPasswordRequest", request);

        model.addAttribute("pageTitle", "Đặt Lại Mật Khẩu");
        return "customer/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute ResetPasswordRequest req,
                                BindingResult result, Model model,
                                RedirectAttributes redirectAttr) {

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "match", "Mật khẩu xác nhận không khớp");
        }

        if (result.hasErrors()) {
            model.addAttribute("tokenExpired", false);      // ← thêm dòng này
            model.addAttribute("resetPasswordRequest", req); // ← thêm dòng này
            model.addAttribute("pageTitle", "Đặt Lại Mật Khẩu");
            return "customer/reset-password";
        }

        try {
            authService.resetPassword(req);
            redirectAttr.addFlashAttribute("toast_success",
                    "Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("tokenExpired", false);      // ← thêm dòng này
            model.addAttribute("resetPasswordRequest", req); // ← thêm dòng này
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Đặt Lại Mật Khẩu");
            return "customer/reset-password";
        }
    }

    // ── Helper ────────────────────────────────────────────
    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    private void autoLogin(String email, String password,
                           HttpServletRequest req, HttpServletResponse res) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication auth = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), req, res);
    }
}

