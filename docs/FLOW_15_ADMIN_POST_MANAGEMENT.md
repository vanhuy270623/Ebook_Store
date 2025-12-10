# 📝 FLOW 15: ADMIN POST/BLOG MANAGEMENT (Quản Lý Bài Viết)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 15.1: List Posts](#flow-151-list-posts)
3. [Flow 15.2: Create Post](#flow-152-create-post)
4. [Flow 15.3: View Post Details](#flow-153-view-post-details)
5. [Flow 15.4: Edit Post](#flow-154-edit-post)
6. [Flow 15.5: Delete Post](#flow-155-delete-post)
7. [Flow 15.6: Toggle Post Published Status](#flow-156-toggle-post-published-status)
8. [Flow 15.7: Generate Slug from Title](#flow-157-generate-slug-from-title)
9. [Error Handling](#error-handling)

---

## Tổng Quan

### Components
- **Controller**: `AdminPostController.java`
- **Service**: `PostService.java`, `FileStorageService.java`
- **Repository**: `PostRepository.java`
- **Entity**: `Post.java`
- **DTOs**: `PostCreateRequest.java`, `PostUpdateRequest.java`

### URLs
- `GET /admin/posts` - Danh sách bài viết
- `GET /admin/posts/create` - Form tạo bài viết
- `POST /admin/posts/create` - Xử lý tạo bài viết
- `GET /admin/posts/view/{id}` - Chi tiết bài viết
- `GET /admin/posts/edit/{id}` - Form sửa bài viết
- `POST /admin/posts/edit/{id}` - Xử lý sửa bài viết
- `POST /admin/posts/delete/{id}` - Xóa bài viết
- `POST /api/admin/posts/{id}/toggle-publish` - Xuất bản/ẩn bài viết
- `GET /api/admin/posts/check-slug` - Kiểm tra slug trùng
- `POST /api/admin/posts/generate-slug` - Tạo slug từ tiêu đề

### Post Structure
```
Post:
  - postId: String (auto: "post_01", "post_02", ...)
  - title: String (unique)
  - slug: String (unique, URL-friendly)
  - excerpt: String (short description)
  - content: Text (full content, HTML)
  - thumbnailUrl: String
  - isPublished: Boolean
  - author: User
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
```

---

## Flow 15.1: List Posts

### Sequence Diagram
```
Admin → Browser → AdminPostController → PostService → PostRepository → Database
  │       │              │                   │              │             │
  │  GET /admin/posts?search={query}&filter={status}                     │
  │────────────────────────►│                                             │
  │       │                 │ searchPosts() OR getPostsByUser()           │
  │       │                 ├───────────────────►│                        │
  │       │                 │                    │ findByTitleContaining()│
  │       │                 │                    ├─────────────►│         │
  │       │                 │                    │              │ SELECT  │
  │       │                 │                    │              ├────────►│
  │       │                 │                    │◄─────────────┤         │
  │       │                 │◄───────────────────┤                        │
  │◄────────────────────────┤ (return admin/posts/list.html)             │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String postsList(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String filter,
                       Authentication authentication,
                       Model model) {
    User currentUser = getCurrentUser(authentication);
    List<Post> posts;
    
    if (search != null && !search.trim().isEmpty()) {
        posts = postService.searchPostsByTitle(search);
        model.addAttribute("search", search);
    } else if ("published".equals(filter)) {
        posts = postService.getPublishedPostsByUser(currentUser);
    } else if ("draft".equals(filter)) {
        posts = postService.getDraftPostsByUser(currentUser);
    } else {
        posts = postService.getPostsByUserSortedByDate(currentUser);
    }
    
    model.addAttribute("posts", posts);
    model.addAttribute("totalPosts", posts.size());
    model.addAttribute("filter", filter);
    
    // Statistics
    long publishedCount = posts.stream().filter(Post::getIsPublished).count();
    long draftCount = posts.size() - publishedCount;
    
    model.addAttribute("publishedCount", publishedCount);
    model.addAttribute("draftCount", draftCount);
    
    return "admin/posts/list";
}
```

**Service**:
```java
@Override
public List<Post> getPostsByUserSortedByDate(User user) {
    return postRepository.findByAuthorOrderByCreatedAtDesc(user);
}

@Override
public List<Post> searchPostsByTitle(String title) {
    return postRepository.findByTitleContainingIgnoreCase(title);
}

@Override
public List<Post> getPublishedPostsByUser(User user) {
    return postRepository.findByAuthorAndIsPublishedTrueOrderByCreatedAtDesc(user);
}
```

**SQL Query**:
```sql
-- Get all posts by user
SELECT p.*, u.full_name as author_name
FROM posts p
INNER JOIN users u ON p.author_id = u.user_id
WHERE p.author_id = ?
ORDER BY p.created_at DESC;

-- Search posts
SELECT * FROM posts
WHERE LOWER(title) LIKE LOWER(CONCAT('%', ?, '%'))
ORDER BY created_at DESC;

-- Get published posts
SELECT * FROM posts
WHERE author_id = ? AND is_published = true
ORDER BY created_at DESC;
```

---

## Flow 15.2: Create Post

### Sequence Diagram
```
Admin → Browser → AdminPostController → PostService → FileStorageService → Database
  │       │              │                   │              │                │
  │  GET /admin/posts/create                                                 │
  │────────────────────────►│                                                │
  │◄────────────────────────┤ (return form with empty PostCreateRequest)    │
  │       │                 │                                                │
  │  POST /admin/posts/create (PostCreateRequest + thumbnailFile)           │
  │────────────────────────►│                                                │
  │       │                 │ Validate input                                 │
  │       │                 │ generateNextPostId()                           │
  │       │                 │ checkSlugDuplicate()                           │
  │       │                 │ saveThumbnail()                                │
  │       │                 ├──────────────────────────────────►│            │
  │       │                 │◄──────────────────────────────────┤            │
  │       │                 │ savePost()                                     │
  │       │                 ├───────────────────►│                           │
  │       │                 │                    │ save()                    │
  │       │                 │                    ├───────────────────────►│  │
  │       │                 │◄───────────────────┤                           │
  │◄────────────────────────┤ redirect:/admin/posts                         │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("post", new PostCreateRequest());
    addCommonFormAttributes(model, false);
    return "admin/posts/form";
}

@PostMapping("/create")
public String createPost(@Valid @ModelAttribute("post") PostCreateRequest request,
                        BindingResult result,
                        @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes,
                        Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, false);
        return "admin/posts/form";
    }
    
    // Check duplicate slug
    if (isSlugDuplicate(request.getSlug(), null)) {
        result.rejectValue("slug", "error.post", "Slug đã tồn tại");
        addCommonFormAttributes(model, false);
        return "admin/posts/form";
    }
    
    try {
        User currentUser = getCurrentUser(authentication);
        String postId = generateNextPostId();
        
        // Upload thumbnail
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = fileStorageService.savePostThumbnail(thumbnailFile);
        }
        
        // Create post
        Post post = new Post();
        post.setPostId(postId);
        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setThumbnailUrl(thumbnailUrl);
        post.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
        post.setAuthor(currentUser);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        postService.savePost(post);
        
        redirectAttributes.addFlashAttribute("successMessage", "Tạo bài viết thành công!");
        return REDIRECT_POSTS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_POSTS;
    }
}
```

**Helper Methods**:
```java
private String generateNextPostId() {
    List<Post> allPosts = postService.getAllPosts();
    int nextNumber = allPosts.size() + 1;
    return String.format("post_%02d", nextNumber);
}

private boolean isSlugDuplicate(String slug, String currentPostId) {
    Post existingPost = postService.getPostBySlug(slug).orElse(null);
    if (existingPost == null) {
        return false;
    }
    return !existingPost.getPostId().equals(currentPostId);
}
```

**SQL Query**:
```sql
-- Check slug exists
SELECT * FROM posts WHERE slug = ?;

-- Insert new post
INSERT INTO posts (post_id, title, slug, excerpt, content, thumbnail_url, 
                   is_published, author_id, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW());
```

**Request DTO**:
```java
public class PostCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
    private String title;
    
    @NotBlank(message = "Slug không được để trống")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang")
    @Size(max = 255, message = "Slug không được quá 255 ký tự")
    private String slug;
    
    @Size(max = 500, message = "Mô tả ngắn không được quá 500 ký tự")
    private String excerpt;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    private Boolean isPublished;
}
```

---

## Flow 15.3: View Post Details

### Implementation Details

**Controller**:
```java
@GetMapping("/view/{id}")
public String viewPost(@PathVariable String id, Model model, 
                      RedirectAttributes redirectAttributes) {
    Post post = postService.getPostById(id).orElse(null);
    
    if (post == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
        return REDIRECT_POSTS;
    }
    
    model.addAttribute("post", post);
    
    return "admin/posts/view";
}
```

**SQL Query**:
```sql
SELECT p.*, u.full_name as author_name, u.avatar_url as author_avatar
FROM posts p
INNER JOIN users u ON p.author_id = u.user_id
WHERE p.post_id = ?;
```

---

## Flow 15.4: Edit Post

### Implementation Details

**Controller**:
```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id, Model model, 
                          RedirectAttributes redirectAttributes) {
    Post post = postService.getPostById(id).orElse(null);
    
    if (post == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
        return REDIRECT_POSTS;
    }
    
    PostUpdateRequest dto = mapToUpdateRequest(post);
    model.addAttribute("post", dto);
    addCommonFormAttributes(model, true);
    
    return "admin/posts/form";
}

@PostMapping("/edit/{id}")
public String updatePost(@PathVariable String id,
                        @Valid @ModelAttribute("post") PostUpdateRequest request,
                        BindingResult result,
                        @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                        RedirectAttributes redirectAttributes,
                        Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, true);
        return "admin/posts/form";
    }
    
    Post post = postService.getPostById(id).orElse(null);
    if (post == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
        return REDIRECT_POSTS;
    }
    
    // Check duplicate slug (exclude current)
    if (isSlugDuplicate(request.getSlug(), id)) {
        result.rejectValue("slug", "error.post", "Slug đã tồn tại");
        addCommonFormAttributes(model, true);
        return "admin/posts/form";
    }
    
    try {
        // Upload new thumbnail if provided
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            if (post.getThumbnailUrl() != null) {
                fileStorageService.deleteFile(post.getThumbnailUrl());
            }
            String thumbnailUrl = fileStorageService.savePostThumbnail(thumbnailFile);
            post.setThumbnailUrl(thumbnailUrl);
        }
        
        // Update fields
        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setIsPublished(request.getIsPublished());
        post.setUpdatedAt(LocalDateTime.now());
        
        postService.savePost(post);
        
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài viết thành công!");
        return REDIRECT_POSTS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_POSTS;
    }
}
```

**SQL Query**:
```sql
UPDATE posts 
SET title = ?, slug = ?, excerpt = ?, content = ?, 
    thumbnail_url = ?, is_published = ?, updated_at = NOW()
WHERE post_id = ?;
```

---

## Flow 15.5: Delete Post

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deletePost(@PathVariable String id, 
                        RedirectAttributes redirectAttributes) {
    try {
        Post post = postService.getPostById(id).orElse(null);
        if (post == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return REDIRECT_POSTS;
        }
        
        // Delete thumbnail
        if (post.getThumbnailUrl() != null) {
            fileStorageService.deleteFile(post.getThumbnailUrl());
        }
        
        postService.deletePost(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
    }
    
    return REDIRECT_POSTS;
}
```

**SQL Query**:
```sql
DELETE FROM posts WHERE post_id = ?;
```

---

## Flow 15.6: Toggle Post Published Status

### Implementation Details

**Controller**:
```java
@PostMapping("/{id}/toggle-publish")
@ResponseBody
public ResponseEntity<Map<String, Object>> togglePublishStatus(@PathVariable String id) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        Post post = postService.getPostById(id).orElse(null);
        if (post == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy bài viết!");
            return ResponseEntity.notFound().build();
        }
        
        // Toggle status
        post.setIsPublished(!post.getIsPublished());
        post.setUpdatedAt(LocalDateTime.now());
        postService.savePost(post);
        
        response.put("success", true);
        response.put("isPublished", post.getIsPublished());
        response.put("message", post.getIsPublished() ? "Đã xuất bản bài viết" : "Đã chuyển về nháp");
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Lỗi: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

## Flow 15.7: Generate Slug from Title

### Implementation Details

**Controller**:
```java
@PostMapping("/generate-slug")
@ResponseBody
public ResponseEntity<Map<String, String>> generateSlug(@RequestParam String title) {
    Map<String, String> response = new HashMap<>();
    
    try {
        String slug = createSlugFromTitle(title);
        
        // Ensure slug is unique
        String uniqueSlug = slug;
        int counter = 1;
        while (postService.getPostBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = slug + "-" + counter;
            counter++;
        }
        
        response.put("slug", uniqueSlug);
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

private String createSlugFromTitle(String title) {
    if (title == null || title.trim().isEmpty()) {
        return "";
    }
    
    // Vietnamese to ASCII conversion
    String slug = title.toLowerCase()
        .replaceAll("à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ", "a")
        .replaceAll("è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ", "e")
        .replaceAll("ì|í|ị|ỉ|ĩ", "i")
        .replaceAll("ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ", "o")
        .replaceAll("ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ", "u")
        .replaceAll("ỳ|ý|ỵ|ỷ|ỹ", "y")
        .replaceAll("đ", "d")
        .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
        .replaceAll("\\s+", "-") // Replace spaces with hyphens
        .replaceAll("-+", "-") // Replace multiple hyphens with single
        .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    
    return slug;
}
```

**AJAX Usage**:
```javascript
// Auto-generate slug when title changes
$('#title').on('blur', function() {
    const title = $(this).val();
    if (title) {
        $.post('/api/admin/posts/generate-slug', { title: title })
            .done(function(data) {
                $('#slug').val(data.slug);
            });
    }
});
```

---

## Error Handling

### Common Errors

**1. Post Not Found (404)**
```json
{
  "error": "NOT_FOUND",
  "message": "Không tìm thấy bài viết với ID: post_01"
}
```

**2. Duplicate Slug (409)**
```json
{
  "error": "DUPLICATE_SLUG",
  "message": "Slug 'huong-dan-doc-sach' đã tồn tại"
}
```

**3. Invalid Slug Format (400)**
```json
{
  "error": "INVALID_SLUG",
  "message": "Slug chỉ chứa chữ thường, số và dấu gạch ngang"
}
```

---

## Best Practices

### 1. Slug Generation
- Auto-generate from title
- Convert Vietnamese to ASCII
- Use lowercase and hyphens
- Ensure uniqueness
- Allow manual editing

### 2. Content Editor
- Use rich text editor (TinyMCE, CKEditor)
- Support images, videos, code blocks
- Auto-save drafts
- Preview before publish

### 3. SEO Optimization
- Use excerpt as meta description
- Generate Open Graph tags
- Sitemap integration
- Canonical URLs

### 4. Image Management
- Optimize thumbnail images
- Support multiple sizes
- Lazy loading
- CDN for production

---

## Security Considerations

### 1. Content Sanitization
```java
// Sanitize HTML content to prevent XSS
public String sanitizeHtml(String html) {
    Safelist safelist = Safelist.relaxed()
        .addTags("h1", "h2", "h3", "h4", "h5", "h6")
        .addAttributes("img", "src", "alt", "title")
        .addAttributes("a", "href", "title", "target");
    
    return Jsoup.clean(html, safelist);
}
```

### 2. Authorization
- Only post author or admin can edit
- Draft posts not visible to public
- Verify ownership before delete

---

## Related Flows
- 🏠 **FLOW 17**: Home Page - Display published posts
- 📊 **FLOW 09**: Admin Dashboard - Post statistics
- 📂 **FLOW 12**: Admin Category Management - Posts can have categories (future)

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

