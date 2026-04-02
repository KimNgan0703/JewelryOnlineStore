package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.entity.NewsletterSubscriber;
import com.jewelryonlinestore.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterSubscriberRepository subscriberRepository;

    @PostMapping("/newsletter/subscribe")
    public String subscribe(@RequestParam("email") String email, RedirectAttributes redirectAttr) {
        if (!subscriberRepository.existsByEmail(email)) {
            subscriberRepository.save(NewsletterSubscriber.builder()
                    .email(email)
                    .createdAt(LocalDateTime.now())
                    .build());
            redirectAttr.addFlashAttribute("toast_success", "Cảm ơn bạn đã đăng ký nhận thông báo!");
        } else {
            redirectAttr.addFlashAttribute("toast_error", "Email này đã được đăng ký trước đó.");
        }
        return "redirect:/"; // Quay lại trang chủ
    }
}