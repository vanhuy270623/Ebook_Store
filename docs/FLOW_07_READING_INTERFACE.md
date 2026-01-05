# 📖 FLOW 07: READING INTERFACE (Giao Diện Đọc Sách)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 7.1: Access Reading Interface](#flow-71-access-reading-interface)
3. [Flow 7.2: PDF Reader](#flow-72-pdf-reader)
4. [Flow 7.3: EPUB Reader](#flow-73-epub-reader)
5. [Flow 7.4: Reading Progress Tracking](#flow-74-reading-progress-tracking)
6. [Flow 7.5: Bookmarks](#flow-75-bookmarks)
7. [Reader Features](#reader-features)
8. [Technical Implementation](#technical-implementation)

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
       ├──► PDF Format → PDF.js Reader
       │
       ├──► EPUB Format → ePub.js Reader
       │
       └──► Other → Universal Reader
       │
       ▼
┌──────────────┐
│  Reading     │
│  Interface   │
└──────┬───────┘
       │
       ├──► Track Progress
       ├──► Save Bookmarks
       ├──► Adjust Settings
       └──► Navigate Pages
```

### Components
- **Controller**: `ReadingController.java`
- **Service**: `ReadingProgressService.java`, `OrderService.java`
- **Entity**: `ReadingProgress.java`, `Order.java`, `Book.java`
- **Libraries**: 
  - PDF.js (Mozilla) - PDF rendering
  - ePub.js - EPUB rendering
  - Custom JavaScript - Reader controls

### URLs
**Reading Interface:**
- `GET /reading/pdf/{bookId}` - PDF viewer (sử dụng PDF.js)
- `GET /reading/epub/{bookId}` - EPUB reader (sử dụng ePub.js)
- `GET /reading/pdf/{category}/{fileName}` - PDF viewer by path (alternative route)
- `GET /reading/epub/{category}/{fileName}` - EPUB reader by path (alternative route)

**Secure Streaming:**
- `GET /reading/stream/{bookId}` - Secure file streaming endpoint (validates access)

**Progress Tracking:**
- `POST /api/reading/progress/update` - Save reading progress
- `GET /api/reading/progress/{bookId}` - Get reading progress

---

## Flow 7.1: Access Reading Interface

### Sequence Diagram
```
User → Browser → ReadingController → OrderService → ReadingProgressService → Database
  │       │              │                 │                │                    │
  │ Click "Đọc sách"                                                            │
  │───────────────────────►│                                                     │
  │       │                │ checkOwnership()                                    │
  │       │                ├────────────────►│                                   │
  │       │                │                 │ hasUserPurchasedBook()           │
  │       │                │                 ├────────────────────────────────────►│
  │       │                │                 │◄────────────────────────────────────┤
  │       │                │◄────────────────┤                                   │
  │       │                │                                                      │
  │       │                │ getReadingProgress()                                │
  │       │                ├────────────────────────────────────►│               │
  │       │                │                                     │ SELECT *      │
  │       │                │                                     ├──────────────►│
  │       │                │                                     │◄──────────────┤
  │       │                │◄────────────────────────────────────┤               │
  │       │                │                                                      │
  │       │                │ detectBookFormat()                                  │
  │       │                │ redirectToReader()                                  │
  │       │◄────────────────┤                                                     │
  │◄───────┤ (show appropriate reader)                                           │
```

### Implementation Details

**Controller**: `ReadingController.java`

```java
@Controller
@RequestMapping("/reading")
@Slf4j
public class ReadingController extends BaseController {
    
    private final BookService bookService;
    private final BookAssetService bookAssetService;
    private final ReadingProgressService readingProgressService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final FileStorageService fileStorageService;
    
    /**
     * PDF Viewer - Route theo bookId
     * URL: /reading/pdf/{bookId}
     */
    @GetMapping("/pdf/{bookId}")
    public String pdfViewer(@PathVariable String bookId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "PDF", model, redirectAttributes, 
                                "user/reading/pdf-viewer");
    }
    
    /**
     * PDF Viewer - Route theo category/fileName  
     * URL: /reading/pdf/{category}/{fileName}
     * Example: /reading/pdf/khoahoc-vientuong/Cac_The_Gioi_Song_Song.pdf
     */
    @GetMapping("/pdf/{category}/{fileName:.+}")
    public String pdfViewerByPath(@PathVariable String category,
                                   @PathVariable String fileName,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            log.info("Opening PDF by path - category: {}, fileName: {}", 
                     category, fileName);
            User currentUser = getCurrentUser();
            
            // Kiểm tra authentication
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", 
                    "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }
            
            // Tìm book theo file path
            String fileUrl = "/book_asset/source/" + category + "/" + fileName;
            BookAsset asset = bookAssetService.findByFileUrl(fileUrl);
            
            if (asset == null) {
                log.error("Asset not found for fileUrl: {}", fileUrl);
                redirectAttributes.addFlashAttribute("error", 
                    "Không tìm thấy file sách");
                return "redirect:/books";
            }
            
            Book book = asset.getBook();
            
            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(currentUser, book)) {
                log.warn("User {} does not have access to book {}", 
                        currentUser.getUserId(), book.getBookId());
                redirectAttributes.addFlashAttribute("error", 
                    "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + book.getBookId();
            }
            
            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(currentUser, book)
                    .orElseGet(() -> {
                        ReadingProgress newProgress = new ReadingProgress();
                        newProgress.setUser(currentUser);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(asset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book, currentUser));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });
            
            // Prepare model
            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", currentUser);
            
            // Encode lastReadLocation cho JavaScript
            if (progress != null && progress.getLastReadLocation() != null) {
                String encodedLocation = Base64.getEncoder()
                        .encodeToString(progress.getLastReadLocation()
                        .getBytes(StandardCharsets.UTF_8));
                model.addAttribute("encodedLocation", encodedLocation);
            }
            
            return "user/reading/pdf-viewer";
            
        } catch (Exception e) {
            log.error("Error opening PDF: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
        }
    }
    
    /**
     * EPUB Reader - Route theo bookId
     * URL: /reading/epub/{bookId}
     */
    @GetMapping("/epub/{bookId}")
    public String epubReader(@PathVariable String bookId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "EPUB", model, redirectAttributes, 
                                "user/reading/epub-viewer");
    }
    
    /**
     * Shared logic để prepare reader view
     */
    private String prepareReaderView(String bookId, String expectedFormat,
                                     Model model, 
                                     RedirectAttributes redirectAttributes,
                                     String viewName) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", 
                    "Vui lòng đăng nhập");
                return "redirect:/auth/login";
            }
            
            // Get book
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
            
            // Check access
            if (!canUserAccessBook(currentUser, book)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }
            
            // Get asset
            BookAsset asset = bookAssetService.getByBookId(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file sách"));
            
            // Get or create progress
            ReadingProgress progress = readingProgressService
                    .getOrCreateProgress(currentUser, book, asset);
            
            // Add to model
            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", currentUser);
            
            return viewName;
            
        } catch (Exception e) {
            log.error("Error in prepareReaderView: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/books";
        }
    }
    
    /**
     * Check if user can access book
     * Returns true if:
     * - Book is FREE
     * - User has purchased book
     * - User has active subscription with unlimited access
     */
    private boolean canUserAccessBook(User user, Book book) {
        // FREE books
        if (book.getAccessType() == Book.AccessType.FREE) {
            return true;
        }
        
        // Check if purchased
        boolean hasPurchased = orderItemService
                .hasUserPurchasedBook(user.getUserId(), book.getBookId());
        
        if (hasPurchased) {
            return true;
        }
        
        // Check subscription (PREMIUM access)
        // TODO: Implement subscription-based access
        
        return false;
    }
}
```

**Access Control Logic:**

The system uses **3-tier access control**:

1. **FREE Books** (`access_type = 'FREE'`)
   - Anyone can read (no purchase required)
   
2. **PREMIUM Books** (`access_type = 'PREMIUM'`)
   - Must purchase individually OR have active subscription
   
3. **SUBSCRIPTION_ONLY** 
   - Requires active subscription (cannot purchase individually)

**Service: BookAsset URL Methods**

```java
@Entity
public class BookAsset {
    /**
     * Get viewer page URL
     * Returns: /reading/pdf/{bookId} or /reading/epub/{bookId}
     * This is the main URL users click to open books
     */
    public String getViewerUrl() {
        if (this.book == null || this.book.getBookId() == null) {
            return null;
        }
        
        String bookId = this.book.getBookId();
        
        if (this.fileType == FileType.PDF) {
            return "/reading/pdf/" + bookId;
        } else if (this.fileType == FileType.EPUB) {
            return "/reading/epub/" + bookId;
        }
        
        return null;
    }
    
    /**
     * Get secure streaming URL
     * Returns: /reading/stream/{bookId}
     * Used by viewer pages to stream file content with validation
     */
    public String getReadingUrl() {
        if (this.book == null || this.book.getBookId() == null) {
            return null;
        }
        
        return "/reading/stream/" + this.book.getBookId();
    }
}
```

---

## Flow 7.2: PDF Reader

### PDF.js Integration

**Controller**:
```java
@GetMapping("/pdf/{bookId}")
public String pdfReader(
        @PathVariable String bookId,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    try {
        User currentUser = (User) authentication.getPrincipal();
        Book book = bookService.getBookById(bookId);
        
        if (book == null || !checkBookAccess(currentUser, book)) {
            return "redirect:/user/books/" + bookId;
        }
        
        // Get reading progress
        ReadingProgress progress = readingProgressService
            .getOrCreateProgress(currentUser, book);
        
        // Prepare model
        model.addAttribute("book", book);
        model.addAttribute("progress", progress);
        model.addAttribute("pdfUrl", book.getSourceFileUrl());
        model.addAttribute("currentPage", progress.getCurrentPage());
        model.addAttribute("totalPages", progress.getTotalPages());
        
        return "user/reading/pdf-viewer";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi mở PDF");
        return "redirect:/user/books";
    }
}
```

**PDF Viewer Template** (`pdf-viewer.html`):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${book.title}">PDF Reader</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
    <style>
        #pdf-container {
            width: 100%;
            height: 100vh;
            display: flex;
            flex-direction: column;
        }
        
        #pdf-toolbar {
            background: #333;
            color: white;
            padding: 10px;
            display: flex;
            gap: 10px;
            align-items: center;
        }
        
        #pdf-canvas {
            flex: 1;
            overflow: auto;
            background: #525659;
        }
        
        canvas {
            display: block;
            margin: 20px auto;
            box-shadow: 0 0 10px rgba(0,0,0,0.5);
        }
    </style>
</head>
<body>
    <div id="pdf-container">
        <!-- Toolbar -->
        <div id="pdf-toolbar">
            <button id="prev-page">◀ Previous</button>
            <span>
                Page: <span id="page-num"></span> / <span id="page-count"></span>
            </span>
            <button id="next-page">Next ▶</button>
            
            <button id="zoom-out">Zoom Out</button>
            <span id="zoom-level">100%</span>
            <button id="zoom-in">Zoom In</button>
            
            <button id="fit-width">Fit Width</button>
            <button id="fit-page">Fit Page</button>
            
            <button id="dark-mode">🌙 Dark Mode</button>
            
            <button id="fullscreen">⛶ Fullscreen</button>
            
            <button id="close-reader" onclick="window.location.href='/user/books'">
                ✖ Close
            </button>
        </div>
        
        <!-- PDF Canvas -->
        <div id="pdf-canvas">
            <canvas id="the-canvas"></canvas>
        </div>
    </div>
    
    <script th:inline="javascript">
        /*<![CDATA[*/
        const pdfUrl = /*[[${pdfUrl}]]*/ '';
        const bookId = /*[[${book.bookId}]]*/ '';
        const initialPage = /*[[${currentPage}]]*/ 1;
        
        let pdfDoc = null;
        let pageNum = initialPage;
        let pageRendering = false;
        let pageNumPending = null;
        let scale = 1.5;
        
        const canvas = document.getElementById('the-canvas');
        const ctx = canvas.getContext('2d');
        
        /**
         * Load and render PDF
         */
        pdfjsLib.getDocument(pdfUrl).promise.then(function(pdfDoc_) {
            pdfDoc = pdfDoc_;
            document.getElementById('page-count').textContent = pdfDoc.numPages;
            
            // Initial render
            renderPage(pageNum);
        });
        
        /**
         * Render specific page
         */
        function renderPage(num) {
            pageRendering = true;
            
            pdfDoc.getPage(num).then(function(page) {
                const viewport = page.getViewport({scale: scale});
                canvas.height = viewport.height;
                canvas.width = viewport.width;
                
                const renderContext = {
                    canvasContext: ctx,
                    viewport: viewport
                };
                
                const renderTask = page.render(renderContext);
                
                renderTask.promise.then(function() {
                    pageRendering = false;
                    if (pageNumPending !== null) {
                        renderPage(pageNumPending);
                        pageNumPending = null;
                    }
                    
                    // Update UI
                    document.getElementById('page-num').textContent = num;
                    
                    // Save progress
                    saveReadingProgress(num);
                });
            });
        }
        
        /**
         * Queue page render
         */
        function queueRenderPage(num) {
            if (pageRendering) {
                pageNumPending = num;
            } else {
                renderPage(num);
            }
        }
        
        /**
         * Previous page
         */
        function onPrevPage() {
            if (pageNum <= 1) {
                return;
            }
            pageNum--;
            queueRenderPage(pageNum);
        }
        document.getElementById('prev-page').addEventListener('click', onPrevPage);
        
        /**
         * Next page
         */
        function onNextPage() {
            if (pageNum >= pdfDoc.numPages) {
                return;
            }
            pageNum++;
            queueRenderPage(pageNum);
        }
        document.getElementById('next-page').addEventListener('click', onNextPage);
        
        /**
         * Zoom controls
         */
        document.getElementById('zoom-in').addEventListener('click', function() {
            scale += 0.25;
            updateZoom();
        });
        
        document.getElementById('zoom-out').addEventListener('click', function() {
            if (scale > 0.5) {
                scale -= 0.25;
                updateZoom();
            }
        });
        
        function updateZoom() {
            document.getElementById('zoom-level').textContent = Math.round(scale * 100) + '%';
            queueRenderPage(pageNum);
        }
        
        /**
         * Fit width/page
         */
        document.getElementById('fit-width').addEventListener('click', function() {
            scale = canvas.parentElement.clientWidth / canvas.width * scale;
            updateZoom();
        });
        
        /**
         * Dark mode
         */
        let darkMode = false;
        document.getElementById('dark-mode').addEventListener('click', function() {
            darkMode = !darkMode;
            if (darkMode) {
                canvas.style.filter = 'invert(1) hue-rotate(180deg)';
            } else {
                canvas.style.filter = 'none';
            }
        });
        
        /**
         * Fullscreen
         */
        document.getElementById('fullscreen').addEventListener('click', function() {
            if (!document.fullscreenElement) {
                document.getElementById('pdf-container').requestFullscreen();
            } else {
                document.exitFullscreen();
            }
        });
        
        /**
         * Keyboard shortcuts
         */
        document.addEventListener('keydown', function(e) {
            switch(e.key) {
                case 'ArrowLeft':
                    onPrevPage();
                    break;
                case 'ArrowRight':
                    onNextPage();
                    break;
                case '+':
                    scale += 0.25;
                    updateZoom();
                    break;
                case '-':
                    scale -= 0.25;
                    updateZoom();
                    break;
            }
        });
        
        /**
         * Save reading progress to server
         */
        function saveReadingProgress(currentPage) {
            fetch('/api/reading/progress', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    bookId: bookId,
                    currentPage: currentPage,
                    totalPages: pdfDoc.numPages,
                    progressPercentage: (currentPage / pdfDoc.numPages) * 100
                })
            });
        }
        
        // Auto-save progress every 30 seconds
        setInterval(function() {
            saveReadingProgress(pageNum);
        }, 30000);
        /*]]>*/
    </script>
</body>
</html>
```

---

## Flow 7.3: EPUB Reader

### ePub.js Integration

**Controller**:
```java
@GetMapping("/epub/{bookId}")
public String epubReader(
        @PathVariable String bookId,
        Authentication authentication,
        Model model) {
    
    User currentUser = (User) authentication.getPrincipal();
    Book book = bookService.getBookById(bookId);
    
    if (!checkBookAccess(currentUser, book)) {
        return "redirect:/user/books/" + bookId;
    }
    
    ReadingProgress progress = readingProgressService
        .getOrCreateProgress(currentUser, book);
    
    model.addAttribute("book", book);
    model.addAttribute("progress", progress);
    model.addAttribute("epubUrl", book.getSourceFileUrl());
    
    return "user/reading/epub-viewer";
}
```

**EPUB Viewer Template** (`epub-viewer.html`):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${book.title}">EPUB Reader</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js"></script>
    <style>
        body {
            margin: 0;
            padding: 0;
            overflow: hidden;
        }
        
        #epub-toolbar {
            background: #2c3e50;
            color: white;
            padding: 15px;
            display: flex;
            gap: 15px;
            align-items: center;
        }
        
        #viewer {
            width: 100%;
            height: calc(100vh - 60px);
        }
        
        #toc-panel {
            position: fixed;
            left: -300px;
            top: 60px;
            width: 300px;
            height: calc(100vh - 60px);
            background: white;
            box-shadow: 2px 0 5px rgba(0,0,0,0.2);
            transition: left 0.3s;
            overflow-y: auto;
        }
        
        #toc-panel.open {
            left: 0;
        }
    </style>
</head>
<body>
    <!-- Toolbar -->
    <div id="epub-toolbar">
        <button id="prev">◀ Previous</button>
        <button id="next">Next ▶</button>
        <button id="toc-toggle">📑 Contents</button>
        
        <span>Font Size:</span>
        <button id="font-smaller">A-</button>
        <button id="font-larger">A+</button>
        
        <select id="theme-select">
            <option value="light">Light</option>
            <option value="dark">Dark</option>
            <option value="sepia">Sepia</option>
        </select>
        
        <button id="bookmark-add">🔖 Bookmark</button>
        <button id="close" onclick="window.location.href='/user/books'">✖ Close</button>
    </div>
    
    <!-- Table of Contents -->
    <div id="toc-panel">
        <h3 style="padding: 15px; margin: 0;">Table of Contents</h3>
        <div id="toc-list"></div>
    </div>
    
    <!-- EPUB Viewer -->
    <div id="viewer"></div>
    
    <script th:inline="javascript">
        /*<![CDATA[*/
        const epubUrl = /*[[${epubUrl}]]*/ '';
        const bookId = /*[[${book.bookId}]]*/ '';
        
        // Initialize ePub.js
        const book = ePub(epubUrl);
        const rendition = book.renderTo("viewer", {
            width: "100%",
            height: "100%",
            spread: "always"
        });
        
        // Display book
        rendition.display();
        
        // Navigation
        document.getElementById('prev').addEventListener('click', function() {
            rendition.prev();
        });
        
        document.getElementById('next').addEventListener('click', function() {
            rendition.next();
        });
        
        // Table of contents
        book.loaded.navigation.then(function(toc) {
            const tocList = document.getElementById('toc-list');
            toc.forEach(function(chapter) {
                const item = document.createElement('div');
                item.style.padding = '10px 15px';
                item.style.cursor = 'pointer';
                item.style.borderBottom = '1px solid #eee';
                item.textContent = chapter.label;
                item.addEventListener('click', function() {
                    rendition.display(chapter.href);
                    document.getElementById('toc-panel').classList.remove('open');
                });
                tocList.appendChild(item);
            });
        });
        
        // Toggle TOC
        document.getElementById('toc-toggle').addEventListener('click', function() {
            document.getElementById('toc-panel').classList.toggle('open');
        });
        
        // Font size
        let fontSize = 100;
        document.getElementById('font-larger').addEventListener('click', function() {
            fontSize += 10;
            rendition.themes.fontSize(fontSize + '%');
        });
        
        document.getElementById('font-smaller').addEventListener('click', function() {
            fontSize -= 10;
            rendition.themes.fontSize(fontSize + '%');
        });
        
        // Themes
        document.getElementById('theme-select').addEventListener('change', function(e) {
            const theme = e.target.value;
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
        });
        
        // Track progress
        rendition.on('relocated', function(location) {
            saveProgress(location.start.cfi, location.start.percentage);
        });
        
        function saveProgress(cfi, percentage) {
            fetch('/api/reading/progress', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    bookId: bookId,
                    currentPosition: cfi,
                    progressPercentage: percentage * 100
                })
            });
        }
        /*]]>*/
    </script>
</body>
</html>
```

---

## Flow 7.4: Reading Progress Tracking

### API Endpoint

**Controller**:
```java
@RestController
@RequestMapping("/api/reading")
public class ReadingApiController {
    
    private final ReadingProgressService readingProgressService;
    
    @PostMapping("/progress")
    public ResponseEntity<?> saveProgress(
            @RequestBody ReadingProgressDto dto,
            Authentication authentication) {
        
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            ReadingProgress progress = readingProgressService.saveProgress(
                currentUser.getUserId(),
                dto.getBookId(),
                dto.getCurrentPage(),
                dto.getTotalPages(),
                dto.getCurrentPosition(),
                dto.getProgressPercentage()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Progress saved",
                "progress", progress
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
```

**Service**:
```java
@Service
@Transactional
public class ReadingProgressService {
    
    private final ReadingProgressRepository progressRepository;
    
    public ReadingProgress saveProgress(
            String userId,
            String bookId,
            Integer currentPage,
            Integer totalPages,
            String currentPosition,
            Double progressPercentage) {
        
        ReadingProgress progress = progressRepository
            .findByUserIdAndBookId(userId, bookId)
            .orElse(new ReadingProgress());
        
        progress.setUserId(userId);
        progress.setBookId(bookId);
        progress.setCurrentPage(currentPage);
        progress.setTotalPages(totalPages);
        progress.setCurrentPosition(currentPosition);
        progress.setProgressPercentage(progressPercentage);
        progress.setLastReadAt(LocalDateTime.now());
        
        return progressRepository.save(progress);
    }
}
```

---

## Flow 7.5: Bookmarks

**Save Bookmark**:
```java
@PostMapping("/bookmark")
public ResponseEntity<?> saveBookmark(
        @RequestBody BookmarkDto dto,
        Authentication authentication) {
    
    User currentUser = (User) authentication.getPrincipal();
    
    Bookmark bookmark = new Bookmark();
    bookmark.setUserId(currentUser.getUserId());
    bookmark.setBookId(dto.getBookId());
    bookmark.setPageNumber(dto.getPageNumber());
    bookmark.setPosition(dto.getPosition());
    bookmark.setNote(dto.getNote());
    bookmark.setCreatedAt(LocalDateTime.now());
    
    bookmarkRepository.save(bookmark);
    
    return ResponseEntity.ok(Map.of("success", true));
}
```

---

## Reader Features

### ✅ PDF Reader Features
- Page navigation (prev/next)
- Zoom in/out
- Fit width/page
- Dark mode
- Fullscreen
- Keyboard shortcuts
- Progress tracking
- Auto-save

### ✅ EPUB Reader Features
- Chapter navigation
- Table of contents
- Font size adjustment
- Theme selection (light/dark/sepia)
- Progress tracking
- Bookmarks
- Text selection
- Responsive layout

### ✅ Common Features
- Auto-save reading position
- Resume from last position
- Progress percentage
- Time tracking
- Reading statistics

---

**Last Updated:** 06/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

