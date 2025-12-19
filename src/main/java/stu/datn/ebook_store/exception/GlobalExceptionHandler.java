package stu.datn.ebook_store.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.thymeleaf.exceptions.TemplateInputException;
import stu.datn.ebook_store.entity.User;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler - Xử lý lỗi tập trung cho toàn bộ application
 *
 * Chức năng:
 * - Bắt và xử lý tất cả exceptions
 * - Log chi tiết lỗi để debug
 * - Trả về error pages thân thiện với user
 * - Cung cấp thông tin debug trong dev mode
 *
 * @author DATN Team
 * @since 10/12/2025
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Tự động thêm thông tin user đã đăng nhập vào tất cả các views
     * Giúp các template có thể truy cập ${currentUser} mà không cần controller thêm thủ công
     */
    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String)) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    return (User) principal;
                }
            }
        } catch (Exception e) {
            log.debug("Could not get current user: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Xử lý NullPointerException - Lỗi phổ biến nhất trong Thymeleaf
     * Nguyên nhân: Truy cập property của object null trong template
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        log.error("❌ NullPointerException tại: {}", request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("errorType", "NullPointerException");
        mav.addObject("errorMessage", "Dữ liệu không tồn tại hoặc chưa được khởi tạo");
        mav.addObject("debugInfo", buildDebugInfo(ex, request));
        mav.addObject("solution", "Kiểm tra các biến trong Controller có được add vào Model chưa. " +
                "Sử dụng th:if để kiểm tra null trong template.");

        return mav;
    }

    /**
     * Xử lý TemplateInputException - Lỗi Thymeleaf template
     * Nguyên nhân: Template không tồn tại, syntax error, biến không tồn tại
     */
    @ExceptionHandler(TemplateInputException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleTemplateInputException(TemplateInputException ex, HttpServletRequest request) {
        log.error("❌ Thymeleaf Template Error tại: {}", request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("errorType", "Thymeleaf Template Error");
        mav.addObject("errorMessage", "Lỗi trong template Thymeleaf");
        mav.addObject("debugInfo", buildDebugInfo(ex, request));
        mav.addObject("solution", "Kiểm tra: 1) Template path đúng chưa, " +
                "2) Thymeleaf syntax đúng chưa, 3) Các biến có tồn tại trong Model không");

        return mav;
    }

    /**
     * Xử lý SQLException - Lỗi database
     * Nguyên nhân: Connection timeout, query error, constraint violation
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleSQLException(SQLException ex, HttpServletRequest request) {
        log.error("❌ SQL Exception tại: {}", request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("errorType", "Database Error");
        mav.addObject("errorMessage", "Lỗi khi truy vấn cơ sở dữ liệu");
        mav.addObject("debugInfo", buildDebugInfo(ex, request));
        mav.addObject("solution", "Kiểm tra: 1) Database connection, " +
                "2) SQL syntax, 3) Foreign key constraints, 4) Duplicate entries");

        return mav;
    }

    /**
     * Xử lý NoHandlerFoundException - Lỗi 404
     * Nguyên nhân: URL không tồn tại, mapping sai
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("⚠️ 404 Not Found: {}", request.getRequestURI());

        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("errorType", "Page Not Found");
        mav.addObject("errorMessage", "Trang bạn tìm kiếm không tồn tại");
        mav.addObject("requestedUrl", request.getRequestURI());

        return mav;
    }

    /**
     * Xử lý AccessDeniedException - Lỗi 403
     * Nguyên nhân: Không có quyền truy cập, Spring Security chặn
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("⚠️ 403 Access Denied: {} - User: {}",
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous");

        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("errorType", "Access Denied");
        mav.addObject("errorMessage", "Bạn không có quyền truy cập trang này");
        mav.addObject("requestedUrl", request.getRequestURI());

        return mav;
    }

    /**
     * Xử lý MaxUploadSizeExceededException - Lỗi upload file quá lớn
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ModelAndView handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.error("❌ File upload quá lớn tại: {}", request.getRequestURI());

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("errorType", "File Upload Error");
        mav.addObject("errorMessage", "File upload vượt quá kích thước cho phép (50MB)");
        mav.addObject("solution", "Vui lòng chọn file nhỏ hơn 50MB");

        return mav;
    }

    /**
     * Xử lý IllegalArgumentException - Lỗi tham số không hợp lệ
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("❌ Illegal Argument tại: {}", request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("errorType", "Invalid Parameter");
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("solution", "Kiểm tra lại các tham số truyền vào");

        return mav;
    }

    /**
     * Xử lý tất cả các exception khác - Fallback handler
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // Ignore browser/dev tools requests - không log để tránh spam
        if (isBrowserToolsRequest(requestUri)) {
            log.debug("Ignored browser tools request: {}", requestUri);
            ModelAndView mav = new ModelAndView("error/404");
            return mav;
        }

        log.error("❌ Unhandled Exception tại: {}", requestUri, ex);

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("errorType", ex.getClass().getSimpleName());
        mav.addObject("errorMessage", "Đã xảy ra lỗi không mong muốn");
        mav.addObject("debugInfo", buildDebugInfo(ex, request));
        mav.addObject("solution", "Vui lòng liên hệ admin hoặc thử lại sau");

        return mav;
    }

    /**
     * Kiểm tra request có phải từ browser tools không
     */
    private boolean isBrowserToolsRequest(String requestUri) {
        if (requestUri == null) return false;

        // Chrome DevTools
        if (requestUri.contains("/.well-known/appspecific/")) return true;
        if (requestUri.contains("/json/version")) return true;
        if (requestUri.contains("/json/list")) return true;

        // Favicon requests
        if (requestUri.endsWith("/favicon.ico")) return true;

        // Source maps
        if (requestUri.endsWith(".map")) return true;

        return false;
    }

    /**
     * Build debug information - Thông tin chi tiết để debug
     */
    private Map<String, Object> buildDebugInfo(Exception ex, HttpServletRequest request) {
        Map<String, Object> debugInfo = new HashMap<>();

        // Request info
        debugInfo.put("requestUrl", request.getRequestURI());
        debugInfo.put("method", request.getMethod());
        debugInfo.put("queryString", request.getQueryString());

        // User info
        if (request.getUserPrincipal() != null) {
            debugInfo.put("user", request.getUserPrincipal().getName());
        } else {
            debugInfo.put("user", "Anonymous");
        }

        // Exception info
        debugInfo.put("exceptionClass", ex.getClass().getName());
        debugInfo.put("exceptionMessage", ex.getMessage());

        // Stack trace (chỉ 5 dòng đầu)
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace.length > 0) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(5, stackTrace.length);
            for (int i = 0; i < limit; i++) {
                sb.append(stackTrace[i].toString()).append("\n");
            }
            debugInfo.put("stackTrace", sb.toString());
        }

        // Parameters
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            debugInfo.put("parameters", params);
        }

        return debugInfo;
    }
}

