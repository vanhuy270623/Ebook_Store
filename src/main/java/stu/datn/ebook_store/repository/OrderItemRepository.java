package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.Order;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByBook(Book book);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user.userId = :userId AND oi.book.bookId = :bookId")
    List<OrderItem> findByUserAndBook(@Param("userId") String userId, @Param("bookId") String bookId);
}

