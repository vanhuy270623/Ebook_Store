package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.entity.User;

/**
 * Service kiểm tra quyền tải xuống sách
 * Tuân thủ nghiệp vụ: Sách trong gói VIP KHÔNG cho phép tải, chỉ sách đã MUA mới được tải
 */
public interface DownloadAuthorizationService {

    /**
     * Kiểm tra user có quyền tải xuống sách hay không
     * @param user User hiện tại (đã đăng nhập)
     * @param book Sách cần tải
     * @return true nếu được phép tải, false nếu không
     */
    boolean canDownload(User user, Book book);

    /**
     * Kiểm tra và throw exception nếu không có quyền tải
     * @param user User hiện tại
     * @param book Sách cần tải
     * @throws IllegalStateException nếu không có quyền
     */
    void validateDownloadPermission(User user, Book book);

    /**
     * Lấy thông báo lỗi khi không có quyền tải
     * @param user User hiện tại
     * @param book Sách cần tải
     * @return Thông báo lỗi chi tiết
     */
    String getDownloadDeniedReason(User user, Book book);
}

