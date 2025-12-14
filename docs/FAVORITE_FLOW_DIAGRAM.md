# FLOW CHỨC NĂNG YÊU THÍCH SÁCH (FAVORITE BOOKS)

## 📋 Tổng quan

Tài liệu này mô tả chi tiết luồng hoạt động của chức năng yêu thích sách trong hệ thống Ebook Store.

---

## 🔄 FLOW 1: THÊM SÁCH VÀO YÊU THÍCH

### Bước 1: User tương tác trên UI
```
User nhìn thấy book card với icon ❤️
  ↓
User click vào icon ❤️
  ↓
JavaScript event listener được kích hoạt
```

**File:** `user-index.js`, `books-common.js`
**Function:** `initActionButtons()`, `initBookActionButtons()`

---

### Bước 2: JavaScript xử lý event
```javascript
// File: user-index.js hoặc books-common.js
btn.addEventListener('click', function(e) {
    e.preventDefault();
    e.stopPropagation();
    
    // Lấy bookId từ URL của book card
    const bookCard = this.closest('a.book-card');
    const bookId = extractBookIdFromUrl(bookCard.href);
    
    if (bookId) {
        toggleFavorite(bookId, icon);
    }
});
```

**Xử lý:**
1. Ngăn default behavior và stop propagation
2. Tìm parent element `a.book-card`
3. Extract bookId từ href (ví dụ: `/books/view/book_01` → `book_01`)
4. Gọi function `toggleFavorite()`

---

### Bước 3: Gửi AJAX request đến server
```javascript
// Function: toggleFavorite(bookId, iconElement)
async function toggleFavorite(bookId, iconElement) {
    console.log('Calling /api/favorites/toggle with bookId:', bookId);
    
    const response = await fetch('/api/favorites/toggle', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ bookId: bookId })
    });
    
    // Kiểm tra response...
}
```

**Request:**
- **Method:** POST
- **URL:** `/api/favorites/toggle`
- **Headers:** `Content-Type: application/json`
- **Body:** `{ "bookId": "book_xxx" }`

---

### Bước 4: Spring Security kiểm tra authorization

```
Request đến Spring Security Filter Chain
  ↓
Kiểm tra path /api/favorites/toggle
  ↓
Đọc cấu hình SecurityConfig:
  - Path /api/favorites/** yêu cầu role USER hoặc ADMIN
  - CSRF disabled cho /api/favorites/**
  ↓
Kiểm tra session có user logged in không?
  ├─ Có → Cho phép tiếp tục
  └─ Không → Trả về 401 Unauthorized
```

**File:** `SecurityConfig.java`
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/favorites/**")
)
.requestMatchers("/api/favorites/**")
    .hasAnyRole("USER", "ADMIN")
```

---

### Bước 5: FavoriteController xử lý request

```
Request đến FavoriteController.toggleFavorite()
  ↓
Lấy user từ session: session.getAttribute("loggedInUser")
  ↓
Kiểm tra currentUser != null?
  ├─ null → Trả về 401 với message "Vui lòng đăng nhập"
  └─ not null → Tiếp tục
  ↓
Lấy bookId từ request body
  ↓
Kiểm tra bookId != null && !empty?
  ├─ invalid → Trả về 400 với message "Thiếu thông tin sách"
  └─ valid → Tiếp tục
  ↓
Gọi readingProgressService.toggleFavorite(currentUser, bookId)
```

**File:** `FavoriteController.java`
```java
@PostMapping("/toggle")
public ResponseEntity<Map<String, Object>> toggleFavorite(
        @RequestBody Map<String, String> request,
        HttpSession session) {
    
    User currentUser = (User) session.getAttribute("loggedInUser");
    if (currentUser == null) {
        // Return 401
    }
    
    String bookId = request.get("bookId");
    boolean isFavorite = readingProgressService.toggleFavorite(currentUser, bookId);
    
    response.put("success", true);
    response.put("isFavorite", isFavorite);
    response.put("message", isFavorite ? 
        "Đã thêm vào sách yêu thích" : 
        "Đã xóa khỏi sách yêu thích");
    
    return ResponseEntity.ok(response);
}
```

---

### Bước 6: ReadingProgressService xử lý logic

```
ReadingProgressServiceImpl.toggleFavorite(user, bookId)
  ↓
Lấy Book entity từ DB: bookRepository.findById(bookId)
  ├─ Không tìm thấy → throw RuntimeException("Book not found")
  └─ Tìm thấy → Tiếp tục
  ↓
Tìm ReadingProgress: readingProgressRepository.findByUserAndBook(user, book)
  ↓
Đã có ReadingProgress?
  ├─ Có → Lấy progress hiện tại
  │         ↓
  │       Toggle isFavorite: !progress.getIsFavorite()
  │         ↓
  │       Update progress
  │
  └─ Không → Tạo mới ReadingProgress
              ↓
            progress.setProgressId(generateProgressId())
            progress.setUser(user)
            progress.setBook(book)
            progress.setIsFavorite(true)
            progress.setProgressPercentage(0.0f)
            progress.setIsCompleted(false)
            progress.setCreatedAt(LocalDateTime.now())
  ↓
Save vào DB: readingProgressRepository.save(progress)
  ↓
Return progress.getIsFavorite()
```

**File:** `ReadingProgressServiceImpl.java`
```java
@Override
@Transactional
public boolean toggleFavorite(User user, String bookId) {
    // Tìm book từ database
    Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
    
    // Tìm hoặc tạo reading progress
    Optional<ReadingProgress> progressOpt = 
        readingProgressRepository.findByUserAndBook(user, book);
    
    ReadingProgress progress;
    
    if (progressOpt.isPresent()) {
        // Nếu đã có, toggle trạng thái
        progress = progressOpt.get();
        progress.setIsFavorite(!progress.getIsFavorite());
    } else {
        // Nếu chưa có, tạo mới với favorite = true
        progress = new ReadingProgress();
        progress.setProgressId(generateProgressId());
        progress.setUser(user);
        progress.setBook(book);
        progress.setIsFavorite(true);
        // ... set các field khác
    }
    
    readingProgressRepository.save(progress);
    return progress.getIsFavorite();
}
```

---

### Bước 7: Database operation

```sql
-- Kiểm tra đã có record chưa
SELECT * FROM reading_progress 
WHERE user_id = ? AND book_id = ?;

-- TH1: Đã có record → UPDATE
UPDATE reading_progress 
SET is_favorite = ?, 
    last_read_at = NOW()
WHERE progress_id = ?;

-- TH2: Chưa có record → INSERT
INSERT INTO reading_progress (
    progress_id, user_id, book_id, 
    is_favorite, progress_percentage, 
    is_completed, created_at, last_read_at
) VALUES (?, ?, ?, 1, 0.0, 0, NOW(), NOW());

-- Unique constraint đảm bảo không trùng lặp:
-- UNIQUE KEY user_book_unique (user_id, book_id)
```

**Bảng:** `reading_progress`
**Trường quan trọng:** 
- `is_favorite` TINYINT(1) DEFAULT 0
- Unique constraint: `(user_id, book_id)`

---

### Bước 8: Trả response về client

```
ReadingProgressService return boolean isFavorite
  ↓
FavoriteController nhận kết quả
  ↓
Tạo JSON response:
{
    "success": true,
    "isFavorite": true/false,
    "message": "Đã thêm vào sách yêu thích" hoặc "Đã xóa khỏi sách yêu thích"
}
  ↓
Return ResponseEntity.ok(response)
  ↓
Spring MVC serialize JSON và gửi về client
  ↓
Response với status 200 OK, Content-Type: application/json
```

---

### Bước 9: JavaScript nhận response và cập nhật UI

```javascript
const data = await response.json();
console.log('Response data:', data);

if (data.success) {
    // Cập nhật icon
    if (data.isFavorite) {
        iconElement.classList.remove('far');
        iconElement.classList.add('fas');
        iconElement.style.color = '#dc3545'; // Màu đỏ
    } else {
        iconElement.classList.remove('fas');
        iconElement.classList.add('far');
        iconElement.style.color = ''; // Màu mặc định
    }
    
    // Hiển thị toast notification
    showToast(data.message, 'success');
}
```

**UI Changes:**
- Icon ❤️ đổi từ `far fa-heart` (outline) → `fas fa-heart` (solid) với màu đỏ
- Toast notification xuất hiện góc phải màn hình
- Animation heartbeat effect

---

### Bước 10: Animation và UX

```css
/* CSS Animation được apply */
.fa-heart.fas {
    animation: heartBeat 0.3s ease-in-out;
}

@keyframes heartBeat {
    0%, 100% { transform: scale(1); }
    25% { transform: scale(1.3); }
    50% { transform: scale(1.1); }
    75% { transform: scale(1.2); }
}
```

**Toast Notification:**
```
┌────────────────────────────────┐
│  ✓  Đã thêm vào sách yêu thích  │
└────────────────────────────────┘
  ↓ (slide in từ bên phải)
  ↓ (hiển thị 3 giây)
  ↓ (slide out về bên phải)
```

---

## 🔄 FLOW 2: XÓA SÁCH KHỎI YÊU THÍCH

Hoàn toàn giống với FLOW 1, chỉ khác:

### Điểm khác biệt:

**Bước 6:** Service toggle `isFavorite` từ `true` → `false`

```java
if (progressOpt.isPresent()) {
    progress = progressOpt.get();
    progress.setIsFavorite(!progress.getIsFavorite()); // true → false
}
```

**Bước 7:** Database UPDATE
```sql
UPDATE reading_progress 
SET is_favorite = 0, 
    last_read_at = NOW()
WHERE progress_id = ?;
```

**Bước 9:** UI update
```javascript
if (!data.isFavorite) {
    iconElement.classList.remove('fas'); // Xóa solid
    iconElement.classList.add('far');     // Thêm outline
    iconElement.style.color = '';          // Xóa màu đỏ
}
showToast("Đã xóa khỏi sách yêu thích", 'success');
```

---

## 🔄 FLOW 3: LOAD TRẠNG THÁI FAVORITE KHI VÀO TRANG

### Khi user vào trang Index/Books:

```
Page load hoàn tất
  ↓
DOMContentLoaded event trigger
  ↓
Call loadFavoriteStatuses()
  ↓
Lấy tất cả book cards: document.querySelectorAll('a.book-card')
  ↓
Với mỗi book card:
  ├─ Extract bookId từ href
  ├─ Gọi API: GET /api/favorites/check/{bookId}
  ├─ Nhận response: { "isFavorite": true/false }
  └─ Nếu isFavorite = true:
        → Icon đổi sang fas fa-heart màu đỏ
```

**File:** `books-common.js`
```javascript
async function loadFavoriteStatuses() {
    const bookCards = document.querySelectorAll('a.book-card');
    
    for (const card of bookCards) {
        const bookId = extractBookIdFromUrl(card.href);
        if (bookId) {
            try {
                const response = await fetch(`/api/favorites/check/${bookId}`);
                const data = await response.json();
                
                if (data.isFavorite) {
                    const heartIcon = card.querySelector('.fa-heart');
                    if (heartIcon) {
                        heartIcon.classList.remove('far');
                        heartIcon.classList.add('fas');
                        heartIcon.style.color = '#dc3545';
                    }
                }
            } catch (error) {
                console.error('Error loading favorite status:', error);
            }
        }
    }
}
```

### API Check Favorite:

```
GET /api/favorites/check/{bookId}
  ↓
FavoriteController.checkFavorite()
  ↓
Lấy user từ session
  ├─ null → Return { "isFavorite": false }
  └─ not null → Tiếp tục
  ↓
Lấy tất cả reading progress của user
  ↓
Stream filter: 
  - rp.getBook().getBookId().equals(bookId)
  - Boolean.TRUE.equals(rp.getIsFavorite())
  ↓
anyMatch() → Return true/false
  ↓
Return { "isFavorite": true/false }
```

---

## 🔄 FLOW 4: XEM SÁCH YÊU THÍCH TRONG LIBRARY

### User vào Library:

```
User click menu "Thư viện" hoặc vào /user/library
  ↓
Browser gửi GET /user/library?tab=favorites
  ↓
UserController.library() method được gọi
  ↓
Lấy currentUser từ Authentication
  ↓
Gọi readingProgressService.getFavoriteBooksByUser(currentUser)
  ↓
Service query DB:
  SELECT * FROM reading_progress 
  WHERE user_id = ? AND is_favorite = 1
  ↓
Return List<ReadingProgress> favoriteBooks
  ↓
Controller add vào model:
  - model.addAttribute("favoriteBooks", favoriteBooks)
  - model.addAttribute("totalFavorites", favoriteBooks.size())
  ↓
Thymeleaf render library.html
  ↓
Tab "Yêu thích" hiển thị danh sách sách
```

**File:** `UserController.java`
```java
@GetMapping("/library")
public String library(...) {
    User currentUser = getCurrentUser(authentication);
    
    // Lấy danh sách yêu thích
    List<ReadingProgress> favoriteBooks = 
        readingProgressService.getFavoriteBooksByUser(currentUser);
    
    model.addAttribute("favoriteBooks", favoriteBooks);
    model.addAttribute("totalFavorites", favoriteBooks.size());
    
    return "user/library";
}
```

**File:** `library.html`
```html
<!-- Tab Yêu thích -->
<div class="tab-pane" id="favorites">
    <div th:each="progress : ${favoriteBooks}">
        <a th:href="@{/books/view/{id}(id=${progress.book.bookId})}">
            <!-- Hiển thị book card -->
            <div class="book-actions">
                <button class="action-btn favorite active">
                    <i class="fas fa-heart"></i>
                </button>
            </div>
        </a>
    </div>
</div>
```

---

## 🔄 FLOW 5: XÓA FAVORITE TỪ LIBRARY

```
User ở tab "Yêu thích" trong Library
  ↓
User click icon ❤️ đỏ trên một cuốn sách
  ↓
JavaScript: library.js → initFavoriteButtons()
  ↓
Extract bookId từ book card
  ↓
Call toggleFavorite(bookId, button)
  ↓
[GIỐNG FLOW 2: Gọi API toggle, DB update is_favorite = 0]
  ↓
Nhận response: { "success": true, "isFavorite": false }
  ↓
JavaScript kiểm tra: đang ở tab favorites?
  ├─ Có → setTimeout(() => window.location.reload(), 500)
  └─ Không → Chỉ update icon
  ↓
Page reload → Sách đã biến mất khỏi tab favorites
```

**File:** `library.js`
```javascript
function toggleFavorite(bookId, button) {
    fetch('/api/favorites/toggle', { ... })
    .then(data => {
        if (data.success) {
            if (!data.isFavorite) {
                // Nếu đang ở tab favorites, reload
                const urlParams = new URLSearchParams(window.location.search);
                if (urlParams.get('tab') === 'favorites') {
                    setTimeout(() => {
                        window.location.reload();
                    }, 500);
                }
            }
        }
    });
}
```

---

## 📊 SEQUENCE DIAGRAM

```
User          Browser         Controller      Service         Repository      Database
 |               |                |              |                |              |
 |--click ❤️---->|                |              |                |              |
 |               |--POST /api---->|              |                |              |
 |               |                |--check----->||              |              |
 |               |                |  session     |                |              |
 |               |                |              |                |              |
 |               |                |--toggle----->|                |              |
 |               |                |              |--findById----->|              |
 |               |                |              |                |--SELECT----->|
 |               |                |              |                |<--Book-------|
 |               |                |              |<--Book---------|              |
 |               |                |              |                |              |
 |               |                |              |--findByUserBook>|              |
 |               |                |              |                |--SELECT----->|
 |               |                |              |                |<-Progress----|
 |               |                |              |<--Progress-----|              |
 |               |                |              |                |              |
 |               |                |              |--toggle flag-->|              |
 |               |                |              |--save--------->|              |
 |               |                |              |                |--UPDATE----->|
 |               |                |              |                |<--OK---------|
 |               |                |              |<--boolean------|              |
 |               |                |<--boolean----|                |              |
 |               |                |              |                |              |
 |               |<--JSON 200-----|              |                |              |
 |<--update UI---|                |              |                |              |
 |               |                |              |                |              |
```

---

## 🎯 ĐIỂM QUAN TRỌNG

### 1. Session Management
```java
// ⚠️ Phải dùng đúng tên attribute
User currentUser = (User) session.getAttribute("loggedInUser");
// KHÔNG PHẢI "currentUser"
```

### 2. Security Configuration
```java
// ⚠️ Phải cấu hình đúng trong SecurityConfig
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/favorites/**"))
.requestMatchers("/api/favorites/**").hasAnyRole("USER", "ADMIN")
```

### 3. Database Constraint
```sql
-- ⚠️ Unique constraint đảm bảo không trùng
UNIQUE KEY user_book_unique (user_id, book_id)
```

### 4. Transaction
```java
// ⚠️ Phải có @Transactional để đảm bảo atomicity
@Override
@Transactional
public boolean toggleFavorite(User user, String bookId) { ... }
```

### 5. Error Handling
```javascript
// ⚠️ Phải kiểm tra response type
const contentType = response.headers.get('content-type');
if (!contentType || !contentType.includes('application/json')) {
    // Handle HTML error page
    if (text.includes('login')) {
        showToast('Vui lòng đăng nhập', 'error');
        window.location.href = '/auth/login';
    }
}
```

---

## 🐛 COMMON ERRORS & SOLUTIONS

### Error 1: 401 Unauthorized dù đã login
**Nguyên nhân:** Session attribute name sai
```java
// SAI
User currentUser = (User) session.getAttribute("currentUser");

// ĐÚNG
User currentUser = (User) session.getAttribute("loggedInUser");
```

### Error 2: SyntaxError: Unexpected token '<'
**Nguyên nhân:** Server trả về HTML thay vì JSON
**Giải pháp:** 
- Kiểm tra SecurityConfig có cho phép path không
- Thêm CSRF ignore cho API endpoint

### Error 3: Cannot resolve symbol 'favoriteBooks'
**Nguyên nhân:** Biến được dùng trước khi khai báo
**Giải pháp:** Khai báo biến trước khi sử dụng
```java
// Khai báo
List<ReadingProgress> favoriteBooks = service.getFavoriteBooks();
// Rồi mới dùng
model.addAttribute("totalFavorites", favoriteBooks.size());
```

### Error 4: Icon không đổi màu
**Nguyên nhân:** Class CSS chưa được toggle đúng
**Giải pháp:**
```javascript
// Remove và add class theo thứ tự
iconElement.classList.remove('far');
iconElement.classList.add('fas');
iconElement.style.color = '#dc3545';
```

---

## 📝 CHECKLIST TESTING

- [ ] Click ❤️ chưa login → Thông báo đăng nhập
- [ ] Click ❤️ đã login → Icon đổi màu đỏ + Toast
- [ ] F5 refresh → Icon vẫn màu đỏ
- [ ] Click ❤️ lần 2 → Icon xám + Toast "Đã xóa"
- [ ] Vào Library → Tab Favorites hiển thị sách
- [ ] Click ❤️ trong Library → Page reload, sách biến mất
- [ ] Stat card "Yêu thích" hiển thị đúng số lượng
- [ ] Hoạt động trên mọi trang: index, books/list, category, search
- [ ] Database: is_favorite = 1 khi favorite, = 0 khi unfavorite
- [ ] Console không có error

---

## 🎉 KẾT LUẬN

Flow hoàn chỉnh từ UI → Backend → Database → UI với:
- ✅ Xử lý session và authentication đúng
- ✅ Security configuration chính xác
- ✅ Database transaction an toàn
- ✅ UI/UX mượt mà với animation
- ✅ Error handling đầy đủ
- ✅ Code clean và maintainable

**Chức năng Favorite hoạt động hoàn hảo! 🚀**

