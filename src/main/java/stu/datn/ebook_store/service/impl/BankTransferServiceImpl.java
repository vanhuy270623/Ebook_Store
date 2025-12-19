package stu.datn.ebook_store.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import stu.datn.ebook_store.dto.BankTransferInfo;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.service.BankTransferService;
import stu.datn.ebook_store.service.OrderService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Implementation của BankTransferService
 * Xử lý toàn bộ logic thanh toán chuyển khoản ngân hàng và tạo QR code
 */
@Service
public class BankTransferServiceImpl implements BankTransferService {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferServiceImpl.class);

    @Value("${bank.name:TPbank}")
    private String bankName;

    @Value("${bank.account_number:79992706999}")
    private String bankAccountNumber;

    @Value("${bank.account_name:CONG TY EBOOK STORE}")
    private String bankAccountName;

    @Value("${bank.branch:Chi nhanh TP.HCM}")
    private String bankBranch;

    @Value("${bank.code:TP}")
    private String bankCode;

    @Value("${bank.qr_template:https://img.vietqr.io/image/{bank_code}-{account_number}-{template}.png?amount={amount}&addInfo={content}&accountName={account_name}}")
    private String qrTemplate;

    @Autowired
    private OrderService orderService;

    @Override
    public String generateQRCodeUrl(Order order, String transferContent) {
        try {
            logger.info("=== Generating QR Code ===");
            logger.info("Bank Code: {}", bankCode);
            logger.info("Account Number: {}", bankAccountNumber);
            logger.info("Account Name: {}", bankAccountName);
            logger.info("Amount: {}", order.getTotalAmount().longValue());
            logger.info("Content: {}", transferContent);
            logger.info("QR Template: {}", qrTemplate);

            String url = qrTemplate
                    .replace("{bank_code}", bankCode)
                    .replace("{account_number}", bankAccountNumber)
                    .replace("{template}", "compact2")
                    .replace("{amount}", String.valueOf(order.getTotalAmount().longValue()))
                    .replace("{content}", URLEncoder.encode(transferContent, StandardCharsets.UTF_8))
                    .replace("{account_name}", URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8));

            logger.info("Generated QR URL: {}", url);

            return url;
        } catch (Exception e) {
            logger.error("Error generating QR code URL: {}", e.getMessage(), e);

            // Return a fallback QR code URL
            return generateFallbackQRCode(order);
        }
    }

    @Override
    public String generateTransferContent(String orderId) {
        return "EBOOKSTORE " + orderId;
    }

    @Override
    public BankTransferInfo getBankInfo() {
        return new BankTransferInfo(
                bankName,
                bankAccountNumber,
                bankAccountName,
                bankBranch,
                bankCode
        );
    }

    @Override
    public boolean confirmTransfer(String orderId) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);

            if (orderOpt.isEmpty()) {
                logger.warn("Order not found: {}", orderId);
                return false;
            }

            Order order = orderOpt.get();

            // Cập nhật trạng thái sang PENDING (đợi xác nhận)
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            orderService.saveOrder(order);

            logger.info("Bank transfer confirmed for order: {}", orderId);
            return true;

        } catch (Exception e) {
            logger.error("Error confirming bank transfer for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tạo QR code dự phòng khi không thể tạo QR từ VietQR
     */
    private String generateFallbackQRCode(Order order) {
        try {
            String data = String.format(
                    "Thanh toan don hang: %s\nSo tien: %s VND\nTK: %s - %s",
                    order.getOrderId(),
                    order.getTotalAmount(),
                    bankAccountNumber,
                    bankName
            );

            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                   URLEncoder.encode(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error generating fallback QR code", e);
            return "";
        }
    }
}

