package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.User;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUser(User user);
}

