package com.jewelryonlinestore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Map /uploads/** → thư mục vật lý, dùng cho ảnh sản phẩm do admin upload.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
        // Static assets
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * Formatter cho LocalDate trong form binding (dd/MM/yyyy).
     */
    @Override
    public void addFormatters(org.springframework.format.FormatterRegistry registry) {
        org.springframework.format.datetime.standard.DateTimeFormatterRegistrar registrar =
                new org.springframework.format.datetime.standard.DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        registrar.registerFormatters(registry);
    }
}
