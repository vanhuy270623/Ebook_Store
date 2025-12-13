package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface PostService {
    List<Post> getAllPosts();
    Optional<Post> getPostById(String postId);
    Optional<Post> getPostBySlug(String slug);
    Post savePost(Post post);
    void deletePost(String postId);
    List<Post> getPublishedPosts();
    List<Post> getPublishedPostsSortedByDate();
    List<Post> getPostsByCategory(Category category);
    List<Post> getPublishedPostsByCategory(Category category);
    List<Post> getPostsByUser(User user);
    List<Post> getPostsByUserSortedByDate(User user);
    List<Post> searchPublishedPostsByKeyword(String keyword);
    List<Post> getTopViewedPosts();
    List<Post> getLatestPosts();
    void publishPost(String postId);
    void unpublishPost(String postId);
    void incrementViewCount(String postId);
}

