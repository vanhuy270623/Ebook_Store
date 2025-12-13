# 📚 Hệ Thống Bookmark - Tài Liệu Hoàn Chỉnh

## 🎯 Tổng Quan

Hệ thống bookmark cho phép:
1. **Auto-save**: Tự động lưu vị trí đọc cuối cùng (1 bookmark/sách)
2. **Manual bookmarks**: User tự tạo nhiều bookmarks với ghi chú

## 🗄️ Database Design

### Thay Đổi Schema

**Thêm trường vào bảng `reading_progress`:**
```sql
ALTER TABLE reading_progress 
ADD COLUMN bookmarks_data JSON DEFAULT NULL;
```

### Format JSON
```json
{
  "bookmarks": [
    {
      "id": "bm_1702456789123",
      "location": "page-20",
      "pageNumber": 20,
      "percentage": 7.14,
      "note": "Chương mở đầu hay",
      "createdAt": "2025-12-13T14:00:00"
    },
    {
      "id": "bm_1702456890456",
      "location": "page-80",
      "pageNumber": 80,
      "percentage": 28.57,
      "note": "Phần climax",
      "createdAt": "2025-12-13T15:00:00"
    }
  ]
}
```

### Cách Hoạt Động

| Trường | Mục Đích | Format |
|--------|----------|--------|
| `last_read_location` | Auto-save vị trí hiện tại | `"page-35"` or `epubcfi(...)` |
| `bookmarks_data` | Manual bookmarks | JSON array |
| `progress_percentage` | % đã đọc | `45.5` (float) |
| `is_completed` | Đã đọc hết chưa | `true`/`false` |

---

## 🏗️ Backend Implementation

### 1. Entity Class

**File**: `ReadingProgress.java`

```java
@Column(name = "bookmarks_data", columnDefinition = "JSON")
private String bookmarksData;

@Getter
@Setter
public static class BookmarkData {
    private String id;
    private String location;
    private Integer pageNumber;
    private Float percentage;
    private String note;
    private String createdAt;
}
```

### 2. Service Methods

**File**: `ReadingProgressService.java`

```java
// Thêm bookmark
void addBookmark(String progressId, String location, Integer pageNumber, Float percentage, String note);

// Xóa bookmark
void removeBookmark(String progressId, String bookmarkId);

// Lấy danh sách bookmarks
List<ReadingProgress.BookmarkData> getBookmarks(String progressId);
```

### 3. API Endpoints

**File**: `ReadingController.java`

#### A. Thêm Bookmark
```java
POST /reading/api/bookmarks/{bookId}
Parameters:
  - location: "page-35" (required)
  - pageNumber: 35 (optional, for PDF)
  - percentage: 12.5 (optional)
  - note: "Chương hay" (optional)
  
Response:
{
  "status": "success",
  "message": "Bookmark added"
}
```

#### B. Lấy Danh Sách Bookmarks
```java
GET /reading/api/bookmarks/{bookId}

Response:
[
  {
    "id": "bm_1702456789123",
    "location": "page-20",
    "pageNumber": 20,
    "percentage": 7.14,
    "note": "Chương mở đầu hay",
    "createdAt": "2025-12-13T14:00:00"
  },
  ...
]
```

#### C. Xóa Bookmark
```java
DELETE /reading/api/bookmarks/{bookId}/{bookmarkId}

Response:
{
  "status": "success",
  "message": "Bookmark removed"
}
```

---

## 💻 Frontend Implementation

### 1. PDF Viewer Integration

**File**: `pdf-viewer.html`

#### A. Load Bookmarks Khi Mở Sách
```javascript
// Load bookmarks from server
async function loadBookmarks() {
    try {
        const response = await fetch(`/reading/api/bookmarks/${bookId}`);
        const bookmarks = await response.json();
        displayBookmarksList(bookmarks);
    } catch (error) {
        console.error('Error loading bookmarks:', error);
    }
}
```

#### B. Lưu Bookmark Thủ Công
```javascript
async function saveManualBookmark() {
    const note = prompt('Ghi chú cho bookmark:');
    if (note === null) return; // User cancelled
    
    const formData = new FormData();
    formData.append('location', `page-${pageNum}`);
    formData.append('pageNumber', pageNum);
    formData.append('percentage', (pageNum / pageCount * 100).toFixed(2));
    formData.append('note', note || '');
    
    const response = await fetch(`/reading/api/bookmarks/${bookId}`, {
        method: 'POST',
        body: formData
    });
    
    if (response.ok) {
        alert('✅ Bookmark đã lưu!');
        loadBookmarks(); // Reload list
    }
}
```

#### C. Hiển thị Danh Sách Bookmarks
```javascript
function displayBookmarksList(bookmarks) {
    const container = document.getElementById('bookmarks-list');
    if (!container) return;
    
    if (bookmarks.length === 0) {
        container.innerHTML = '<p class="text-muted">Chưa có bookmark</p>';
        return;
    }
    
    container.innerHTML = bookmarks.map(bm => `
        <div class="bookmark-item" data-page="${bm.pageNumber}">
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1" onclick="jumpToBookmark('${bm.location}')">
                    <strong>📖 Trang ${bm.pageNumber}</strong>
                    <small class="text-muted d-block">${bm.percentage.toFixed(1)}%</small>
                    ${bm.note ? `<p class="mb-0 mt-1">"${bm.note}"</p>` : ''}
                    <small class="text-muted">${formatDate(bm.createdAt)}</small>
                </div>
                <button class="btn btn-sm btn-danger" 
                        onclick="deleteBookmark('${bm.id}', event)">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
}
```

#### D. Jump Đến Bookmark
```javascript
function jumpToBookmark(location) {
    const pageNumber = parseInt(location.split('-')[1]);
    if (pageNumber) {
        pageNum = pageNumber;
        renderPage(pageNum);
    }
}
```

#### E. Xóa Bookmark
```javascript
async function deleteBookmark(bookmarkId, event) {
    event.stopPropagation(); // Prevent jump
    
    if (!confirm('Xóa bookmark này?')) return;
    
    const response = await fetch(
        `/reading/api/bookmarks/${bookId}/${bookmarkId}`,
        { method: 'DELETE' }
    );
    
    if (response.ok) {
        loadBookmarks(); // Reload list
    }
}
```

### 2. UI Components

#### A. Sidebar với Bookmarks
```html
<!-- Thêm vào sidebar -->
<div class="sidebar-section">
    <h6>📚 Bookmarks</h6>
    <button class="btn btn-primary btn-sm mb-2" onclick="saveManualBookmark()">
        <i class="fas fa-bookmark"></i> Thêm Bookmark
    </button>
    <div id="bookmarks-list"></div>
</div>
```

#### B. CSS Styling
```css
.bookmark-item {
    padding: 10px;
    border-left: 3px solid #007bff;
    background: #f8f9fa;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.2s;
}

.bookmark-item:hover {
    background: #e9ecef;
    transform: translateX(5px);
}

.bookmark-item strong {
    color: #007bff;
}
```

---

## 🔄 Luồng Hoạt Động

### Auto-Save (Tự Động)

```
User đọc sách PDF
    ↓
Mỗi 30s hoặc khi chuyển trang
    ↓
POST /reading/api/progress/{bookId}
    ↓
Backend lưu last_read_location = "page-35"
    ↓
Lần sau mở sách → Mở tại trang 35 ✅
```

### Manual Bookmark

```
User đọc đến trang thú vị
    ↓
Click "Thêm Bookmark"
    ↓
Nhập ghi chú (VD: "Chương hay")
    ↓
POST /reading/api/bookmarks/{bookId}
    location=page-45&note=Chương hay
    ↓
Backend thêm vào bookmarks_data JSON
    ↓
Frontend reload danh sách bookmarks ✅
```

### Jump Đến Bookmark

```
User xem danh sách bookmarks
    ↓
Click vào bookmark "Trang 45"
    ↓
jumpToBookmark("page-45")
    ↓
Parse → pageNum = 45
    ↓
renderPage(45) ✅
```

---

## 🧪 Testing

### Test Case 1: Auto-Save
```
1. Mở sách PDF
2. Đọc đến trang 25
3. Đóng browser
4. Mở lại sách
✅ Expected: Mở tại trang 25
```

### Test Case 2: Thêm Bookmark
```
1. Đọc đến trang 50
2. Click "Thêm Bookmark"
3. Nhập note: "Phần quan trọng"
4. Submit
✅ Expected: Bookmark xuất hiện trong list
```

### Test Case 3: Jump Bookmark
```
1. Click vào bookmark "Trang 80"
✅ Expected: Chuyển đến trang 80
```

### Test Case 4: Xóa Bookmark
```
1. Click nút xóa trên bookmark
2. Confirm
✅ Expected: Bookmark biến mất khỏi list
```

### Test Case 5: Nhiều Bookmarks
```
1. Tạo 5 bookmarks ở các trang khác nhau
2. Check list hiển thị đầy đủ
3. Jump đến từng bookmark
✅ Expected: Tất cả hoạt động đúng
```

---

## 📊 Database Queries

### Lấy Bookmarks
```sql
SELECT 
    progress_id,
    JSON_EXTRACT(bookmarks_data, '$.bookmarks') as bookmarks_list
FROM reading_progress
WHERE user_id = ? AND book_id = ?;
```

### Đếm Số Bookmarks
```sql
SELECT 
    book_id,
    JSON_LENGTH(bookmarks_data, '$.bookmarks') as bookmark_count
FROM reading_progress
WHERE user_id = ?;
```

### Tìm Bookmark Theo Note
```sql
SELECT *
FROM reading_progress
WHERE JSON_SEARCH(bookmarks_data, 'one', '%keyword%', NULL, '$.bookmarks[*].note') IS NOT NULL;
```

---

## 🚀 Deployment

### Step 1: Run SQL Migration
```bash
mysql -u root -p ebook_store < DB/add_bookmarks_table.sql
```

### Step 2: Restart Application
```bash
mvn spring-boot:run
```

### Step 3: Test
```
1. Mở sách PDF
2. Click "Thêm Bookmark"
3. Verify bookmark saved
4. Jump to bookmark
5. Delete bookmark
```

---

## 📝 Notes

### Advantages
- ✅ Đơn giản: Không cần bảng mới
- ✅ Flexible: JSON dễ mở rộng
- ✅ Performance: Ít JOIN queries
- ✅ Backward Compatible: Không ảnh hưởng code cũ

### Limitations
- ⚠️ JSON query phức tạp hơn SQL thường
- ⚠️ Cần parse JSON mỗi lần đọc/ghi
- ⚠️ Không thể index từng bookmark

### Future Enhancements
- 🔜 Export bookmarks to file
- 🔜 Import bookmarks from file
- 🔜 Share bookmarks with friends
- 🔜 Bookmark tags/categories
- 🔜 Search trong bookmarks

---

**Status**: ✅ Design Complete  
**Date**: December 13, 2025  
**Next**: Frontend UI Implementation

