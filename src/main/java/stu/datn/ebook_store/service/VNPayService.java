package stu.datn.ebook_store.service;

import jakarta.servlet.http.HttpServletRequest;
import stu.datn.ebook_store.entity.Order;

import java.util.Map;

/**
 * Service xử lý logic thanh toán VNPay
 * Chịu trách nhiệm: Tạo payment URL, xác thực callback, xử lý response
 */
public interface VNPayService {

    /**
     * Tạo URL thanh toán VNPay
     * @param order Đơn hàng cần thanh toán
     * @param request HTTP request để lấy IP client
     * @return URL thanh toán VNPay
     */
    String createPaymentUrl(Order order, HttpServletRequest request);

    /**
     * Xác thực callback từ VNPay
     * @param params Query parameters từ VNPay callback
     * @return true nếu signature hợp lệ, false nếu không
     */
    boolean validateCallback(Map<String, String> params);

    /**
     * Lấy response code từ VNPay callback
     * @param params Query parameters
     * @return Response code
     */
    String getResponseCode(Map<String, String> params);

    /**
     * Lấy transaction ID từ VNPay callback
     * @param params Query parameters
     * @return Transaction ID
     */
    String getTransactionId(Map<String, String> params);

    /**
     * Lấy order ID từ VNPay callback
     * @param params Query parameters
     * @return Order ID
     */
    String getOrderId(Map<String, String> params);

    /**
     * Lấy client IP từ HTTP request
     * @param request HTTP request
     * @return Client IP address
     */
    String getClientIp(HttpServletRequest request);
}

