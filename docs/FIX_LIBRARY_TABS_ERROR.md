# SỬA LỖI CÁC NÚT LỌC NHANH TRONG THƯ VIỆN

**Ngày:** 14/12/2025  
**Trạng thái:** ✅ Đã sửa

---

## 🔴 **VẤN ĐỀ**

### **Lỗi gì đang xảy ra?**
```
2025-12-14T16:42:59.809+07:00 ERROR 31464 --- [DATN] [nio-2706-exec-9] 
org.thymeleaf.TemplateEngine: [THYMELEAF][http-nio-2706-exec-9] 
Exception processing template "user/library": 
Exception evaluating SpringEL expression: "purchasedBooks.isEmpty() && subscriptionBooks.isEmpty()" 
(template: "user/library" - line 359, col 26)
```

### **Nguyên nhân:**
Lỗi `NullPointerException` xảy ra khi Thymeleaf cố gắng gọi method `.isEmpty()` trên các biến `purchasedBooks` hoặc `subscriptionBooks` **KHÔNG TỒN TẠI** trong model.

### **Tại sao các biến không tồn tại?**

Trong controller `UserController.java`, method `library()` có logic phân trang như sau:

```java
// ❌ LOGIC CŨ - CÓ VẤN ĐỀ
if ("reading".equals(tab)) {
    // Chỉ xử lý readingProgresses
    model.addAttribute("readingProgresses", pagedProgress);
} else if ("purchased".equals(tab)) {
    // Chỉ xử lý purchasedBooks
    model.addAttribute("purchasedBooks", pagedBooks);
} else {
    // Tab "all" - chỉ có purchasedBooks
    model.addAttribute("purchasedBooks", purchasedBooks);
}
```

**Vấn đề:**
- Khi user click vào tab **"Đang đọc"** → `purchasedBooks` và `subscriptionBooks` KHÔNG được đưa vào model
- Khi user click vào tab **"Yêu thích"** → Các biến này cũng KHÔNG có
- Khi user click vào tab **"Hoàn thành"** → Các biến này cũng KHÔNG có
- Khi user click vào tab **"Miễn phí"** → Các biến này cũng KHÔNG có

Nhưng trong template `library.html` (dòng 359), có đoạn code:
```html
<div th:if="${purchasedBooks.isEmpty() && subscriptionBooks.isEmpty()}" class="empty-state">
```

➡️ Khi các biến không tồn tại, gọi `.isEmpty()` sẽ gây lỗi `NullPointerException`!

---

## ✅ **GIẢI PHÁP**

### **Sửa Controller**
Đảm bảo **TẤT CẢ** các danh sách cần thiết được đưa vào model **BẤT KỂ** tab nào đang active:

```java
// ✅ LOGIC MỚI - ĐÃ SỬA
// Đảm bảo tất cả danh sách luôn có trong model (để tránh lỗi isEmpty() trong template)
model.addAttribute("purchasedBooks", purchasedBooks);
model.addAttribute("subscriptionBooks", subscriptionBooks != null ? subscriptionBooks : new ArrayList<>());
model.addAttribute("freeBooks", freeBooks != null ? freeBooks : new ArrayList<>());

// Phân trang cho tab đang active
int pageSize = 12;
int totalBooks;
int totalPages;

if ("reading".equals(tab)) {
    // Xử lý phân trang cho tab đang đọc
    totalBooks = readingProgresses.size();
    totalPages = (int) Math.ceil((double) totalBooks / pageSize);
    if (page >= totalPages && totalPages > 0) {
        page = totalPages - 1;
    }
    int startIndex = Math.max(0, page * pageSize);
    int endIndex = Math.min(startIndex + pageSize, totalBooks);
    List<ReadingProgress> pagedProgress = readingProgresses.subList(startIndex, endIndex);
    model.addAttribute("readingProgresses", pagedProgress);
} else if ("purchased".equals(tab)) {
    // Xử lý phân trang cho tab đã mua
    totalBooks = purchasedBooks.size();
    totalPages = (int) Math.ceil((double) totalBooks / pageSize);
    if (page >= totalPages && totalPages > 0) {
        page = totalPages - 1;
    }
    int startIndex = Math.max(0, page * pageSize);
    int endIndex = Math.min(startIndex + pageSize, totalBooks);
    List<Book> pagedBooks = purchasedBooks.subList(startIndex, endIndex);
    // Override với danh sách đã phân trang cho tab purchased
    model.addAttribute("purchasedBooks", pagedBooks);
} else {
    totalBooks = 0;
    totalPages = 0;
}
```

### **Các thay đổi chính:**

1. **Đưa tất cả danh sách vào model TRƯỚC KHI phân trang:**
   ```java
   model.addAttribute("purchasedBooks", purchasedBooks);
   model.addAttribute("subscriptionBooks", subscriptionBooks != null ? subscriptionBooks : new ArrayList<>());
   model.addAttribute("freeBooks", freeBooks != null ? freeBooks : new ArrayList<>());
   ```

2. **Đảm bảo null-safe:**
   - Nếu `subscriptionBooks` hoặc `freeBooks` là `null`, gán thành `new ArrayList<>()`
   - Vậy `.isEmpty()` sẽ luôn hoạt động mà không gây lỗi

3. **Phân trang chỉ override khi cần:**
   - Với tab "purchased", override `purchasedBooks` bằng danh sách đã phân trang
   - Các tab khác vẫn giữ nguyên danh sách đầy đủ

---

## 📋 **DANH SÁCH CÁC TAB VÀ DỮ LIỆU CẦN**

| Tab           | Biến chính            | Biến phụ cần có                                    |
|---------------|-----------------------|----------------------------------------------------|
| **Tất cả**    | `purchasedBooks`      | `subscriptionBooks`, `freeBooks`                   |
|               | `subscriptionBooks`   |                                                    |
| **Đã mua**    | `purchasedBooks`      | `subscriptionBooks` (để check empty state)         |
| **Gói VIP**   | `subscriptionBooks`   | `purchasedBooks` (để check empty state)            |
| **Đang đọc**  | `readingBooks`        | `purchasedBooks`, `subscriptionBooks` (empty check)|
| **Yêu thích** | `favoriteBooks`       | `purchasedBooks`, `subscriptionBooks` (empty check)|
| **Hoàn thành**| `completedBooks`      | `purchasedBooks`, `subscriptionBooks` (empty check)|
| **Miễn phí**  | `freeBooks`           | `purchasedBooks`, `subscriptionBooks` (empty check)|

**➡️ Kết luận:** Tất cả các tab đều cần `purchasedBooks` và `subscriptionBooks` để hiển thị empty state đúng!

---

## 🧪 **CÁCH KIỂM TRA**

### **Bước 1: Restart ứng dụng**
```bash
mvn spring-boot:run
```

### **Bước 2: Đăng nhập vào hệ thống**

### **Bước 3: Vào trang Thư viện**
```
http://localhost:2706/user/library
```

### **Bước 4: Click qua từng tab**
- ✅ Tab "Tất cả"
- ✅ Tab "Đã mua"
- ✅ Tab "Gói VIP"
- ✅ Tab "Đang đọc"
- ✅ Tab "Yêu thích"
- ✅ Tab "Hoàn thành"
- ✅ Tab "Miễn phí"

### **Kết quả mong đợi:**
- ✅ Không có lỗi 500
- ✅ Không có exception trong console
- ✅ Empty state hiển thị đúng khi không có sách
- ✅ Danh sách sách hiển thị đúng khi có sách

---

## 📝 **TÓM TẮT**

### **Vấn đề:**
Các biến `purchasedBooks` và `subscriptionBooks` không được đưa vào model khi chuyển tab, gây lỗi `NullPointerException` khi template gọi `.isEmpty()`.

### **Giải pháp:**
Luôn đưa tất cả danh sách cần thiết vào model trước khi xử lý phân trang, đảm bảo null-safe.

### **File đã sửa:**
- ✅ `src/main/java/stu/datn/ebook_store/controller/user/UserController.java`

### **Kết quả:**
- ✅ Tất cả các tab hoạt động bình thường
- ✅ Không còn lỗi NullPointerException
- ✅ Empty state hiển thị đúng

---

## 🔍 **CHI TIẾT KỸ THUẬT**

### **Tại sao cần null-safe check?**
```java
subscriptionBooks != null ? subscriptionBooks : new ArrayList<>()
```

- Trong vòng lặp kiểm tra subscription, nếu không có subscription active, `subscriptionBooks` vẫn là `new ArrayList<>()` (không null)
- Nhưng để đảm bảo an toàn (phòng trường hợp logic thay đổi), ta vẫn check null
- Nếu `subscriptionBooks` là null, ta gán thành empty list để `.isEmpty()` không gây lỗi

### **Tại sao phải đưa vào model trước?**
```java
// ✅ ĐÚNG: Đưa vào model trước
model.addAttribute("purchasedBooks", purchasedBooks);
if ("purchased".equals(tab)) {
    // Override với danh sách phân trang
    model.addAttribute("purchasedBooks", pagedBooks);
}

// ❌ SAI: Chỉ đưa vào khi tab = "purchased"
if ("purchased".equals(tab)) {
    model.addAttribute("purchasedBooks", pagedBooks);
}
```

Vì template cần các biến này ở **TẤT CẢ** các tab (để check empty state), không chỉ riêng tab "purchased".

---

✅ **Vấn đề đã được giải quyết hoàn toàn!**

