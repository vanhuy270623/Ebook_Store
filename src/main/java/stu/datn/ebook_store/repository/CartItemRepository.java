package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.CartItem;
import stu.datn.ebook_store.entity.CartItemId;
import stu.datn.ebook_store.entity.Cart;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndBook(Cart cart, Book book);

    long countByCart(Cart cart);

    void deleteByCartAndBook(Cart cart, Book book);
}

