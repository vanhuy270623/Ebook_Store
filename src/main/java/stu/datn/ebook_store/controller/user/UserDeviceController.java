package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.dto.DeviceResponseDto;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.UserDevice;
import stu.datn.ebook_store.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserDeviceController - Quản lý thiết bị
 * Chịu trách nhiệm về tracking thiết bị và bảo mật phiên đăng nhập.
 *
 * Endpoints:
 * - GET  /user/devices                    : Trang quản lý thiết bị
 * - POST /user/devices/{deviceId}/remove  : Xóa thiết bị
 * - GET  /user/api/devices                : API lấy danh sách thiết bị (JSON)
 */
@Controller
@RequestMapping("/user")
public class UserDeviceController {

    private final UserService userService;

    @Autowired
    public UserDeviceController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Trang quản lý thiết bị
     */
    @GetMapping("/devices")
    public String devicesPage(Authentication authentication,
                             jakarta.servlet.http.HttpSession session,
                             Model model) {
        User currentUser = getCurrentUser(authentication);
        String currentDeviceId = (String) session.getAttribute("currentDeviceId");

        // Lấy danh sách devices
        List<UserDevice> devices = userService.getUserDevices(currentUser.getUserId());

        // Lấy subscription hiện tại để biết max_devices
        int maxDevices = getUserMaxDevices(currentUser.getUserId());
        String subscriptionInfo = getSubscriptionInfo(currentUser.getUserId());

        // Kiểm tra xem user có phải admin không
        boolean isAdmin = currentUser.getRole() != null &&
                         currentUser.getRole().getRoleName() == stu.datn.ebook_store.entity.Role.RoleName.ADMIN;

        // Tạo DTO cho view
        List<DeviceResponseDto> deviceDtos = devices.stream()
            .map(d -> DeviceResponseDto.fromEntity(
                d, d.getDeviceId().equals(currentDeviceId)))
            .collect(Collectors.toList());

        model.addAttribute("devices", deviceDtos);
        model.addAttribute("currentDeviceId", currentDeviceId);
        model.addAttribute("maxDevices", maxDevices);
        model.addAttribute("currentCount", devices.size());
        model.addAttribute("violationCount", currentUser.getDeviceViolationCount());
        model.addAttribute("subscriptionInfo", subscriptionInfo);
        model.addAttribute("isAdmin", isAdmin);

        return "user/devices/manage";
    }

    /**
     * Helper: Lấy max devices từ subscription
     */
    private int getUserMaxDevices(String userId) {
        return userService.getUserMaxDevices(userId);
    }

    /**
     * Helper: Lấy thông tin subscription hiện tại
     */
    private String getSubscriptionInfo(String userId) {
        return userService.getUserSubscriptionInfo(userId);
    }

    /**
     * Xóa thiết bị
     */
    @PostMapping("/devices/{deviceId}/remove")
    @ResponseBody
    public Map<String, Object> removeDevice(
            @PathVariable String deviceId,
            Authentication authentication,
            jakarta.servlet.http.HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser(authentication);
        String currentDeviceId = (String) session.getAttribute("currentDeviceId");

        try {
            userService.removeDeviceWithCurrentCheck(currentUser.getUserId(), deviceId, currentDeviceId);
            response.put("success", true);
            response.put("message", "Xóa thiết bị thành công");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * API: Lấy danh sách devices (JSON)
     */
    @GetMapping("/api/devices")
    @ResponseBody
    public Map<String, Object> getDevicesApi(Authentication authentication,
                                             jakarta.servlet.http.HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser(authentication);
        String currentDeviceId = (String) session.getAttribute("currentDeviceId");

        List<UserDevice> devices = userService.getUserDevices(currentUser.getUserId());
        List<DeviceResponseDto> deviceDtos = devices.stream()
            .map(d -> DeviceResponseDto.fromEntity(
                d, d.getDeviceId().equals(currentDeviceId)))
            .collect(Collectors.toList());

        int maxDevices = getUserMaxDevices(currentUser.getUserId());
        String subscriptionInfo = getSubscriptionInfo(currentUser.getUserId());

        // Kiểm tra xem user có phải admin không
        boolean isAdmin = currentUser.getRole() != null &&
                         currentUser.getRole().getRoleName() == stu.datn.ebook_store.entity.Role.RoleName.ADMIN;

        response.put("success", true);
        response.put("devices", deviceDtos);
        response.put("currentCount", devices.size());
        response.put("maxDevices", maxDevices);
        response.put("violationCount", currentUser.getDeviceViolationCount());
        response.put("subscriptionInfo", subscriptionInfo);
        response.put("isAdmin", isAdmin);

        return response;
    }
}

