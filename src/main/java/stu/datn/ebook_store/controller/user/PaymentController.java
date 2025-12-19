package stu.datn.ebook_store.controller.user;

import stu.datn.ebook_store.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.BankTransferInfo;
import stu.datn.ebook_store.dto.UserSubscription;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller xử lý thanh toán cho cả sách lẻ và gói đăng ký
 * Endpoints: /payment/*
 *
 * Note: Business logic đã được chuyển sang VNPayService và BankTransferService
 */
@Controller
@RequestMapping("/payment")
public class PaymentController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final SubscriptionService subscriptionService;
    private final VNPayService vnPayService;
    private final BankTransferService bankTransferService;

    @Autowired
    public PaymentController(OrderService orderService,
                           OrderItemService orderItemService,
                           SubscriptionService subscriptionService,
                           VNPayService vnPayService,
                           BankTransferService bankTransferService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.subscriptionService = subscriptionService;
        this.vnPayService = vnPayService;
        this.bankTransferService = bankTransferService;
    }


    /**
     * Lấy địa chỉ IP của client
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Lấy IP đầu tiên nếu có nhiều IP
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress != null ? ipAddress : "127.0.0.1";
    }

    /**
     * Khởi tạo thanh toán VNPay
     */
    @GetMapping("/vnpay")
    public String initiateVNPayPayment(
            @RequestParam String orderId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/auth/login";
            }

            // Lấy order
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này");
            }

            // Tạo payment URL qua VNPayService
            String paymentUrl = vnPayService.createPaymentUrl(order, request);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            logger.error("Error initiating VNPay payment for order {}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }


    /**
     * Callback từ VNPay
     * Business logic validation đã chuyển sang VNPayService
     */
    @GetMapping("/vnpay/return")
    public String vnpayReturn(
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        try {
            // Xác thực chữ ký qua VNPayService
            if (!vnPayService.validateCallback(params)) {
                logger.warn("Invalid VNPay callback signature");
                redirectAttributes.addFlashAttribute("error", "Chữ ký không hợp lệ");
                return "redirect:/payment/error";
            }

            // Lấy thông tin từ callback
            String orderId = vnPayService.getOrderId(params);
            String responseCode = vnPayService.getResponseCode(params);
            String transactionNo = vnPayService.getTransactionId(params);

            Order order = orderService.getOrderById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/payment/error";
            }

            // Cập nhật trạng thái
            if ("00".equals(responseCode)) {
                // Thanh toán thành công
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                order.setTransactionId(transactionNo);

                // Nếu là subscription order, kích hoạt subscription
                if (order.getOrderType() == Order.OrderType.SUBSCRIPTION && order.getSubscription() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    order.setStartDate(now);

                    // Tính end_date dựa trên duration của subscription
                    int durationDays = order.getSubscription().getDurationDays();
                    LocalDateTime endDate = now.plusDays(durationDays);
                    order.setEndDate(endDate);

                    logger.info("Subscription activated for order: {}, user: {}, plan: {}, end_date: {}",
                            orderId, order.getUser().getUserId(),
                            order.getSubscription().getPackageName(), endDate);
                }

                orderService.saveOrder(order);

                redirectAttributes.addFlashAttribute("success", "Thanh toán thành công!");
                return "redirect:/payment/success?orderId=" + orderId;
            } else {
                // Thanh toán thất bại
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderService.saveOrder(order);

                redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại. Mã lỗi: " + responseCode);
                return "redirect:/payment/error?orderId=" + orderId;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "redirect:/payment/error";
        }
    }

    /**
     * Trang thanh toán thành công
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String orderId, Model model) {
        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order != null) {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            model.addAttribute("orderItems", orderItems);
        }
        model.addAttribute("order", order);
        return "user/payment/success";
    }

    /**
     * Trang thanh toán thất bại
     */
    @GetMapping("/error")
    public String paymentError(@RequestParam(required = false) String orderId, Model model) {
        if (orderId != null) {
            Order order = orderService.getOrderById(orderId).orElse(null);
            if (order != null) {
                List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
                model.addAttribute("orderItems", orderItems);
            }
            model.addAttribute("order", order);
        }
        return "user/payment/error";
    }

    /**
     * Khởi tạo thanh toán chuyển khoản ngân hàng (QR Code)
     * Business logic đã chuyển sang BankTransferService
     */
    @GetMapping("/bank-transfer")
    public String initiateBankTransferPayment(
            @RequestParam String orderId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/auth/login";
            }

            // Lấy order
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này");
            }

            // Cập nhật phương thức thanh toán
            order.setPaymentMethod(Order.PaymentMethod.BANK_TRANSFER);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            orderService.saveOrder(order);

            // Tạo nội dung chuyển khoản qua BankTransferService
            String transferContent = bankTransferService.generateTransferContent(orderId);

            // Tạo URL QR code qua BankTransferService
            String qrUrl = bankTransferService.generateQRCodeUrl(order, transferContent);

            // Lấy thông tin ngân hàng qua BankTransferService
            BankTransferInfo bankInfo = bankTransferService.getBankInfo();

            // Lấy danh sách order items
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

            // Thêm thông tin vào model
            model.addAttribute("order", order);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("qrCodeUrl", qrUrl);
            model.addAttribute("bankName", bankInfo.getBankName());
            model.addAttribute("bankAccountNumber", bankInfo.getAccountNumber());
            model.addAttribute("bankAccountName", bankInfo.getAccountName());
            model.addAttribute("bankBranch", bankInfo.getBranch());
            model.addAttribute("transferContent", transferContent);

            return "user/payment/bank-transfer";

        } catch (Exception e) {
            logger.error("Error initiating bank transfer for order {}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }


    /**
     * Xác nhận đã chuyển khoản
     * Business logic đã chuyển sang BankTransferService
     */
    @PostMapping("/bank-transfer/confirm")
    @ResponseBody
    public Map<String, Object> confirmBankTransfer(
            @RequestParam String orderId) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("=== Confirm Bank Transfer ===");
            logger.info("Received orderId: {}", orderId);

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                logger.warn("User not authenticated for bank transfer confirmation");
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập");
                return response;
            }

            logger.info("Current user: {}", currentUser.getUserId());

            // Lấy order
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

            logger.info("Order found: {} for user: {}", order.getOrderId(), order.getUser().getUserId());

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                logger.warn("User {} attempted to confirm order {} belonging to user {}",
                           currentUser.getUserId(), orderId, order.getUser().getUserId());
                throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
            }

            // Kiểm tra trạng thái hiện tại
            if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
                logger.warn("Order {} status is not PENDING: {}", orderId, order.getPaymentStatus());
                response.put("success", false);
                response.put("message", "Đơn hàng đã được xác nhận trước đó");
                return response;
            }

            // Cập nhật trạng thái thành WAITING_APPROVAL
            order.setPaymentStatus(Order.PaymentStatus.WAITING_APPROVAL);
            orderService.saveOrder(order);

            System.out.println("Order updated to WAITING_APPROVAL");
            System.out.println("=== Confirm Success ===");

            response.put("success", true);
            response.put("message", "Đã xác nhận chuyển khoản. Đơn hàng đang chờ admin duyệt.");
            response.put("redirectUrl", "/payment/bank-transfer/waiting?orderId=" + orderId);

        } catch (Exception e) {
            System.err.println("ERROR in confirmBankTransfer: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }

        return response;
    }

    /**
     * Trang chờ duyệt thanh toán
     */
    @GetMapping("/bank-transfer/waiting")
    public String bankTransferWaiting(
            @RequestParam String orderId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/auth/login";
            }

            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
            }

            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

            model.addAttribute("order", order);
            model.addAttribute("orderItems", orderItems);

            return "user/payment/waiting-approval";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/order/history";
        }
    }



    // ========================================================================
    // SUBSCRIPTION PAYMENT METHODS
    // ========================================================================

    /**
     * Trang thanh toán gói đăng ký
     */
    @GetMapping("/subscription/checkout/{subscriptionId}")
    public String showSubscriptionCheckout(@PathVariable String subscriptionId,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đăng ký gói");
            return "redirect:/auth/login";
        }

        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
            return "redirect:/subscription/plans";
        }

        Subscription subscription = subscriptionOpt.get();

        // Kiểm tra nếu user đã có gói active
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(currentUser.getUserId(), Order.OrderType.SUBSCRIPTION);
        Optional<UserSubscription> activeSubscription = subscriptionOrders.stream()
                .filter(order -> {
                    if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED &&
                        order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                        return false;
                    }
                    return order.getEndDate() != null &&
                           order.getEndDate().isAfter(java.time.LocalDateTime.now());
                })
                .map(UserSubscription::new)
                .findFirst();

        model.addAttribute("subscription", subscription);
        
        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

        return "user/payment/subscription-checkout";
    }

    /**
     * Xử lý thanh toán gói đăng ký
     */
    @PostMapping("/subscription/process")
    public String processSubscriptionPayment(@RequestParam("subscriptionId") String subscriptionId,
                                            @RequestParam("paymentMethod") String paymentMethod,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
            return "redirect:/subscription/plans";
        }

        Subscription subscription = subscriptionOpt.get();

        try {
            // Tạo đơn hàng
            Order order = new Order();
            order.setOrderId("SUB_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setUser(currentUser);
            order.setSubscription(subscription);
            order.setOrderType(Order.OrderType.SUBSCRIPTION);
            order.setTotalAmount(subscription.getPrice());
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Set thời gian gói
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(subscription.getDurationDays());
            order.setStartDate(startDate);
            order.setEndDate(endDate);

            // Set phương thức thanh toán
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                order.setPaymentMethod(Order.PaymentMethod.VNPAY);
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                order.setPaymentMethod(Order.PaymentMethod.BANK_TRANSFER);
            } else {
                order.setPaymentMethod(Order.PaymentMethod.CREDIT_CARD);
            }

            // Lưu đơn hàng
            order = orderService.saveOrder(order);

            // Xử lý theo phương thức thanh toán
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                // Chuyển đến VNPAY qua VNPayService
                String paymentUrl = vnPayService.createPaymentUrl(order, request);
                return "redirect:" + paymentUrl;
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                // Chuyển đến trang chuyển khoản ngân hàng
                return "redirect:/payment/subscription/bank-transfer?orderId=" + order.getOrderId();
            } else {
                redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ");
                return "redirect:/subscription/plans";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/subscription/plans";
        }
    }

    /**
     * Trang chuyển khoản ngân hàng cho gói đăng ký
     */
    @GetMapping("/subscription/bank-transfer")
    public String showSubscriptionBankTransfer(@RequestParam("orderId") String orderId,
                                              Model model,
                                              RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/subscription/plans";
        }

        Order order = orderOpt.get();

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này");
            return "redirect:/subscription/plans";
        }

        // Lấy thông tin ngân hàng qua BankTransferService
        BankTransferInfo bankInfo = bankTransferService.getBankInfo();

        model.addAttribute("order", order);
        model.addAttribute("bankName", bankInfo.getBankName());
        model.addAttribute("bankAccountNumber", bankInfo.getAccountNumber());
        model.addAttribute("bankAccountName", bankInfo.getAccountName());
        model.addAttribute("bankBranch", bankInfo.getBranch());


        return "user/payment/subscription-bank-transfer";
    }

    /**
     * Trang thanh toán thành công cho gói đăng ký
     */
    @GetMapping("/subscription/success")
    public String subscriptionPaymentSuccess(@RequestParam("orderId") String orderId,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/subscription/plans";
        }

        Order order = orderOpt.get();

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này");
            return "redirect:/subscription/plans";
        }

        model.addAttribute("order", order);

        return "user/payment/subscription-success";
    }

    /**
     * Trang thanh toán thất bại cho gói đăng ký
     */
    @GetMapping("/subscription/failed")
    public String subscriptionPaymentFailed(@RequestParam(value = "orderId", required = false) String orderId,
                                           @RequestParam(value = "message", required = false) String message,
                                           Model model) {

        User currentUser = getCurrentUser();
        if (currentUser != null && orderId != null) {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(order -> {
                if (order.getUser().getUserId().equals(currentUser.getUserId())) {
                    model.addAttribute("order", order);
                }
            });
        }

        model.addAttribute("errorMessage", message != null ? message : "Thanh toán không thành công");

        return "user/payment/subscription-failed";
    }
}

