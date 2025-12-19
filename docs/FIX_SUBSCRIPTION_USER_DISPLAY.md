# Sửa Lỗi Hiển Thị Thông Tin User Trong Trang Subscription

**Ngày:** 14/12/2025  
**Vấn đề:** Các trang Thymeleaf của subscription không hiển thị thông tin người dùng đang đăng nhập

## 🔴 Lỗi Ban Đầu

```
2025-12-14T15:50:09.900+07:00 ERROR 27732 --- [DATN] [nio-2706-exec-2] org.thymeleaf.TemplateEngine
Exception processing template "user/subscription/plans": 
Exception evaluating SpringEL expression: "user.avatarUrl != null" 
(template: "user/subscription/plans" - line 43, col 30)
```

## 📋 Phân Tích Nguyên Nhân

### Bước 1: Xác định vấn đề
- **Template Thymeleaf** cần biến `${user}` để hiển thị:
  - Avatar của người dùng
  - Tên đầy đủ (fullName)
  - Các menu điều hướng cá nhân

### Bước 2: Tìm nguyên nhân gốc
Trong file `SubscriptionController.java`:

**❌ Code LỖI:**
```java
@GetMapping("/plans")
public String showSubscriptionPlans(Authentication authentication, Model model) {
    List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
    model.addAttribute("subscriptions", subscriptions);

    User currentUser = getCurrentUser(authentication);
    if (currentUser != null) {
        // ❌ THIẾU: Không thêm user vào model
        Optional<UserSubscription> activeSubscription = 
            getActiveSubscription(currentUser.getUserId());
        
        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());
    }
    
    return "user/subscription/plans";
}
```

**Vấn đề:**
- Controller lấy `currentUser` từ `Authentication` ✅
- NHƯNG không thêm `currentUser` vào `Model` ❌
- Template không tìm thấy `${user}` → Exception

## ✅ Giải Pháp

### Sửa trong `SubscriptionController.java`

#### 1. Phương thức `showSubscriptionPlans()` (dòng 83-102)

**✅ Code ĐÚNG:**
```java
@GetMapping("/plans")
public String showSubscriptionPlans(Authentication authentication, Model model) {
    List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
    model.addAttribute("subscriptions", subscriptions);

    User currentUser = getCurrentUser(authentication);
    if (currentUser != null) {
        // ✅ THÊM: Đưa user vào model để template có thể truy cập
        model.addAttribute("user", currentUser);
        
        Optional<UserSubscription> activeSubscription = 
            getActiveSubscription(currentUser.getUserId());
        
        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());
    } else {
        model.addAttribute("hasActiveSubscription", false);
    }
    
    return "user/subscription/plans";
}
```

#### 2. Phương thức `mySubscriptions()` (dòng 107-127)

**✅ Code ĐÚNG:**
```java
@GetMapping("/my-subscriptions")
public String mySubscriptions(Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
    User currentUser = getCurrentUser(authentication);
    if (currentUser == null) {
        redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
        return "redirect:/auth/login";
    }

    // ✅ THÊM: Đưa user vào model để template có thể truy cập
    model.addAttribute("user", currentUser);

    List<UserSubscription> subscriptions = 
        getUserSubscriptionHistory(currentUser.getUserId());
    model.addAttribute("subscriptions", subscriptions);

    Optional<UserSubscription> activeSubscription = 
        getActiveSubscription(currentUser.getUserId());
    model.addAttribute("activeSubscription", activeSubscription.orElse(null));
    model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

    return "user/subscription/my-subscriptions";
}
```

## 🎯 Kết Quả

### Trước khi sửa:
- ❌ Lỗi Thymeleaf Exception khi truy cập `/subscription/plans`
- ❌ Lỗi Thymeleaf Exception khi truy cập `/subscription/my-subscriptions`
- ❌ Không hiển thị avatar và tên người dùng

### Sau khi sửa:
- ✅ Trang `/subscription/plans` hoạt động bình thường
- ✅ Trang `/subscription/my-subscriptions` hoạt động bình thường
- ✅ Hiển thị đúng avatar và tên người dùng
- ✅ Menu dropdown user hoạt động đầy đủ

## 📝 Bài Học

### Nguyên tắc quan trọng khi làm việc với Spring MVC + Thymeleaf:

1. **Controller phải cung cấp đầy đủ dữ liệu cho View:**
   ```java
   // Template cần gì thì Controller phải thêm vào Model
   model.addAttribute("user", currentUser);
   model.addAttribute("subscriptions", subscriptions);
   ```

2. **Kiểm tra template để biết cần biến gì:**
   ```html
   <!-- Template yêu cầu biến ${user} -->
   <img th:if="${user.avatarUrl != null}" th:src="@{${user.avatarUrl}}" />
   <span th:text="${user.fullName}">Người dùng</span>
   ```

3. **Các Controller khác đã làm đúng (tham khảo):**
   ```java
   // UserController.java - làm đúng
   @GetMapping("/library")
   public String library(Authentication authentication, Model model) {
       User currentUser = getCurrentUser(authentication);
       model.addAttribute("user", currentUser); // ✅ Có thêm user
       // ...
       return "user/library";
   }
   ```

## 🔧 Build và Test

```bash
# Clean và build lại
.\mvnw.cmd clean package -DskipTests

# Kết quả
[INFO] BUILD SUCCESS
[INFO] Total time: 8.792 s
```

## 📌 File Đã Sửa

- `src/main/java/stu/datn/ebook_store/controller/user/SubscriptionController.java`
  - Phương thức `showSubscriptionPlans()` - thêm dòng 89
  - Phương thức `mySubscriptions()` - thêm dòng 114

## 🎉 Hoàn Thành

Tất cả các trang subscription giờ đây hiển thị đúng thông tin người dùng đang đăng nhập!

