package stu.datn.ebook_store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:F:/datn_uploads/book_asset}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle /book_asset/** requests - main upload directory
        registry.addResourceHandler("/book_asset/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/");

        // Handle /uploads/** requests (alias for compatibility)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/");

        // Handle /books/** requests (for backward compatibility)
        registry.addResourceHandler("/books/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/image/covers/");
    }
}

