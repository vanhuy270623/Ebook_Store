package stu.datn.ebook_store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ===== ENABLE CSRF PROTECTION =====
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                // ===== AUTHORIZATION RULES =====
                .authorizeHttpRequests(authorize -> authorize
                        // === PUBLIC ACCESS ===
                        .requestMatchers(
                                "/", "/home", "/login", "/register", // Các trang chung
                                "/auth/**", // Auth endpoints (/auth/login, /auth/register)
                                "/css/**", "/js/**", "/images/**", "/fonts/**", // Tài nguyên tĩnh
                                "/user_template/**", "/admin_template/**", // Template resources
                                "/shared/**", // Shared resources (CSS, JS, fonts)
                                "/static/**", // Static resources path
                                "/book_asset/image/**", // Public book images
                                "/uploads/**" // Upload directory access
                        ).permitAll()

                        // === PROTECTED BOOK SOURCE FILES ===
                        .requestMatchers("/book_asset/source/**").authenticated() // ⚠️ YÊU CẦU ĐĂNG NHẬP

                        // === ADMIN ACCESS ONLY ===
                        .requestMatchers("/admin/**").hasRole("ADMIN") // ⚠️ CHỈ ADMIN

                        // === USER & ADMIN ACCESS ===
                        .requestMatchers(
                                "/user/**", // All user routes
                                "/subscription/manage" // Manage subscription
                        ).hasAnyRole("USER", "ADMIN") // ⚠️ USER hoặc ADMIN

                        // === DEFAULT ===
                        .anyRequest().authenticated() // Yêu cầu đăng nhập cho các route khác
                )

                // ===== TẮT FORM LOGIN MẶC ĐỊNH =====
                // Vẫn dùng custom AuthController xử lý
                .formLogin(form -> form.disable())

                // ===== TẮT LOGOUT MẶC ĐỊNH =====
                // Vẫn dùng custom GET /logout
                .logout(logout -> logout.disable())

                // ===== SESSION MANAGEMENT =====
                // Đảm bảo SecurityContext được lưu vào session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                )

                // ===== ACCESS DENIED HANDLER =====
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/auth/login?error=access_denied")
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Redirect về login nếu chưa authenticated
                            response.sendRedirect("/auth/login");
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
