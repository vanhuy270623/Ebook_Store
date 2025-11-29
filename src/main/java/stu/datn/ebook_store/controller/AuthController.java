package stu.datn.ebook_store.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.LoginDto;
import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.UserService;

import java.util.Collections;


@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET: Hiển thị trang login
     */
    @GetMapping("/auth/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginDto", new LoginDto());
        // Trả về file /templates/auth/login.html
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String processLogin(@ModelAttribute("loginDto") LoginDto loginDto,
                              BindingResult bindingResult,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/auth/login";
        }

        try {
            // Gọi service để xác thực đăng nhập
            User user = userService.authenticateUser(loginDto.getUsername(), loginDto.getPassword());

            // ===== TÍCH HỢP VỚI SPRING SECURITY =====
            // Tạo Authentication object với role
            String roleName = "ROLE_" + user.getRole().getRoleName().name();

            // Tạo Authentication với User object làm principal
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(roleName))
            );

            // Tạo SecurityContext và set Authentication
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Lưu SecurityContext vào session theo cách Spring Security yêu cầu
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            // Lưu thông tin user vào session (để dùng trong template)
            session.setAttribute("loggedInUser", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole().getRoleName().name());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("email", user.getEmail());

            // Cập nhật last_login
            userService.updateLastLogin(user.getUserId());

            // Chuyển hướng theo role
            if ("ADMIN".equals(user.getRole().getRoleName().name())) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/user/index";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /**
     * GET: Logout (deprecated - use POST /auth/logout)
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        // Xóa HttpSession
        session.invalidate();

        // Xóa Spring Security Authentication
        SecurityContextHolder.clearContext();

        redirectAttributes.addFlashAttribute("successMessage", "Đăng xuất thành công!");
        return "redirect:/auth/login";
    }

    /**
     * POST: Logout (recommended - CSRF protected)
     */
    @PostMapping("/auth/logout")
    public String logoutPost(HttpSession session, RedirectAttributes redirectAttributes) {
        // Xóa HttpSession
        session.invalidate();

        // Xóa Spring Security Authentication
        SecurityContextHolder.clearContext();

        redirectAttributes.addFlashAttribute("successMessage", "Đăng xuất thành công!");
        return "redirect:/auth/login";
    }

    /**
     * GET: Hiển thị trang register
     */
    @GetMapping("/auth/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register";
    }

    /**
     * POST: Xử lý dữ liệu từ form register
     */
    @PostMapping("/auth/register")
    public String processRegistration(@ModelAttribute("registerDto") RegisterDto registerDto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/register";
        }
    }
}