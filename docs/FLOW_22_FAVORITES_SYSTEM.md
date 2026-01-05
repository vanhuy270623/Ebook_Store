# FLOW 22: Favorites System (Hệ Thống Yêu Thích)

**Dự án:** Ebook Store  
**Ngày tạo:** 20/12/2025  
**Người tạo:** Development Team  
**Phiên bản:** 1.0  

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Luồng Xử Lý](#luồng-xử-lý)
3. [Implementation Details](#implementation-details)
4. [Database Schema](#database-schema)
5. [Testing](#testing)

---

## Tổng Quan

### Mục Đích
Cho phép user đánh dấu sách yêu thích để dễ dàng truy cập lại sau.

### Key Features
- ✅ Toggle favorite (add/remove)
- ✅ View all favorites
- ✅ Heart icon indicator
- ✅ AJAX update (no page reload)
- ✅ Favorite count

### Actors
- **User**: Đánh dấu và xem sách yêu thích

### Preconditions
- User đã đăng nhập

---

## Luồng Xử Lý

### Sequence Diagram: Toggle Favorite

```
User          FavoriteController    ReadingProgressService    Database
 │                    │                       │                  │
 │──POST /api/favorites/toggle──>            │                  │
 │  Body: {bookId: "book_001"}               │                  │
 │                    │                       │                  │
 │                    │──getCurrentUser()────>│                  │
 │                    │<─────User────────────│                  │
 │                    │                       │                  │
 │                    │──toggleFavorite()────>│                  │
 │                    │                       │──getProgress()──>│
 │                    │                       │<─progress────────│
 │                    │                       │                  │
 │                    │                       │──IF progress == null──>
 │                    │                       │  CREATE new      │
 │                    │                       │  SET isFavorite=true
 │                    │                       │                  │
 │                    │                       │──ELSE────────────>
 │                    │                       │  TOGGLE isFavorite
 │                    │                       │                  │
 │                    │                       │──save()─────────>│
 │                    │                       │<─success─────────│
 │                    │<───isFavorite=true───│                  │
 │                    │                       │                  │
 │<──{success:true,   │                       │                  │
 │   isFavorite:true}─│                       │                  │
 │                    │                       │                  │
```

### Sequence Diagram: Check Favorite Status

```
User          FavoriteController    ReadingProgressService    Database
 │                    │                       │                  │
 │──GET /api/favorites/check/{bookId}───>    │                  │
 │                    │                       │                  │
 │                    │──getCurrentUser()────>│                  │
 │                    │<─────User────────────│                  │
 │                    │                       │                  │
 │                    │──getProgress(user,bookId)──>            │
 │                    │                       │──SELECT─────────>│
 │                    │                       │<─progress────────│
 │                    │<───progress──────────│                  │
 │                    │                       │                  │
 │                    │──IF progress != null  │                  │
 │                    │    isFavorite = progress.isFavorite      │
 │                    │  ELSE                 │                  │
 │                    │    isFavorite = false │                  │
 │                    │                       │                  │
 │<──{isFavorite:true}│                       │                  │
 │                    │                       │                  │
```

---

## Implementation Details

### Controller: `FavoriteController.java`

**Location:** `src/main/java/stu/datn/ebook_store/controller/user/FavoriteController.java`

**Endpoints:**
```
POST /api/favorites/toggle         : Toggle favorite status (AJAX)
GET  /api/favorites/check/{bookId} : Check if book is favorite (AJAX)
GET  /user/favorites               : View favorites page (HTML - UserDashboardController)
```

**Architecture Note:**
- Favorites được lưu trong bảng `reading_progress` với field `is_favorite`
- Không có bảng `favorites` riêng
- Khi user toggle favorite, system sẽ:
  1. Tìm hoặc tạo mới ReadingProgress record
  2. Toggle field `is_favorite` (true/false)
  3. Lưu vào database

#### 1. Toggle Favorite
```java
@PostMapping("/toggle")
public ResponseEntity<Map<String, Object>> toggleFavorite(
        @RequestBody Map<String, String> request) {

    Map<String, Object> response = new HashMap<>();

    try {
        // Kiểm tra user đã đăng nhập chưa
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thêm sách yêu thích");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Lấy bookId từ request
        String bookId = request.get("bookId");
        if (bookId == null || bookId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Thiếu thông tin sách");
            return ResponseEntity.badRequest().body(response);
        }

        // Toggle favorite
        boolean isFavorite = readingProgressService.toggleFavorite(currentUser, bookId);

        response.put("success", true);
        response.put("isFavorite", isFavorite);
        response.put("message", isFavorite ?
            "Đã thêm vào sách yêu thích" :
            "Đã xóa khỏi sách yêu thích");

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

#### 2. Check Favorite
```java
@GetMapping("/check/{bookId}")
public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable String bookId) {

    Map<String, Object> response = new HashMap<>();

    try {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            response.put("isFavorite", false);
            return ResponseEntity.ok(response);
        }

        // Kiểm tra trong reading progress
        boolean isFavorite = readingProgressService.getReadingProgressByUser(currentUser)
                .stream()
                .anyMatch(rp -> rp.getBook().getBookId().equals(bookId) &&
                               Boolean.TRUE.equals(rp.getIsFavorite()));

        response.put("isFavorite", isFavorite);
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("isFavorite", false);
        response.put("error", e.getMessage());
        return ResponseEntity.ok(response);
    }
}
```

#### 3. View Favorites Page (UserDashboardController)
```java
@GetMapping("/favorites")
public String favorites(Authentication authentication, Model model) {
    
    User currentUser = (User) authentication.getPrincipal();
    model.addAttribute("user", currentUser);
    
    // Layout variables
    model.addAttribute("pageTitle", "Sách yêu thích");
    model.addAttribute("currentPage", "favorites");

    // Lấy danh sách sách yêu thích
    List<ReadingProgress> favoriteProgresses = readingProgressService.getFavoriteBooksByUser(currentUser);

    // Filter null books
    List<ReadingProgress> safeFavorites = favoriteProgresses.stream()
            .filter(rp -> rp.getBook() != null)
            .collect(Collectors.toList());

    model.addAttribute("favoriteBooks", safeFavorites);
    
    return "user/favorites";
}
```

---

### Service: `ReadingProgressService.java`

**Location:** `src/main/java/stu/datn/ebook_store/service/ReadingProgressService.java`

**Interface Method:**
```java
boolean toggleFavorite(User user, String bookId);
List<ReadingProgress> getFavoriteBooksByUser(User user);
```

#### toggleFavorite()
```java
@Override
@Transactional
public boolean toggleFavorite(User user, String bookId) {
    // Tìm book từ database
    Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

    // Tìm hoặc tạo reading progress cho user và book
    Optional<ReadingProgress> progressOpt = readingProgressRepository.findByUserAndBook(user, book);
    ReadingProgress progress;

    if (progressOpt.isPresent()) {
        // Nếu đã có progress, toggle trạng thái favorite
        progress = progressOpt.get();
        progress.setIsFavorite(!progress.getIsFavorite());
    } else {
        // Nếu chưa có progress, tạo mới với favorite = true
        progress = new ReadingProgress();
        progress.setProgressId(generateProgressId());
        progress.setUser(user);
        progress.setBook(book);
        progress.setIsFavorite(true);
        progress.setProgressPercentage(0.0f);
        progress.setIsCompleted(false);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setLastReadAt(LocalDateTime.now());
    }

    readingProgressRepository.save(progress);
    return progress.getIsFavorite();
}
```

#### getFavoriteBooksByUser()
```java
@Override
public List<ReadingProgress> getFavoriteBooksByUser(User user) {
    return readingProgressRepository.findByUserAndIsFavoriteTrue(user);
}
```

**Repository Method:**
```java
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, String> {
    List<ReadingProgress> findByUserAndIsFavoriteTrue(User user);
    Optional<ReadingProgress> findByUserAndBook(User user, Book book);
    // ... other methods
}
```

---

## Database Schema

### Table: `reading_progress`

**Relevant Column:**
```sql
CREATE TABLE `reading_progress` (
  `progress_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `is_favorite` tinyint(1) DEFAULT '0',  -- ← Favorite flag
  -- ... other columns
  PRIMARY KEY (`progress_id`),
  UNIQUE KEY `unique_user_book` (`user_id`, `book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Query: Get Favorite Books

```sql
SELECT 
    b.book_id,
    b.title,
    b.cover_image_url,
    b.author,
    rp.last_read_at
FROM reading_progress rp
JOIN books b ON rp.book_id = b.book_id
WHERE rp.user_id = ?
  AND rp.is_favorite = 1
ORDER BY rp.last_read_at DESC;
```

---

## Frontend Implementation

### Heart Icon Button

**Book Detail Page:**
```html
<button id="favorite-btn" 
        class="btn btn-outline-danger favorite-btn"
        data-book-id="[[${book.bookId}]]">
    <i class="far fa-heart"></i>
    <span class="favorite-text">Yêu thích</span>
</button>
```

**JavaScript:**
```javascript
document.addEventListener('DOMContentLoaded', function() {
    const favoriteBtn = document.getElementById('favorite-btn');
    const bookId = favoriteBtn.dataset.bookId;
    
    // Load initial state
    loadFavoriteState(bookId);
    
    // Click handler
    favoriteBtn.addEventListener('click', function() {
        toggleFavorite(bookId);
    });
});

async function loadFavoriteState(bookId) {
    try {
        const response = await fetch(`/api/favorites/check/${bookId}`);
        const data = await response.json();
        
        updateFavoriteUI(data.isFavorite);
    } catch (error) {
        console.error('Error loading favorite state:', error);
    }
}

async function toggleFavorite(bookId) {
    try {
        const response = await fetch('/api/favorites/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ bookId: bookId })
        });
        
        const data = await response.json();
        
        if (data.success) {
            updateFavoriteUI(data.isFavorite);
            showToast('success', data.message);
        } else {
            if (response.status === 401) {
                // Redirect to login
                window.location.href = '/auth/login';
            } else {
                showToast('error', data.message);
            }
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        showToast('error', 'Có lỗi xảy ra');
    }
}

function updateFavoriteUI(isFavorite) {
    const btn = document.getElementById('favorite-btn');
    const icon = btn.querySelector('i');
    const text = btn.querySelector('.favorite-text');
    
    if (isFavorite) {
        icon.classList.remove('far');
        icon.classList.add('fas'); // Filled heart
        btn.classList.add('active');
        text.textContent = 'Đã yêu thích';
    } else {
        icon.classList.remove('fas');
        icon.classList.add('far'); // Outline heart
        btn.classList.remove('active');
        text.textContent = 'Yêu thích';
    }
}
```

### CSS Styling

```css
.favorite-btn {
    transition: all 0.3s ease;
}

.favorite-btn:hover {
    transform: scale(1.1);
}

.favorite-btn.active {
    background-color: #dc3545;
    border-color: #dc3545;
    color: white;
}

.favorite-btn .fa-heart {
    transition: all 0.3s ease;
}

.favorite-btn:active .fa-heart {
    animation: heartBeat 0.3s;
}

@keyframes heartBeat {
    0%, 100% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.3);
    }
}
```

### Favorites Page

**Template:** `user/favorites.html`

```html
<div class="container">
    <h2>Sách Yêu Thích</h2>
    
    <div id="favorites-container" class="books-grid">
        <!-- Loaded via JavaScript -->
    </div>
    
    <div id="empty-state" style="display: none;">
        <i class="fas fa-heart-broken fa-5x text-muted"></i>
        <p>Bạn chưa có sách yêu thích nào</p>
        <a href="/user/books" class="btn btn-primary">
            Khám phá sách
        </a>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    loadFavorites();
});

async function loadFavorites() {
    try {
        const response = await fetch('/api/favorites');
        const data = await response.json();
        
        if (data.success && data.favorites.length > 0) {
            renderFavorites(data.favorites);
        } else {
            document.getElementById('empty-state').style.display = 'block';
        }
    } catch (error) {
        console.error('Error loading favorites:', error);
    }
}

function renderFavorites(favorites) {
    const container = document.getElementById('favorites-container');
    
    container.innerHTML = favorites.map(book => `
        <div class="book-card">
            <div class="book-cover">
                <img src="${book.coverImageUrl}" alt="${book.title}">
                <button class="btn-remove-favorite" 
                        onclick="removeFavorite('${book.bookId}')">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <h4>${book.title}</h4>
            <a href="/user/books/details/${book.bookId}" class="btn btn-primary">
                Xem chi tiết
            </a>
        </div>
    `).join('');
}

async function removeFavorite(bookId) {
    if (!confirm('Xóa sách này khỏi danh sách yêu thích?')) {
        return;
    }
    
    try {
        const response = await fetch('/api/favorites/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ bookId: bookId })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('success', 'Đã xóa khỏi yêu thích');
            loadFavorites(); // Reload list
        }
    } catch (error) {
        console.error('Error removing favorite:', error);
    }
}
</script>
```

---

## Testing

### Test Cases

#### TC-1: Add to Favorites
**Precondition:** User đã đăng nhập

**Steps:**
1. Navigate to book detail page
2. Click heart icon
3. Verify icon changes to filled

**Expected Result:**
- Icon: outline → filled
- Button text: "Yêu thích" → "Đã yêu thích"
- Toast: "Đã thêm vào sách yêu thích"
- Database: is_favorite = 1

---

#### TC-2: Remove from Favorites
**Precondition:** Book already in favorites

**Steps:**
1. Click heart icon again
2. Verify icon changes to outline

**Expected Result:**
- Icon: filled → outline
- Button text: "Đã yêu thích" → "Yêu thích"
- Toast: "Đã xóa khỏi sách yêu thích"
- Database: is_favorite = 0

---

#### TC-3: Toggle Without Login
**Precondition:** User not logged in

**Steps:**
1. Click heart icon
2. Verify redirect

**Expected Result:**
- Status: 401 Unauthorized
- Redirect to `/auth/login`
- Error message: "Vui lòng đăng nhập"

---

#### TC-4: View Favorites Page
**Steps:**
1. Add 3 books to favorites
2. Navigate to `/user/favorites`

**Expected Result:**
- 3 books displayed
- Sorted by last read date
- Each has "Xóa" button

---

#### TC-5: Empty Favorites
**Precondition:** No favorites

**Steps:**
1. Navigate to `/user/favorites`

**Expected Result:**
- Empty state shown
- Message: "Bạn chưa có sách yêu thích nào"
- Link to browse books

---

#### TC-6: Favorite Persistence
**Steps:**
1. Add book to favorites
2. Logout
3. Login again
4. Check book detail

**Expected Result:**
- Heart icon still filled
- Favorite status persisted

---

#### TC-7: API Check Favorite
**Steps:**
```
GET /api/favorites/check/book_001
```

**Expected Result:**
```json
{
  "isFavorite": true
}
```

---

#### TC-8: Animation Effect
**Steps:**
1. Click heart icon rapidly
2. Observe animation

**Expected Result:**
- Heartbeat animation plays
- Smooth transition
- No flickering

---

## Best Practices

### 1. User Feedback
✅ **DO**: Immediate visual feedback
```javascript
// Update UI immediately, then call API
updateFavoriteUI(!currentState);
toggleFavorite(bookId);
```

❌ **DON'T**: Wait for API response to update UI
```javascript
// BAD - User sees delay
await toggleFavorite(bookId);
updateFavoriteUI(newState);
```

### 2. Error Handling
✅ **DO**: Graceful degradation
```javascript
try {
    await toggleFavorite(bookId);
} catch (error) {
    // Revert UI change
    updateFavoriteUI(previousState);
    showToast('error', 'Không thể cập nhật');
}
```

### 3. Performance
✅ **DO**: Debounce rapid clicks
```javascript
let toggleTimeout;
function toggleFavorite(bookId) {
    clearTimeout(toggleTimeout);
    toggleTimeout = setTimeout(() => {
        // Call API
    }, 300);
}
```

---

## Future Enhancements

### 1. Favorite Collections
- User tạo collections (folders)
- Organize favorites into groups
- Share collections with others

### 2. Favorite Recommendations
- "Users who liked this also liked..."
- Smart recommendations based on favorites

### 3. Favorite Export
- Export favorites to CSV/JSON
- Import favorites from file

### 4. Favorite Notifications
- Notify khi favorite book có discount
- Notify khi favorite author releases new book

---

## Related Flows

- **FLOW 17**: Home Page & Book Browse
- **FLOW 20**: User Library & Reading History
- **FLOW 07**: Reading Interface

---

**Status:** ✅ COMPLETE  
**Implementation:** 100%  
**Last Updated:** 20/12/2025

