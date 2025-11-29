package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.AuthorCreateRequest;
import stu.datn.ebook_store.dto.request.AuthorUpdateRequest;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.service.AuthorService;
import stu.datn.ebook_store.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý tác giả (CRUD Author)
 * Pattern: Tương tự AdminUserController và AdminBookController
 */
@Controller
@RequestMapping("/admin/authors")
public class AdminAuthorController {

    private static final String REDIRECT_AUTHORS = "redirect:/admin/authors";

    private final AuthorService authorService;
    private final FileStorageService fileStorageService;

    @Autowired
    public AdminAuthorController(AuthorService authorService, FileStorageService fileStorageService) {
        this.authorService = authorService;
        this.fileStorageService = fileStorageService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Sinh Author ID tự động theo format: "author_XX"
     */
    private String generateNextAuthorId() {
        List<Author> allAuthors = authorService.getAllAuthors();
        int nextNumber = allAuthors.size() + 1;
        return String.format("author_%02d", nextNumber);
    }

    /**
     * Kiểm tra tên tác giả đã tồn tại (trừ tác giả đang sửa)
     */
    private boolean isAuthorNameDuplicate(String name, String currentAuthorId) {
        Author existingAuthor = authorService.getAuthorByName(name).orElse(null);
        if (existingAuthor == null) {
            return false;
        }
        return !existingAuthor.getAuthorId().equals(currentAuthorId);
    }

    /**
     * Map Author entity sang AuthorUpdateRequest DTO
     */
    private AuthorUpdateRequest mapToUpdateRequest(Author author) {
        AuthorUpdateRequest dto = new AuthorUpdateRequest();
        dto.setAuthorId(author.getAuthorId());
        dto.setName(author.getName());
        dto.setBiography(author.getBiography());
        dto.setAvatarUrl(author.getAvatarUrl());
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách tác giả
     */
    @GetMapping
    public String authorsList(@RequestParam(required = false) String search, Model model) {
        List<Author> authors;

        if (search != null && !search.trim().isEmpty()) {
            authors = authorService.searchAuthorsByName(search);
            model.addAttribute("search", search);
        } else {
            authors = authorService.getAllAuthors();
        }

        model.addAttribute("authors", authors);
        model.addAttribute("totalAuthors", authors.size());
        return "admin/authors/list";
    }

    /**
     * Xem chi tiết tác giả
     */
    @GetMapping("/view/{id}")
    public String viewAuthor(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Author author = authorService.getAuthorById(id).orElse(null);

        if (author == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tác giả với ID: " + id);
            return REDIRECT_AUTHORS;
        }

        model.addAttribute("author", author);
        return "admin/authors/view";
    }

    /**
     * Hiển thị form thêm tác giả mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("authorRequest", new AuthorCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/authors/form";
    }

    /**
     * Hiển thị form chỉnh sửa tác giả
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Author author = authorService.getAuthorById(id).orElse(null);

        if (author == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tác giả!");
            return REDIRECT_AUTHORS;
        }

        // Chuyển đổi Author entity sang DTO
        AuthorUpdateRequest authorUpdateRequest = mapToUpdateRequest(author);

        model.addAttribute("authorRequest", authorUpdateRequest);
        model.addAttribute("author", author);
        addCommonFormAttributes(model, true);

        return "admin/authors/form";
    }

    /**
     * Tạo tác giả mới
     */
    @PostMapping("/create")
    public String createAuthor(@Valid @ModelAttribute("authorRequest") AuthorCreateRequest request,
                              BindingResult bindingResult,
                              @RequestParam(value = "avatarImage", required = false) MultipartFile avatarImage,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, false);
            return "admin/authors/form";
        }

        // Kiểm tra tên tác giả trùng
        if (authorService.getAuthorByName(request.getName()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên tác giả đã tồn tại!");
            return "redirect:/admin/authors/add";
        }

        try {
            // Upload avatar nếu có
            if (avatarImage != null && !avatarImage.isEmpty()) {
                String avatarUrl = fileStorageService.storeAuthorAvatar(avatarImage);
                request.setAvatarUrl(avatarUrl);
            }

            // Tạo Author entity
            String newAuthorId = generateNextAuthorId();
            Author newAuthor = new Author();
            newAuthor.setAuthorId(newAuthorId);
            newAuthor.setName(request.getName());
            newAuthor.setBiography(request.getBiography());
            newAuthor.setAvatarUrl(request.getAvatarUrl());
            newAuthor.setCreatedAt(LocalDateTime.now());

            authorService.saveAuthor(newAuthor);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm tác giả thành công! ID: " + newAuthorId + ", Tên: " + request.getName());

            return REDIRECT_AUTHORS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/authors/add";
        }
    }

    /**
     * Cập nhật tác giả
     */
    @PostMapping("/update")
    public String updateAuthor(@Valid @ModelAttribute("authorRequest") AuthorUpdateRequest request,
                              BindingResult bindingResult,
                              @RequestParam(value = "avatarImage", required = false) MultipartFile avatarImage,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);

            Author author = authorService.getAuthorById(request.getAuthorId()).orElse(null);
            model.addAttribute("author", author);
            addCommonFormAttributes(model, true);

            return "admin/authors/form";
        }

        Author existingAuthor = authorService.getAuthorById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả"));

        // Kiểm tra tên tác giả trùng (trừ chính nó)
        if (isAuthorNameDuplicate(request.getName(), request.getAuthorId())) {
            redirectAttributes.addFlashAttribute("error", "Tên tác giả đã được sử dụng bởi tác giả khác!");
            return "redirect:/admin/authors/edit/" + request.getAuthorId();
        }

        try {
            // Upload avatar nếu có
            if (avatarImage != null && !avatarImage.isEmpty()) {
                String avatarUrl = fileStorageService.storeAuthorAvatar(avatarImage);
                request.setAvatarUrl(avatarUrl);
            }

            // Cập nhật thông tin
            existingAuthor.setName(request.getName());
            existingAuthor.setBiography(request.getBiography());
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                existingAuthor.setAvatarUrl(request.getAvatarUrl());
            }

            authorService.saveAuthor(existingAuthor);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tác giả thành công!");

            return REDIRECT_AUTHORS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/authors/edit/" + request.getAuthorId();
        }
    }

    /**
     * Xóa tác giả
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAuthor(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            authorService.deleteAuthor(id);
            response.put("success", true);
            response.put("message", "Xóa tác giả thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa tác giả";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload avatar tác giả
     */
    @PostMapping("/upload-avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAuthorAvatar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String avatarUrl = fileStorageService.storeAuthorAvatar(file);
            response.put("success", true);
            response.put("url", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị thống kê tác giả
     */
    @GetMapping("/statistics")
    public String authorsStatistics(Model model) {
        List<Author> allAuthors = authorService.getAllAuthors();
        model.addAttribute("totalAuthors", allAuthors.size());
        model.addAttribute("recentAuthors", allAuthors.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList()));
        return "admin/authors/statistics";
    }
}

