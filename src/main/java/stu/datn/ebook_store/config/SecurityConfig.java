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
                // ===== CSRF PROTECTION =====
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/admin/books/delete/**") // Ignore CSRF for delete endpoint
                )

                // ===== AUTHORIZATION RULES =====
                .authorizeHttpRequests(authorize -> authorize
                        // === PUBLIC ACCESS ===
                        .requestMatchers(
                                "/", "/home", "/login", "/register", // Các trang chung
                                "/auth/**", // Auth endpoints (/auth/login, /auth/register)
                                "/favicon.ico", "/favicon.svg", // Favicon files
                                "/css/**", "/js/**", "/images/**", // Tài nguyên tĩnh
                                "/user_template/**", "/admin_template/**", "/shared/**", // Template resources
                                "/static/**", // Static resources path
                                "/book_asset/**", // All book asset paths (images, previews)
                                "/uploads/**" // Upload directory access (alias)
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

                // ===== FORM LOGIN - DISABLED (Dùng custom AuthController) =====
                .formLogin(form -> form.disable())

                // ===== LOGOUT - DISABLED (Dùng custom /logout) =====
                .logout(logout -> logout.disable())

                // ===== HTTP BASIC - DISABLED =====
                .httpBasic(httpBasic -> httpBasic.disable())

                // ===== SESSION MANAGEMENT =====
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                )

                // ===== EXCEPTION HANDLING =====
                .exceptionHandling(exception -> exception
                        // AccessDeniedHandler: Khi đã login nhưng không đủ quyền
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String ajaxHeader = request.getHeader("X-Requested-With");
                            boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);

                            if (isAjax) {
                                response.setStatus(403);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"Không có quyền truy cập!\"}");
                            } else {
                                response.sendRedirect("/auth/login?error=access_denied");
                            }
                        })
                        // AuthenticationEntryPoint: Khi chưa login hoặc session hết hạn
                        .authenticationEntryPoint((request, response, authException) -> {
                            String ajaxHeader = request.getHeader("X-Requested-With");
                            String acceptHeader = request.getHeader("Accept");

                            boolean isAjax = "XMLHttpRequest".equals(ajaxHeader) ||
                                           (acceptHeader != null && acceptHeader.contains("application/json"));

                            if (isAjax) {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!\"}");
                            } else {
                                response.sendRedirect("/auth/login");
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
