# 🎨 FLOW 14: ADMIN BANNER MANAGEMENT (Quản Lý Banner/Quảng Cáo)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 14.1: List Banners](#flow-141-list-banners)
3. [Flow 14.2: Create Banner](#flow-142-create-banner)
4. [Flow 14.3: View Banner Details](#flow-143-view-banner-details)
5. [Flow 14.4: Edit Banner](#flow-144-edit-banner)
6. [Flow 14.5: Delete Banner](#flow-145-delete-banner)
7. [Flow 14.6: Toggle Banner Active Status](#flow-146-toggle-banner-active-status)
8. [Banner Positions](#banner-positions)
9. [Error Handling](#error-handling)

---

## Tổng Quan

### Components
- **Controller**: `AdminBannerController.java`
- **Service**: `BannerService.java`, `FileStorageService.java`
- **Repository**: `BannerRepository.java`
- **Entity**: `Banner.java`
- **DTOs**: `BannerCreateRequest.java`, `BannerUpdateRequest.java`

### URLs
- `GET /admin/banners` - Danh sách banner
- `GET /admin/banners/create` - Form tạo banner
- `POST /admin/banners/create` - Xử lý tạo banner
- `GET /admin/banners/view/{id}` - Chi tiết banner
- `GET /admin/banners/edit/{id}` - Form sửa banner
- `POST /admin/banners/edit/{id}` - Xử lý sửa banner
- `POST /admin/banners/delete/{id}` - Xóa banner
- `POST /api/admin/banners/{id}/toggle-status` - Bật/tắt banner

### Banner Structure
```
Banner:
  - bannerId: String (auto: "banner_01", "banner_02", ...)
  - title: String
  - imageUrl: String (required)
  - targetUrl: String (link when clicked)
  - position: Enum (HOME, CATEGORY, DETAIL, CHECKOUT)
  - isActive: Boolean
  - createdBy: User
  - createdAt: LocalDateTime
```

### Banner Positions
```java
public enum BannerPosition {
    HOME,       // Trang chủ
    CATEGORY,   // Trang danh mục
    DETAIL,     // Trang chi tiết sách
    CHECKOUT    // Trang thanh toán
}
```

---

## Flow 14.1: List Banners

### Sequence Diagram
```
Admin → Browser → AdminBannerController → BannerService → BannerRepository → Database
  │       │               │                    │                │              │
  │  GET /admin/banners                                                        │
  │────────────────────────►│                                                  │
  │       │                 │ getBannersByUser()                               │
  │       │                 ├────────────────────►│                            │
  │       │                 │                     │ findByCreatedBy()          │
  │       │                 │                     ├───────────────►│           │
  │       │                 │                     │                │ SELECT    │
  │       │                 │                     │                ├──────────►│
  │       │                 │                     │                │◄──────────┤
  │       │                 │                     │◄───────────────┤           │
  │       │                 │◄────────────────────┤                            │
  │◄────────────────────────┤ (return admin/banners/list.html)                │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String bannersList(Authentication authentication, Model model) {
    User currentUser = getCurrentUser(authentication);
    List<Banner> banners = bannerService.getBannersByUserSortedByDate(currentUser);
    
    model.addAttribute("banners", banners);
    model.addAttribute("totalBanners", banners.size());
    
    // Statistics
    long activeBanners = banners.stream().filter(Banner::getIsActive).count();
    long inactiveBanners = banners.size() - activeBanners;
    
    model.addAttribute("activeBanners", activeBanners);
    model.addAttribute("inactiveBanners", inactiveBanners);
    
    return "admin/banners/list";
}
```

**Service**:
```java
@Override
public List<Banner> getBannersByUserSortedByDate(User user) {
    return bannerRepository.findByCreatedByOrderByCreatedAtDesc(user);
}

@Override
public List<Banner> getActiveBannersByPosition(Banner.BannerPosition position) {
    return bannerRepository.findByPositionAndIsActiveTrueOrderByCreatedAtDesc(position);
}
```

**SQL Query**:
```sql
-- Get all banners for current user
SELECT b.*, u.full_name as creator_name
FROM banners b
INNER JOIN users u ON b.created_by = u.user_id
WHERE b.created_by = ?
ORDER BY b.created_at DESC;

-- Get active banners by position (for public display)
SELECT * FROM banners
WHERE position = ? AND is_active = true
ORDER BY created_at DESC;
```

**Response Data**:
```json
{
  "banners": [
    {
      "bannerId": "banner_01",
      "title": "Khuyến mãi mùa hè",
      "imageUrl": "/book_asset/image/banners/summer_sale.jpg",
      "targetUrl": "/user/books?category=promotion",
      "position": "HOME",
      "isActive": true,
      "createdAt": "2025-12-01T10:00:00",
      "createdBy": {
        "userId": "admin",
        "fullName": "Admin User"
      }
    }
  ]
}
```

---

## Flow 14.2: Create Banner

### Sequence Diagram
```
Admin → Browser → AdminBannerController → BannerService → FileStorageService → Database
  │       │               │                    │                 │                │
  │  GET /admin/banners/create                                                   │
  │────────────────────────►│                                                    │
  │◄────────────────────────┤ (return form with empty BannerCreateRequest)      │
  │       │                 │                                                    │
  │  POST /admin/banners/create (BannerCreateRequest + imageFile)               │
  │────────────────────────►│                                                    │
  │       │                 │ Validate input                                     │
  │       │                 │ generateNextBannerId()                             │
  │       │                 │ saveBannerImage()                                  │
  │       │                 ├─────────────────────────────────────►│             │
  │       │                 │◄─────────────────────────────────────┤             │
  │       │                 │ saveBanner()                                       │
  │       │                 ├────────────────────►│                              │
  │       │                 │                     │ save()                       │
  │       │                 │                     ├────────────────────────────►│ │
  │       │                 │◄────────────────────┤                              │
  │◄────────────────────────┤ redirect:/admin/banners                           │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("banner", new BannerCreateRequest());
    addCommonFormAttributes(model, false);
    return "admin/banners/form";
}

@PostMapping("/create")
public String createBanner(@Valid @ModelAttribute("banner") BannerCreateRequest request,
                          BindingResult result,
                          @RequestParam(value = "imageFile", required = true) MultipartFile imageFile,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    // Validation
    if (result.hasErrors()) {
        addCommonFormAttributes(model, false);
        return "admin/banners/form";
    }
    
    // Validate image is required
    if (imageFile == null || imageFile.isEmpty()) {
        result.rejectValue("imageUrl", "error.banner", "Ảnh banner là bắt buộc");
        addCommonFormAttributes(model, false);
        return "admin/banners/form";
    }
    
    try {
        User currentUser = getCurrentUser(authentication);
        String bannerId = generateNextBannerId();
        
        // Upload banner image
        String imageUrl = fileStorageService.saveBannerImage(imageFile);
        
        // Create banner
        Banner banner = new Banner();
        banner.setBannerId(bannerId);
        banner.setTitle(request.getTitle());
        banner.setImageUrl(imageUrl);
        banner.setTargetUrl(request.getTargetUrl());
        banner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
        banner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        banner.setCreatedBy(currentUser);
        banner.setCreatedAt(LocalDateTime.now());
        
        bannerService.saveBanner(banner);
        
        redirectAttributes.addFlashAttribute("successMessage", "Tạo banner thành công!");
        return REDIRECT_BANNERS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_BANNERS;
    }
}
```

**File Storage**:
```java
public String saveBannerImage(MultipartFile file) throws IOException {
    // Validate file
    validateImageFile(file);
    
    // Generate filename
    String extension = getFileExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID().toString() + extension;
    
    // Save to disk
    Path uploadPath = Paths.get(UPLOAD_DIR, "book_asset", "image", "banners");
    Files.createDirectories(uploadPath);
    
    Path filePath = uploadPath.resolve(filename);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    
    return "/book_asset/image/banners/" + filename;
}
```

**SQL Query**:
```sql
INSERT INTO banners (banner_id, title, image_url, target_url, position, 
                     is_active, created_by, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, NOW());
```

**Request DTO**:
```java
public class BannerCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
    private String title;
    
    @NotBlank(message = "Vị trí hiển thị không được để trống")
    private String position; // HOME, CATEGORY, DETAIL, CHECKOUT
    
    @Size(max = 500, message = "URL đích không được quá 500 ký tự")
    private String targetUrl;
    
    private Boolean isActive;
}
```

---

## Flow 14.3: View Banner Details

### Implementation Details

**Controller**:
```java
@GetMapping("/view/{id}")
public String viewBanner(@PathVariable String id, Model model, 
                        RedirectAttributes redirectAttributes) {
    Banner banner = bannerService.getBannerById(id).orElse(null);
    
    if (banner == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy banner!");
        return REDIRECT_BANNERS;
    }
    
    model.addAttribute("banner", banner);
    
    return "admin/banners/view";
}
```

---

## Flow 14.4: Edit Banner

### Implementation Details

**Controller**:
```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id, Model model, 
                          RedirectAttributes redirectAttributes) {
    Banner banner = bannerService.getBannerById(id).orElse(null);
    
    if (banner == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy banner!");
        return REDIRECT_BANNERS;
    }
    
    BannerUpdateRequest dto = mapToUpdateRequest(banner);
    model.addAttribute("banner", dto);
    addCommonFormAttributes(model, true);
    
    return "admin/banners/form";
}

@PostMapping("/edit/{id}")
public String updateBanner(@PathVariable String id,
                          @Valid @ModelAttribute("banner") BannerUpdateRequest request,
                          BindingResult result,
                          @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, true);
        return "admin/banners/form";
    }
    
    Banner banner = bannerService.getBannerById(id).orElse(null);
    if (banner == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy banner!");
        return REDIRECT_BANNERS;
    }
    
    try {
        // Upload new image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image
            if (banner.getImageUrl() != null) {
                fileStorageService.deleteFile(banner.getImageUrl());
            }
            String imageUrl = fileStorageService.saveBannerImage(imageFile);
            banner.setImageUrl(imageUrl);
        }
        
        // Update fields
        banner.setTitle(request.getTitle());
        banner.setTargetUrl(request.getTargetUrl());
        banner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
        banner.setIsActive(request.getIsActive());
        
        bannerService.saveBanner(banner);
        
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật banner thành công!");
        return REDIRECT_BANNERS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_BANNERS;
    }
}
```

**SQL Query**:
```sql
UPDATE banners 
SET title = ?, image_url = ?, target_url = ?, position = ?, is_active = ?
WHERE banner_id = ?;
```

---

## Flow 14.5: Delete Banner

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deleteBanner(@PathVariable String id, 
                          RedirectAttributes redirectAttributes) {
    try {
        Banner banner = bannerService.getBannerById(id).orElse(null);
        if (banner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy banner!");
            return REDIRECT_BANNERS;
        }
        
        // Delete banner image
        if (banner.getImageUrl() != null) {
            fileStorageService.deleteFile(banner.getImageUrl());
        }
        
        bannerService.deleteBanner(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa banner thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
    }
    
    return REDIRECT_BANNERS;
}
```

**SQL Query**:
```sql
DELETE FROM banners WHERE banner_id = ?;
```

---

## Flow 14.6: Toggle Banner Active Status

### Sequence Diagram
```
Admin → Browser → AdminBannerController → BannerService → Database
  │       │               │                    │              │
  │  POST /api/admin/banners/{id}/toggle-status               │
  │────────────────────────►│                                 │
  │       │                 │ getBannerById(id)               │
  │       │                 ├────────────────────►│           │
  │       │                 │◄────────────────────┤           │
  │       │                 │ toggleStatus()                  │
  │       │                 │ saveBanner()                    │
  │       │                 ├────────────────────►│           │
  │       │                 │                     │ UPDATE    │
  │       │                 │                     ├──────────►│
  │       │                 │◄────────────────────┤           │
  │◄────────────────────────┤ JSON: {"success": true}        │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/{id}/toggle-status")
@ResponseBody
public ResponseEntity<Map<String, Object>> toggleBannerStatus(@PathVariable String id) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        Banner banner = bannerService.getBannerById(id).orElse(null);
        if (banner == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy banner!");
            return ResponseEntity.notFound().build();
        }
        
        // Toggle status
        banner.setIsActive(!banner.getIsActive());
        bannerService.saveBanner(banner);
        
        response.put("success", true);
        response.put("isActive", banner.getIsActive());
        response.put("message", banner.getIsActive() ? "Đã kích hoạt banner" : "Đã tắt banner");
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Lỗi: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

**SQL Query**:
```sql
UPDATE banners 
SET is_active = NOT is_active
WHERE banner_id = ?;
```

---

## Banner Positions

### Position Usage

**1. HOME (Trang chủ)**
- Full-width banner at top
- Recommended size: 1920x400px
- Best for promotions, featured books

**2. CATEGORY (Trang danh mục)**
- Banner above book list
- Recommended size: 1200x300px
- Category-specific promotions

**3. DETAIL (Trang chi tiết sách)**
- Sidebar banner
- Recommended size: 300x600px
- Related books, cross-sell

**4. CHECKOUT (Trang thanh toán)**
- Small banner near cart
- Recommended size: 300x250px
- Last-minute deals

### Display Logic

**Public Display Service**:
```java
public List<Banner> getActiveBannersForPosition(Banner.BannerPosition position) {
    return bannerRepository.findByPositionAndIsActiveTrueOrderByCreatedAtDesc(position);
}
```

**Thymeleaf Template**:
```html
<!-- Display HOME banners -->
<div th:if="${homeBanners != null and !homeBanners.isEmpty()}" class="banner-container">
    <div th:each="banner : ${homeBanners}" class="banner-item">
        <a th:href="${banner.targetUrl}">
            <img th:src="${banner.imageUrl}" th:alt="${banner.title}" />
        </a>
    </div>
</div>
```

---

## Error Handling

### Common Errors

**1. Banner Not Found (404)**
```json
{
  "error": "NOT_FOUND",
  "message": "Không tìm thấy banner với ID: banner_01"
}
```

**2. Image Required (400)**
```json
{
  "error": "IMAGE_REQUIRED",
  "message": "Ảnh banner là bắt buộc"
}
```

**3. Invalid Position (400)**
```json
{
  "error": "INVALID_POSITION",
  "message": "Vị trí banner không hợp lệ: INVALID"
}
```

---

## Best Practices

### 1. Image Guidelines
- **Format**: JPG, PNG, WebP
- **Max size**: 5MB
- **Recommended dimensions**:
  - HOME: 1920x400px
  - CATEGORY: 1200x300px
  - DETAIL: 300x600px
  - CHECKOUT: 300x250px

### 2. Performance
- Optimize images before upload
- Use lazy loading for banners
- Cache active banners
- Limit number of active banners per position (3-5)

### 3. Target URLs
- Use relative URLs for internal links
- Validate URLs before save
- Track banner click-through rates
- Support deep links to specific products

### 4. Banner Rotation
- Show most recent banners first
- Implement view/click tracking
- A/B testing for effectiveness
- Schedule banners (future enhancement)

---

## Security Considerations

### 1. Authorization
```java
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController extends BaseAdminController {
    // ...
}
```

### 2. File Upload Security
- Validate image file types
- Check file size limits
- Sanitize filenames
- Store in secure location

### 3. URL Validation
- Prevent malicious URLs
- Check for XSS in target URLs
- Whitelist allowed domains (optional)

---

## Related Flows
- 🏠 **FLOW 17**: Home Page - Display HOME banners
- 📚 **FLOW 02**: Admin Book Management - Link banners to books
- 📊 **FLOW 09**: Admin Dashboard - Banner statistics

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

