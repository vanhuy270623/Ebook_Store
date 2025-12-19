package stu.datn.ebook_store.service;

import stu.datn.ebook_store.dto.BankTransferInfo;
import stu.datn.ebook_store.entity.Order;

/**
 * Service xử lý logic thanh toán chuyển khoản ngân hàng
 * Chịu trách nhiệm: Tạo QR code, xác nhận chuyển khoản
 */
public interface BankTransferService {

    /**
     * Tạo URL QR code cho thanh toán
     * @param order Đơn hàng cần thanh toán
     * @param transferContent Nội dung chuyển khoản
     * @return URL của QR code
     */
    String generateQRCodeUrl(Order order, String transferContent);

    /**
     * Tạo nội dung chuyển khoản chuẩn
     * Format: EBOOKSTORE {orderId}
     * @param orderId ID đơn hàng
     * @return Nội dung chuyển khoản
     */
    String generateTransferContent(String orderId);

    /**
     * Lấy thông tin ngân hàng để hiển thị
     * @return BankTransferInfo chứa thông tin ngân hàng
     */
    BankTransferInfo getBankInfo();

    /**
     * Xác nhận đã chuyển khoản (cập nhật order status)
     * @param orderId ID đơn hàng
     * @return true nếu xác nhận thành công
     */
    boolean confirmTransfer(String orderId);
}

