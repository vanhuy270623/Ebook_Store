package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByPackageName(Subscription.PackageName packageName);

    List<Subscription> findByIsActiveTrueOrderByDisplayOrderAsc();
}

