# 🎯 KIẾN TRÚC CHỨC NĂNG ĐỌC SÁCH - TECHNICAL OVERVIEW

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  PDF Viewer  │  │ EPUB Viewer  │  │Reader Selector│         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┴──────────────────┘
                             │
┌─────────────────────────────┼─────────────────────────────────┐
│                    CONTROLLER LAYER                             │
│                  ReadingController.java                         │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  • openBook()         - Auto-detect và open reader       │ │
│  │  • pdfViewer()        - Open PDF reader                  │ │
│  │  • epubReader()       - Open EPUB reader                 │ │
│  │  • saveProgress()     - API lưu tiến độ                  │ │
│  │  • getProgress()      - API lấy tiến độ                  │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬─────────────────────────────────┘
                              │
┌─────────────────────────────┼─────────────────────────────────┐
│                      SERVICE LAYER                              │
│  ┌────────────────────┐  ┌──────────────────┐                │
│  │ReadingProgressSvc  │  │BookAssetService  │                │
│  │• getProgress()     │  │• getAssets()     │                │
│  │• saveProgress()    │  │• uploadAsset()   │                │
│  │• markComplete()    │  │• deleteAsset()   │                │
│  └────────┬───────────┘  └─────────┬────────┘                │
└───────────┼──────────────────────────┼─────────────────────────┘
            │                          │
┌───────────┼──────────────────────────┼─────────────────────────┐
│                    REPOSITORY LAYER                             │
│  ┌────────────────────┐  ┌──────────────────┐                │
│  │ReadingProgressRepo │  │BookAssetRepo     │                │
│  │JpaRepository       │  │JpaRepository     │                │
│  └────────┬───────────┘  └─────────┬────────┘                │
└───────────┼──────────────────────────┼─────────────────────────┘
            │                          │
┌───────────┴──────────────────────────┴─────────────────────────┐
│                         DATABASE                                │
│  ┌──────────────────┐  ┌──────────────────┐                   │
│  │reading_progress  │  │   bookassets     │                   │
│  │• progress_id     │  │• book_asset_id   │                   │
│  │• user_id         │  │• book_id         │                   │
│  │• book_id         │  │• file_type       │                   │
│  │• last_location   │  │• file_url        │                   │
│  │• percentage      │  │• file_size       │                   │
│  └──────────────────┘  └──────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
            │
┌───────────┴─────────────────────────────────────────────────────┐
│                      FILE STORAGE                               │
│              F:/datn_uploads/book_asset/                        │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  source/                                                  │ │
│  │  ├── khoahoc-vientuong/                                  │ │
│  │  ├── kienthuc-hocthuat/                                  │ │
│  │  ├── kinhte-quanly/                                      │ │
│  │  ├── tamly-kynangsong/                                   │ │
│  │  └── tieuthuyet-vanhoc/                                  │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Request Flow

### Flow 1: Mở sách lần đầu

```
User clicks "Đọc sách"
    ↓
GET /reading/book/{bookId}
    ↓
ReadingController.openBook()
    ├─→ Check authentication
    ├─→ Check book exists
    ├─→ Check user has access (canUserAccessBook)
    ├─→ Get BookAssets (PDF/EPUB)
    ├─→ Get or Create ReadingProgress
    │   ├─→ If exists: Load from DB
    │   └─→ If not: Create new with 0% progress
    └─→ Forward to appropriate viewer
        ├─→ PDF → pdf-viewer.html
        └─→ EPUB → epub-viewer.html
```

### Flow 2: Load file trong viewer

**PDF:**
```
pdf-viewer.html loads
    ↓
JavaScript initializes
    ↓
Load PDF.js library
    ↓
Construct PDF path: /uploads/source/{fileUrl}
    ↓
Request goes to WebMvcConfig ResourceHandler
    ↓
File served from: F:/datn_uploads/book_asset/source/{fileUrl}
    ↓
PDF.js renders to canvas
    ↓
Display page {initialPage}
```

**EPUB:**
```
epub-viewer.html loads
    ↓
JavaScript initializes
    ↓
Load ePub.js library
    ↓
Construct EPUB path: /uploads/source/{fileUrl}
    ↓
Request goes to WebMvcConfig ResourceHandler
    ↓
File served from: F:/datn_uploads/book_asset/source/{fileUrl}
    ↓
ePub.js parses and renders
    ↓
Load Table of Contents
    ↓
Display at saved location or beginning
```

### Flow 3: Save progress tự động

```
User reads book (changes page/chapter)
    ↓
JavaScript triggers saveProgress() every 30s
    ↓
POST /reading/api/progress/{bookId}
    ├─→ currentPage: int
    ├─→ totalPages: int
    └─→ bookmarkData: string (optional)
    ↓
ReadingController.saveProgress()
    ├─→ Get User from authentication
    ├─→ Get Book by ID
    ├─→ Get or Create ReadingProgress
    ├─→ Update fields:
    │   ├─→ lastReadLocation
    │   ├─→ progressPercentage
    │   ├─→ lastReadAt
    │   └─→ isCompleted (if >= 99%)
    └─→ Save to database
    ↓
Return JSON response
```

---

## 🗂️ Data Models

### ReadingProgress Entity

```java
@Entity
@Table(name = "reading_progress")
public class ReadingProgress {
    @Id
    private String progressId;          // UUID
    
    @ManyToOne
    private User user;                  // User đang đọc
    
    @ManyToOne
    private Book book;                  // Sách đang đọc
    
    @ManyToOne
    private BookAsset bookAsset;        // Asset file (PDF/EPUB)
    
    private String lastReadLocation;    // "5" (PDF page) hoặc CFI string (EPUB)
    
    private Float progressPercentage;   // 0.0 - 100.0
    
    private Boolean isCompleted;        // Đọc xong chưa?
    
    private Boolean isFavorite;         // Yêu thích?
    
    @Enumerated(EnumType.STRING)
    private AccessType accessType;      // FREE, PURCHASED, SUBSCRIPTION
    
    private LocalDateTime lastReadAt;   // Lần đọc gần nhất
    
    private LocalDateTime createdAt;    // Lần đọc đầu tiên
}
```

### BookAsset Entity

```java
@Entity
@Table(name = "bookassets")
public class BookAsset {
    @Id
    private String bookAssetId;         // UUID
    
    @ManyToOne
    private Book book;                  // Sách chứa asset này
    
    @Enumerated(EnumType.STRING)
    private FileType fileType;          // PDF, EPUB
    
    private String fileUrl;             // "category/filename.pdf" (relative path)
    
    private Long fileSize;              // Bytes
    
    private String previewUrl;          // URL preview (optional)
    
    private LocalDateTime createdAt;
}
```

---

## 📡 API Endpoints Detail

### 1. GET /reading/book/{bookId}

**Purpose:** Mở sách với auto-detect format

**Parameters:**
- `bookId` (path): ID của sách

**Response:**
- HTML page (pdf-viewer hoặc epub-viewer)

**Example:**
```
GET http://localhost:2706/reading/book/book_001
→ Redirects to PDF or EPUB viewer
```

---

### 2. POST /reading/api/progress/{bookId}

**Purpose:** Lưu reading progress

**Parameters:**
- `bookId` (path): ID của sách
- `currentPage` (form): Trang hiện tại hoặc % progress
- `totalPages` (form): Tổng số trang
- `bookmarkData` (form, optional): JSON string location data

**Request:**
```http
POST /reading/api/progress/book_001
Content-Type: application/x-www-form-urlencoded

currentPage=5&totalPages=100&bookmarkData={"cfi":"..."}
```

**Response:**
```json
{
  "status": "success",
  "message": "Progress saved",
  "percentage": 5.0
}
```

**Error Response:**
```json
{
  "status": "error",
  "message": "User not found"
}
```

---

### 3. GET /reading/api/progress/{bookId}

**Purpose:** Lấy reading progress

**Parameters:**
- `bookId` (path): ID của sách

**Response:**
```json
{
  "progressId": "uuid-here",
  "lastReadLocation": "5",
  "progressPercentage": 5.0,
  "isCompleted": false,
  "lastReadAt": "2024-12-13T10:30:00"
}
```

---

## 🔐 Security & Access Control

### Authentication Check

```java
if (authentication == null) {
    return "redirect:/auth/login";
}
```

### Authorization Check

```java
private boolean canUserAccessBook(User user, Book book) {
    // 1. Admin can read everything
    if (user.isAdmin()) return true;
    
    // 2. Free books
    if (book.isFree()) return true;
    
    // 3. Check if user purchased
    if (orderService.hasUserPurchasedBook(user, book)) {
        return true;
    }
    
    // 4. Check subscription
    if (subscriptionService.hasActiveSubscription(user)) {
        return true;
    }
    
    return false;
}
```

---

## 🎨 Frontend Architecture

### PDF Viewer (pdf-viewer.html)

**Libraries:**
- PDF.js v3.11.174

**Key Variables:**
```javascript
let pdfDoc = null;          // PDF document object
let pageNum = 1;            // Current page number
let pageCount = 0;          // Total pages
let scale = 1.0;            // Zoom level
let canvas = null;          // Canvas element
let ctx = null;             // Canvas context
```

**Key Functions:**
```javascript
loadPDF()               // Load PDF file
renderPage(num)         // Render specific page
previousPage()          // Navigate to previous page
nextPage()              // Navigate to next page
zoomIn() / zoomOut()    // Zoom controls
saveProgress()          // Save current progress
saveBookmark()          // Save bookmark
toggleDarkMode()        // Toggle dark mode
```

---

### EPUB Viewer (epub-viewer.html)

**Libraries:**
- ePub.js v0.3.93

**Key Variables:**
```javascript
let book = null;            // ePub book object
let rendition = null;       // Rendition object
let currentLocation = null; // Current reading location
```

**Key Functions:**
```javascript
loadEPUB()                  // Load EPUB file
setupNavigation()           // Setup navigation events
loadTableOfContents()       // Load TOC
goToChapter(href)           // Navigate to chapter
previousPage() / nextPage() // Navigation
changeFontSize()            // Font customization
changeFontFamily()          // Font family
toggleDarkMode()            // Theme toggle
saveProgress()              // Save progress
```

---

## 🔧 Configuration

### application.properties

```properties
# File Upload Configuration
file.upload-dir=F:/datn_uploads/book_asset/image/covers

# Static Resources
spring.web.resources.static-locations=classpath:/static/,file:F:/datn_uploads/

# Multipart Settings
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### WebMvcConfig.java

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Source files (PDF, EPUB)
        registry.addResourceHandler("/uploads/source/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
        
        // Cover images
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:F:/datn_uploads/book_asset/");
    }
}
```

---

## 📊 Performance Considerations

### Optimization Strategies

1. **Lazy Loading**
   - Load pages on demand
   - Don't load entire book into memory

2. **Caching**
   - Browser cache for static assets
   - Redis cache for progress data (future)

3. **CDN**
   - Serve PDF.js và ePub.js từ CDN
   - Faster load times

4. **Database Indexing**
   ```sql
   INDEX idx_user_book (user_id, book_id)
   INDEX idx_last_read_at (last_read_at)
   ```

5. **File Storage**
   - Organize by category
   - Use relative paths
   - Separate drive for uploads

---

## 🐛 Error Handling

### Frontend Errors

```javascript
try {
    await loadPDF();
} catch (error) {
    console.error('Error loading PDF:', error);
    showLoading(false);
    alert('Lỗi khi tải file PDF. Vui lòng thử lại sau.');
}
```

### Backend Errors

```java
try {
    ReadingProgress progress = readingProgressService.save(progress);
    return success();
} catch (Exception e) {
    log.error("Error saving progress: {}", e.getMessage());
    return error(e.getMessage());
}
```

---

## 📈 Monitoring & Logging

### Key Metrics to Track

1. **User Engagement**
   - Total books opened
   - Average reading time
   - Completion rate

2. **Performance**
   - Page load time
   - File load time
   - API response time

3. **Errors**
   - File not found errors
   - Progress save failures
   - Authentication failures

### Logging

```java
log.info("User {} opened book {}", userId, bookId);
log.debug("Progress saved: {}%", percentage);
log.error("Error loading book {}: {}", bookId, e.getMessage());
```

---

## 🚀 Future Enhancements

### Phase 1 (Priority High)
- [ ] Purchase verification
- [ ] Subscription check
- [ ] Reading analytics dashboard
- [ ] Social features (share progress)

### Phase 2 (Priority Medium)
- [ ] Offline reading support
- [ ] Sync across devices
- [ ] Reading goals and achievements
- [ ] Note-taking and highlights

### Phase 3 (Priority Low)
- [ ] Text-to-speech
- [ ] Translation support
- [ ] Reading recommendations based on progress
- [ ] Community features

---

## 📚 References

- [PDF.js Documentation](https://mozilla.github.io/pdf.js/)
- [ePub.js Documentation](https://github.com/futurepress/epub.js/)
- [Spring Boot Resource Handling](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)

---

*Last updated: 13/12/2024*

