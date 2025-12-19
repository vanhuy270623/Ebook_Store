# HƯỚNG DẪN CHỨC NĂNG YÊU THÍCH SÁCH (FAVORITE BOOKS)

## Tổng quan
Đã triển khai thành công chức năng thêm/xóa sách yêu thích, sử dụng trường `is_favorite` có sẵn trong bảng `reading_progress`.

## Các thành phần đã tạo/cập nhật

### 1. Backend - Java

#### 1.1. Entity
- **ReadingProgress.java** - Đã có sẵn trường `isFavorite` (Boolean)

#### 1.2. Service
- **ReadingProgressService.java**
  - Thêm method: `boolean toggleFavorite(User user, String bookId)`
  
- **ReadingProgressServiceImpl.java**
  - Implement `toggleFavorite()`: Tự động tạo reading_progress nếu chưa có, toggle trạng thái favorite

#### 1.3. Controller
- **FavoriteController.java** (MỚI)
  - `POST /api/favorites/toggle` - Toggle favorite status
  - `GET /api/favorites/check/{bookId}` - Kiểm tra trạng thái favorite
  
- **UserController.java**
  - Cập nhật method `library()`: Load danh sách sách yêu thích từ `getFavoriteBooksByUser()`

### 2. Frontend - JavaScript & HTML

#### 2.1. JavaScript Files

**books-common.js** (MỚI)
- File dùng chung cho tất cả các trang books
- Functions:
  - `initBookActionButtons()` - Khởi tạo nút favorite
  - `toggleFavorite(bookId, iconElement)` - Gọi API toggle
  - `loadFavoriteStatuses()` - Load trạng thái favorite khi trang load
  - `extractBookIdFromUrl(url)` - Lấy bookId từ URL

**user-index.js** (CẬP NHẬT)
- Thêm xử lý nút favorite trong index.html
- Functions tương tự books-common.js

**library.js** (CẬP NHẬT)
- Xử lý nút favorite trong library.html
- `initFavoriteButtons()` - Khởi tạo event listeners
- `toggleFavorite(bookId, button)` - Gọi API và reload trang nếu đang ở tab favorites

#### 2.2. HTML Templates

**Các trang đã thêm books-common.js:**
- list.html
- category.html
- search.html
- trending.html
- newest.html
- top-rated.html
- by-access-type.html

**library.html**
- Tab "Yêu thích" đã có sẵn, hiển thị từ `favoriteBooks`
- Nút favorite có class `action-btn favorite active`

**index.html**
- Đã có nút favorite trong mỗi book card
- JavaScript xử lý click event

## Cách sử dụng

### 1. Thêm sách vào yêu thích

**Từ trang Index/Books:**
1. User click vào icon trái tim (♥) trên book card
2. System gọi `POST /api/favorites/toggle` với `{bookId: "book_xxx"}`
3. Nếu chưa có reading_progress → Tạo mới với `isFavorite = true`
4. Nếu đã có → Toggle `isFavorite` (true ↔ false)
5. Icon đổi màu đỏ khi favorite, xám khi không favorite

**Từ trang Library:**
1. User vào tab "Yêu thích"
2. Click icon trái tim để xóa khỏi yêu thích
3. Trang tự động reload sau 500ms

### 2. Xem danh sách sách yêu thích

**Cách 1: Từ Library**
- Vào `/user/library?tab=favorites`
- Hiển thị tất cả sách có `isFavorite = true`

**Cách 2: Kiểm tra số lượng**
- Stat card "Yêu thích" trên library page
- Hiển thị `totalFavorites`

## API Endpoints

### POST /api/favorites/toggle
Toggle trạng thái yêu thích của sách.

**Request:**
```json
{
  "bookId": "book_xxx"
}
```

**Response Success:**
```json
{
  "success": true,
  "isFavorite": true,
  "message": "Đã thêm vào sách yêu thích"
}
```

**Response Error (chưa đăng nhập):**
```json
{
  "success": false,
  "message": "Vui lòng đăng nhập để thêm sách yêu thích"
}
```
HTTP Status: 401

### GET /api/favorites/check/{bookId}
Kiểm tra xem sách có được yêu thích hay không.

**Response:**
```json
{
  "isFavorite": true
}
```

## Luồng dữ liệu

```
User click ❤️ 
    ↓
JavaScript gọi API /api/favorites/toggle
    ↓
FavoriteController nhận request
    ↓
ReadingProgressService.toggleFavorite()
    ↓
Kiểm tra reading_progress:
    - Nếu chưa có → Tạo mới (isFavorite = true)
    - Nếu có rồi → Toggle isFavorite
    ↓
Lưu vào database
    ↓
Trả về JSON {success, isFavorite, message}
    ↓
JavaScript cập nhật UI
```

## Database Schema

```sql
-- Bảng reading_progress đã có sẵn
CREATE TABLE reading_progress (
    progress_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    book_id VARCHAR(50),
    book_asset_id VARCHAR(50),
    last_read_location VARCHAR(500),
    progress_percentage FLOAT,
    is_completed TINYINT(1),
    is_favorite TINYINT(1) DEFAULT 0,  -- ← Trường này được dùng
    access_type VARCHAR(20),
    last_read_at DATETIME,
    created_at DATETIME,
    bookmarks_data JSON,
    UNIQUE KEY user_book_unique (user_id, book_id)
);
```

## Testing

### Test Case 1: Thêm sách yêu thích lần đầu
1. Chưa đăng nhập → Hiện thông báo "Vui lòng đăng nhập"
2. Đã đăng nhập → Click ❤️
3. Icon đổi sang màu đỏ (fas fa-heart)
4. Hiện toast "Đã thêm vào sách yêu thích"
5. Kiểm tra DB: `is_favorite = 1`

### Test Case 2: Xóa khỏi yêu thích
1. Click ❤️ lần nữa trên sách đã favorite
2. Icon đổi sang màu xám (far fa-heart)
3. Hiện toast "Đã xóa khỏi sách yêu thích"
4. Kiểm tra DB: `is_favorite = 0`

### Test Case 3: Xem trong Library
1. Vào `/user/library?tab=favorites`
2. Hiển thị tất cả sách có `is_favorite = true`
3. Click ❤️ để xóa → Trang reload → Sách biến mất

### Test Case 4: Load trạng thái khi vào trang
1. Vào trang `/books` hoặc `/user/index`
2. JavaScript gọi `/api/favorites/check/{bookId}` cho mỗi sách
3. Nếu `isFavorite = true` → Icon màu đỏ

## Lưu ý quan trọng

1. **Không cần đọc sách mới favorite được**
   - Toggle favorite tự động tạo reading_progress nếu chưa có

2. **Unique constraint**
   - Mỗi user chỉ có 1 reading_progress cho 1 book
   - Đảm bảo bởi UNIQUE KEY `user_book_unique (user_id, book_id)`

3. **Session management**
   - Cần đăng nhập để sử dụng chức năng
   - `currentUser` được lấy từ `HttpSession`

4. **Animation**
   - Icon heart có animation heartBeat khi toggle
   - Toast notification slide in/out từ bên phải

5. **Performance**
   - `loadFavoriteStatuses()` có thể chậm nếu trang có nhiều sách
   - Có thể cải thiện bằng cách load batch hoặc server-side render

## Files cần build

```
src/main/java/stu/datn/ebook_store/
├── controller/user/FavoriteController.java (MỚI)
├── service/ReadingProgressService.java (CẬP NHẬT)
├── service/impl/ReadingProgressServiceImpl.java (CẬP NHẬT)
└── controller/user/UserController.java (CẬP NHẬT)

src/main/resources/
├── static/user_template/js/
│   ├── books-common.js (MỚI)
│   ├── user-index.js (CẬP NHẬT)
│   └── library.js (CẬP NHẬT)
└── templates/user/books/
    ├── list.html (CẬP NHẬT)
    ├── category.html (CẬP NHẬT)
    ├── search.html (CẬP NHẬT)
    ├── trending.html (CẬP NHẬT)
    ├── newest.html (CẬP NHẬT)
    ├── top-rated.html (CẬP NHẬT)
    └── by-access-type.html (CẬP NHẬT)
```

## Triển khai

1. **Build project:**
   ```bash
   mvn clean install
   ```

2. **Restart server:**
   ```bash
   # Hoặc restart trong IDE
   mvn spring-boot:run
   ```

3. **Test:**
   - Vào http://localhost:8080/user/index
   - Click icon ❤️ trên bất kỳ sách nào
   - Kiểm tra library: http://localhost:8080/user/library?tab=favorites

## Kết luận

Chức năng favorite đã được triển khai hoàn chỉnh với:
- ✅ Backend API RESTful
- ✅ Frontend JavaScript tương tác mượt mà
- ✅ Tích hợp vào tất cả trang books
- ✅ Hiển thị trong Library
- ✅ Thông báo toast user-friendly
- ✅ Animation đẹp mắt
- ✅ Xử lý lỗi đầy đủ

Người dùng có thể dễ dàng đánh dấu sách yêu thích và quản lý trong thư viện cá nhân!

