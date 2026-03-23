package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.service.FileStorageService;
import com.jewelryonlinestore.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileStorageService fileStorageService;

    @Override
    public String upload(MultipartFile file, String folder) {
        return fileStorageService.store(file, folder);
    }

    @Override
    public boolean delete(String path) {
        return fileStorageService.delete(path);
    }
}

