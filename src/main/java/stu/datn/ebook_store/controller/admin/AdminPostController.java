package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.PostCreateRequest;
import stu.datn.ebook_store.dto.request.PostUpdateRequest;
import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.PostService;
import stu.datn.ebook_store.service.FileStorageService;
import stu.datn.ebook_store.service.CategoryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý bài viết/blog (CRUD Post)
 * Pattern: Tương tự AdminBannerController, AdminCouponController
 */
@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {

    private static final String REDIRECT_POSTS = "redirect:/admin/posts";

    private final PostService postService;
    private final FileStorageService fileStorageService;
    private final CategoryService categoryService;

    @Autowired
    public AdminPostController(PostService postService, FileStorageService fileStorageService,
                              CategoryService categoryService) {
        this.postService = postService;
        this.fileStorageService = fileStorageService;
        this.categoryService = categoryService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Sinh Post ID tự động theo format: "post_XX"
     */
    private String generateNextPostId() {
        List<Post> allPosts = postService.getAllPosts();
        int nextNumber = allPosts.size() + 1;
        return String.format("post_%02d", nextNumber);
    }

    /**
     * Kiểm tra slug đã tồn tại (trừ post đang sửa)
     */
    private boolean isSlugDuplicate(String slug, String currentPostId) {
        Post existingPost = postService.getPostBySlug(slug).orElse(null);
        if (existingPost == null) {
            return false;
        }
        return !existingPost.getPostId().equals(currentPostId);
    }

    /**
     * Map Post entity sang PostUpdateRequest DTO
     */
    private PostUpdateRequest mapToUpdateRequest(Post post) {
        PostUpdateRequest dto = new PostUpdateRequest();
        dto.setPostId(post.getPostId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setContent(post.getContent());
        dto.setThumbnailUrl(post.getThumbnailUrl());
        dto.setIsPublished(post.getIsPublished());
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("categories", categoryService.getAllCategories());
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách bài viết
     */
    @GetMapping
    public String postsList(@RequestParam(required = false) String search, Model model) {
        List<Post> posts;

        if (search != null && !search.trim().isEmpty()) {
            posts = postService.searchPublishedPostsByKeyword(search);
            model.addAttribute("search", search);
        } else {
            posts = postService.getAllPosts();
        }

        model.addAttribute("posts", posts);
        model.addAttribute("totalPosts", posts.size());

        // Thống kê nhanh
        long publishedPosts = posts.stream()
                .filter(Post::getIsPublished)
                .count();
        model.addAttribute("publishedPosts", publishedPosts);

        return "admin/posts/list";
    }

    /**
     * Xem chi tiết bài viết
     */
    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(id).orElse(null);

        if (post == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết với ID: " + id);
            return REDIRECT_POSTS;
        }

        model.addAttribute("post", post);
        if (post.getCategory() != null) {
            model.addAttribute("categoryName", post.getCategory().getCategoryName());
        }
        return "admin/posts/view";
    }

    /**
     * Hiển thị form thêm bài viết mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("postRequest", new PostCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/posts/form";
    }

    /**
     * Hiển thị form chỉnh sửa bài viết
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(id).orElse(null);

        if (post == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
            return REDIRECT_POSTS;
        }

        // Chuyển đổi Post entity sang DTO
        PostUpdateRequest postUpdateRequest = mapToUpdateRequest(post);

        model.addAttribute("postRequest", postUpdateRequest);
        model.addAttribute("post", post);
        addCommonFormAttributes(model, true);

        return "admin/posts/form";
    }

    /**
     * Tạo bài viết mới
     */
    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute("postRequest") PostCreateRequest request,
                            BindingResult bindingResult,
                            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, false);
            return "admin/posts/form";
        }

        // Kiểm tra slug trùng
        if (postService.getPostBySlug(request.getSlug()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Slug bài viết đã tồn tại!");
            return "redirect:/admin/posts/add";
        }

        try {
            User currentUser = getCurrentUser(authentication);

            // Upload thumbnail nếu có
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailUrl = fileStorageService.storeFile(thumbnail, "posts");
                request.setThumbnailUrl(thumbnailUrl);
            }

            // Tạo Post entity
            String newPostId = generateNextPostId();
            Post newPost = new Post();
            newPost.setPostId(newPostId);
            newPost.setUser(currentUser);
            newPost.setTitle(request.getTitle());
            newPost.setSlug(request.getSlug());
            newPost.setExcerpt(request.getExcerpt());
            newPost.setContent(request.getContent());
            newPost.setThumbnailUrl(request.getThumbnailUrl());
            newPost.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
            newPost.setViewCount(0);
            newPost.setCreatedAt(LocalDateTime.now());
            newPost.setUpdatedAt(LocalDateTime.now());

            postService.savePost(newPost);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm bài viết thành công! ID: " + newPostId + ", Tiêu đề: " + request.getTitle());

            return REDIRECT_POSTS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/posts/add";
        }
    }

    /**
     * Cập nhật bài viết
     */
    @PostMapping("/update")
    public String updatePost(@Valid @ModelAttribute("postRequest") PostUpdateRequest request,
                            BindingResult bindingResult,
                            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);

            Post post = postService.getPostById(request.getPostId()).orElse(null);
            model.addAttribute("post", post);
            addCommonFormAttributes(model, true);

            return "admin/posts/form";
        }

        Post existingPost = postService.getPostById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        // Kiểm tra slug trùng (trừ chính nó)
        if (isSlugDuplicate(request.getSlug(), request.getPostId())) {
            redirectAttributes.addFlashAttribute("error", "Slug bài viết đã được sử dụng bởi bài viết khác!");
            return "redirect:/admin/posts/edit/" + request.getPostId();
        }

        try {
            // Upload thumbnail nếu có
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailUrl = fileStorageService.storeFile(thumbnail, "posts");
                request.setThumbnailUrl(thumbnailUrl);
            }

            // Cập nhật thông tin
            existingPost.setTitle(request.getTitle());
            existingPost.setSlug(request.getSlug());
            existingPost.setExcerpt(request.getExcerpt());
            existingPost.setContent(request.getContent());
            if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isEmpty()) {
                existingPost.setThumbnailUrl(request.getThumbnailUrl());
            }
            existingPost.setIsPublished(request.getIsPublished());
            existingPost.setUpdatedAt(LocalDateTime.now());

            postService.savePost(existingPost);
            redirectAttributes.addFlashAttribute("success", "Cập nhật bài viết thành công!");

            return REDIRECT_POSTS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/posts/edit/" + request.getPostId();
        }
    }

    /**
     * Xóa bài viết
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            postService.deletePost(id);
            response.put("success", true);
            response.put("message", "Xóa bài viết thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa bài viết";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Công bố/ẩn bài viết
     */
    @PostMapping("/toggle-publish/{id}")
    public String togglePublishStatus(@PathVariable String id,
                                     RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(id).orElse(null);
        if (post == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
            return REDIRECT_POSTS;
        }

        if (post.getIsPublished()) {
            postService.unpublishPost(id);
        } else {
            postService.publishPost(id);
        }

        redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái bài viết thành công!");
        return REDIRECT_POSTS;
    }

    /**
     * Hiển thị thống kê bài viết
     */
    @GetMapping("/statistics")
    public String postsStatistics(Model model) {
        List<Post> allPosts = postService.getAllPosts();

        model.addAttribute("totalPosts", allPosts.size());

        long publishedPosts = allPosts.stream()
                .filter(Post::getIsPublished)
                .count();
        model.addAttribute("publishedPosts", publishedPosts);

        model.addAttribute("topViewedPosts", postService.getTopViewedPosts());
        model.addAttribute("latestPosts", postService.getLatestPosts());

        return "admin/posts/statistics";
    }
}

