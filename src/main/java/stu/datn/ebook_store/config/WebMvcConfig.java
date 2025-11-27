package stu.datn.ebook_store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:F:/datn_uploads/books}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.replace("\\", "/") + "/")
                .addResourceLocations("file:F:/datn_uploads/");
        registry.addResourceHandler("/book_asset/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/");
    }
}

