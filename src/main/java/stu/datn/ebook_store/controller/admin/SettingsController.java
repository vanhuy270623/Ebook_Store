package stu.datn.ebook_store.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;

/**
 * Controller xử lý cài đặt hệ thống
 * Endpoint: /admin/settings
 */
@Controller
@RequestMapping("/admin/settings")
public class SettingsController extends BaseController {

    /**
     * Hiển thị trang cài đặt
     */
    @GetMapping
    public String settings(Model model) {
        // Load settings từ database hoặc config file
        // Placeholder - sẽ implement sau

        model.addAttribute("siteName", "Ebook Store");
        model.addAttribute("siteEmail", "admin@ebookstore.com");
        model.addAttribute("itemsPerPage", 10);
        model.addAttribute("enableRegistration", true);
        model.addAttribute("enableReviews", true);
        model.addAttribute("maintenanceMode", false);

        return "admin/settings";
    }

    /**
     * Cập nhật cài đặt
     */
    @PostMapping("/update")
    public String updateSettings(
            @RequestParam String siteName,
            @RequestParam String siteEmail,
            @RequestParam int itemsPerPage,
            @RequestParam(required = false) boolean enableRegistration,
            @RequestParam(required = false) boolean enableReviews,
            @RequestParam(required = false) boolean maintenanceMode,
            RedirectAttributes redirectAttributes) {

        try {
            // Save settings to database hoặc config file
            // Placeholder - sẽ implement sau

            redirectAttributes.addFlashAttribute("success", "Cập nhật cài đặt thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/settings";
    }
}

