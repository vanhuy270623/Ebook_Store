# 📚 FLOW 02: ADMIN BOOK MANAGEMENT (Quản Lý Sách)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 2.1: List Books](#flow-21-list-books)
3. [Flow 2.2: Create Book](#flow-22-create-book)
4. [Flow 2.3: Edit Book](#flow-23-edit-book)
5. [Flow 2.4: Delete Book](#flow-24-delete-book)
6. [Flow 2.5: Upload Files](#flow-25-upload-files)
7. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Components
- **Controller**: `AdminBookController.java`
- **Service**: `BookService.java`, `FileStorageService.java`
- **Repository**: `BookRepository.java`, `AuthorRepository.java`, `BookCategoryRepository.java`
- **Entity**: `Book.java`, `Author.java`, `BookCategory.java`

### URLs
- `GET /admin/books` - Danh sách sách
- `GET /admin/books/create` - Form tạo sách
- `POST /admin/books/create` - Xử lý tạo sách
- `GET /admin/books/edit/{id}` - Form sửa sách
- `POST /admin/books/edit/{id}` - Xử lý sửa sách
- `POST /admin/books/delete/{id}` - Xóa sách

### File Storage
- **Cover Images**: `F:/datn_uploads/book_asset/image/covers/{category}/`
- **Source Files**: `F:/datn_uploads/book_asset/source/{category}/`
- **Preview Files**: `F:/datn_uploads/book_asset/preview/`

---

## Flow 2.1: List Books

### Sequence Diagram
```
Admin → Browser → AdminBookController → BookService → BookRepository → Database
  │       │              │                   │              │             │
  │  GET /admin/books                                                     │
  │───────────────────────►│                                              │
  │       │                │ getAllBooks()                                │
  │       │                ├──────────────────►│                          │
  │       │                │                   │ findAll()                │
  │       │                │                   ├─────────────►│           │
  │       │                │                   │              │ SELECT *  │
  │       │                │                   │              ├──────────►│
  │       │                │                   │              │◄──────────┤
  │       │                │                   │◄─────────────┤           │
  │       │                │◄──────────────────┤                          │
  │◄───────────────────────┤ (return admin/books/list.html)              │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String booksList(Model model) {
    List<Book> books = bookService.getAllBooks();
    model.addAttribute("books", books);
    model.addAttribute("totalBooks", books.size());
    return "admin/books/list";
}
```

**Service**:
```java
@Override
public List<Book> getAllBooks() {
    return bookRepository.findAll();
}
```

**SQL Query**:
```sql
SELECT b.*, bc.category_name, bc.category_slug,
       GROUP_CONCAT(a.author_name) as authors
FROM books b
LEFT JOIN book_category bc ON b.book_category_id = bc.book_category_id
LEFT JOIN book_authors ba ON b.book_id = ba.book_id
LEFT JOIN authors a ON ba.author_id = a.author_id
GROUP BY b.book_id
ORDER BY b.created_at DESC;
```

**Response Data**:
```json
{
  "books": [
    {
      "bookId": 1,
      "title": "Dac Nhan Tam",
      "isbn": "978-0-123456-78-9",
      "price": 99000.0,
      "discountPrice": 79000.0,
      "coverImageUrl": "/uploads/covers/tamly-kynangsong/dacnhantam.jpg",
      "bookCategory": {
        "categoryId": 1,
        "categoryName": "Tâm Lý - Kỹ Năng Sống"
      },
      "authors": ["Dale Carnegie"],
      "stockQuantity": 100,
      "isAvailable": true
    }
  ]
}
```

---

## Flow 2.2: Create Book

### Sequence Diagram
```
Admin → Browser → AdminBookController → BookService → FileStorageService → Database
  │       │              │                   │              │                │
  │  GET /admin/books/create                                                 │
  │───────────────────────►│                                                 │
  │       │                │ getCategories()                                 │
  │       │                │ getAuthors()                                    │
  │◄───────────────────────┤ (return form)                                  │
  │       │                │                                                 │
  │  POST /admin/books/create (BookDto + files)                             │
  │───────────────────────►│                                                 │
  │       │                │ createBook()                                    │
  │       │                ├──────────────────►│                             │
  │       │                │                   │ saveCoverImage()            │
  │       │                │                   ├─────────────►│              │
  │       │                │                   │◄─────────────┤              │
  │       │                │                   │ saveSourceFile()            │
  │       │                │                   ├─────────────►│              │
  │       │                │                   │◄─────────────┤              │
  │       │                │                   │ save(book)                  │
  │       │                │                   ├──────────────────────────►│ │
  │       │                │                   │◄──────────────────────────┤ │
  │       │                │◄──────────────────┤                             │
  │◄───────────────────────┤ redirect:/admin/books                          │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("book", new BookDto());
    model.addAttribute("categories", bookCategoryService.getAllCategories());
    model.addAttribute("authors", authorService.getAllAuthors());
    return "admin/books/create";
}

@PostMapping("/create")
public String createBook(@Valid @ModelAttribute("book") BookDto bookDto,
                        @RequestParam("coverImage") MultipartFile coverImage,
                        @RequestParam("sourceFile") MultipartFile sourceFile,
                        RedirectAttributes redirectAttributes) {
    try {
        // 1. Save cover image
        String coverImageUrl = fileStorageService.saveCoverImage(
            coverImage, 
            bookDto.getCategorySlug()
        );
        bookDto.setCoverImageUrl(coverImageUrl);
        
        // 2. Save source file
        String sourceFileUrl = fileStorageService.saveSourceFile(
            sourceFile,
            bookDto.getCategorySlug()
        );
        bookDto.setSourceFileUrl(sourceFileUrl);
        
        // 3. Create book
        Book book = bookService.createBook(bookDto);
        
        redirectAttributes.addFlashAttribute("success", 
            "Tạo sách thành công!");
        return "redirect:/admin/books";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi: " + e.getMessage());
        return "redirect:/admin/books/create";
    }
}
```

**Service Logic**:
```java
@Override
@Transactional
public Book createBook(BookDto bookDto) {
    // 1. Validate ISBN unique
    if (bookRepository.existsByIsbn(bookDto.getIsbn())) {
        throw new RuntimeException("ISBN đã tồn tại");
    }
    
    // 2. Create book entity
    Book book = new Book();
    book.setTitle(bookDto.getTitle());
    book.setIsbn(bookDto.getIsbn());
    book.setPrice(bookDto.getPrice());
    book.setDiscountPrice(bookDto.getDiscountPrice());
    book.setDescription(bookDto.getDescription());
    book.setCoverImageUrl(bookDto.getCoverImageUrl());
    book.setSourceFileUrl(bookDto.getSourceFileUrl());
    book.setStockQuantity(bookDto.getStockQuantity());
    book.setPublisher(bookDto.getPublisher());
    book.setPublicationYear(bookDto.getPublicationYear());
    book.setPageCount(bookDto.getPageCount());
    book.setLanguage(bookDto.getLanguage());
    book.setIsAvailable(true);
    
    // 3. Set category
    BookCategory category = bookCategoryRepository
        .findById(bookDto.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category không tồn tại"));
    book.setBookCategory(category);
    
    // 4. Set authors
    List<Author> authors = authorRepository
        .findAllById(bookDto.getAuthorIds());
    book.setAuthors(new HashSet<>(authors));
    
    // 5. Save book
    return bookRepository.save(book);
}
```

**File Storage Service**:
```java
public String saveCoverImage(MultipartFile file, String categorySlug) 
    throws IOException {
    
    // 1. Validate file
    if (file.isEmpty()) {
        throw new RuntimeException("File trống");
    }
    
    String contentType = file.getContentType();
    if (!contentType.startsWith("image/")) {
        throw new RuntimeException("File phải là ảnh");
    }
    
    // 2. Generate filename
    String originalFilename = file.getOriginalFilename();
    String extension = originalFilename.substring(
        originalFilename.lastIndexOf(".")
    );
    String filename = UUID.randomUUID().toString() + extension;
    
    // 3. Create directory
    Path uploadPath = Paths.get(uploadDir, "book_asset", "image", 
        "covers", categorySlug);
    Files.createDirectories(uploadPath);
    
    // 4. Save file
    Path filePath = uploadPath.resolve(filename);
    Files.copy(file.getInputStream(), filePath, 
        StandardCopyOption.REPLACE_EXISTING);
    
    // 5. Return relative URL
    return "/uploads/covers/" + categorySlug + "/" + filename;
}
```

**SQL Queries**:
```sql
-- Check ISBN exists
SELECT COUNT(*) FROM books WHERE isbn = ?

-- Insert book
INSERT INTO books (
    title, isbn, price, discount_price, description,
    cover_image_url, source_file_url, stock_quantity,
    publisher, publication_year, page_count, language,
    book_category_id, is_available, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, NOW(), NOW())

-- Insert book_authors (many-to-many)
INSERT INTO book_authors (book_id, author_id)
VALUES (?, ?), (?, ?), ...
```

---

## Flow 2.3: Edit Book

### Sequence Diagram
```
Admin → Browser → AdminBookController → BookService → FileStorageService → Database
  │       │              │                   │              │                │
  │  GET /admin/books/edit/{id}                                             │
  │───────────────────────►│                                                 │
  │       │                │ getBookById()                                   │
  │       │                ├──────────────────►│                             │
  │       │                │                   │ findById()                  │
  │       │                │                   ├──────────────────────────►│ │
  │       │                │                   │◄──────────────────────────┤ │
  │       │                │◄──────────────────┤                             │
  │◄───────────────────────┤ (return form with data)                        │
  │       │                │                                                 │
  │  POST /admin/books/edit/{id} (BookDto + files)                          │
  │───────────────────────►│                                                 │
  │       │                │ updateBook()                                    │
  │       │                ├──────────────────►│                             │
  │       │                │                   │ if (newCoverImage)          │
  │       │                │                   │   deleteCoverImage()        │
  │       │                │                   │   saveCoverImage()          │
  │       │                │                   │ if (newSourceFile)          │
  │       │                │                   │   deleteSourceFile()        │
  │       │                │                   │   saveSourceFile()          │
  │       │                │                   │ update(book)                │
  │       │                │                   ├──────────────────────────►│ │
  │       │                │                   │◄──────────────────────────┤ │
  │       │                │◄──────────────────┤                             │
  │◄───────────────────────┤ redirect:/admin/books                          │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/edit/{id}")
public String updateBook(@PathVariable Long id,
                        @Valid @ModelAttribute("book") BookDto bookDto,
                        @RequestParam(value = "coverImage", required = false) 
                            MultipartFile coverImage,
                        @RequestParam(value = "sourceFile", required = false) 
                            MultipartFile sourceFile,
                        RedirectAttributes redirectAttributes) {
    try {
        // 1. Get existing book
        Book existingBook = bookService.getBookById(id);
        
        // 2. Update cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            // Delete old image
            fileStorageService.deleteFile(existingBook.getCoverImageUrl());
            // Save new image
            String newCoverUrl = fileStorageService.saveCoverImage(
                coverImage, bookDto.getCategorySlug()
            );
            bookDto.setCoverImageUrl(newCoverUrl);
        } else {
            bookDto.setCoverImageUrl(existingBook.getCoverImageUrl());
        }
        
        // 3. Update source file if provided
        if (sourceFile != null && !sourceFile.isEmpty()) {
            fileStorageService.deleteFile(existingBook.getSourceFileUrl());
            String newSourceUrl = fileStorageService.saveSourceFile(
                sourceFile, bookDto.getCategorySlug()
            );
            bookDto.setSourceFileUrl(newSourceUrl);
        } else {
            bookDto.setSourceFileUrl(existingBook.getSourceFileUrl());
        }
        
        // 4. Update book
        bookService.updateBook(id, bookDto);
        
        redirectAttributes.addFlashAttribute("success", 
            "Cập nhật sách thành công!");
        return "redirect:/admin/books";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi: " + e.getMessage());
        return "redirect:/admin/books/edit/" + id;
    }
}
```

**SQL Update**:
```sql
UPDATE books SET
    title = ?,
    isbn = ?,
    price = ?,
    discount_price = ?,
    description = ?,
    cover_image_url = ?,
    source_file_url = ?,
    stock_quantity = ?,
    publisher = ?,
    publication_year = ?,
    page_count = ?,
    language = ?,
    book_category_id = ?,
    updated_at = NOW()
WHERE book_id = ?

-- Update authors (delete old + insert new)
DELETE FROM book_authors WHERE book_id = ?
INSERT INTO book_authors (book_id, author_id) VALUES (?, ?), ...
```

---

## Flow 2.4: Delete Book

### Sequence Diagram
```
Admin → Browser → AdminBookController → BookService → FileStorageService → Database
  │       │              │                   │              │                │
  │  POST /admin/books/delete/{id}                                          │
  │───────────────────────►│                                                 │
  │       │                │ deleteBook(id)                                  │
  │       │                ├──────────────────►│                             │
  │       │                │                   │ getBookById()               │
  │       │                │                   ├──────────────────────────►│ │
  │       │                │                   │◄──────────────────────────┤ │
  │       │                │                   │ deleteCoverImage()          │
  │       │                │                   ├─────────────►│              │
  │       │                │                   │ deleteSourceFile()          │
  │       │                │                   ├─────────────►│              │
  │       │                │                   │ delete(book)                │
  │       │                │                   ├──────────────────────────►│ │
  │       │                │                   │◄──────────────────────────┤ │
  │       │                │◄──────────────────┤                             │
  │◄───────────────────────┤ redirect:/admin/books                          │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deleteBook(@PathVariable Long id, 
                        RedirectAttributes redirectAttributes) {
    try {
        bookService.deleteBook(id);
        redirectAttributes.addFlashAttribute("success", 
            "Xóa sách thành công!");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi: " + e.getMessage());
    }
    return "redirect:/admin/books";
}
```

**Service**:
```java
@Override
@Transactional
public void deleteBook(Long id) {
    // 1. Get book
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
    
    // 2. Check if book has orders (prevent delete)
    if (orderItemRepository.existsByBook(book)) {
        throw new RuntimeException(
            "Không thể xóa sách đã có trong đơn hàng"
        );
    }
    
    // 3. Delete files
    fileStorageService.deleteFile(book.getCoverImageUrl());
    fileStorageService.deleteFile(book.getSourceFileUrl());
    
    // 4. Delete book (cascade will delete book_authors)
    bookRepository.delete(book);
}
```

**SQL**:
```sql
-- Check orders exist
SELECT COUNT(*) FROM order_items WHERE book_id = ?

-- Delete book_authors (cascade)
DELETE FROM book_authors WHERE book_id = ?

-- Delete book
DELETE FROM books WHERE book_id = ?
```

---

## Flow 2.5: Upload Files

### File Upload Process
```
1. Client selects file
   ↓
2. Validate file type & size
   ↓
3. Generate unique filename (UUID)
   ↓
4. Create directory (if not exists)
   ↓
5. Save file to disk
   ↓
6. Return relative URL
```

### Validation Rules

| File Type | Max Size | Allowed Extensions | Storage Path |
|-----------|----------|-------------------|--------------|
| Cover Image | 5 MB | .jpg, .jpeg, .png | /covers/{category}/ |
| Source File | 50 MB | .pdf, .epub | /source/{category}/ |
| Preview File | 10 MB | .pdf | /preview/ |

---

## Debugging Endpoints

### 1. List All Books

**cURL**:
```bash
curl -X GET http://localhost:8080/admin/books \
  -H "Cookie: JSESSIONID=xxx" \
  -L
```

**Expected Response**: HTML page with books table

### 2. Create Book (Postman)

**URL**: `POST http://localhost:8080/admin/books/create`

**Headers**:
```
Cookie: JSESSIONID=xxx
Content-Type: multipart/form-data
```

**Body (form-data)**:
```
title: Test Book
isbn: 978-0-123456-78-9
price: 100000
discountPrice: 80000
description: Test description
categoryId: 1
authorIds: 1,2
stockQuantity: 50
publisher: Test Publisher
publicationYear: 2024
pageCount: 300
language: Vietnamese
coverImage: [file]
sourceFile: [file]
```

### 3. Debug Endpoint - Check Book Details

**Controller** (add this):
```java
@GetMapping("/debug/book/{id}")
@ResponseBody
public Map<String, Object> debugBook(@PathVariable Long id) {
    Book book = bookService.getBookById(id);
    Map<String, Object> result = new HashMap<>();
    result.put("bookId", book.getBookId());
    result.put("title", book.getTitle());
    result.put("isbn", book.getIsbn());
    result.put("price", book.getPrice());
    result.put("category", book.getBookCategory().getCategoryName());
    result.put("authors", book.getAuthors().stream()
        .map(Author::getAuthorName)
        .collect(Collectors.toList()));
    result.put("coverImageUrl", book.getCoverImageUrl());
    result.put("sourceFileUrl", book.getSourceFileUrl());
    result.put("stockQuantity", book.getStockQuantity());
    result.put("isAvailable", book.getIsAvailable());
    return result;
}
```

**Test URL**: `http://localhost:8080/admin/debug/book/1`

### 4. Debug File Upload

**Controller**:
```java
@PostMapping("/debug/upload-test")
@ResponseBody
public Map<String, Object> testUpload(
    @RequestParam("file") MultipartFile file) {
    
    Map<String, Object> result = new HashMap<>();
    result.put("originalFilename", file.getOriginalFilename());
    result.put("contentType", file.getContentType());
    result.put("size", file.getSize());
    result.put("isEmpty", file.isEmpty());
    
    try {
        String savedUrl = fileStorageService.saveCoverImage(file, "test");
        result.put("savedUrl", savedUrl);
        result.put("success", true);
    } catch (Exception e) {
        result.put("error", e.getMessage());
        result.put("success", false);
    }
    
    return result;
}
```

### 5. Database Verification

**Check book created**:
```sql
SELECT b.*, bc.category_name,
       GROUP_CONCAT(a.author_name) as authors
FROM books b
LEFT JOIN book_category bc ON b.book_category_id = bc.book_category_id
LEFT JOIN book_authors ba ON b.book_id = ba.book_id
LEFT JOIN authors a ON ba.author_id = a.author_id
WHERE b.book_id = ?
GROUP BY b.book_id;
```

**Check files exist**:
```sql
SELECT book_id, title, cover_image_url, source_file_url
FROM books
WHERE book_id = ?;
```

**Verify on file system**:
```bash
# Check cover image
Test-Path "F:\datn_uploads\book_asset\image\covers\{category}\{filename}"

# Check source file
Test-Path "F:\datn_uploads\book_asset\source\{category}\{filename}"
```

### 6. Common Issues & Solutions

| Issue | Debug Method | Solution |
|-------|-------------|----------|
| File upload failed | Check file size, type | Adjust `spring.servlet.multipart.max-file-size` |
| Cover image not displayed | Check file path | Verify static resource mapping |
| ISBN duplicate error | Check database | Use unique ISBN |
| Category not found | Check category_id | Verify category exists |
| Authors not linked | Check book_authors table | Verify ManyToMany mapping |

### 7. Logging Configuration

**application.properties**:
```properties
# File upload logging
logging.level.com.example.ebook_store.service.FileStorageService=DEBUG
logging.level.com.example.ebook_store.controller.AdminBookController=DEBUG
logging.level.com.example.ebook_store.service.BookService=DEBUG

# Multipart logging
logging.level.org.springframework.web.multipart=DEBUG

# SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# File upload limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### 8. Breakpoint Locations

**AdminBookController**:
- Line: `bookService.createBook(bookDto)`
- Line: `fileStorageService.saveCoverImage(coverImage, ...)`

**BookService**:
- Line: `bookRepository.existsByIsbn(bookDto.getIsbn())`
- Line: `bookRepository.save(book)`

**FileStorageService**:
- Line: `Files.copy(file.getInputStream(), filePath, ...)`

---

## Test Scenarios

### Scenario 1: Tạo sách với đầy đủ thông tin
1. Login admin
2. Navigate to `/admin/books/create`
3. Fill form + upload files
4. Submit
5. Verify: Book xuất hiện trong list
6. Verify: Files tồn tại trên disk
7. Verify: Database records correct

### Scenario 2: Tạo sách với ISBN trùng
1. Tạo sách với ISBN đã tồn tại
2. Verify: Error message hiển thị
3. Verify: Book không được tạo

### Scenario 3: Upload file sai định dạng
1. Upload file .txt làm cover image
2. Verify: Validation error
3. Verify: File không được save

### Scenario 4: Sửa sách (update cover image)
1. Edit book existing
2. Upload cover image mới
3. Submit
4. Verify: Old image deleted
5. Verify: New image saved
6. Verify: Database updated

### Scenario 5: Xóa sách
1. Delete book chưa có order
2. Verify: Files deleted from disk
3. Verify: Database record deleted
4. Verify: book_authors entries deleted

### Scenario 6: Xóa sách có order (should fail)
1. Try delete book có trong order
2. Verify: Error message
3. Verify: Book NOT deleted

---

**Last Updated**: 30/11/2025
**Version**: 2.0

