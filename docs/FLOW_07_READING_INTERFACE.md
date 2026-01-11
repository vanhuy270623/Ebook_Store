# 📖 FLOW 07: READING INTERFACE (Giao Diện Đọc Sách)

> **Cập nhật lần cuối:** 11/01/2026  
> **Phiên bản:** 2.0 - Bao gồm Anti-Skimming Validation & Time-Capping

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 7.1: Access Reading Interface](#flow-71-access-reading-interface)
3. [Flow 7.2: PDF Reader](#flow-72-pdf-reader)
4. [Flow 7.3: EPUB Reader](#flow-73-epub-reader)
5. [Flow 7.4: Anti-Skimming & Progress Tracking](#flow-74-anti-skimming--progress-tracking)
6. [Flow 7.5: Bookmarks](#flow-75-bookmarks)
7. [Reader Features](#reader-features)
8. [Technical Implementation](#technical-implementation)
9. [API Reference](#api-reference)

---

## Tổng Quan

### Reading Flow Overview
```
┌──────────────┐
│ User Browses │
│    Books     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ View Book    │
│   Detail     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Click "Đọc    │
│   sách"      │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Verify       │
│ Ownership    │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────┐
│         /reading/book/{bookId}       │
│  (Auto-detect format & redirect)     │
└──────┬───────────────────────────────┘
       │
       ├──► Only PDF  → /reading/pdf/{bookId}
       │
       ├──► Only EPUB → /reading/epub/{bookId}
       │
       └──► Both formats → Show format chooser
       │
       ▼
┌──────────────┐
│  Reading     │
│  Interface   │
└──────┬───────┘
       │
       ├──► Track Progress (Anti-Skimming)
       ├──► Save Bookmarks
       ├──► Adjust Settings
       └──► Navigate Pages/Chapters
```

### Components

| Layer | Component | Mô tả |
|-------|-----------|-------|
| **Controller** | `ReadingController.java` | Xử lý routing và view rendering |
| **API Controller** | `ReadingProgressApiController.java` | REST API cho progress sync |
| **Service** | `ReadingProgressServiceImpl.java` | Logic anti-skimming & time-capping |
| **Entity** | `ReadingProgress.java` | Lưu trữ tiến độ, bookmarks, thời gian đọc |
| **DTO** | `ProgressSyncRequest.java`, `ProgressSyncResponse.java` | Request/Response sync |
| **Frontend** | `reading-progress-tracker.js` | Client-side tracking |
| **Libraries** | PDF.js 3.11.174, ePub.js 0.3.93 | Rendering engines |

### URLs

**Reading Interface:**
| URL | Method | Mô tả |
|-----|--------|-------|
| `/reading/book/{bookId}` | GET | Auto-detect format & redirect |
| `/reading/pdf/{bookId}` | GET | PDF Viewer |
| `/reading/epub/{bookId}` | GET | EPUB Reader |
| `/reading/pdf/{category}/{fileName}` | GET | PDF Viewer by path |
| `/reading/epub/{category}/{fileName}` | GET | EPUB Reader by path |
| `/reading/stream/{bookId}` | GET | Secure file streaming |

**Progress API:**
| URL | Method | Mô tả |
|-----|--------|-------|
| `/api/reading/sync` | POST | Đồng bộ tiến độ với anti-skimming |
| `/api/reading/can-review/{bookId}` | GET | Kiểm tra quyền đánh giá |
| `/api/reading/progress/{bookId}` | GET | Lấy thông tin tiến độ |

**Bookmarks API:**
| URL | Method | Mô tả |
|-----|--------|-------|
| `/reading/api/bookmarks/{bookId}` | GET | Lấy danh sách bookmarks |
| `/reading/api/bookmarks/{bookId}` | POST | Thêm bookmark mới |
| `/reading/api/bookmarks/{bookId}/{id}` | DELETE | Xóa bookmark |

---

## Flow 7.1: Access Reading Interface

### Sequence Diagram
```
User → Browser → ReadingController → Services → Database
  │       │              │               │           │
  │ Click "Đọc sách"    │               │           │
  │ /reading/book/{id}  │               │           │
  │─────────────────────►│               │           │
  │       │              │ checkAuth()   │           │
  │       │              │ canUserAccessBook()       │
  │       │              ├───────────────►│           │
  │       │              │               │ Query     │
  │       │              │               ├──────────►│
  │       │              │◄──────────────┤           │
  │       │              │               │           │
  │       │              │ getAssets()   │           │
  │       │              │ (PDF/EPUB?)   │           │
  │       │              ├───────────────►│           │
  │       │              │◄──────────────┤           │
  │       │              │               │           │
  │       │              │ Auto-redirect │           │
  │       │◄─────────────┤ or show chooser          │
  │◄──────┤              │               │           │
```

### Controller Implementation

**File:** `ReadingController.java`

```java
@Controller
@RequestMapping("/reading")
@Slf4j
public class ReadingController extends BaseController {
    
    private final BookService bookService;
    private final BookAssetService bookAssetService;
    private final ReadingProgressService readingProgressService;
    private final OrderItemService orderItemService;
    
    /**
     * Trang chọn format đọc sách (PDF/EPUB)
     * - Nếu chỉ có 1 file → TỰ ĐỘNG redirect
     * - Nếu có cả 2 file → Hiển thị trang chọn
     */
    @GetMapping("/book/{bookId}")
    public String chooseFormat(@PathVariable String bookId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        
        // Authentication check
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }
        
        Book book = bookRepository.findByIdWithAuthors(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        // Access control
        if (!canUserAccessBook(user, book)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc");
            return "redirect:/books/view/" + bookId;
        }
        
        List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
        
        // Filter readable formats
        List<BookAsset> readableAssets = assets.stream()
                .filter(a -> FileType.PDF.equals(a.getFileType()) || 
                            FileType.EPUB.equals(a.getFileType()))
                .toList();
        
        boolean hasPDF = readableAssets.stream()
                .anyMatch(a -> FileType.PDF.equals(a.getFileType()));
        boolean hasEPUB = readableAssets.stream()
                .anyMatch(a -> FileType.EPUB.equals(a.getFileType()));
        
        // Auto-redirect if only one format
        if (hasPDF && !hasEPUB) {
            return "redirect:/reading/pdf/" + bookId;
        }
        if (hasEPUB && !hasPDF) {
            return "redirect:/reading/epub/" + bookId;
        }
        
        // Show format chooser if both available
        model.addAttribute("book", book);
        model.addAttribute("hasPDF", hasPDF);
        model.addAttribute("hasEPUB", hasEPUB);
        return "user/reading/reader";
    }
    
    /**
     * PDF Viewer
     */
    @GetMapping("/pdf/{bookId}")
    public String pdfViewer(@PathVariable String bookId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "PDF", model, redirectAttributes, 
                                "user/reading/pdf-viewer");
    }
    
    /**
     * EPUB Reader
     */
    @GetMapping("/epub/{bookId}")
    public String epubReader(@PathVariable String bookId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "EPUB", model, redirectAttributes, 
                                "user/reading/epub-viewer");
    }
}
```

### Access Control Logic

**3-Tier Access Control:**

| Access Type | Điều kiện |
|-------------|-----------|
| `FREE` | Ai cũng đọc được (không cần mua) |
| `PREMIUM` | Phải mua hoặc có subscription |
| `SUBSCRIPTION_ONLY` | Chỉ có subscription mới đọc được |

```java
private boolean canUserAccessBook(User user, Book book) {
    // FREE books - anyone can read
    if (book.getAccessType() == Book.AccessType.FREE) {
        return true;
    }
    
    // Check purchase
    boolean hasPurchased = orderItemService
            .hasUserPurchasedBook(user.getUserId(), book.getBookId());
    
    if (hasPurchased) {
        return true;
    }
    
    // Check active subscription (for PREMIUM/SUBSCRIPTION_ONLY)
    // TODO: Implement subscription-based access
    
    return false;
}
```

---

## Flow 7.2: PDF Reader

### Template Structure

**File:** `pdf-viewer.html`

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${book.title + ' - PDF Reader'}">PDF Reader</title>
    <link rel="stylesheet" th:href="@{/user_template/css/reading.css}">
    <link rel="stylesheet" th:href="@{/css/reading-progress.css}">
</head>
<body>
<div class="reader-container">
    <!-- Header with book info and controls -->
    <div class="reader-header">
        <div class="book-info">
            <img th:src="${book.coverImageUrl}" alt="Cover">
            <div class="book-details">
                <h3 th:text="${book.title}">Book Title</h3>
                <p th:text="${book.authorNames}">Author</p>
            </div>
        </div>
        
        <div class="reader-controls">
            <!-- Page Navigation -->
            <button onclick="previousPage()">◀</button>
            <input type="number" id="pageInput" value="1" onchange="goToPage(this.value)">
            <span>/ <span id="totalPages">1</span></span>
            <button onclick="nextPage()">▶</button>
            
            <!-- Zoom Controls -->
            <button onclick="zoomOut()">−</button>
            <input type="number" id="zoomInput" value="100">%
            <button onclick="zoomIn()">+</button>
            
            <!-- Actions -->
            <button onclick="toggleDarkMode()">🌙 Chế độ tối</button>
            <button onclick="toggleBookmarksSidebar()">📑 Bookmarks</button>
            <button onclick="saveManualBookmark()">+ Bookmark</button>
        </div>
    </div>
    
    <!-- PDF Canvas -->
    <div class="pdf-viewer">
        <canvas id="pdfCanvas"></canvas>
    </div>
</div>

<!-- Toast Container for notifications -->
<div id="toast-container" class="toast-container"></div>

<!-- Data for JavaScript -->
<div id="pdf-data"
     th:data-book-id="${book.bookId}"
     th:data-book-asset-id="${asset.bookAssetId}"
     th:data-asset-path="${asset.readingUrl}"
     th:data-encoded-location="${encodedLocation}"
     style="display: none;"></div>

<!-- Scripts -->
<script src="https://unpkg.com/pdfjs-dist@3.11.174/build/pdf.min.js"></script>
<script th:src="@{/js/reading-progress-tracker.js}"></script>
<script th:src="@{/user_template/js/pdf-reader.js}"></script>
</body>
</html>
```

### PDF.js Integration

**File:** `pdf-reader.js`

```javascript
// Global variables
let pdfDoc = null;
let currentPage = 1;
let totalPages = 0;
let scale = 1.5;
let progressTracker = null;

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', async function() {
    const dataEl = document.getElementById('pdf-data');
    const bookId = dataEl.dataset.bookId;
    const bookAssetId = dataEl.dataset.bookAssetId;
    const assetPath = dataEl.dataset.assetPath;
    
    // Build streaming URL
    const streamUrl = `/reading/stream/${bookId}?assetId=${bookAssetId}&format=PDF`;
    
    // Load PDF
    pdfDoc = await pdfjsLib.getDocument(streamUrl).promise;
    totalPages = pdfDoc.numPages;
    
    // Initialize Anti-Skimming Tracker
    progressTracker = new ReadingProgressTracker({
        bookId: bookId,
        bookAssetId: bookAssetId,
        format: 'PDF',
        syncInterval: 30000,  // Sync every 30 seconds
        
        onSyncSuccess: (data) => {
            // Check both 'isSkimming' and 'skimming' (Jackson serialization)
            const isSkimmingDetected = data.isSkimming || data.skimming;
            if (isSkimmingDetected) {
                showSkimmingWarning(data.message);
            }
        }
    });
    progressTracker.start();
    
    // Render first page
    renderPage(1);
});

// Render specific page
async function renderPage(num) {
    const page = await pdfDoc.getPage(num);
    const viewport = page.getViewport({ scale });
    
    const canvas = document.getElementById('pdfCanvas');
    const ctx = canvas.getContext('2d');
    canvas.height = viewport.height;
    canvas.width = viewport.width;
    
    await page.render({ canvasContext: ctx, viewport }).promise;
    
    currentPage = num;
    updateUI();
    
    // Update progress tracker
    const percentage = (num / totalPages) * 100;
    progressTracker.updateProgress(`page-${num}`, percentage);
}

// Navigation
function nextPage() {
    if (currentPage < totalPages) renderPage(currentPage + 1);
}

function previousPage() {
    if (currentPage > 1) renderPage(currentPage - 1);
}

// Show skimming warning toast
function showSkimmingWarning(message) {
    const toast = document.createElement('div');
    toast.className = 'reading-toast warning';
    toast.innerHTML = `
        <i class="fas fa-exclamation-triangle"></i>
        <span>${message}</span>
    `;
    
    document.getElementById('toast-container').appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 100);
    setTimeout(() => toast.remove(), 5000);
}
```

---

## Flow 7.3: EPUB Reader

### ePub.js Integration

**File:** `epub-reader.js`

```javascript
let book = null;
let rendition = null;
let progressTracker = null;

document.addEventListener('DOMContentLoaded', async function() {
    const dataEl = document.getElementById('epub-data');
    const bookId = dataEl.dataset.bookId;
    const bookAssetId = dataEl.dataset.bookAssetId;
    const streamUrl = `/reading/stream/${bookId}?assetId=${bookAssetId}&format=EPUB`;
    
    // Download and initialize EPUB
    const response = await fetch(streamUrl);
    const blob = await response.blob();
    const arrayBuffer = await blob.arrayBuffer();
    
    book = ePub(arrayBuffer);
    rendition = book.renderTo('epub-viewer', {
        width: '100%',
        height: '100%',
        spread: 'auto'
    });
    
    // Wait for locations to be generated
    await book.ready;
    await book.locations.generate(1024);
    
    // Initialize Anti-Skimming Tracker
    progressTracker = new ReadingProgressTracker({
        bookId: bookId,
        bookAssetId: bookAssetId,
        format: 'EPUB',
        syncInterval: 30000,
        
        onSyncSuccess: (data) => {
            const isSkimmingDetected = data.isSkimming || data.skimming;
            if (isSkimmingDetected) {
                showSkimmingWarning(data.message);
            }
        }
    });
    progressTracker.start();
    
    // Track location changes
    rendition.on('relocated', (location) => {
        const cfi = location.start.cfi;
        const percentage = book.locations.percentageFromCfi(cfi) * 100;
        progressTracker.updateProgress(cfi, percentage);
    });
    
    // Display book
    rendition.display();
});

// Navigation
function nextPage() { rendition.next(); }
function prevPage() { rendition.prev(); }

// Table of Contents
async function loadTOC() {
    const toc = await book.loaded.navigation;
    // Render TOC items...
}

// Theme switching
function setTheme(theme) {
    switch(theme) {
        case 'dark':
            rendition.themes.override('color', '#fff');
            rendition.themes.override('background', '#1a1a1a');
            break;
        case 'sepia':
            rendition.themes.override('color', '#5b4636');
            rendition.themes.override('background', '#f4ecd8');
            break;
        default:
            rendition.themes.override('color', '#000');
            rendition.themes.override('background', '#fff');
    }
}
```

---

## Flow 7.4: Anti-Skimming & Progress Tracking

### Overview

Hệ thống **Anti-Skimming Validation** sử dụng chiến thuật **Time-Capping** để:
- Ngăn chặn việc "lướt nhanh" để đạt 100% progress
- Đảm bảo tiến độ tương ứng với thời gian đọc thực tế
- Yêu cầu đọc ít nhất 20% để có thể viết review

### Time-Capping Formula

```
MAX_PROGRESS_GAIN_PER_SECOND = 0.5%

Công thức:
- Client báo: "Tôi đã đọc từ 10% → 50% trong 30 giây"
- Max allowed: 30s × 0.5%/s = 15%
- Actual gain: min(40%, 15%) = 15%
- Final progress: 10% + 15% = 25%

Kết quả: Tiến độ được giới hạn ở 25% thay vì 50%
```

### Sequence Diagram

```
┌────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌──────────┐
│  Frontend  │    │ ProgressTracker  │    │   API Server    │    │ Database │
└─────┬──────┘    └────────┬─────────┘    └────────┬────────┘    └────┬─────┘
      │                    │                       │                   │
      │ User reads pages   │                       │                   │
      │───────────────────►│                       │                   │
      │                    │                       │                   │
      │                    │ Track active time     │                   │
      │                    │ (1 second intervals)  │                   │
      │                    │                       │                   │
      │                    │ Every 30s: POST /api/reading/sync         │
      │                    │──────────────────────►│                   │
      │                    │                       │                   │
      │                    │                       │ Calculate velocity│
      │                    │                       │ Apply time-capping│
      │                    │                       │                   │
      │                    │                       │ Save to DB        │
      │                    │                       │──────────────────►│
      │                    │                       │◄──────────────────│
      │                    │                       │                   │
      │                    │◄──────────────────────│                   │
      │                    │ Response: {           │                   │
      │                    │   isSkimming: true,   │                   │
      │                    │   currentProgress: 25%│                   │
      │                    │ }                     │                   │
      │                    │                       │                   │
      │ Show warning toast │                       │                   │
      │◄───────────────────│                       │                   │
```

### Backend Service Implementation

**File:** `ReadingProgressServiceImpl.java`

```java
@Service
@Transactional
public class ReadingProgressServiceImpl implements ReadingProgressService {
    
    // Tốc độ tối đa: 0.5% mỗi giây
    // = ~3.3 phút để đọc 100% sách
    private static final float MAX_PROGRESS_GAIN_PER_SECOND = 0.5f;
    
    // Ngưỡng để đánh giá: 20%
    private static final float REVIEW_ELIGIBILITY_THRESHOLD = 20.0f;
    
    @Override
    public ProgressSyncResponse syncProgress(User user, ProgressSyncRequest request) {
        // 1. Lấy hoặc tạo ReadingProgress
        ReadingProgress progress = getOrCreateProgress(user, request);
        
        float oldProgress = progress.getProgressPercentage();
        float clientProgress = request.getProgressPercentage();
        int deltaTime = request.getActiveTimeDelta();
        
        // 2. LUÔN LƯU VỊ TRÍ (đảm bảo UX - mở lại đúng chỗ)
        progress.setLastReadLocation(request.getCurrentLocationRaw());
        
        // 3. TÍNH TOÁN TIẾN ĐỘ với TIME-CAPPING
        float clientGain = clientProgress - oldProgress;
        float maxPossibleGain = deltaTime * MAX_PROGRESS_GAIN_PER_SECOND;
        float actualGain = Math.min(clientGain, maxPossibleGain);
        float finalProgress = Math.min(oldProgress + actualGain, 100.0f);
        
        boolean wasCapped = clientGain > maxPossibleGain;
        
        // 4. Lưu tiến độ đã kiểm soát
        progress.setProgressPercentage(finalProgress);
        
        // 5. Tích lũy thời gian đọc
        if (deltaTime > 0) {
            long totalTime = progress.getTotalActiveSeconds() + deltaTime;
            progress.setTotalActiveSeconds(totalTime);
        }
        
        // 6. Kiểm tra hoàn thành
        if (finalProgress >= 100.0f) {
            progress.setIsCompleted(true);
        }
        
        // 7. Lưu và trả về response
        ReadingProgress saved = repository.save(progress);
        
        String message = wasCapped 
            ? String.format("⚠️ Tiến độ điều chỉnh từ %.1f%% → %.1f%%. Hãy đọc chậm lại!", 
                           clientProgress, finalProgress)
            : "✅ Tiến độ đã được đồng bộ!";
        
        return ProgressSyncResponse.builder()
                .success(true)
                .isSkimming(wasCapped)
                .currentProgress(saved.getProgressPercentage())
                .totalActiveTime(saved.getTotalActiveSeconds())
                .canReview(saved.getProgressPercentage() >= REVIEW_ELIGIBILITY_THRESHOLD)
                .isCompleted(saved.getIsCompleted())
                .message(message)
                .build();
    }
}
```

### Frontend Progress Tracker

**File:** `reading-progress-tracker.js`

```javascript
class ReadingProgressTracker {
    constructor(options) {
        this.bookId = options.bookId;
        this.bookAssetId = options.bookAssetId;
        this.format = options.format || 'PDF';
        this.syncInterval = options.syncInterval || 30000;
        
        // State
        this.currentLocation = null;
        this.currentProgress = 0.0;
        this.activeTimeAccumulator = 0;
        this.isActive = false;
        
        // Callbacks
        this.onSyncSuccess = options.onSyncSuccess;
        this.onSkimmingDetected = options.onSkimmingDetected;
        
        this._setupActivityDetection();
    }
    
    _setupActivityDetection() {
        const events = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart'];
        let timer = null;
        
        const markActive = () => {
            this.isActive = true;
            if (timer) clearTimeout(timer);
            timer = setTimeout(() => this.isActive = false, 5000);
        };
        
        events.forEach(e => document.addEventListener(e, markActive, { passive: true }));
        
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) this.isActive = false;
        });
    }
    
    start() {
        // Accumulate active time every second
        this.activeTimer = setInterval(() => {
            if (this.isActive) this.activeTimeAccumulator++;
        }, 1000);
        
        // Auto sync
        this.syncTimer = setInterval(() => this.syncToServer(), this.syncInterval);
    }
    
    updateProgress(location, percentage) {
        this.currentLocation = location;
        this.currentProgress = percentage;
    }
    
    async syncToServer() {
        if (!this.currentLocation || this.activeTimeAccumulator === 0) return;
        
        const payload = {
            bookId: this.bookId,
            bookAssetId: this.bookAssetId,
            currentLocationRaw: this.currentLocation,
            progressPercentage: this.currentProgress,
            activeTimeDelta: this.activeTimeAccumulator,
            format: this.format
        };
        
        const response = await fetch('/api/reading/sync', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        const data = await response.json();
        
        // Reset accumulator
        this.activeTimeAccumulator = 0;
        
        // Check skimming (handle Jackson serialization)
        const isSkimmingDetected = data.isSkimming || data.skimming;
        if (isSkimmingDetected) {
            this._showToast('warning', 'Cảnh báo', data.message);
            if (this.onSkimmingDetected) this.onSkimmingDetected(data);
        }
        
        if (this.onSyncSuccess) this.onSyncSuccess(data);
    }
}
```

### DTO Definitions

**ProgressSyncRequest.java:**
```java
@Getter @Setter
public class ProgressSyncRequest {
    private String bookId;
    private String bookAssetId;
    private String currentLocationRaw;  // "page-15" or CFI
    private Float progressPercentage;   // 0-100
    private Integer activeTimeDelta;    // seconds
    private String format;              // PDF or EPUB
}
```

**ProgressSyncResponse.java:**
```java
@Getter @Setter @Builder
public class ProgressSyncResponse {
    private boolean success;
    
    @JsonProperty("isSkimming")
    private boolean isSkimming;
    
    private Float currentProgress;
    private Long totalActiveTime;
    private boolean canReview;
    
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    
    private String message;
    private Float readingVelocity;
    private String progressId;
}
```

> **Lưu ý:** Annotation `@JsonProperty("isSkimming")` đảm bảo Jackson serialize đúng tên field với tiền tố `is`.

---

## Flow 7.5: Bookmarks

### Bookmark Data Structure

```java
@Data
public static class BookmarkData {
    private String id;
    private String location;      // page-X or CFI
    private Integer pageNumber;   // For PDF
    private Float percentage;     // 0-100
    private String note;
    private LocalDateTime createdAt;
}
```

### API Endpoints

**Add Bookmark:**
```java
@PostMapping("/api/bookmarks/{bookId}")
@ResponseBody
public String addBookmark(@PathVariable String bookId,
                          @RequestParam String location,
                          @RequestParam(required = false) Integer pageNumber,
                          @RequestParam(required = false) Float percentage,
                          @RequestParam(required = false) String note) {
    User user = getCurrentUser();
    ReadingProgress progress = getOrCreateProgress(user, bookId);
    
    readingProgressService.addBookmark(
        progress.getProgressId(), 
        location, 
        pageNumber, 
        percentage, 
        note
    );
    
    return "{\"status\":\"success\"}";
}
```

**Get Bookmarks:**
```java
@GetMapping("/api/bookmarks/{bookId}")
@ResponseBody
public List<BookmarkData> getBookmarks(@PathVariable String bookId) {
    User user = getCurrentUser();
    ReadingProgress progress = getProgress(user, bookId);
    return readingProgressService.getBookmarks(progress.getProgressId());
}
```

---

## Reader Features

### ✅ PDF Reader Features
| Feature | Mô tả |
|---------|-------|
| Page Navigation | Prev/Next, Jump to page |
| Zoom Controls | Zoom in/out, Fit width/page |
| Dark Mode | Invert colors cho đọc ban đêm |
| Bookmarks | Lưu và quản lý bookmarks |
| Progress Tracking | Anti-skimming validation |
| Resume Reading | Mở lại đúng trang đã đọc |
| Keyboard Shortcuts | ←/→ navigation, +/- zoom |

### ✅ EPUB Reader Features
| Feature | Mô tả |
|---------|-------|
| Chapter Navigation | Next/Prev chapter |
| Table of Contents | Sidebar TOC |
| Font Size | Adjustable text size |
| Themes | Light/Dark/Sepia |
| Progress Tracking | Anti-skimming validation |
| Resume Reading | Mở lại đúng CFI |
| Responsive Layout | Adapt to screen size |

### ✅ Common Features
| Feature | Mô tả |
|---------|-------|
| Auto-save | Lưu tự động mỗi 30 giây |
| Time Tracking | Đếm thời gian đọc thực |
| Review Eligibility | Yêu cầu 20% để review |
| Skimming Detection | Cảnh báo đọc quá nhanh |
| Toast Notifications | Hiển thị thông báo |

---

## API Reference

### POST /api/reading/sync

**Request:**
```json
{
  "bookId": "book_13",
  "bookAssetId": "asset_13",
  "currentLocationRaw": "page-25",
  "progressPercentage": 18.25,
  "activeTimeDelta": 30,
  "format": "PDF"
}
```

**Response (Normal):**
```json
{
  "success": true,
  "isSkimming": false,
  "currentProgress": 18.25,
  "totalActiveTime": 450,
  "canReview": false,
  "isCompleted": false,
  "message": "✅ Tiến độ đã được đồng bộ thành công!",
  "readingVelocity": 0.27,
  "progressId": "prog_01"
}
```

**Response (Skimming Detected):**
```json
{
  "success": true,
  "isSkimming": true,
  "currentProgress": 15.23,
  "totalActiveTime": 34,
  "canReview": false,
  "isCompleted": false,
  "message": "⚠️ Tiến độ đã được điều chỉnh từ 18.2% → 15.2%. Hãy đọc chậm lại!",
  "readingVelocity": 0.61
}
```

### GET /api/reading/can-review/{bookId}

**Response:**
```json
{
  "canReview": true,
  "requiredProgress": 20.0,
  "message": "Bạn có thể đánh giá sách này"
}
```

---

## Technical Implementation

### File Structure

```
src/main/java/stu/datn/ebook_store/
├── controller/user/
│   ├── ReadingController.java          # View routing
│   └── ReadingProgressApiController.java # REST API
├── service/
│   ├── ReadingProgressService.java     # Interface
│   └── impl/ReadingProgressServiceImpl.java # Time-capping logic
├── dto/
│   ├── ProgressSyncRequest.java
│   └── ProgressSyncResponse.java
└── entity/
    └── ReadingProgress.java            # Entity with bookmarks

src/main/resources/
├── templates/user/reading/
│   ├── pdf-viewer.html
│   ├── epub-viewer.html
│   └── reader.html (format chooser)
├── static/
│   ├── js/reading-progress-tracker.js
│   ├── user_template/js/
│   │   ├── pdf-reader.js
│   │   └── epub-reader.js
│   └── css/reading-progress.css
```

### Database Schema

```sql
-- ReadingProgress entity
CREATE TABLE reading_progress (
    progress_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    book_asset_id VARCHAR(50),
    progress_percentage FLOAT DEFAULT 0,
    last_read_location VARCHAR(500),
    total_active_seconds BIGINT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    access_type VARCHAR(20),
    bookmarks_json TEXT,
    created_at DATETIME,
    last_read_at DATETIME,
    updated_at DATETIME,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    UNIQUE KEY unique_user_book (user_id, book_id)
);

CREATE INDEX idx_progress_user ON reading_progress(user_id);
CREATE INDEX idx_progress_book ON reading_progress(book_id);
```

---

**Last Updated:** 11/01/2026  
**Status:** ✅ COMPLETE  
**Version:** 2.0

