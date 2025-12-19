package stu.datn.ebook_store.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.DeviceInfoDto;
import stu.datn.ebook_store.dto.LoginDto;
import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.UserDevice;
import stu.datn.ebook_store.service.UserService;

import java.util.Collections;
import java.util.Map;


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
                              @RequestParam(required = false) String deviceFingerprint,
                              @RequestParam(required = false) String deviceName,
                              @RequestParam(required = false) String deviceType,
                              BindingResult bindingResult,
                              HttpSession session,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/auth/login";
        }

        try {
            // Tạo DeviceInfo từ request
            DeviceInfoDto deviceInfo = new DeviceInfoDto();
            deviceInfo.setDeviceFingerprint(deviceFingerprint);
            deviceInfo.setDeviceName(deviceName);
            deviceInfo.setDeviceType(deviceType != null ? deviceType : "WEB");
            deviceInfo.setUserAgent(request.getHeader("User-Agent"));

            // ===== XÁC THỰC VỚI DEVICE CHECKING =====
            Map<String, Object> authResult = userService.authenticateWithDeviceCheck(
                loginDto.getUsername(),
                loginDto.getPassword(),
                deviceInfo,
                request
            );

            String status = (String) authResult.get("status");

            // Xử lý kết quả theo status
            if ("ACCOUNT_LOCKED".equals(status)) {
                redirectAttributes.addFlashAttribute("errorMessage", authResult.get("reason"));
                return "redirect:/auth/login";
            }

            if ("DEVICE_LIMIT_EXCEEDED".equals(status)) {
                int violationCount = (Integer) authResult.get("violationCount");
                int maxDevices = (Integer) authResult.get("maxDevices");

                String errorMsg = String.format(
                    "⚠️ Bạn đã đạt giới hạn %d thiết bị. " +
                    "Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị. " +
                    "Cảnh báo: %d/3 lần vi phạm.",
                    maxDevices, violationCount
                );

                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                redirectAttributes.addFlashAttribute("showDeviceManagement", true);
                return "redirect:/auth/login";
            }

            // SUCCESS - Tiếp tục login bình thường
            User user = (User) authResult.get("user");
            UserDevice device = (UserDevice) authResult.get("device");
            Boolean isNewDevice = (Boolean) authResult.getOrDefault("isNewDevice", false);

            // ===== TÍCH HỢP VỚI SPRING SECURITY =====
            String roleName = "ROLE_" + user.getRole().getRoleName().name();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(roleName))
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            // Lưu thông tin user vào session
            session.setAttribute("loggedInUser", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole().getRoleName().name());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("currentDeviceId", device.getDeviceId());

            // Cập nhật last_login
            userService.updateLastLogin(user.getUserId());

            // Thông báo nếu là device mới
            if (isNewDevice) {
                redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Đăng nhập thành công! Thiết bị mới đã được đăng ký: " + device.getDeviceName());
            }

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
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        // Xử lý lỗi validation từ annotations
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessages = new StringBuilder();
            bindingResult.getAllErrors().forEach(error -> {
                errorMessages.append("• ").append(error.getDefaultMessage()).append("\n");
            });
            redirectAttributes.addFlashAttribute("errorMessage", errorMessages.toString().trim());
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
            return "redirect:/auth/register";
        }

        try {
            userService.registerUser(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Đăng ký thành công! Vui lòng đăng nhập để tiếp tục.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
            return "redirect:/auth/register";
        }
    }
}