package stu.datn.ebook_store.controller.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserController {

    @GetMapping("/index")
    public String userIndex(HttpSession session, Model model) {
        // Spring Security đã check authentication và role (USER hoặc ADMIN)
        // Chỉ cần lấy thông tin từ session để hiển thị
        model.addAttribute("username", session.getAttribute("username"));
        return "user/index";
    }
}

