package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final BannerService bannerService;
    private final BlogService blogService;

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
                               @RequestParam(required = false) String imageUrlText, // Link ảnh dán tay
                               RedirectAttributes redirectAttr) {
        try {
            bannerService.createBanner(title, linkUrl, sortOrder, image, imageUrlText);
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
                               @RequestParam(required = false) String imageUrlText, // Link ảnh dán tay
                               RedirectAttributes redirectAttr) {
        bannerService.updateBanner(id, title, linkUrl, sortOrder, image, imageUrlText);
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

    // ... (Toàn bộ các API /blog bên dưới bạn giữ nguyên không sửa gì) ...
    @GetMapping("/blog")
    public String blogList(@RequestParam(defaultValue = "0") int page, Model model) {
        var posts = blogService.adminGetAllPosts(page, 15);
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("currentPage", posts.getNumber());
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("activeSection", "blog");
        model.addAttribute("pageTitle", "Quản Lý Blog");
        return "admin/content";
    }

    @GetMapping("/blog/new")
    public String newBlogForm(Model model) {
        model.addAttribute("activeSection", "blog");
        model.addAttribute("pageTitle", "Bài Viết Mới");
        return "admin/content";
    }

    @PostMapping("/blog")
    public String createPost(@RequestParam String title,
                             @RequestParam String content,
                             @RequestParam(required = false) String excerpt,
                             @RequestParam(required = false) MultipartFile featuredImage,
                             @RequestParam(defaultValue = "false") boolean publish,
                             Authentication auth,
                             RedirectAttributes redirectAttr) {
        blogService.createPost(title, content, excerpt, featuredImage, publish, auth);
        redirectAttr.addFlashAttribute("toast_success", "Đã tạo bài viết!");
        return "redirect:/admin/content/blog";
    }

    @PostMapping("/blog/{id}/publish")
    @ResponseBody
    public ResponseEntity<ApiResponse<Boolean>> publishPost(@PathVariable Long id) {
        boolean published = blogService.togglePublish(id);
        return ResponseEntity.ok(ApiResponse.ok(published ? "Đã đăng bài" : "Đã ẩn bài", published));
    }

    @DeleteMapping("/blog/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa bài viết", null));
    }
}