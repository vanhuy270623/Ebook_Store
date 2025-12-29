package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.service.PostService;
import stu.datn.ebook_store.service.CategoryService;

import java.util.List;
import java.util.Optional;

/**
 * UserPostController - Controller xử lý hiển thị bài viết cho người dùng
 */
@Controller
@RequestMapping("/posts")
public class UserPostController {

    private final PostService postService;
    private final CategoryService categoryService;

    @Autowired
    public UserPostController(PostService postService, CategoryService categoryService) {
        this.postService = postService;
        this.categoryService = categoryService;
    }

    /**
     * Lấy thông tin user hiện tại (nếu đã đăng nhập)
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Trang danh sách bài viết
     */
    @GetMapping("")
    public String listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {

        User currentUser = getCurrentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentPage", "posts");

        List<Post> posts;

        // Lọc theo category
        if (category != null && !category.isEmpty()) {
            Optional<Category> categoryObj = categoryService.getCategoryById(category);
            if (categoryObj.isPresent()) {
                posts = postService.getPublishedPostsByCategory(categoryObj.get());
                model.addAttribute("selectedCategory", categoryObj.get());
            } else {
                posts = postService.getPublishedPostsSortedByDate();
            }
        }
        // Tìm kiếm
        else if (search != null && !search.isEmpty()) {
            posts = postService.searchPublishedPostsByKeyword(search);
            model.addAttribute("searchKeyword", search);
        }
        // Mặc định: tất cả bài viết đã xuất bản
        else {
            posts = postService.getPublishedPostsSortedByDate();
        }

        // Lấy bài viết xem nhiều nhất và mới nhất cho sidebar
        List<Post> topViewedPosts = postService.getTopViewedPosts();
        List<Post> latestPosts = postService.getLatestPosts();
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("posts", posts);
        model.addAttribute("topViewedPosts", topViewedPosts);
        model.addAttribute("latestPosts", latestPosts);
        model.addAttribute("categories", categories);

        return "user/posts/list";
    }

    /**
     * Trang chi tiết bài viết (theo slug)
     */
    @GetMapping("/{slug}")
    public String viewPost(
            @PathVariable String slug,
            Authentication authentication,
            Model model) {

        Optional<Post> postOpt = postService.getPostBySlug(slug);
        if (postOpt.isEmpty() || !postOpt.get().getIsPublished()) {
            return "redirect:/posts";
        }

        Post post = postOpt.get();

        // Tăng lượt xem
        postService.incrementViewCount(post.getPostId());

        User currentUser = getCurrentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentPage", "posts");
        model.addAttribute("post", post);

        // Lấy bài viết liên quan (cùng category hoặc mới nhất)
        List<Post> relatedPosts;
        if (post.getCategory() != null) {
            relatedPosts = postService.getPublishedPostsByCategory(post.getCategory())
                    .stream()
                    .filter(p -> !p.getPostId().equals(post.getPostId()))
                    .limit(5)
                    .toList();
        } else {
            relatedPosts = postService.getLatestPosts()
                    .stream()
                    .filter(p -> !p.getPostId().equals(post.getPostId()))
                    .limit(5)
                    .toList();
        }

        model.addAttribute("relatedPosts", relatedPosts);

        return "user/posts/detail";
    }
}

