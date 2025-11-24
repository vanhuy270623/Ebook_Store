package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.Subscription.PackageName;

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
}

