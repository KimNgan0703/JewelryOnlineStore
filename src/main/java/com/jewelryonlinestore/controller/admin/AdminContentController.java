package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final BannerService bannerService;

    @GetMapping("/banners")
    public String bannerList(Model model) {
        model.addAttribute("banners", bannerService.getAllBanners());
        model.addAttribute("activeSection", "banners");
        model.addAttribute("pageTitle", "Quản Lý Banner");
        return "admin/content";
    }

    @PostMapping("/banners")
    public String createBanner(@RequestParam String title,
                               @RequestParam(required = false) String linkUrl,
                               @RequestParam int sortOrder,
                               @RequestParam(required = false) MultipartFile image,
                               @RequestParam(required = false) String imageUrlText,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                               RedirectAttributes redirectAttr) {
        try {
            bannerService.createBanner(title, linkUrl, sortOrder, image, imageUrlText, startDate, endDate);
            redirectAttr.addFlashAttribute("toast_success", "Đã thêm banner!");
        } catch (IllegalArgumentException e) {
            redirectAttr.addFlashAttribute("toast_error", e.getMessage());
        }
        return "redirect:/admin/content/banners";
    }

    @PostMapping("/banners/{id}/update")
    public String updateBanner(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam(required = false) String linkUrl,
                               @RequestParam int sortOrder,
                               @RequestParam(required = false) MultipartFile image,
                               @RequestParam(required = false) String imageUrlText,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                               RedirectAttributes redirectAttr) {
        bannerService.updateBanner(id, title, linkUrl, sortOrder, image, imageUrlText, startDate, endDate);
        redirectAttr.addFlashAttribute("toast_success", "Đã cập nhật banner!");
        return "redirect:/admin/content/banners";
    }

    @PatchMapping("/banners/{id}/toggle")
    @ResponseBody
    public ResponseEntity<ApiResponse<Boolean>> toggleBanner(@PathVariable Long id) {
        boolean active = bannerService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok(active ? "Đã bật banner" : "Đã ẩn banner", active));
    }

    @DeleteMapping("/banners/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa banner", null));
    }
}