package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUser(User user);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByTransactionId(String transactionId);

    List<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus);

    List<Order> findByOrderType(Order.OrderType orderType);

    List<Order> findByUserAndOrderType(User user, Order.OrderType orderType);

    List<Order> findByUserAndPaymentStatus(User user, Order.PaymentStatus paymentStatus);

    List<Order> findBySubscription(Subscription subscription);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    long countByPaymentStatus(Order.PaymentStatus paymentStatus);
}

