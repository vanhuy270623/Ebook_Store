package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.dto.UserSubscriptionDto;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.Subscription.PackageName;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.SubscriptionRepository;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrderService orderService;
    private final BookService bookService;

    @Autowired
    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   OrderService orderService,
                                   BookService bookService) {
        this.subscriptionRepository = subscriptionRepository;
        this.orderService = orderService;
        this.bookService = bookService;
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

    /**
     * Lấy thông tin subscription đang active của user
     * Business Logic: Tìm subscription order đang active và lấy danh sách sách tương ứng
     */
    @Override
    public UserSubscriptionDto getUserActiveSubscription(User user) {
        // Lấy danh sách subscription orders của user
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(
                user.getUserId(),
                Order.OrderType.SUBSCRIPTION
        );

        // Tìm subscription đang active
        for (Order order : subscriptionOrders) {
            if (isSubscriptionActive(order)) {
                return buildActiveSubscriptionDto(order);
            }
        }

        // Không có subscription active
        return new UserSubscriptionDto(false);
    }

    /**
     * Kiểm tra subscription có đang active không
     * Logic: Phải có payment status là COMPLETED hoặc PAID và end_date chưa hết hạn
     */
    private boolean isSubscriptionActive(Order order) {
        boolean isPaid = order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                        order.getPaymentStatus() == Order.PaymentStatus.PAID;

        boolean notExpired = order.getEndDate() != null &&
                            order.getEndDate().isAfter(LocalDateTime.now());

        return isPaid && notExpired;
    }

    /**
     * Build DTO từ subscription order đang active
     * Bao gồm: package name, end date, danh sách sách subscription
     */
    private UserSubscriptionDto buildActiveSubscriptionDto(Order order) {
        UserSubscriptionDto dto = new UserSubscriptionDto();
        dto.setHasActiveSubscription(true);

        // Set package name
        String packageName = (order.getSubscription() != null &&
                             order.getSubscription().getPackageName() != null) ?
                order.getSubscription().getPackageName().toString() : "VIP";
        dto.setSubscriptionPackageName(packageName);

        // Set end date
        dto.setSubscriptionEndDate(order.getEndDate());

        // Set max devices
        int maxDevices = (order.getSubscription() != null) ?
                order.getSubscription().getMaxDevices() : 3;
        dto.setMaxDevices(maxDevices);

        // Lấy danh sách sách subscription
        List<Book> subscriptionBooks = getSubscriptionBooks();
        dto.setSubscriptionBooks(subscriptionBooks);

        return dto;
    }

    /**
     * Lấy tất cả sách có ACCESS_TYPE là SUBSCRIPTION hoặc BOTH
     * Business Logic: Merge 2 danh sách và loại bỏ trùng lặp
     */
    private List<Book> getSubscriptionBooks() {
        List<Book> subscriptionBooks = bookService.getBooksByAccessType(Book.AccessType.SUBSCRIPTION);
        List<Book> bothBooks = bookService.getBooksByAccessType(Book.AccessType.BOTH);

        // Merge và loại bỏ trùng lặp
        List<Book> allSubscriptionBooks = new ArrayList<>(subscriptionBooks);
        allSubscriptionBooks.addAll(bothBooks);

        return allSubscriptionBooks.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra user có subscription đang active không
     */
    @Override
    public boolean hasActiveSubscription(String userId) {
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(
                userId,
                Order.OrderType.SUBSCRIPTION
        );

        return subscriptionOrders.stream()
                .anyMatch(this::isSubscriptionActive);
    }
}

