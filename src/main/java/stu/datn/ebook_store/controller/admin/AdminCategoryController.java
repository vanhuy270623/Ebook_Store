package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.dto.request.BookCategoryCreateRequest;
import stu.datn.ebook_store.dto.request.BookCategoryUpdateRequest;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.service.BookCategoryService;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AdminDashboardController xử lý quản lý danh mục (CRUD Category)
 * Pattern: Tương tự AdminAuthorController, AdminUserController, AdminBookController
 */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController extends BaseController {

    private static final String REDIRECT_CATEGORIES = "redirect:/admin/categories";

    private final BookCategoryService bookCategoryService;

    @Autowired
    public AdminCategoryController(BookCategoryService bookCategoryService) {
        this.bookCategoryService = bookCategoryService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Sinh Category ID tự động theo format: "category_XX"
     */
    private String generateNextCategoryId() {
        List<BookCategory> allCategories = bookCategoryService.getAllCategories();
        int nextNumber = allCategories.size() + 1;
        return String.format("category_%02d", nextNumber);
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại (trừ danh mục đang sửa)
     */
    private boolean isCategoryNameDuplicate(String categoryName, String currentCategoryId) {
        return bookCategoryService.getCategoryByName(categoryName)
                .map(existingCategory -> !existingCategory.getBookCategoryId().equals(currentCategoryId))
                .orElse(false);
    }

    /**
     * Map Category entity sang BookCategoryUpdateRequest DTO
     */
    private BookCategoryUpdateRequest mapToUpdateRequest(BookCategory category) {
        BookCategoryUpdateRequest dto = new BookCategoryUpdateRequest();
        dto.setCategoryId(category.getBookCategoryId());
        dto.setCategoryName(category.getCategoryName());
        dto.setDescription(category.getDescription());
        dto.setIconUrl(category.getIconUrl());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
    }

    /**
     * Xử lý validation errors và thêm vào model
     */
    private String handleValidationErrors(BindingResult bindingResult, Model model, boolean isEdit,
                                        BookCategory category) {
        String errors = bindingResult.getAllErrors().stream()
                .map(org.springframework.validation.ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        model.addAttribute("error", errors);

        if (category != null) {
            model.addAttribute("category", category);
        }
        addCommonFormAttributes(model, isEdit);
        return "admin/categories/form";
    }

    /**
     * Tạo và lưu category mới hoặc cập nhật category hiện có
     */
    private BookCategory createOrUpdateCategory(BookCategoryCreateRequest createRequest,
                                              BookCategoryUpdateRequest updateRequest,
                                              BookCategory existingCategory) {
        BookCategory category = existingCategory != null ? existingCategory : new BookCategory();

        String categoryName, description, iconUrl;
        Integer displayOrder;
        Boolean isActive;

        if (createRequest != null) {
            categoryName = createRequest.getCategoryName();
            description = createRequest.getDescription();
            iconUrl = createRequest.getIconUrl();
            displayOrder = createRequest.getDisplayOrder();
            isActive = createRequest.getIsActive();

            // Set new ID và slug cho category mới
            category.setBookCategoryId(generateNextCategoryId());
        } else {
            categoryName = updateRequest.getCategoryName();
            description = updateRequest.getDescription();
            iconUrl = updateRequest.getIconUrl();
            displayOrder = updateRequest.getDisplayOrder();
            isActive = updateRequest.getIsActive();
        }

        category.setCategoryName(categoryName);
        category.setCategorySlug(createSlug(categoryName));
        category.setDescription(description);
        category.setIconUrl(iconUrl);
        category.setDisplayOrder(displayOrder);
        category.setIsActive(isActive);

        return category;
    }

    /**
     * Helper method để tìm category theo ID và xử lý trường hợp không tìm thấy
     */
    private BookCategory findCategoryOrRedirect(String categoryId, RedirectAttributes redirectAttributes) {
        BookCategory category = bookCategoryService.getCategoryById(categoryId).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục với ID: " + categoryId);
        }
        return category;
    }

    /**
     * Tạo slug từ tên danh mục (VD: "Khoa Học - Viễn Tưởng" -> "khoa-hoc-vien-tuong")
     */
    private String createSlug(String input) {
        if (input == null || input.isEmpty()) return "uncategorized";

        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách danh mục
     */
    @GetMapping
    public String categoriesList(@RequestParam(value = "search", required = false) String search,
                                 Model model) {
        List<BookCategory> categories;

        // Tìm kiếm nếu có từ khóa
        if (search != null && !search.trim().isEmpty()) {
            categories = bookCategoryService.searchCategories(search);
            model.addAttribute("search", search);
        } else {
            categories = bookCategoryService.getAllCategories();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("totalCategories", categories.size());
        return "admin/categories/list";
    }

    /**
     * Xem chi tiết danh mục
     */
    @GetMapping("/view/{id}")
    public String viewCategory(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        BookCategory category = findCategoryOrRedirect(id, redirectAttributes);
        if (category == null) {
            return REDIRECT_CATEGORIES;
        }

        model.addAttribute("category", category);
        return "admin/categories/view";
    }

    /**
     * Hiển thị form thêm danh mục mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("categoryRequest", new BookCategoryCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/categories/form";
    }

    /**
     * Hiển thị form chỉnh sửa danh mục
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        BookCategory category = findCategoryOrRedirect(id, redirectAttributes);
        if (category == null) {
            return REDIRECT_CATEGORIES;
        }

        // Chuyển đổi Category entity sang DTO
        BookCategoryUpdateRequest bookCategoryUpdateRequest = mapToUpdateRequest(category);

        model.addAttribute("categoryRequest", bookCategoryUpdateRequest);
        model.addAttribute("category", category);
        addCommonFormAttributes(model, true);

        return "admin/categories/form";
    }

    /**
     * Tạo danh mục mới
     */
    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("categoryRequest") BookCategoryCreateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult, model, false, null);
        }

        // Kiểm tra tên danh mục trùng
        if (bookCategoryService.getCategoryByName(request.getCategoryName()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã tồn tại!");
            return "redirect:/admin/categories/add";
        }

        try {
            // Tạo Category entity sử dụng helper method
            BookCategory newCategory = createOrUpdateCategory(request, null, null);
            bookCategoryService.saveCategory(newCategory);

            redirectAttributes.addFlashAttribute("success",
                    "Thêm danh mục thành công! ID: " + newCategory.getBookCategoryId() + ", Tên: " + request.getCategoryName());
            return REDIRECT_CATEGORIES;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/categories/add";
        }
    }

    /**
     * Cập nhật danh mục
     */
    @PostMapping("/update")
    public String updateCategory(@Valid @ModelAttribute("categoryRequest") BookCategoryUpdateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        BookCategory existingCategory = bookCategoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult, model, true, existingCategory);
        }

        // Kiểm tra tên danh mục trùng (trừ chính nó)
        if (isCategoryNameDuplicate(request.getCategoryName(), request.getCategoryId())) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã được sử dụng bởi danh mục khác!");
            return "redirect:/admin/categories/edit/" + request.getCategoryId();
        }

        try {
            // Cập nhật thông tin sử dụng helper method
            BookCategory updatedCategory = createOrUpdateCategory(null, request, existingCategory);
            bookCategoryService.saveCategory(updatedCategory);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công!");

            return REDIRECT_CATEGORIES;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/categories/edit/" + request.getCategoryId();
        }
    }

    /**
     * Xóa danh mục
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            bookCategoryService.deleteCategory(id);
            response.put("success", true);
            response.put("message", "Xóa danh mục thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa danh mục";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }
}

