package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.CategoryCreateRequest;
import stu.datn.ebook_store.dto.request.CategoryUpdateRequest;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.service.CategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý danh mục (CRUD Category)
 * Pattern: Tương tự AdminAuthorController, AdminUserController, AdminBookController
 */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private static final String REDIRECT_CATEGORIES = "redirect:/admin/categories";

    private final CategoryService categoryService;

    @Autowired
    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Sinh Category ID tự động theo format: "category_XX"
     */
    private String generateNextCategoryId() {
        List<Category> allCategories = categoryService.getAllCategories();
        int nextNumber = allCategories.size() + 1;
        return String.format("category_%02d", nextNumber);
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại (trừ danh mục đang sửa)
     */
    private boolean isCategoryNameDuplicate(String categoryName, String currentCategoryId) {
        Category existingCategory = categoryService.getCategoryByName(categoryName).orElse(null);
        if (existingCategory == null) {
            return false;
        }
        return !existingCategory.getCategoryId().equals(currentCategoryId);
    }

    /**
     * Map Category entity sang CategoryUpdateRequest DTO
     */
    private CategoryUpdateRequest mapToUpdateRequest(Category category) {
        CategoryUpdateRequest dto = new CategoryUpdateRequest();
        dto.setCategoryId(category.getCategoryId());
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

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách danh mục
     */
    @GetMapping
    public String categoriesList(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("totalCategories", categories.size());
        return "admin/categories/list";
    }

    /**
     * Xem chi tiết danh mục
     */
    @GetMapping("/view/{id}")
    public String viewCategory(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id).orElse(null);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục với ID: " + id);
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
        model.addAttribute("categoryRequest", new CategoryCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/categories/form";
    }

    /**
     * Hiển thị form chỉnh sửa danh mục
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id).orElse(null);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return REDIRECT_CATEGORIES;
        }

        // Chuyển đổi Category entity sang DTO
        CategoryUpdateRequest categoryUpdateRequest = mapToUpdateRequest(category);

        model.addAttribute("categoryRequest", categoryUpdateRequest);
        model.addAttribute("category", category);
        addCommonFormAttributes(model, true);

        return "admin/categories/form";
    }

    /**
     * Tạo danh mục mới
     */
    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("categoryRequest") CategoryCreateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, false);
            return "admin/categories/form";
        }

        // Kiểm tra tên danh mục trùng
        if (categoryService.getCategoryByName(request.getCategoryName()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã tồn tại!");
            return "redirect:/admin/categories/add";
        }

        try {
            // Tạo Category entity
            String newCategoryId = generateNextCategoryId();
            Category newCategory = new Category();
            newCategory.setCategoryId(newCategoryId);
            newCategory.setCategoryName(request.getCategoryName());
            newCategory.setDescription(request.getDescription());
            newCategory.setIconUrl(request.getIconUrl());
            newCategory.setDisplayOrder(request.getDisplayOrder());
            newCategory.setIsActive(request.getIsActive());

            categoryService.saveCategory(newCategory);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm danh mục thành công! ID: " + newCategoryId + ", Tên: " + request.getCategoryName());

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
    public String updateCategory(@Valid @ModelAttribute("categoryRequest") CategoryUpdateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);

            Category category = categoryService.getCategoryById(request.getCategoryId()).orElse(null);
            model.addAttribute("category", category);
            addCommonFormAttributes(model, true);

            return "admin/categories/form";
        }

        Category existingCategory = categoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Kiểm tra tên danh mục trùng (trừ chính nó)
        if (isCategoryNameDuplicate(request.getCategoryName(), request.getCategoryId())) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã được sử dụng bởi danh mục khác!");
            return "redirect:/admin/categories/edit/" + request.getCategoryId();
        }

        try {
            // Cập nhật thông tin
            existingCategory.setCategoryName(request.getCategoryName());
            existingCategory.setDescription(request.getDescription());
            existingCategory.setIconUrl(request.getIconUrl());
            existingCategory.setDisplayOrder(request.getDisplayOrder());
            existingCategory.setIsActive(request.getIsActive());

            categoryService.saveCategory(existingCategory);
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
            categoryService.deleteCategory(id);
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

    /**
     * Hiển thị thống kê danh mục
     */
    @GetMapping("/statistics")
    public String categoriesStatistics(Model model) {
        List<Category> allCategories = categoryService.getAllCategories();
        model.addAttribute("totalCategories", allCategories.size());
        model.addAttribute("categories", allCategories);
        return "admin/categories/statistics";
    }
}

