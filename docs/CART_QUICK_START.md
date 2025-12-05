# 🛒 Quick Start: Hệ Thống Giỏ Hàng

**Cập nhật:** 4/12/2025  
**Phiên bản:** 2.0

---

## 🎯 Giới Thiệu

Hệ thống giỏ hàng đã được nâng cấp với:
- ✅ Badge hiển thị số lượng real-time (tối đa 5+)
- ✅ Toast notification đẹp mắt
- ✅ Cập nhật không cần reload trang
- ✅ Kiểm tra trùng lặp tự động

---

## 🚀 Cách Sử Dụng

### 1. Cho Developer

#### Thêm Nút "Thêm Vào Giỏ" Trong Template

```html
<!-- Cách 1: Form đơn giản -->
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <button type="submit" class="btn btn-primary">
        <i class="fas fa-cart-plus"></i> Thêm vào giỏ
    </button>
</form>

<!-- Cách 2: Với redirect parameter (quay lại trang hiện tại) -->
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <input type="hidden" name="redirect" th:value="@{/books/view/{id}(id=${book.bookId})}"/>
    <button type="submit" class="btn btn-primary">
        <i class="fas fa-cart-plus"></i> Thêm vào giỏ
    </button>
</form>

<!-- Cách 3: Icon button trong danh sách -->
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" 
      method="post" 
      class="d-inline">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <input type="hidden" name="redirect" value="/books"/>
    <button type="submit" class="action-btn" title="Thêm vào giỏ">
        <i class="fas fa-cart-plus"></i>
    </button>
</form>
```

#### Thêm Cart Badge Vào Header

```html
<a class="nav-link position-relative" th:href="@{/cart}" id="cartLink">
    <i class="fas fa-shopping-cart"></i>
    <span id="cartCountBadge" 
          class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" 
          style="display: none; font-size: 10px;">0</span>
</a>
```

#### Load Scripts Cần Thiết

```html
<!-- Cuối trang, trước </body> -->
<script th:src="@{/user_template/js/bootstrap.bundle.min.js}"></script>
<script th:src="@{/user_template/js/user-main.js}"></script>

<!-- Script xử lý flash messages -->
<script th:inline="javascript">
    document.addEventListener('DOMContentLoaded', function() {
        /*[# th:if="${success}"]*/
        const successMsg = /*[[${success}]]*/ '';
        if (successMsg) {
            showToast(successMsg, 'success');
            if (typeof window.updateCartCount === 'function') {
                setTimeout(() => window.updateCartCount(), 500);
            }
        }
        /*[/]*/
        
        /*[# th:if="${error}"]*/
        const errorMsg = /*[[${error}]]*/ '';
        if (errorMsg) {
            showToast(errorMsg, 'error');
        }
        /*[/]*/
        
        /*[# th:if="${info}"]*/
        const infoMsg = /*[[${info}]]*/ '';
        if (infoMsg) {
            showToast(infoMsg, 'info');
        }
        /*[/]*/
    });
    
    function showToast(message, type = 'info') {
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.style.cssText = 'position: fixed; top: 80px; right: 20px; z-index: 9999; width: 350px;';
            document.body.appendChild(toastContainer);
        }
        
        const toast = document.createElement('div');
        toast.className = 'toast align-items-center text-white border-0 show mb-2';
        toast.style.cssText = 'box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);';
        
        const colors = {
            'success': 'bg-success',
            'error': 'bg-danger',
            'info': 'bg-info'
        };
        toast.classList.add(colors[type] || 'bg-info');
        
        const icons = {
            'success': '<i class="fas fa-check-circle me-2"></i>',
            'error': '<i class="fas fa-exclamation-circle me-2"></i>',
            'info': '<i class="fas fa-info-circle me-2"></i>'
        };
        
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    ${icons[type] || ''}${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 5000);
        
        toast.querySelector('.btn-close').addEventListener('click', () => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        });
    }
</script>
```

---

### 2. Cho User (End User)

#### Thêm Sách Vào Giỏ Hàng

**Từ trang chi tiết sách:**
1. Xem thông tin sách
2. Click nút "Thêm vào giỏ"
3. Thấy thông báo: "✓ Đã thêm [Tên sách] vào giỏ hàng"
4. Badge trên icon giỏ hàng cập nhật ngay
5. Tiếp tục xem sách khác hoặc đi đến giỏ hàng

**Từ trang danh sách:**
1. Di chuột qua sách
2. Click icon giỏ hàng ở góc
3. Thấy thông báo thành công
4. Badge cập nhật
5. Tiếp tục mua sắm

#### Xem Giỏ Hàng

1. Click icon giỏ hàng trên header (có badge số đỏ)
2. Xem danh sách sách đã chọn
3. Kiểm tra tổng tiền
4. Có thể:
   - Xóa từng sản phẩm
   - Xóa toàn bộ giỏ
   - Tiếp tục mua sắm
   - Thanh toán

#### Xóa Sản Phẩm

**Xóa 1 sản phẩm:**
1. Trong giỏ hàng, click nút thùng rác
2. Thấy thông báo: "✓ Xóa sách khỏi giỏ thành công"
3. Badge tự động giảm
4. Tổng tiền cập nhật

**Xóa toàn bộ:**
1. Click "Xóa toàn bộ giỏ hàng"
2. Tất cả sản phẩm bị xóa
3. Badge biến mất
4. Hiện "Giỏ hàng trống"

---

## 📱 Tính Năng Nổi Bật

### Badge Thông Minh

```
Giỏ trống     → Badge ẩn
1 sản phẩm    → Badge hiện "1"
2 sản phẩm    → Badge hiện "2"
...
5 sản phẩm    → Badge hiện "5"
6+ sản phẩm   → Badge hiện "5+"
```

### Thông Báo Thông Minh

**Khi thêm sách mới:**
> ✓ Đã thêm "Lập Trình Python" vào giỏ hàng

**Khi sách đã có:**
> ℹ Sách này đã có trong giỏ hàng

**Khi có lỗi:**
> ✕ Lỗi: Sách không tồn tại

### Cập Nhật Real-time

- ✅ Badge cập nhật ngay không cần reload
- ✅ Thông báo hiện tức thì
- ✅ Đồng bộ giữa các tab (mỗi 30s)

---

## 🔧 API Reference

### REST Endpoints

```
GET  /cart              → Xem giỏ hàng
POST /cart/add/{id}     → Thêm sách vào giỏ
POST /cart/remove       → Xóa 1 sản phẩm
POST /cart/clear        → Xóa toàn bộ
GET  /cart/count        → Lấy số lượng (AJAX)
```

### JavaScript Functions

```javascript
// Cập nhật số lượng giỏ hàng
window.updateCartCount();

// Hiển thị thông báo
showToast('Message', 'success'); // 'success', 'error', 'info'

// Lắng nghe sự kiện
document.addEventListener('cartUpdated', function() {
    console.log('Cart was updated!');
});
```

---

## 🎨 Customization

### Thay Đổi Màu Badge

```css
/* Trong style.css hoặc inline */
#cartCountBadge {
    background-color: #ff6b6b !important; /* Đỏ hồng */
    /* hoặc */
    background-color: #4ecdc4 !important; /* Xanh mint */
}
```

### Thay Đổi Vị Trí Toast

```javascript
toastContainer.style.cssText = 'position: fixed; top: 20px; left: 20px; ...';
// Góc trên trái

toastContainer.style.cssText = 'position: fixed; bottom: 20px; right: 20px; ...';
// Góc dưới phải
```

### Thay Đổi Thời Gian Toast

```javascript
setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
}, 3000); // 3 giây thay vì 5 giây
```

---

## 🐛 Troubleshooting

### Badge không hiển thị

**Kiểm tra:**
1. Badge có ID `cartCountBadge`?
2. Script `user-main.js` đã load?
3. Console có lỗi JavaScript không?

**Fix:**
```html
<!-- Đảm bảo có ID -->
<span id="cartCountBadge" class="badge">0</span>

<!-- Đảm bảo script được load -->
<script th:src="@{/user_template/js/user-main.js}"></script>
```

### Toast không hiện

**Kiểm tra:**
1. Function `showToast()` có được define?
2. Flash attribute có được set?

**Fix:**
```java
// Trong controller
redirectAttributes.addFlashAttribute("success", "Thông báo");
```

### Badge không cập nhật

**Kiểm tra:**
1. API `/cart/count` có hoạt động?
2. AJAX call có bị block?

**Test:**
```javascript
// Trong console
fetch('/cart/count')
    .then(r => r.json())
    .then(d => console.log(d));
```

### Số lượng không đúng

**Fix:**
```javascript
// Force refresh
window.updateCartCount();
```

---

## 💡 Tips & Best Practices

### Cho Developer

1. **Luôn set flash attributes:**
   ```java
   redirectAttributes.addFlashAttribute("success", "Message");
   redirectAttributes.addFlashAttribute("cartUpdated", true);
   ```

2. **Sử dụng redirect parameter:**
   ```html
   <input type="hidden" name="redirect" value="/current-page"/>
   ```

3. **Test với nhiều sản phẩm:**
   - 0 items → Badge ẩn
   - 1-5 items → Hiển thị số
   - 6+ items → Hiển thị "5+"

4. **Kiểm tra CSRF token:**
   ```html
   <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
   ```

### Cho User

1. **Badge đỏ** trên giỏ hàng cho biết số sản phẩm
2. **Click badge** để xem giỏ hàng
3. **Thông báo xanh** = thành công
4. **Thông báo đỏ** = lỗi
5. **Thông báo xanh dương** = thông tin

---

## 📊 Performance Tips

1. **Badge update:** ~100ms
2. **Toast show:** Instant
3. **Auto-refresh:** 30s interval (không ảnh hưởng UX)
4. **API call:** Lightweight, ~50ms

**Tối ưu:**
- Sử dụng cache cho cart count
- Debounce cho rapid clicks
- Lazy load scripts nếu cần

---

## 🎓 Examples

### Example 1: Simple Add to Cart

```html
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <button type="submit">Thêm vào giỏ</button>
</form>
```

### Example 2: With Icon

```html
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <button type="submit" class="btn btn-primary">
        <i class="fas fa-cart-plus me-2"></i>Thêm vào giỏ
    </button>
</form>
```

### Example 3: Quick Add (Icon Only)

```html
<form th:action="@{/cart/add/{id}(id=${book.bookId})}" 
      method="post" 
      class="d-inline">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <input type="hidden" name="redirect" value="/books"/>
    <button type="submit" class="btn btn-sm btn-outline-primary">
        <i class="fas fa-cart-plus"></i>
    </button>
</form>
```

---

## ✅ Checklist Triển Khai

### Backend
- [ ] CartController có tất cả endpoints
- [ ] REST API `/cart/count` hoạt động
- [ ] Kiểm tra duplicate items
- [ ] CSRF protection enabled
- [ ] Flash attributes được set

### Frontend
- [ ] Badge có ID `cartCountBadge`
- [ ] Script `user-main.js` được load
- [ ] Toast function được define
- [ ] Form có CSRF token
- [ ] Redirect parameter (nếu cần)

### Testing
- [ ] Thêm sách → Badge tăng
- [ ] Xóa sách → Badge giảm
- [ ] Badge hiện "5+" khi >5
- [ ] Badge ẩn khi 0
- [ ] Toast hiện đúng
- [ ] Responsive mobile OK

---

## 🚀 Go Live!

Sau khi hoàn thành checklist, hệ thống giỏ hàng sẵn sàng cho production!

**Test cuối:**
1. Thêm nhiều sách
2. Xem badge cập nhật
3. Xóa vài sản phẩm
4. Clear toàn bộ
5. Check trên mobile

**Nếu tất cả OK → Deploy! 🎉**

---

## 📞 Support

**Có vấn đề?**
1. Check console browser (F12)
2. Check server logs
3. Xem docs chi tiết: `CART_IMPROVEMENTS_04_12_2025.md`

**Happy coding! 🛒✨**

