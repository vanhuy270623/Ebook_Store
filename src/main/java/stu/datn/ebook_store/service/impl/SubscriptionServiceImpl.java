package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.Subscription.PackageName;
import stu.datn.ebook_store.repository.SubscriptionRepository;
import stu.datn.ebook_store.service.SubscriptionService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Override
    public Optional<Subscription> getSubscriptionById(String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    @Override
    public Optional<Subscription> getSubscriptionByPackageName(PackageName packageName) {
        return subscriptionRepository.findByPackageName(packageName);
    }

    @Override
    public Subscription saveSubscription(Subscription subscription) {
        if (subscription.getSubscriptionId() == null || subscription.getSubscriptionId().isEmpty()) {
            subscription.setSubscriptionId(generateSubscriptionId());
        }
        return subscriptionRepository.save(subscription);
    }

    @Override
    public void deleteSubscription(String subscriptionId) {
        subscriptionRepository.deleteById(subscriptionId);
    }

    @Override
    public List<Subscription> getActiveSubscriptions() {
        return subscriptionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public void activateSubscription(String subscriptionId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setIsActive(true);
            subscriptionRepository.save(subscription);
        }
    }

    @Override
    public void deactivateSubscription(String subscriptionId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setIsActive(false);
            subscriptionRepository.save(subscription);
        }
    }

    private String generateSubscriptionId() {
        long count = subscriptionRepository.count();
        return "subscription_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

