# 📂 FLOW 12: ADMIN CATEGORY MANAGEMENT (Quản Lý Danh Mục)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 12.1: List Categories](#flow-121-list-categories)
3. [Flow 12.2: Create Category](#flow-122-create-category)
4. [Flow 12.3: View Category Details](#flow-123-view-category-details)
5. [Flow 12.4: Edit Category](#flow-124-edit-category)
6. [Flow 12.5: Delete Category](#flow-125-delete-category)
7. [Flow 12.6: Check Duplicate Category Name](#flow-126-check-duplicate-category-name)
8. [Error Handling](#error-handling)

---

## Tổng Quan

### Components
- **Controller**: `AdminCategoryController.java`
- **Service**: `CategoryService.java`
- **Repository**: `CategoryRepository.java`
- **Entity**: `Category.java`
- **DTOs**: `CategoryCreateRequest.java`, `CategoryUpdateRequest.java`

### URLs
- `GET /admin/categories` - Danh sách danh mục
- `GET /admin/categories/create` - Form tạo danh mục
- `POST /admin/categories/create` - Xử lý tạo danh mục
- `GET /admin/categories/view/{id}` - Chi tiết danh mục
- `GET /admin/categories/edit/{id}` - Form sửa danh mục
- `POST /admin/categories/edit/{id}` - Xử lý sửa danh mục
- `POST /admin/categories/delete/{id}` - Xóa danh mục
- `GET /api/admin/categories/check-name` - Kiểm tra tên trùng

### Category Structure
```
Category:
  - categoryId: String (auto: "category_01", "category_02", ...)
  - categoryName: String (unique)
  - description: String
  - iconUrl: String
  - displayOrder: Integer
  - isActive: Boolean
  - createdAt: LocalDateTime
```

---

## Flow 12.1: List Categories

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  GET /admin/categories                                                           │
  │─────────────────────────►│                                                       │
  │       │                  │ getAllCategories()                                    │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ findAll()                      │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │                   │ SELECT *   │
  │       │                  │                      │                   ├───────────►│
  │       │                  │                      │                   │◄───────────┤
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ (return admin/categories/list.html)                  │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String categoriesList(Model model) {
    List<Category> categories = categoryService.getAllCategories();
    model.addAttribute("categories", categories);
    model.addAttribute("totalCategories", categories.size());
    return "admin/categories/list";
}
```

**Service**:
```java
@Override
public List<Category> getAllCategories() {
    return categoryRepository.findAll();
}
```

**SQL Query**:
```sql
SELECT c.category_id, c.category_name, c.description, 
       c.icon_url, c.display_order, c.is_active, c.created_at,
       COUNT(bc.book_id) as book_count
FROM categories c
LEFT JOIN book_category bc ON c.category_id = bc.category_id
GROUP BY c.category_id
ORDER BY c.display_order ASC, c.category_name ASC;
```

**Response Data**:
```json
{
  "categories": [
    {
      "categoryId": "category_01",
      "categoryName": "Tâm Lý - Kỹ Năng Sống",
      "description": "Sách về phát triển bản thân, kỹ năng mềm",
      "iconUrl": "/book_asset/image/icons/tamly.png",
      "displayOrder": 1,
      "isActive": true,
      "bookCount": 25
    },
    {
      "categoryId": "category_02",
      "categoryName": "Kinh Tế - Quản Lý",
      "description": "Sách về kinh tế, quản trị kinh doanh",
      "iconUrl": "/book_asset/image/icons/kinhte.png",
      "displayOrder": 2,
      "isActive": true,
      "bookCount": 18
    }
  ]
}
```

---

## Flow 12.2: Create Category

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  GET /admin/categories/create                                                    │
  │─────────────────────────►│                                                       │
  │◄─────────────────────────┤ (return form with empty CategoryCreateRequest)       │
  │       │                  │                                                       │
  │  POST /admin/categories/create (CategoryCreateRequest + iconFile)               │
  │─────────────────────────►│                                                       │
  │       │                  │ Validate input                                        │
  │       │                  │ generateNextCategoryId()                              │
  │       │                  │ checkNameDuplicate()                                  │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ findByCategoryName()           │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │       │                  │ uploadIcon()                                          │
  │       │                  │ saveCategory()                                        │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ save()                         │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │                   │ INSERT     │
  │       │                  │                      │                   ├───────────►│
  │       │                  │                      │                   │◄───────────┤
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ redirect:/admin/categories                           │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("category", new CategoryCreateRequest());
    addCommonFormAttributes(model, false);
    return "admin/categories/form";
}

@PostMapping("/create")
public String createCategory(@Valid @ModelAttribute("category") CategoryCreateRequest request,
                            BindingResult result,
                            @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
                            RedirectAttributes redirectAttributes,
                            Model model) {
    // Validation
    if (result.hasErrors()) {
        addCommonFormAttributes(model, false);
        return "admin/categories/form";
    }
    
    // Check duplicate name
    if (isCategoryNameDuplicate(request.getCategoryName(), null)) {
        result.rejectValue("categoryName", "error.category", "Tên danh mục đã tồn tại");
        addCommonFormAttributes(model, false);
        return "admin/categories/form";
    }
    
    try {
        // Generate ID
        String categoryId = generateNextCategoryId();
        
        // Upload icon
        String iconUrl = null;
        if (iconFile != null && !iconFile.isEmpty()) {
            iconUrl = fileStorageService.saveIcon(iconFile);
        }
        
        // Create category
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setIconUrl(iconUrl);
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setCreatedAt(LocalDateTime.now());
        
        categoryService.saveCategory(category);
        
        redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục thành công!");
        return REDIRECT_CATEGORIES;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_CATEGORIES;
    }
}
```

**Helper Method**:
```java
private String generateNextCategoryId() {
    List<Category> allCategories = categoryService.getAllCategories();
    int nextNumber = allCategories.size() + 1;
    return String.format("category_%02d", nextNumber);
}

private boolean isCategoryNameDuplicate(String categoryName, String currentCategoryId) {
    Category existingCategory = categoryService.getCategoryByName(categoryName).orElse(null);
    if (existingCategory == null) {
        return false;
    }
    return !existingCategory.getCategoryId().equals(currentCategoryId);
}
```

**SQL Query**:
```sql
-- Check name exists
SELECT * FROM categories WHERE category_name = ?;

-- Insert new category
INSERT INTO categories (category_id, category_name, description, icon_url, 
                       display_order, is_active, created_at)
VALUES (?, ?, ?, ?, ?, ?, NOW());
```

**Request DTO**:
```java
public class CategoryCreateRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không được quá 255 ký tự")
    private String categoryName;
    
    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;
    
    private String iconUrl;
    
    @Min(value = 0, message = "Thứ tự hiển thị phải >= 0")
    private Integer displayOrder;
    
    private Boolean isActive;
}
```

---

## Flow 12.3: View Category Details

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  GET /admin/categories/view/{id}                                                 │
  │─────────────────────────►│                                                       │
  │       │                  │ getCategoryById(id)                                   │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ findById(id)                   │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │                   │ SELECT     │
  │       │                  │                      │                   ├───────────►│
  │       │                  │                      │                   │◄───────────┤
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │       │                  │ getBooksByCategory(id)                                │
  │       │                  ├─────────────────────►│                                │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ (return admin/categories/view.html)                  │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/view/{id}")
public String viewCategory(@PathVariable String id, Model model, 
                          RedirectAttributes redirectAttributes) {
    Category category = categoryService.getCategoryById(id).orElse(null);
    
    if (category == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
        return REDIRECT_CATEGORIES;
    }
    
    // Get books in this category
    List<Book> booksInCategory = bookService.getBooksByCategory(id);
    
    model.addAttribute("category", category);
    model.addAttribute("books", booksInCategory);
    model.addAttribute("bookCount", booksInCategory.size());
    
    return "admin/categories/view";
}
```

**SQL Query**:
```sql
-- Get category details
SELECT * FROM categories WHERE category_id = ?;

-- Get books in category
SELECT b.* FROM books b
INNER JOIN book_category bc ON b.category_id = bc.category_id
WHERE bc.category_id = ?
ORDER BY b.created_at DESC;
```

---

## Flow 12.4: Edit Category

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  GET /admin/categories/edit/{id}                                                 │
  │─────────────────────────►│                                                       │
  │       │                  │ getCategoryById(id)                                   │
  │       │                  ├─────────────────────►│                                │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ (return form with CategoryUpdateRequest)             │
  │       │                  │                                                       │
  │  POST /admin/categories/edit/{id} (CategoryUpdateRequest)                       │
  │─────────────────────────►│                                                       │
  │       │                  │ Validate input                                        │
  │       │                  │ checkNameDuplicate(excludeId)                         │
  │       │                  │ uploadIcon() if provided                              │
  │       │                  │ updateCategory()                                      │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ save()                         │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │                   │ UPDATE     │
  │       │                  │                      │                   ├───────────►│
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ redirect:/admin/categories                           │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id, Model model, 
                          RedirectAttributes redirectAttributes) {
    Category category = categoryService.getCategoryById(id).orElse(null);
    
    if (category == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
        return REDIRECT_CATEGORIES;
    }
    
    CategoryUpdateRequest dto = mapToUpdateRequest(category);
    model.addAttribute("category", dto);
    addCommonFormAttributes(model, true);
    
    return "admin/categories/form";
}

@PostMapping("/edit/{id}")
public String updateCategory(@PathVariable String id,
                            @Valid @ModelAttribute("category") CategoryUpdateRequest request,
                            BindingResult result,
                            @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
                            RedirectAttributes redirectAttributes,
                            Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, true);
        return "admin/categories/form";
    }
    
    Category category = categoryService.getCategoryById(id).orElse(null);
    if (category == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
        return REDIRECT_CATEGORIES;
    }
    
    // Check duplicate name (exclude current)
    if (isCategoryNameDuplicate(request.getCategoryName(), id)) {
        result.rejectValue("categoryName", "error.category", "Tên danh mục đã tồn tại");
        addCommonFormAttributes(model, true);
        return "admin/categories/form";
    }
    
    try {
        // Upload new icon if provided
        if (iconFile != null && !iconFile.isEmpty()) {
            String iconUrl = fileStorageService.saveIcon(iconFile);
            category.setIconUrl(iconUrl);
        }
        
        // Update fields
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());
        
        categoryService.saveCategory(category);
        
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        return REDIRECT_CATEGORIES;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_CATEGORIES;
    }
}
```

**SQL Query**:
```sql
UPDATE categories 
SET category_name = ?, description = ?, icon_url = ?, 
    display_order = ?, is_active = ?
WHERE category_id = ?;
```

---

## Flow 12.5: Delete Category

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  POST /admin/categories/delete/{id}                                              │
  │─────────────────────────►│                                                       │
  │       │                  │ checkBooksInCategory(id)                              │
  │       │                  ├─────────────────────►│                                │
  │       │                  │◄─────────────────────┤                                │
  │       │                  │ deleteCategory(id)                                    │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ deleteById(id)                 │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │                   │ DELETE     │
  │       │                  │                      │                   ├───────────►│
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ redirect:/admin/categories                           │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deleteCategory(@PathVariable String id, 
                            RedirectAttributes redirectAttributes) {
    try {
        Category category = categoryService.getCategoryById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
            return REDIRECT_CATEGORIES;
        }
        
        // Check if category has books
        List<Book> booksInCategory = bookService.getBooksByCategory(id);
        if (!booksInCategory.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa danh mục có " + booksInCategory.size() + " sách!");
            return REDIRECT_CATEGORIES;
        }
        
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
    }
    
    return REDIRECT_CATEGORIES;
}
```

**SQL Query**:
```sql
-- Check books in category
SELECT COUNT(*) FROM books b
INNER JOIN book_category bc ON b.category_id = bc.category_id
WHERE bc.category_id = ?;

-- Delete category
DELETE FROM categories WHERE category_id = ?;
```

---

## Flow 12.6: Check Duplicate Category Name

### Sequence Diagram
```
Admin → Browser → AdminCategoryController → CategoryService → CategoryRepository → Database
  │       │                │                      │                   │              │
  │  GET /api/admin/categories/check-name?name={name}&excludeId={id}                │
  │─────────────────────────►│                                                       │
  │       │                  │ getCategoryByName(name)                               │
  │       │                  ├─────────────────────►│                                │
  │       │                  │                      │ findByCategoryName()           │
  │       │                  │                      ├──────────────────►│            │
  │       │                  │                      │◄──────────────────┤            │
  │       │                  │◄─────────────────────┤                                │
  │◄─────────────────────────┤ JSON: {"exists": true/false}                         │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/api/admin/categories/check-name")
@ResponseBody
public ResponseEntity<Map<String, Object>> checkCategoryName(
        @RequestParam String name,
        @RequestParam(required = false) String excludeId) {
    
    boolean exists = isCategoryNameDuplicate(name, excludeId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("exists", exists);
    response.put("message", exists ? "Tên danh mục đã tồn tại" : "Tên danh mục hợp lệ");
    
    return ResponseEntity.ok(response);
}
```

**SQL Query**:
```sql
SELECT * FROM categories WHERE category_name = ?;
```

**Response**:
```json
{
  "exists": false,
  "message": "Tên danh mục hợp lệ"
}
```

---

## Error Handling

### Common Errors

**1. Category Not Found (404)**
```json
{
  "error": "NOT_FOUND",
  "message": "Không tìm thấy danh mục với ID: category_01"
}
```

**2. Duplicate Category Name (409)**
```json
{
  "error": "DUPLICATE_NAME",
  "message": "Tên danh mục 'Tâm Lý - Kỹ Năng Sống' đã tồn tại"
}
```

**3. Category Has Books (409)**
```json
{
  "error": "CATEGORY_HAS_BOOKS",
  "message": "Không thể xóa danh mục có 15 sách",
  "bookCount": 15
}
```

**4. Validation Error (400)**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "fieldErrors": {
    "categoryName": "Tên danh mục không được để trống",
    "displayOrder": "Thứ tự hiển thị phải >= 0"
  }
}
```

### Error Handling in Controller
```java
@ExceptionHandler(RuntimeException.class)
public String handleException(RuntimeException ex, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    return REDIRECT_CATEGORIES;
}
```

---

## Best Practices

### 1. Category ID Generation
- Format: `category_01`, `category_02`, ...
- Auto-increment based on existing categories
- Padded with zeros for sorting

### 2. Display Order
- Use integers (0, 1, 2, ...) for ordering
- Allow gaps for future insertions
- Sort by display_order ASC, then by name

### 3. Category Deletion
- Always check for dependent books
- Provide clear error messages
- Consider soft delete for data integrity

### 4. Icon Management
- Store in `/book_asset/image/icons/`
- Support common formats: PNG, JPG, SVG
- Validate file size (max 2MB)
- Use CDN for production

### 5. Form Validation
- Client-side validation with JavaScript
- Server-side validation with Bean Validation
- AJAX validation for duplicate names
- Clear error messages in Vietnamese

---

## Security Considerations

### 1. Authorization
```java
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController extends BaseAdminController {
    // ...
}
```

### 2. CSRF Protection
- All POST/PUT/DELETE requests require CSRF token
- Thymeleaf automatically includes token in forms

### 3. Input Sanitization
- Sanitize HTML in description field
- Prevent XSS attacks
- Validate file uploads

### 4. SQL Injection Prevention
- Use JPA/Hibernate prepared statements
- Never concatenate SQL strings
- Validate input parameters

---

## Testing Checklist

### Unit Tests
- ✅ Test category ID generation
- ✅ Test duplicate name detection
- ✅ Test CRUD operations
- ✅ Test validation rules

### Integration Tests
- ✅ Test create category flow
- ✅ Test update category flow
- ✅ Test delete with books validation
- ✅ Test duplicate name API

### UI Tests
- ✅ Test form validation
- ✅ Test icon upload
- ✅ Test list sorting by display order
- ✅ Test delete confirmation modal

---

## Performance Optimization

### 1. Database Indexing
```sql
CREATE INDEX idx_category_name ON categories(category_name);
CREATE INDEX idx_category_display_order ON categories(display_order);
CREATE INDEX idx_category_active ON categories(is_active);
```

### 2. Caching
```java
@Cacheable(value = "categories", key = "'all'")
public List<Category> getAllCategories() {
    return categoryRepository.findAll();
}

@CacheEvict(value = "categories", allEntries = true)
public void saveCategory(Category category) {
    categoryRepository.save(category);
}
```

### 3. Pagination
- Implement pagination for large category lists
- Use `Page<Category>` with Spring Data JPA
- Default page size: 20 items

---

## Related Flows
- 📚 **FLOW 02**: Admin Book Management - Books belong to categories
- 🏠 **FLOW 17**: Home Page & Public Book Browse - Category filtering
- 📊 **FLOW 09**: Admin Dashboard - Category statistics

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

