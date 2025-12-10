# ✍️ FLOW 13: ADMIN AUTHOR MANAGEMENT (Quản Lý Tác Giả)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 13.1: List Authors](#flow-131-list-authors)
3. [Flow 13.2: Create Author](#flow-132-create-author)
4. [Flow 13.3: View Author Details](#flow-133-view-author-details)
5. [Flow 13.4: Edit Author](#flow-134-edit-author)
6. [Flow 13.5: Delete Author](#flow-135-delete-author)
7. [Flow 13.6: Search Authors](#flow-136-search-authors)
8. [Flow 13.7: Upload Author Avatar](#flow-137-upload-author-avatar)
9. [Error Handling](#error-handling)

---

## Tổng Quan

### Components
- **Controller**: `AdminAuthorController.java`
- **Service**: `AuthorService.java`, `FileStorageService.java`
- **Repository**: `AuthorRepository.java`, `BookAuthorRepository.java`
- **Entity**: `Author.java`, `BookAuthor.java`
- **DTOs**: `AuthorCreateRequest.java`, `AuthorUpdateRequest.java`

### URLs
- `GET /admin/authors` - Danh sách tác giả
- `GET /admin/authors/create` - Form tạo tác giả
- `POST /admin/authors/create` - Xử lý tạo tác giả
- `GET /admin/authors/view/{id}` - Chi tiết tác giả
- `GET /admin/authors/edit/{id}` - Form sửa tác giả
- `POST /admin/authors/edit/{id}` - Xử lý sửa tác giả
- `POST /admin/authors/delete/{id}` - Xóa tác giả
- `GET /api/admin/authors/check-name` - Kiểm tra tên trùng

### Author Structure
```
Author:
  - authorId: String (auto: "author_01", "author_02", ...)
  - name: String (unique)
  - biography: Text
  - avatarUrl: String
  - createdAt: LocalDateTime
  - books: List<Book> (Many-to-Many through book_authors)
```

---

## Flow 13.1: List Authors

### Sequence Diagram
```
Admin → Browser → AdminAuthorController → AuthorService → AuthorRepository → Database
  │       │               │                    │                 │              │
  │  GET /admin/authors?search={query}                                          │
  │────────────────────────►│                                                   │
  │       │                 │ searchAuthorsByName() OR getAllAuthors()          │
  │       │                 ├────────────────────►│                             │
  │       │                 │                     │ findByNameContaining()      │
  │       │                 │                     ├────────────────►│           │
  │       │                 │                     │                 │ SELECT    │
  │       │                 │                     │                 ├──────────►│
  │       │                 │                     │                 │◄──────────┤
  │       │                 │                     │◄────────────────┤           │
  │       │                 │◄────────────────────┤                             │
  │◄────────────────────────┤ (return admin/authors/list.html)                 │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String authorsList(@RequestParam(required = false) String search, Model model) {
    List<Author> authors;
    
    if (search != null && !search.trim().isEmpty()) {
        authors = authorService.searchAuthorsByName(search);
        model.addAttribute("search", search);
    } else {
        authors = authorService.getAllAuthors();
    }
    
    model.addAttribute("authors", authors);
    model.addAttribute("totalAuthors", authors.size());
    
    return "admin/authors/list";
}
```

**Service**:
```java
@Override
public List<Author> getAllAuthors() {
    return authorRepository.findAll();
}

@Override
public List<Author> searchAuthorsByName(String name) {
    return authorRepository.findByNameContainingIgnoreCase(name);
}
```

**SQL Query**:
```sql
-- Get all authors with book count
SELECT a.author_id, a.name, a.biography, a.avatar_url, a.created_at,
       COUNT(ba.book_id) as book_count
FROM authors a
LEFT JOIN book_authors ba ON a.author_id = ba.author_id
GROUP BY a.author_id
ORDER BY a.name ASC;

-- Search authors
SELECT a.*, COUNT(ba.book_id) as book_count
FROM authors a
LEFT JOIN book_authors ba ON a.author_id = ba.author_id
WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', ?, '%'))
GROUP BY a.author_id
ORDER BY a.name ASC;
```

**Response Data**:
```json
{
  "authors": [
    {
      "authorId": "author_02",
      "name": "Dale Carnegie",
      "biography": "Tác giả Đắc Nhân Tâm...",
      "avatarUrl": "/book_asset/image/authors/avatar2.png",
      "bookCount": 3,
      "createdAt": "2025-11-20T19:16:57"
    },
    {
      "authorId": "author_03",
      "name": "Aoyama Gosho",
      "biography": "Tác giả Thám tử lừng danh Conan...",
      "avatarUrl": "/book_asset/image/authors/avatar3.png",
      "bookCount": 5,
      "createdAt": "2025-11-20T19:16:57"
    }
  ]
}
```

---

## Flow 13.2: Create Author

### Sequence Diagram
```
Admin → Browser → AdminAuthorController → AuthorService → FileStorageService → Database
  │       │               │                    │                 │                │
  │  GET /admin/authors/create                                                   │
  │────────────────────────►│                                                    │
  │◄────────────────────────┤ (return form with empty AuthorCreateRequest)      │
  │       │                 │                                                    │
  │  POST /admin/authors/create (AuthorCreateRequest + avatarFile)              │
  │────────────────────────►│                                                    │
  │       │                 │ Validate input                                     │
  │       │                 │ generateNextAuthorId()                             │
  │       │                 │ checkNameDuplicate()                               │
  │       │                 ├────────────────────►│                              │
  │       │                 │                     │ findByName()                 │
  │       │                 │                     ├────────────────►│            │
  │       │                 │◄────────────────────┤                              │
  │       │                 │ saveAuthorAvatar()                                 │
  │       │                 ├─────────────────────────────────────►│             │
  │       │                 │◄─────────────────────────────────────┤             │
  │       │                 │ saveAuthor()                                       │
  │       │                 ├────────────────────►│                              │
  │       │                 │                     │ save()                       │
  │       │                 │                     ├────────────────────────────►│ │
  │       │                 │                     │◄────────────────────────────┤ │
  │       │                 │◄────────────────────┤                              │
  │◄────────────────────────┤ redirect:/admin/authors                           │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("author", new AuthorCreateRequest());
    addCommonFormAttributes(model, false);
    return "admin/authors/form";
}

@PostMapping("/create")
public String createAuthor(@Valid @ModelAttribute("author") AuthorCreateRequest request,
                          BindingResult result,
                          @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    // Validation
    if (result.hasErrors()) {
        addCommonFormAttributes(model, false);
        return "admin/authors/form";
    }
    
    // Check duplicate name
    if (isAuthorNameDuplicate(request.getName(), null)) {
        result.rejectValue("name", "error.author", "Tên tác giả đã tồn tại");
        addCommonFormAttributes(model, false);
        return "admin/authors/form";
    }
    
    try {
        // Generate ID
        String authorId = generateNextAuthorId();
        
        // Upload avatar
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarUrl = fileStorageService.saveAuthorAvatar(avatarFile);
        }
        
        // Create author
        Author author = new Author();
        author.setAuthorId(authorId);
        author.setName(request.getName());
        author.setBiography(request.getBiography());
        author.setAvatarUrl(avatarUrl);
        author.setCreatedAt(LocalDateTime.now());
        
        authorService.saveAuthor(author);
        
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tác giả thành công!");
        return REDIRECT_AUTHORS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_AUTHORS;
    }
}
```

**Helper Methods**:
```java
private String generateNextAuthorId() {
    List<Author> allAuthors = authorService.getAllAuthors();
    int nextNumber = allAuthors.size() + 1;
    return String.format("author_%02d", nextNumber);
}

private boolean isAuthorNameDuplicate(String name, String currentAuthorId) {
    Author existingAuthor = authorService.getAuthorByName(name).orElse(null);
    if (existingAuthor == null) {
        return false;
    }
    return !existingAuthor.getAuthorId().equals(currentAuthorId);
}
```

**File Storage Service**:
```java
public String saveAuthorAvatar(MultipartFile file) throws IOException {
    // Validate file
    validateImageFile(file);
    
    // Generate filename
    String originalFilename = file.getOriginalFilename();
    String extension = getFileExtension(originalFilename);
    String filename = UUID.randomUUID().toString() + extension;
    
    // Save to disk
    Path uploadPath = Paths.get(UPLOAD_DIR, "book_asset", "image", "authors");
    Files.createDirectories(uploadPath);
    
    Path filePath = uploadPath.resolve(filename);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    
    return "/book_asset/image/authors/" + filename;
}
```

**SQL Query**:
```sql
-- Check name exists
SELECT * FROM authors WHERE name = ?;

-- Insert new author
INSERT INTO authors (author_id, name, biography, avatar_url, created_at)
VALUES (?, ?, ?, ?, NOW());
```

**Request DTO**:
```java
public class AuthorCreateRequest {
    @NotBlank(message = "Tên tác giả không được để trống")
    @Size(max = 255, message = "Tên tác giả không được quá 255 ký tự")
    private String name;
    
    @Size(max = 5000, message = "Tiểu sử không được quá 5000 ký tự")
    private String biography;
    
    private String avatarUrl;
}
```

---

## Flow 13.3: View Author Details

### Sequence Diagram
```
Admin → Browser → AdminAuthorController → AuthorService → Database
  │       │               │                    │              │
  │  GET /admin/authors/view/{id}                             │
  │────────────────────────►│                                 │
  │       │                 │ getAuthorById(id)               │
  │       │                 ├────────────────────►│           │
  │       │                 │                     │ SELECT    │
  │       │                 │                     ├──────────►│
  │       │                 │◄────────────────────┤           │
  │       │                 │ getBooksByAuthor(id)            │
  │       │                 ├────────────────────►│           │
  │       │                 │◄────────────────────┤           │
  │◄────────────────────────┤ (return admin/authors/view.html)│
```

### Implementation Details

**Controller**:
```java
@GetMapping("/view/{id}")
public String viewAuthor(@PathVariable String id, Model model, 
                        RedirectAttributes redirectAttributes) {
    Author author = authorService.getAuthorById(id).orElse(null);
    
    if (author == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tác giả!");
        return REDIRECT_AUTHORS;
    }
    
    // Get books by this author
    List<Book> booksByAuthor = bookService.getBooksByAuthor(id);
    
    model.addAttribute("author", author);
    model.addAttribute("books", booksByAuthor);
    model.addAttribute("bookCount", booksByAuthor.size());
    
    return "admin/authors/view";
}
```

**SQL Query**:
```sql
-- Get author details
SELECT * FROM authors WHERE author_id = ?;

-- Get books by author
SELECT b.* FROM books b
INNER JOIN book_authors ba ON b.book_id = ba.book_id
WHERE ba.author_id = ?
ORDER BY b.created_at DESC;
```

---

## Flow 13.4: Edit Author

### Sequence Diagram
```
Admin → Browser → AdminAuthorController → AuthorService → FileStorageService → Database
  │       │               │                    │                 │                │
  │  GET /admin/authors/edit/{id}                                                │
  │────────────────────────►│                                                    │
  │       │                 │ getAuthorById(id)                                  │
  │       │                 ├────────────────────►│                              │
  │       │                 │◄────────────────────┤                              │
  │◄────────────────────────┤ (return form with AuthorUpdateRequest)            │
  │       │                 │                                                    │
  │  POST /admin/authors/edit/{id} (AuthorUpdateRequest)                        │
  │────────────────────────►│                                                    │
  │       │                 │ Validate input                                     │
  │       │                 │ checkNameDuplicate(excludeId)                      │
  │       │                 │ saveAuthorAvatar() if provided                     │
  │       │                 ├─────────────────────────────────────►│             │
  │       │                 │◄─────────────────────────────────────┤             │
  │       │                 │ updateAuthor()                                     │
  │       │                 ├────────────────────►│                              │
  │       │                 │                     │ save()                       │
  │       │                 │                     ├────────────────────────────►│ │
  │       │                 │◄────────────────────┤                              │
  │◄────────────────────────┤ redirect:/admin/authors                           │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id, Model model, 
                          RedirectAttributes redirectAttributes) {
    Author author = authorService.getAuthorById(id).orElse(null);
    
    if (author == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tác giả!");
        return REDIRECT_AUTHORS;
    }
    
    AuthorUpdateRequest dto = mapToUpdateRequest(author);
    model.addAttribute("author", dto);
    addCommonFormAttributes(model, true);
    
    return "admin/authors/form";
}

@PostMapping("/edit/{id}")
public String updateAuthor(@PathVariable String id,
                          @Valid @ModelAttribute("author") AuthorUpdateRequest request,
                          BindingResult result,
                          @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, true);
        return "admin/authors/form";
    }
    
    Author author = authorService.getAuthorById(id).orElse(null);
    if (author == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tác giả!");
        return REDIRECT_AUTHORS;
    }
    
    // Check duplicate name (exclude current)
    if (isAuthorNameDuplicate(request.getName(), id)) {
        result.rejectValue("name", "error.author", "Tên tác giả đã tồn tại");
        addCommonFormAttributes(model, true);
        return "admin/authors/form";
    }
    
    try {
        // Upload new avatar if provided
        if (avatarFile != null && !avatarFile.isEmpty()) {
            // Delete old avatar
            if (author.getAvatarUrl() != null) {
                fileStorageService.deleteFile(author.getAvatarUrl());
            }
            String avatarUrl = fileStorageService.saveAuthorAvatar(avatarFile);
            author.setAvatarUrl(avatarUrl);
        }
        
        // Update fields
        author.setName(request.getName());
        author.setBiography(request.getBiography());
        
        authorService.saveAuthor(author);
        
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tác giả thành công!");
        return REDIRECT_AUTHORS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_AUTHORS;
    }
}
```

**SQL Query**:
```sql
UPDATE authors 
SET name = ?, biography = ?, avatar_url = ?
WHERE author_id = ?;
```

---

## Flow 13.5: Delete Author

### Sequence Diagram
```
Admin → Browser → AdminAuthorController → AuthorService → Database
  │       │               │                    │              │
  │  POST /admin/authors/delete/{id}                          │
  │────────────────────────►│                                 │
  │       │                 │ checkBooksForAuthor(id)         │
  │       │                 ├────────────────────►│           │
  │       │                 │◄────────────────────┤           │
  │       │                 │ deleteAuthor(id)                │
  │       │                 ├────────────────────►│           │
  │       │                 │                     │ DELETE    │
  │       │                 │                     ├──────────►│
  │       │                 │◄────────────────────┤           │
  │◄────────────────────────┤ redirect:/admin/authors        │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deleteAuthor(@PathVariable String id, 
                          RedirectAttributes redirectAttributes) {
    try {
        Author author = authorService.getAuthorById(id).orElse(null);
        if (author == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tác giả!");
            return REDIRECT_AUTHORS;
        }
        
        // Check if author has books
        List<Book> booksByAuthor = bookService.getBooksByAuthor(id);
        if (!booksByAuthor.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa tác giả có " + booksByAuthor.size() + " sách!");
            return REDIRECT_AUTHORS;
        }
        
        // Delete avatar file
        if (author.getAvatarUrl() != null) {
            fileStorageService.deleteFile(author.getAvatarUrl());
        }
        
        authorService.deleteAuthor(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa tác giả thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
    }
    
    return REDIRECT_AUTHORS;
}
```

**SQL Query**:
```sql
-- Check books by author
SELECT COUNT(*) FROM book_authors WHERE author_id = ?;

-- Delete author
DELETE FROM authors WHERE author_id = ?;
```

---

## Flow 13.6: Search Authors

### Implementation Details

**Controller**:
```java
@GetMapping("/search")
@ResponseBody
public ResponseEntity<List<AuthorDTO>> searchAuthors(@RequestParam String query) {
    List<Author> authors = authorService.searchAuthorsByName(query);
    List<AuthorDTO> dtos = authors.stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
}
```

**Service**:
```java
@Override
public List<Author> searchAuthorsByName(String name) {
    if (name == null || name.trim().isEmpty()) {
        return Collections.emptyList();
    }
    return authorRepository.findByNameContainingIgnoreCase(name.trim());
}
```

**Repository**:
```java
public interface AuthorRepository extends JpaRepository<Author, String> {
    List<Author> findByNameContainingIgnoreCase(String name);
    Optional<Author> findByName(String name);
}
```

---

## Flow 13.7: Upload Author Avatar

### Implementation Details

**File Validation**:
```java
private void validateImageFile(MultipartFile file) throws IOException {
    // Check if file is empty
    if (file.isEmpty()) {
        throw new IllegalArgumentException("File không được để trống");
    }
    
    // Check file size (max 5MB)
    long maxSize = 5 * 1024 * 1024; // 5MB
    if (file.getSize() > maxSize) {
        throw new IllegalArgumentException("Kích thước file không được vượt quá 5MB");
    }
    
    // Check file type
    String contentType = file.getContentType();
    List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    if (!allowedTypes.contains(contentType)) {
        throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG)");
    }
}
```

**Storage Location**:
```
F:/datn_uploads/book_asset/image/authors/
├── avatar.png
├── avatar2.png
├── avatar3.png
├── avatar04.png
└── avatar5.png
```

---

## Error Handling

### Common Errors

**1. Author Not Found (404)**
```json
{
  "error": "NOT_FOUND",
  "message": "Không tìm thấy tác giả với ID: author_01"
}
```

**2. Duplicate Author Name (409)**
```json
{
  "error": "DUPLICATE_NAME",
  "message": "Tên tác giả 'Dale Carnegie' đã tồn tại"
}
```

**3. Author Has Books (409)**
```json
{
  "error": "AUTHOR_HAS_BOOKS",
  "message": "Không thể xóa tác giả có 8 sách",
  "bookCount": 8
}
```

**4. File Upload Error (400)**
```json
{
  "error": "FILE_UPLOAD_ERROR",
  "message": "Kích thước file không được vượt quá 5MB"
}
```

---

## Best Practices

### 1. Author ID Generation
- Format: `author_01`, `author_02`, ...
- Auto-increment based on existing authors
- Padded with zeros for sorting

### 2. Avatar Management
- Store in `/book_asset/image/authors/`
- Support formats: JPG, PNG
- Max file size: 5MB
- Delete old avatar when updating
- Use UUID for filename uniqueness

### 3. Biography Field
- Support rich text/Markdown
- Sanitize HTML to prevent XSS
- Max length: 5000 characters
- Allow line breaks

### 4. Name Uniqueness
- Enforce unique constraint in database
- Check duplicates before save
- Case-insensitive comparison
- Provide clear error messages

### 5. Author Deletion
- Always check for dependent books
- Delete associated avatar file
- Consider soft delete for data integrity
- Provide confirmation dialog

---

## Security Considerations

### 1. Authorization
```java
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/authors")
public class AdminAuthorController extends BaseAdminController {
    // ...
}
```

### 2. File Upload Security
- Validate file type and size
- Use UUID for filenames (prevent path traversal)
- Store outside web root
- Scan for malware in production

### 3. XSS Prevention
- Sanitize biography HTML
- Escape output in templates
- Use Content Security Policy

---

## Related Flows
- 📚 **FLOW 02**: Admin Book Management - Books have authors (Many-to-Many)
- 🏠 **FLOW 17**: Home Page & Public Book Browse - Display author info
- 📊 **FLOW 09**: Admin Dashboard - Author statistics

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

