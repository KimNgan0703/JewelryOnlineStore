package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.BlogPost;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.BlogPostRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.service.BlogService;
import com.jewelryonlinestore.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPost> adminGetAllPosts(int page, int size) {
        return blogPostRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public BlogPost createPost(String title, String content, String excerpt, MultipartFile featuredImage,
                               boolean publish, Authentication auth) {
        User author = resolveUser(auth);
        String slug = uniqueSlug(title, null);
        String image = featuredImage != null && !featuredImage.isEmpty()
                ? fileStorageService.store(featuredImage, "blog")
                : null;

        BlogPost post = BlogPost.builder()
                .title(title)
                .slug(slug)
                .content(content)
                .excerpt(excerpt)
                .featuredImage(image)
                .author(author)
                .authorName(author.getCustomer() != null ? author.getCustomer().getFullName() : author.getEmail())
                .build();

        if (publish) {
            post.publish();
        }
        return blogPostRepository.save(post);
    }

    @Override
    @Transactional
    public boolean togglePublish(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + id));
        if (post.isPublished()) {
            post.unpublish();
        } else {
            post.publish();
        }
        return blogPostRepository.save(post).isPublished();
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + id));
        if (post.getFeaturedImage() != null && !post.getFeaturedImage().isBlank()) {
            fileStorageService.delete(post.getFeaturedImage());
        }
        blogPostRepository.delete(post);
    }

    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String uniqueSlug(String input, Long excludeId) {
        String base = toSlug(input);
        String candidate = base;
        int index = 1;
        while (blogPostRepository.existsBySlugAndIdNot(candidate, excludeId == null ? -1L : excludeId)) {
            candidate = base + "-" + index++;
        }
        return candidate;
    }

    private String toSlug(String input) {
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = WHITESPACE.matcher(noAccent).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ROOT);
    }
}

