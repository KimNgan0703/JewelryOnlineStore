package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.dto.response.AddressResponse;
import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * C01 — Quản lý thông tin cá nhân, địa chỉ, đổi mật khẩu.
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService    authService;
    private final AddressService addressService;

    // ── Trang hồ sơ ──────────────────────────────────────
    @GetMapping("/profile")
    public String profilePage(Authentication auth, Model model) {
        Customer customer = authService.getCustomerProfile(auth);
        populateAccountModel(model, auth, "profile", toUpdateProfileRequest(customer), new ChangePasswordRequest());
        return "customer/account";
    }

    // ── Cập nhật thông tin ───────────────────────────────
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute UpdateProfileRequest req,
                                BindingResult result,
                                Authentication auth,
                                Model model,
                                RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            populateAccountModel(model, auth, "profile", req, new ChangePasswordRequest());
            return "customer/account";
        }
        authService.updateProfile(req, auth);
        redirectAttr.addFlashAttribute("toast_success", "Cập nhật thông tin thành công!");
        return "redirect:/account/profile";
    }

    // ── Đổi mật khẩu ─────────────────────────────────────
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest req,
                                 BindingResult result,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes redirectAttr) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "match", "Mật khẩu xác nhận không khớp");
        }
        if (result.hasErrors()) {
            Customer customer = authService.getCustomerProfile(auth);
            populateAccountModel(model, auth, "password", toUpdateProfileRequest(customer), req);
            return "customer/account";
        }
        try {
            authService.changePassword(req, auth);
            redirectAttr.addFlashAttribute("toast_success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttr.addFlashAttribute("toast_error", e.getMessage());
        }
        return "redirect:/account/profile";
    }

    // ── Trang địa chỉ ─────────────────────────────────────
    @GetMapping("/addresses")
    public String addressPage(Authentication auth, Model model) {
        Customer customer = authService.getCustomerProfile(auth);
        populateAccountModel(model, auth, "addresses", toUpdateProfileRequest(customer), new ChangePasswordRequest());
        return "customer/account";
    }

    // ── Thêm địa chỉ (AJAX) ──────────────────────────────
    @PostMapping("/addresses")
    @ResponseBody
    public ResponseEntity<ApiResponse<AddressResponse>>
    addAddress(@Valid @RequestBody AddressRequest req,
               Authentication auth) {
        var addr = addressService.addAddress(req, auth);
        return ResponseEntity.ok(ApiResponse.ok("Thêm địa chỉ thành công!", addr));
    }

    // ── Sửa địa chỉ (AJAX) ───────────────────────────────
    @PutMapping("/addresses/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<AddressResponse>>
    updateAddress(@PathVariable Long id,
                  @Valid @RequestBody AddressRequest req,
                  Authentication auth) {
        var addr = addressService.updateAddress(id, req, auth);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật địa chỉ thành công!", addr));
    }

    // ── Đặt mặc định (AJAX) ──────────────────────────────
    @PatchMapping("/addresses/{id}/default")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> setDefault(@PathVariable Long id,
                                                        Authentication auth) {
        addressService.setDefault(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt làm địa chỉ mặc định", null));
    }

    // ── Xóa địa chỉ (AJAX) ───────────────────────────────
    @DeleteMapping("/addresses/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id,
                                                           Authentication auth) {
        addressService.deleteAddress(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa địa chỉ", null));
    }

    private void populateAccountModel(Model model,
                                      Authentication auth,
                                      String activeTab,
                                      UpdateProfileRequest updateProfileRequest,
                                      ChangePasswordRequest changePasswordRequest) {
        Customer customer = authService.getCustomerProfile(auth);
        model.addAttribute("customer", customer);
        model.addAttribute("addresses", addressService.getMyAddresses(auth));
        model.addAttribute("updateProfileRequest", updateProfileRequest != null ? updateProfileRequest : toUpdateProfileRequest(customer));
        model.addAttribute("changePasswordRequest", changePasswordRequest != null ? changePasswordRequest : new ChangePasswordRequest());
        model.addAttribute("addressRequest", new AddressRequest());
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("pageTitle", "Tài Khoản Của Tôi");
    }

    private UpdateProfileRequest toUpdateProfileRequest(Customer customer) {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName(customer.getFullName());
        req.setPhone(customer.getPhone());
        req.setBirthDate(customer.getBirthDate());
        req.setGender(customer.getGender() == null ? "" : customer.getGender().name().toLowerCase());
        return req;
    }

}
