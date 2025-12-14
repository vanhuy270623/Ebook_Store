# ✅ CART COUNTER FIX - COMPLETED

## Vấn đề
Cart counter chỉ hiển thị ở trang `/cart/view` mà không hiển thị ở các trang khác (index, books, ...).

## Nguyên nhân
1. **Navbar layout thiếu id:** Element `<span>` hiển thị số lượng giỏ hàng không có `id="cartCountBadge"`
2. **JavaScript không tìm thấy element:** Hàm `setupCartCounter()` trong `user-main.js` tìm element theo id `cartCountBadge`

## Giải pháp đã thực hiện

### 1. Cập nhật Navbar Layout ✅
File: `src/main/resources/templates/user/layout/navbar.html`

**TRƯỚC:**
```html
<span th:if="${cartItemCount != null && cartItemCount > 0}"
      class="badge bg-danger position-absolute top-0 start-100 translate-middle rounded-pill"
      th:text="${cartItemCount}">0</span>
```

**SAU:**
```html
<span id="cartCountBadge"
      class="badge bg-danger position-absolute top-0 start-100 translate-middle rounded-pill"
      style="display: none; font-size: 10px;"
      th:text="${cartItemCount != null && cartItemCount > 0 ? cartItemCount : '0'}"
      th:style="${cartItemCount != null && cartItemCount > 0 ? 'display: inline-block; font-size: 10px;' : 'display: none;'}">0</span>
```

**Thay đổi:**
- ✅ Thêm `id="cartCountBadge"` để JavaScript có thể tìm thấy
- ✅ Thêm inline style mặc định `display: none`
- ✅ Sử dụng `th:style` để hiển thị động khi có items
- ✅ Luôn render element (không dùng `th:if`) để JavaScript có thể access

### 2. Verify JavaScript Setup ✅
File: `src/main/resources/static/user_template/js/user-main.js`

Hàm `setupCartCounter()` đã có sẵn và hoạt động đúng:

```javascript
setupCartCounter: function() {
    const cartBadge = document.getElementById('cartCountBadge');
    if (!cartBadge) return; // Chỉ chạy nếu có badge trong header

    // Hàm cập nhật số lượng giỏ hàng
    const updateCartCount = () => {
        fetch('/cart/count')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const count = data.count || 0;
                    const displayCount = data.displayCount || '0';

                    if (count > 0) {
                        cartBadge.textContent = displayCount;
                        cartBadge.style.display = 'inline-block';
                    } else {
                        cartBadge.style.display = 'none';
                    }
                }
            })
            .catch(error => {
                console.error('Error fetching cart count:', error);
            });
    };

    // Cập nhật ngay khi load trang
    updateCartCount();

    // Cập nhật mỗi 30 giây
    setInterval(updateCartCount, 30000);

    // Lắng nghe sự kiện cartUpdated
    document.addEventListener('cartUpdated', updateCartCount);

    // Expose global
    window.updateCartCount = updateCartCount;
}
```

**Features:**
- ✅ Fetch cart count từ API `/cart/count`
- ✅ Hiển thị badge khi count > 0
- ✅ Ẩn badge khi count = 0
- ✅ Auto-refresh mỗi 30 giây
- ✅ Listen custom event `cartUpdated`
- ✅ Expose global function `window.updateCartCount()`

### 3. Verify API Endpoint ✅
File: `src/main/java/stu/datn/ebook_store/controller/user/CartController.java`

Endpoint `/cart/count` đã tồn tại và trả về đúng format:

```java
@GetMapping("/count")
@ResponseBody
public Map<String, Object> getCartCount(Authentication authentication) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = getCurrentUser(authentication);
            Optional<Cart> cartOpt = cartService.getCartByUser(currentUser);
            
            int count = 0;
            if (cartOpt.isPresent()) {
                Cart cart = cartOpt.get();
                List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
                count = cartItems.size();
            }
            
            response.put("count", count);
            response.put("displayCount", count > 5 ? "5+" : String.valueOf(count));
            response.put("success", true);
        } else {
            response.put("count", 0);
            response.put("displayCount", "0");
            response.put("success", true);
        }
    } catch (Exception e) {
        response.put("count", 0);
        response.put("displayCount", "0");
        response.put("success", false);
        response.put("error", e.getMessage());
    }
    
    return response;
}
```

**Response format:**
```json
{
  "count": 3,
  "displayCount": "3",
  "success": true
}
```

## Cách hoạt động

### 1. Page Load
1. User vào bất kỳ trang nào (index, books, library, ...)
2. Navbar layout được render với `id="cartCountBadge"`
3. JavaScript `user-main.js` được load
4. `EbookStore.init()` gọi `setupCartCounter()`
5. `setupCartCounter()` tìm element `cartCountBadge`
6. Gọi API `/cart/count` để lấy số lượng
7. Cập nhật badge hiển thị

### 2. Auto Refresh
- Mỗi 30 giây tự động gọi API để cập nhật
- Đảm bảo số lượng đồng bộ nếu user mở nhiều tabs

### 3. Manual Update
- Khi add/remove item: gọi `window.updateCartCount()`
- Hoặc dispatch event: `document.dispatchEvent(new Event('cartUpdated'))`

## Testing

### Test 1: Kiểm tra badge hiển thị
1. Login vào system
2. Vào trang index (`/user/index`)
3. Mở DevTools Console
4. Kiểm tra: `document.getElementById('cartCountBadge')`
5. Expected: Element tồn tại

### Test 2: Kiểm tra API
1. Mở DevTools Network tab
2. Reload trang
3. Tìm request đến `/cart/count`
4. Expected: Status 200, response JSON có `success: true`

### Test 3: Kiểm tra số lượng
1. Giỏ hàng trống: Badge ẩn (`display: none`)
2. Add 1 item: Badge hiện số "1"
3. Add thêm items: Badge cập nhật số
4. Remove items: Badge giảm số
5. Xóa hết: Badge ẩn

### Test 4: Kiểm tra các trang
- ✅ `/user/index` - Homepage
- ✅ `/books` - Books list
- ✅ `/books/view/{id}` - Book detail
- ✅ `/user/library` - Library
- ✅ `/user/dashboard` - Dashboard
- ✅ `/subscription/plans` - Subscription
- ✅ Tất cả các trang khác sử dụng navbar layout

## Các file đã sửa

1. ✅ `src/main/resources/templates/user/layout/navbar.html`
   - Thêm `id="cartCountBadge"` 
   - Cập nhật style handling

2. ✅ `src/main/resources/static/user_template/js/user-main.js`
   - Đã có sẵn `setupCartCounter()` (đã verify)

3. ✅ `src/main/java/stu/datn/ebook_store/controller/user/CartController.java`
   - Endpoint `/cart/count` đã tồn tại (đã verify)

## Kết quả

✅ Cart counter hiện nay hoạt động trên **TẤT CẢ các trang** sử dụng navbar layout
✅ Auto-refresh mỗi 30 giây
✅ Real-time update khi add/remove items
✅ Responsive và consistent UI

## Notes

- Badge chỉ hiển thị khi `count > 0`
- Display format: "1", "2", "3", ..., "5+" (nếu > 5)
- Font size: 10px để vừa với badge nhỏ
- Position: absolute, top-0, start-100 (góc trên bên phải icon)

---
**Status:** ✅ FIXED  
**Date:** 14/12/2024  
**Impact:** All pages using navbar layout

