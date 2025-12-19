package stu.datn.ebook_store.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

/**
 * Debug AdminDashboardController - Các endpoint để debug và kiểm tra hệ thống
 *
 * ⚠️ CHÚ Ý: Chỉ sử dụng trong môi trường development
 * ⚠️ PHẢI TẮT hoặc bảo mật các endpoint này trước khi deploy production
 *
 * Chức năng:
 * - Kiểm tra database connection
 * - Test Thymeleaf templates
 * - Xem session data
 * - Xem model attributes
 * - Test null pointer scenarios
 * - Debug authentication
 *
 * @author DATN Team
 * @since 10/12/2025
 */
@Controller
@RequestMapping("/debug")
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BookService bookService;

    /**
     * Debug Dashboard - Trang chính hiển thị các debug tools
     * URL: /debug
     */
    @GetMapping
    public String debugDashboard(Model model, HttpServletRequest request) {
        log.info("=== DEBUG DASHBOARD ACCESSED ===");

        Map<String, Object> debugInfo = new HashMap<>();

        // 1. Server info
        debugInfo.put("serverPort", request.getServerPort());
        debugInfo.put("contextPath", request.getContextPath());
        debugInfo.put("serverName", request.getServerName());

        // 2. Authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> authInfo = new HashMap<>();
        if (auth != null) {
            authInfo.put("authenticated", auth.isAuthenticated());
            authInfo.put("principal", auth.getPrincipal().getClass().getSimpleName());
            authInfo.put("authorities", auth.getAuthorities());
        }
        debugInfo.put("authentication", authInfo);

        // 3. Session info
        HttpSession session = request.getSession(false);
        Map<String, Object> sessionInfo = new HashMap<>();
        if (session != null) {
            sessionInfo.put("sessionId", session.getId());
            sessionInfo.put("creationTime", new Date(session.getCreationTime()));
            sessionInfo.put("lastAccessedTime", new Date(session.getLastAccessedTime()));

            // List all session attributes
            Map<String, Object> sessionAttrs = new HashMap<>();
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String name = attrNames.nextElement();
                sessionAttrs.put(name, session.getAttribute(name).getClass().getSimpleName());
            }
            sessionInfo.put("attributes", sessionAttrs);
        }
        debugInfo.put("session", sessionInfo);

        model.addAttribute("debugInfo", debugInfo);
        model.addAttribute("pageTitle", "Debug Dashboard");

        return "debug/dashboard";
    }

    /**
     * Test Database Connection
     * URL: /debug/db-test
     *
     * Kiểm tra:
     * - Database connection có hoạt động không
     * - Query đơn giản có chạy được không
     */
    @GetMapping("/db-test")
    @ResponseBody
    public Map<String, Object> testDatabaseConnection() {
        log.info("=== TESTING DATABASE CONNECTION ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Test connection
            Connection connection = dataSource.getConnection();
            result.put("status", "SUCCESS");
            result.put("connected", !connection.isClosed());
            result.put("database", connection.getCatalog());
            result.put("url", connection.getMetaData().getURL());
            connection.close();

            // Test query
            List<Book> books = bookService.getAllBooks();
            result.put("booksCount", books.size());
            result.put("message", "✅ Database connection OK");

        } catch (Exception e) {
            log.error("❌ Database connection failed", e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            result.put("message", "❌ Database connection FAILED");
        }

        return result;
    }

    /**
     * Test Null Pointer - Cố tình tạo NullPointerException để test exception handler
     * URL: /debug/test-null
     */
    @GetMapping("/test-null")
    public String testNullPointer(Model model) {
        log.warn("⚠️ Testing NullPointerException");

        // Cố tình không add object vào model
        // Sau đó template sẽ cố truy cập → NullPointerException

        return "debug/test-null-template";
    }

    /**
     * Test Template Not Found - Test lỗi template không tồn tại
     * URL: /debug/test-404-template
     */
    @GetMapping("/test-404-template")
    public String testTemplateNotFound() {
        log.warn("⚠️ Testing template not found");

        // Return template không tồn tại
        return "this/template/does/not/exist";
    }

    /**
     * View Model Attributes - Xem tất cả attributes trong Model
     * URL: /debug/model-viewer
     */
    @GetMapping("/model-viewer")
    public String viewModelAttributes(
            @RequestParam(required = false) String testParam,
            Model model,
            HttpServletRequest request) {

        log.info("=== MODEL VIEWER ===");

        // Add some test data
        model.addAttribute("testString", "Hello Debug");
        model.addAttribute("testNumber", 12345);
        model.addAttribute("testBoolean", true);
        model.addAttribute("testList", Arrays.asList("Item 1", "Item 2", "Item 3"));

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        model.addAttribute("testMap", testMap);

        // Add request info
        model.addAttribute("requestUrl", request.getRequestURI());
        model.addAttribute("queryString", request.getQueryString());
        model.addAttribute("testParam", testParam);

        // Get all model attributes
        Map<String, Object> modelMap = model.asMap();
        model.addAttribute("allAttributes", modelMap);

        return "debug/model-viewer";
    }

    /**
     * Test Thymeleaf Expressions - Test các expression phổ biến
     * URL: /debug/thymeleaf-test
     */
    @GetMapping("/thymeleaf-test")
    public String testThymeleafExpressions(Model model) {
        log.info("=== THYMELEAF EXPRESSIONS TEST ===");

        // Test data
        model.addAttribute("text", "Hello Thymeleaf");
        model.addAttribute("number", 100);
        model.addAttribute("price", 299000.0);
        model.addAttribute("isActive", true);
        model.addAttribute("nullValue", null);

        // Lists
        model.addAttribute("emptyList", new ArrayList<>());
        model.addAttribute("stringList", Arrays.asList("Java", "Spring", "Thymeleaf"));

        // Objects
        Book testBook = new Book();
        testBook.setBookId("test-book-1");
        testBook.setTitle("Test Book");
        testBook.setPrice(new BigDecimal("199000.00"));
        model.addAttribute("book", testBook);

        // Null object
        model.addAttribute("nullBook", null);

        return "debug/thymeleaf-test";
    }

    /**
     * Test Authentication - Kiểm tra trạng thái authentication
     * URL: /debug/auth-test
     */
    @GetMapping("/auth-test")
    public String testAuthentication(Model model) {
        log.info("=== AUTHENTICATION TEST ===");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> authInfo = new HashMap<>();

        if (auth != null) {
            authInfo.put("authenticated", auth.isAuthenticated());
            authInfo.put("name", auth.getName());
            authInfo.put("principal", auth.getPrincipal());
            authInfo.put("authorities", auth.getAuthorities());
            authInfo.put("details", auth.getDetails());

            // Check if principal is User object
            if (auth.getPrincipal() instanceof User user) {
                authInfo.put("userId", user.getUserId());
                authInfo.put("email", user.getEmail());
                authInfo.put("role", user.getRole().getRoleName());
            }
        } else {
            authInfo.put("message", "No authentication found");
        }

        model.addAttribute("authInfo", authInfo);

        return "debug/auth-test";
    }

    /**
     * Test Exception - Test custom exception
     * URL: /debug/test-exception
     */
    @GetMapping("/test-exception")
    public String testException(@RequestParam(defaultValue = "generic") String type) {
        log.warn("⚠️ Testing exception: {}", type);

        switch (type) {
            case "null":
                // Trigger NullPointerException
                String str = null;
                str.length(); // This will throw NPE
                break;

            case "illegal":
                // Trigger IllegalArgumentException
                throw new IllegalArgumentException("This is a test illegal argument exception");

            case "runtime":
                // Trigger RuntimeException
                throw new RuntimeException("This is a test runtime exception");

            default:
                // Generic exception
                throw new RuntimeException("Generic test exception");
        }

        return "debug/test-exception";
    }

    /**
     * View All Books - Simple list để test query
     * URL: /debug/books
     */
    @GetMapping("/books")
    public String viewAllBooks(Model model) {
        log.info("=== DEBUG: View All Books ===");

        try {
            List<Book> books = bookService.getAllBooks();
            model.addAttribute("books", books);
            model.addAttribute("booksCount", books.size());
            model.addAttribute("status", "SUCCESS");
        } catch (Exception e) {
            log.error("Error loading books", e);
            model.addAttribute("status", "ERROR");
            model.addAttribute("error", e.getMessage());
        }

        return "debug/books";
    }

    /**
     * View Request Info - Hiển thị tất cả thông tin request
     * URL: /debug/request-info
     */
    @GetMapping("/request-info")
    public String viewRequestInfo(HttpServletRequest request, Model model) {
        log.info("=== REQUEST INFO ===");

        Map<String, Object> requestInfo = new HashMap<>();

        // Basic info
        requestInfo.put("method", request.getMethod());
        requestInfo.put("requestURI", request.getRequestURI());
        requestInfo.put("requestURL", request.getRequestURL().toString());
        requestInfo.put("queryString", request.getQueryString());
        requestInfo.put("protocol", request.getProtocol());
        requestInfo.put("scheme", request.getScheme());
        requestInfo.put("serverName", request.getServerName());
        requestInfo.put("serverPort", request.getServerPort());
        requestInfo.put("remoteAddr", request.getRemoteAddr());
        requestInfo.put("remoteHost", request.getRemoteHost());

        // Headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        requestInfo.put("headers", headers);

        // Parameters
        requestInfo.put("parameters", request.getParameterMap());

        // Attributes
        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            attributes.put(attrName, request.getAttribute(attrName));
        }
        requestInfo.put("attributes", attributes);

        model.addAttribute("requestInfo", requestInfo);

        return "debug/request-info";
    }

    /**
     * Clear Session - Xóa session hiện tại
     * URL: /debug/clear-session
     */
    @GetMapping("/clear-session")
    @ResponseBody
    public Map<String, Object> clearSession(HttpServletRequest request) {
        log.info("=== CLEARING SESSION ===");

        Map<String, Object> result = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            session.invalidate();
            result.put("status", "SUCCESS");
            result.put("message", "Session cleared: " + sessionId);
        } else {
            result.put("status", "INFO");
            result.put("message", "No session to clear");
        }

        return result;
    }

    /**
     * System Info - Thông tin hệ thống
     * URL: /debug/system-info
     */
    @GetMapping("/system-info")
    @ResponseBody
    public Map<String, Object> getSystemInfo() {
        log.info("=== SYSTEM INFO ===");

        Map<String, Object> systemInfo = new HashMap<>();

        // Java info
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("javaHome", System.getProperty("java.home"));

        // OS info
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        systemInfo.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        systemInfo.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        systemInfo.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        systemInfo.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB");

        // User info
        systemInfo.put("userDir", System.getProperty("user.dir"));
        systemInfo.put("userName", System.getProperty("user.name"));

        return systemInfo;
    }
}

