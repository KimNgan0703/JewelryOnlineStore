package com.jewelryonlinestore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

	private final Path rootLocation;

	public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
		this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.rootLocation);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not initialize upload directory", ex);
		}
	}

	public String store(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}

		String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
		String extension = extractExtension(originalName);
		String safeFileName = UUID.randomUUID() + extension;

		Path targetDir = resolveAndValidate(folder);
		try {
			Files.createDirectories(targetDir);
			Path target = targetDir.resolve(safeFileName).normalize();
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not store file", ex);
		}

		String safeFolder = sanitizeForReturn(folder);
		return safeFolder.isBlank() ? safeFileName : safeFolder + "/" + safeFileName;
	}

	public String store(MultipartFile file) {
		return store(file, "");
	}

	public Resource loadAsResource(String storedPath) {
		Path file = resolveAndValidate(storedPath);
		try {
			Resource resource = new UrlResource(file.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new IllegalArgumentException("File not found: " + storedPath);
			}
			return resource;
		} catch (MalformedURLException ex) {
			throw new IllegalStateException("Could not read file", ex);
		}
	}

	public boolean delete(String storedPath) {
		Path file = resolveAndValidate(storedPath);
		try {
			return Files.deleteIfExists(file);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not delete file", ex);
		}
	}

	private Path resolveAndValidate(String relativePath) {
		String cleaned = sanitizeRelativePath(relativePath);
		Path resolved = rootLocation.resolve(cleaned).normalize();
		if (!resolved.startsWith(rootLocation)) {
			throw new IllegalArgumentException("Invalid file path");
		}
		return resolved;
	}

	private String sanitizeRelativePath(String value) {
		String cleaned = StringUtils.cleanPath(Objects.requireNonNullElse(value, ""));
		if (cleaned.contains("..")) {
			throw new IllegalArgumentException("Invalid path");
		}
		return cleaned.replace('\\', '/');
	}

	private String sanitizeForReturn(String folder) {
		String cleaned = sanitizeRelativePath(folder).trim();
		while (cleaned.startsWith("/")) {
			cleaned = cleaned.substring(1);
		}
		return cleaned;
	}

	private String extractExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
	}
}
