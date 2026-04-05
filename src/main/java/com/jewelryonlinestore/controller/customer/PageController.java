package com.jewelryonlinestore.controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/privacy")
    public String privacyPage(Model model) {
        model.addAttribute("pageTitle", "Chính sách & Điều khoản");
        return "customer/privacy";
    }

    // THÊM HÀM NÀY ĐỂ HỨNG LINK ABOUT US
    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "Về Chúng Tôi");
        return "customer/about"; // Trỏ đến file about.html
    }
}