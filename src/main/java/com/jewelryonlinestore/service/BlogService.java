package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {
	Page<BlogPost> adminGetAllPosts(int page, int size);
	BlogPost createPost(String title, String content, String excerpt,
						MultipartFile featuredImage, boolean publish,
						Authentication auth);
	boolean togglePublish(Long id);
	void deletePost(Long id);
}

