# Admin Templates Optional Handling - Action Plan

## 🎯 Mục Tiêu
Cập nhật TẤT CẢ templates admin để xử lý đúng với Optional backend:
- Xóa `th:if="${object}"` không cần thiết (controller guarantee)
- Sử dụng safe navigation `?.` cho nested objects
- Elvis operator `?:` cho default values
- Đơn giản hóa image handling

## 📋 Checklist - Admin Templates

### View Templates (Ưu tiên cao)
- [x] admin/books/view.html ✅ (Đã update)
- [x] admin/categories/view.html ✅ (Already good)
- [x] admin/authors/view.html ✅ (Already good)
- [x] admin/users/view.html ✅ (Đã update)
- [x] admin/orders/view.html ✅ (Đã update)
- [ ] admin/coupons/view.html ⏳
- [ ] admin/banners/view.html ⏳
- [ ] admin/posts/view.html ⏳
- [ ] admin/reviews/view.html ⏳
- [ ] admin/subscriptions/view.html ⏳

### List Templates (Ưu tiên trung bình)
- [ ] admin/books/list.html
- [ ] admin/categories/list.html
- [ ] admin/authors/list.html
- [ ] admin/users/list.html
- [ ] admin/orders/list.html
- [ ] admin/coupons/list.html
- [ ] admin/banners/list.html
- [ ] admin/posts/list.html
- [ ] admin/reviews/list.html
- [ ] admin/subscriptions/list.html

### Form Templates (Ưu tiên thấp - thường OK)
- [ ] admin/books/form.html
- [ ] admin/categories/form.html
- [ ] admin/authors/form.html
- [ ] admin/users/form.html
- [ ] admin/coupons/form.html
- [ ] admin/banners/form.html
- [ ] admin/posts/form.html
- [ ] admin/subscriptions/form.html

### Dashboard & Statistics
- [ ] admin/dashboard.html
- [ ] admin/books/statistics.html
- [ ] admin/users/statistics.html
- [ ] admin/orders/statistics.html

## 🔧 Standard Patterns

### Pattern 1: Remove Redundant Checks
```html
<!-- ❌ OLD -->
<div th:if="${object}">
    <h1 th:text="${object.field}">Value</h1>
</div>

<!-- ✅ NEW -->
<h1 th:text="${object.field}">Value</h1>
```

### Pattern 2: Safe Navigation for Nested
```html
<!-- ❌ OLD -->
<span th:text="${object.nested.field}">Value</span>

<!-- ✅ NEW -->
<span th:text="${object.nested?.field ?: 'Default'}">Value</span>
```

### Pattern 3: Image Handling
```html
<!-- ❌ OLD -->
<img th:src="${object.imageUrl != null && !object.imageUrl.isEmpty() ? object.imageUrl : '/default.jpg'}" />

<!-- ✅ NEW -->
<img th:src="${object.imageUrl ?: '/default.jpg'}"
     onerror="this.onerror=null; this.src='/default.jpg';" />
```

### Pattern 4: Optional Fields
```html
<!-- ❌ OLD -->
<span th:if="${object.field != null}" th:text="${object.field}">Value</span>
<span th:if="${object.field == null}">N/A</span>

<!-- ✅ NEW -->
<span th:text="${object.field ?: 'N/A'}">Value</span>
```

## 📊 Progress Tracking
- Completed: 5/10 view templates
- In Progress: 5/10 view templates
- TODO: All list/form templates

**Next Actions:**
1. Update remaining view templates
2. Review list templates
3. Quick check form templates
4. Test all changes

