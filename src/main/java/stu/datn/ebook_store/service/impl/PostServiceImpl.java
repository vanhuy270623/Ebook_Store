package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.PostRepository;
import stu.datn.ebook_store.service.PostService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Autowired
    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    public Optional<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }

    @Override
    public Optional<Post> getPostBySlug(String slug) {
        return postRepository.findBySlug(slug);
    }

    @Override
    public Post savePost(Post post) {
        if (post.getPostId() == null || post.getPostId().isEmpty()) {
            post.setPostId(generatePostId());
        }
        return postRepository.save(post);
    }

    @Override
    public void deletePost(String postId) {
        postRepository.deleteById(postId);
    }

    @Override
    public List<Post> getPublishedPosts() {
        return postRepository.findByIsPublishedTrue();
    }

    @Override
    public List<Post> getPublishedPostsSortedByDate() {
        return postRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<Post> getPostsByCategory(Category category) {
        return postRepository.findByCategory(category);
    }

    @Override
    public List<Post> getPublishedPostsByCategory(Category category) {
        return postRepository.findByCategoryAndIsPublishedTrue(category);
    }

    @Override
    public List<Post> getPostsByUser(User user) {
        return postRepository.findByUser(user);
    }

    @Override
    public List<Post> getPostsByUserSortedByDate(User user) {
        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public List<Post> searchPublishedPostsByKeyword(String keyword) {
        return postRepository.searchPublishedByKeyword(keyword);
    }

    @Override
    public List<Post> getTopViewedPosts() {
        return postRepository.findTop10ByIsPublishedTrueOrderByViewCountDesc();
    }

    @Override
    public List<Post> getLatestPosts() {
        return postRepository.findTop10ByIsPublishedTrueOrderByCreatedAtDesc();
    }

    @Override
    public void publishPost(String postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setIsPublished(true);
            postRepository.save(post);
        }
    }

    @Override
    public void unpublishPost(String postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setIsPublished(false);
            postRepository.save(post);
        }
    }

    @Override
    public void incrementViewCount(String postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }

    private String generatePostId() {
        long count = postRepository.count();
        return "post_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

