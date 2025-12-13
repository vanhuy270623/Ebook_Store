package stu.datn.ebook_store.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class để generate BCrypt password hash
 * Dùng để test hoặc tạo password mới cho user
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Test passwords
        String[] passwords = {"admin", "user123", "password123", "123456"};

        System.out.println("=== BCrypt Password Generator ===\n");

        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Plain text: " + password);
            System.out.println("BCrypt hash: " + hash);
            System.out.println("Verify: " + encoder.matches(password, hash));
            System.out.println("---");
        }

        // Custom password encoding
        System.out.println("\n=== Custom Password ===");
        String customPassword = "mypassword"; // Thay đổi ở đây
        String customHash = encoder.encode(customPassword);
        System.out.println("Password: " + customPassword);
        System.out.println("Hash: " + customHash);
        System.out.println("\nCopy hash này vào SQL INSERT statement:");
        System.out.println("password_hash = '" + customHash + "'");
    }
}

