package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {
    Optional<Post> findBySlug(String slug);

    List<Post> findByIsPublishedTrue();

    List<Post> findByIsPublishedTrueOrderByCreatedAtDesc();

    List<Post> findByCategory(Category category);

    List<Post> findByCategoryAndIsPublishedTrue(Category category);

    List<Post> findByUser(User user);

    List<Post> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT p FROM Post p WHERE p.isPublished = true AND (p.title LIKE %:keyword% OR p.excerpt LIKE %:keyword% OR p.content LIKE %:keyword%)")
    List<Post> searchPublishedByKeyword(@Param("keyword") String keyword);

    List<Post> findTop10ByIsPublishedTrueOrderByViewCountDesc();

    List<Post> findTop10ByIsPublishedTrueOrderByCreatedAtDesc();
}

