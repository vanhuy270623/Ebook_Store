package stu.datn.ebook_store.service;

import stu.datn.ebook_store.dto.UserSubscriptionDto;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.Subscription.PackageName;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {
    List<Subscription> getAllSubscriptions();
    Optional<Subscription> getSubscriptionById(String subscriptionId);
    Optional<Subscription> getSubscriptionByPackageName(PackageName packageName);
    Subscription saveSubscription(Subscription subscription);
    void deleteSubscription(String subscriptionId);
    List<Subscription> getActiveSubscriptions();
    void activateSubscription(String subscriptionId);
    void deactivateSubscription(String subscriptionId);

    /**
     * Tìm kiếm subscription theo từ khóa
     */
    List<Subscription> searchSubscriptions(String keyword);

    /**
     * Lấy thông tin subscription đang active của user
     * Bao gồm: subscription status, package name, end date, subscription books
     */
    UserSubscriptionDto getUserActiveSubscription(User user);

    /**
     * Kiểm tra user có subscription đang active không
     */
    boolean hasActiveSubscription(String userId);
}

