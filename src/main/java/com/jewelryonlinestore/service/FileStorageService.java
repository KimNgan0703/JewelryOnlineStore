package com.jewelryonlinestore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

	private final Cloudinary cloudinary;

	// Tiêm cấu hình CloudinaryConfig vào đây
	public FileStorageService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	public String store(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}

		try {
			// Đẩy ảnh thẳng lên Cloudinary
			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", folder,
					"public_id", UUID.randomUUID().toString()
			));

			// Trả về đường link URL bảo mật (https) của ảnh vừa upload
			return uploadResult.get("secure_url").toString();

		} catch (IOException ex) {
			throw new IllegalStateException("Lỗi khi tải ảnh lên Cloudinary", ex);
		}
	}

	public String store(MultipartFile file) {
		return store(file, "jewelry_online_store");
	}

	public boolean delete(String publicIdOrUrl) {
		// Trích xuất publicId từ URL nếu cần, sau đó xóa ảnh trên Cloudinary
		try {
			// Giả định bạn truyền publicId vào, nếu truyền url thì cần cắt chuỗi trước
			Map result = cloudinary.uploader().destroy(publicIdOrUrl, ObjectUtils.emptyMap());
			return "ok".equals(result.get("result"));
		} catch (IOException e) {
			return false;
		}
	}
}