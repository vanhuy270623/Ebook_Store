package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.OrderItemRepository;
import stu.datn.ebook_store.service.DownloadAuthorizationService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service triển khai logic kiểm tra quyền tải xuống
 *
 * QUY TẮC NGHIỆP VỤ:
 * 1. Sách MIỄN PHÍ (FREE) và cho phép tải → ĐƯỢC TẢI
 * 2. Sách đã MUA LẺ (order_type = 'BOOK') và thanh toán thành công → ĐƯỢC TẢI
 * 3. Sách trong GÓI VIP (chỉ có subscription, chưa mua lẻ) → KHÔNG TẢI (chỉ đọc online)
 * 4. Sách BOTH: Nếu đã mua lẻ → ĐƯỢC TẢI, nếu chỉ có VIP → KHÔNG TẢI
 * 5. Sách phải có flag is_downloadable = true
 */
@Service
public class DownloadAuthorizationServiceImpl implements DownloadAuthorizationService {

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);

    private final OrderItemRepository orderItemRepository;

    @Autowired
    public DownloadAuthorizationServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean canDownload(User user, Book book) {
        // Kiểm tra 1: Sách phải cho phép tải
        if (book.getIsDownloadable() == null || !book.getIsDownloadable()) {
            return false;
        }

        // Kiểm tra 2: Sách MIỄN PHÍ → cho phép tải
        if (book.getAccessType() == Book.AccessType.FREE) {
            return true;
        }

        // Kiểm tra 3: Kiểm tra user đã MUA LẺ sách này chưa
        boolean hasPurchasedBook = hasPurchasedBook(user.getUserId(), book.getBookId());

        // Kiểm tra 4: Logic theo access_type
        switch (book.getAccessType()) {
            case PURCHASE:
                // Sách chỉ bán lẻ → phải mua mới được tải
                return hasPurchasedBook;

            case SUBSCRIPTION:
                // Sách chỉ trong gói → KHÔNG CHO TẢI (chỉ đọc online)
                return false;

            case BOTH:
                // Sách cả hai loại → CHỈ cho tải nếu đã MUA LẺ
                // Nếu chỉ có VIP mà chưa mua lẻ → KHÔNG TẢI
                return hasPurchasedBook;

            default:
                return false;
        }
    }

    @Override
    public void validateDownloadPermission(User user, Book book) {
        if (!canDownload(user, book)) {
            throw new IllegalStateException(getDownloadDeniedReason(user, book));
        }
    }

    @Override
    public String getDownloadDeniedReason(User user, Book book) {
        // Kiểm tra cờ downloadable
        if (book.getIsDownloadable() == null || !book.getIsDownloadable()) {
            return "Sách này không hỗ trợ tải xuống. Bạn chỉ có thể đọc online.";
        }

        // Kiểm tra đã mua lẻ chưa
        boolean hasPurchasedBook = hasPurchasedBook(user.getUserId(), book.getBookId());

        // Phân loại theo access_type
        switch (book.getAccessType()) {
            case FREE:
                return "Lỗi hệ thống: Sách miễn phí nhưng không thể tải.";

            case PURCHASE:
                if (!hasPurchasedBook) {
                    return "Bạn cần mua sách này để tải xuống. Vui lòng thêm vào giỏ hàng và thanh toán.";
                }
                return "Bạn không có quyền tải xuống sách này.";

            case SUBSCRIPTION:
                return "Sách này thuộc gói VIP và chỉ cho phép đọc online, không hỗ trợ tải xuống.";

            case BOTH:
                if (!hasPurchasedBook) {
                    return "Sách này cần mua lẻ để tải xuống. " +
                           "Gói VIP của bạn chỉ cho phép đọc online. " +
                           "Vui lòng mua sách để có quyền tải về.";
                }
                return "Bạn không có quyền tải xuống sách này.";

            default:
                return "Không thể xác định quyền tải xuống.";
        }
    }

    /**
     * Kiểm tra user đã MUA LẺ sách này chưa
     * (order_type = 'BOOK' và payment_status IN ('COMPLETED', 'PAID'))
     */
    private boolean hasPurchasedBook(String userId, String bookId) {
        List<String> purchasedBookIds = orderItemRepository.findPurchasedBookIds(
                userId,
                Order.OrderType.BOOK,  // CHỈ lấy order MUA LẺ
                PAID_STATUSES,         // Đã thanh toán
                EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH) // access_type
        );
        return purchasedBookIds.contains(bookId);
    }
}

