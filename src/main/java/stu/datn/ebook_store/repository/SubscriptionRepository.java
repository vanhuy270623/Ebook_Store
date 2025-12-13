package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByPackageName(Subscription.PackageName packageName);

    List<Subscription> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find the active subscription for a user based on their orders
     * @param userId The user ID
     * @param now Current timestamp to check if subscription is still valid
     * @return Optional containing the active subscription
     */
    @Query("SELECT o.subscription FROM Order o WHERE o.user.userId = :userId " +
           "AND o.orderType = 'SUBSCRIPTION' " +
           "AND o.paymentStatus = 'COMPLETED' " +
           "AND o.endDate > :now " +
           "ORDER BY o.endDate DESC")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
}

