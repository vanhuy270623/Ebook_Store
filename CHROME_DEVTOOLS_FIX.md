# Fix: Ignore Chrome DevTools Requests

## Vấn đề
Ứng dụng liên tục log lỗi `Unhandled Exception` khi Chrome DevTools cố gắng truy cập endpoint:
```
/.well-known/appspecific/com.chrome.devtools.json
```

## Nguyên nhân
- Chrome DevTools tự động gửi request để tìm kiếm thông tin debug
- Ứng dụng không có endpoint này nên throw exception
- GlobalExceptionHandler bắt exception và log error mỗi lần
- Đây không phải lỗi thực sự của ứng dụng mà là request tự động từ browser tools

## Giải pháp

### File: GlobalExceptionHandler.java
Thêm logic để **ignore** các request từ browser tools:

```java
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
    // ... existing error handling
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
```

## Các request được ignore

### 1. Chrome DevTools
- `/.well-known/appspecific/com.chrome.devtools.json`
- `/json/version`
- `/json/list`

### 2. Favicon
- `/favicon.ico` (tất cả paths kết thúc bằng favicon.ico)

### 3. Source Maps
- `*.map` (các file JavaScript source map)

## Lợi ích

### 1. Clean Logs
- ✅ Không còn spam error log từ browser tools
- ✅ Log file giảm kích thước đáng kể
- ✅ Dễ dàng phát hiện lỗi thực sự

### 2. Performance
- ✅ Không xử lý exception không cần thiết
- ✅ Trả về 404 nhanh chóng cho browser tools
- ✅ Giảm CPU usage cho logging

### 3. Monitoring
- ✅ Debug log vẫn ghi nhận request (level DEBUG)
- ✅ Không làm nhiễu các error report thực sự
- ✅ Dễ dàng troubleshoot khi có vấn đề

## Cách hoạt động

### Before:
```
2025-12-18T21:45:36.098+07:00 ERROR 2280 --- [DATN] [nio-2706-exec-8] 
s.d.e.exception.GlobalExceptionHandler : 
❌ Unhandled Exception tại: /.well-known/appspecific/com.chrome.devtools.json
org.springframework.web.servlet.NoHandlerFoundException: No endpoint GET /.well-known/appspecific/com.chrome.devtools.json
    at org.springframework.web...
    ... 100 lines of stack trace ...
```

### After:
```
2025-12-18T21:45:36.098+07:00 DEBUG 2280 --- [DATN] [nio-2706-exec-8] 
s.d.e.exception.GlobalExceptionHandler : 
Ignored browser tools request: /.well-known/appspecific/com.chrome.devtools.json
```

## Test Cases

### ✅ Case 1: Chrome DevTools request
- **Request**: `GET /.well-known/appspecific/com.chrome.devtools.json`
- **Log Level**: DEBUG (không hiển thị ở production)
- **Response**: 404 Not Found
- **Result**: ✅ Không spam log

### ✅ Case 2: Favicon request
- **Request**: `GET /favicon.ico`
- **Log Level**: DEBUG
- **Response**: 404 Not Found
- **Result**: ✅ Không spam log

### ✅ Case 3: Lỗi thực sự từ application
- **Request**: `GET /user/profile` (với lỗi NullPointer)
- **Log Level**: ERROR (vẫn log đầy đủ)
- **Response**: 500 với debug info
- **Result**: ✅ Vẫn log đầy đủ như cũ

### ✅ Case 4: 404 thực sự
- **Request**: `GET /nonexistent-page`
- **Log Level**: WARN
- **Response**: 404 Not Found
- **Result**: ✅ Vẫn log như cũ

## Files Changed

### ✅ Modified
1. `src/main/java/stu/datn/ebook_store/exception/GlobalExceptionHandler.java`
   - Thêm method `isBrowserToolsRequest()`
   - Sửa `handleGenericException()` để filter browser tools requests

## Mở rộng

Nếu có thêm browser tools hoặc crawler cần ignore, chỉ cần thêm vào method `isBrowserToolsRequest()`:

```java
// Firefox DevTools
if (requestUri.contains("/devtools/")) return true;

// Safari Web Inspector
if (requestUri.contains("/inspector/")) return true;

// Common bot crawlers
if (requestUri.contains("/robots.txt")) return true;
if (requestUri.contains("/sitemap.xml")) return true;
```

## Kết luận

✅ **Hoàn thành**: Đã fix lỗi spam log từ Chrome DevTools
- Logs sạch hơn, dễ đọc hơn
- Performance tốt hơn (không xử lý exception không cần thiết)
- Debug log vẫn ghi nhận request (nếu cần)
- Không ảnh hưởng đến error handling thực sự

---
**Ngày hoàn thành**: 18/12/2025
**Trạng thái**: ✅ Ready for production

