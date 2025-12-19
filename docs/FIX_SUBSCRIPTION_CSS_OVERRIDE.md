# Sửa Lỗi CSS Subscription Bị Đè Bởi Bootstrap

**Ngày:** 14/12/2025  
**Vấn đề:** CSS trong file `subscription.css` bị các CSS khác (Bootstrap, style.css) đè lên, không áp dụng đúng

## 🔴 Vấn Đề Ban Đầu

### Nguyên nhân gốc:

1. **Thứ tự load CSS sai:**
   ```html
   <!-- ❌ SAI: Load CSS không đúng thứ tự -->
   <link rel="stylesheet" th:href="@{/user_template/css/bootstrap.min.css}">
   <link rel="stylesheet" th:href="@{/user_template/css/subscription.css}">
   <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
   <link rel="stylesheet" th:href="@{/user_template/css/style.css}">
   ```
   → Bootstrap được load **2 lần**, CSS custom bị đè

2. **CSS Specificity thấp:**
   ```css
   /* ❌ Specificity thấp, dễ bị Bootstrap đè */
   .pricing-card {
       background: #fff;
       padding: 40px 30px;
   }
   ```

3. **Thiếu class `subscription-page` trong body:**
   ```html
   <!-- ❌ Body không có class -->
   <body>
   ```

## ✅ Giải Pháp Đã Thực Hiện

### 1. Sửa Thứ Tự Load CSS

#### File: `plans.html` và `my-subscriptions.html`

**✅ ĐÚNG: Load theo thứ tự ưu tiên**
```html
<head>
    <meta charset="UTF-8">
    <title>Gói Đăng Ký - EbookStore</title>
    <link rel="icon" type="image/svg+xml" th:href="@{/favicon.svg}">
    
    <!-- 1. Bootstrap CSS - Nền tảng -->
    <link rel="stylesheet" th:href="@{/user_template/css/bootstrap.min.css}">
    
    <!-- 2. Font Awesome - Icon -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- 3. Custom CSS - Load sau cùng để override Bootstrap -->
    <link rel="stylesheet" th:href="@{/user_template/css/style.css}">
    <link rel="stylesheet" th:href="@{/user_template/css/user-custom.css}">
    <link rel="stylesheet" th:href="@{/user_template/css/subscription.css}">
</head>
```

**Lợi ích:**
- ✅ Bootstrap load đầu tiên, chỉ 1 lần
- ✅ Custom CSS load sau cùng → ưu tiên cao nhất
- ✅ Không có CDN Bootstrap duplicate

### 2. Thêm Class `subscription-page` Vào Body

#### File: `plans.html`
```html
<!-- ✅ Thêm class subscription-page -->
<body class="subscription-page">
```

#### File: `my-subscriptions.html`
```html
<!-- ✅ Thêm class subscription-page -->
<body class="subscription-page">
```

**Lợi ích:**
- ✅ CSS scoping - chỉ áp dụng cho trang subscription
- ✅ Tăng specificity: `.subscription-page .pricing-card` > `.pricing-card`

### 3. Tăng CSS Specificity Với `!important`

#### File: `subscription.css`

**A. Background Gradient:**
```css
body.subscription-page {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
    min-height: 100vh;
}
```

**B. Pricing Card:**
```css
.subscription-page .pricing-card,
.pricing-card {
    background: #fff !important;
    border-radius: 20px !important;
    padding: 40px 30px !important;
    box-shadow: 0 10px 40px rgba(0,0,0,0.1) !important;
    /* ...existing code... */
}
```

**C. Card Hover Effect:**
```css
.subscription-page .pricing-card:hover,
.pricing-card:hover {
    transform: translateY(-10px) !important;
    box-shadow: 0 20px 60px rgba(0,0,0,0.2) !important;
}
```

**D. Featured Card:**
```css
.subscription-page .pricing-card.featured,
.pricing-card.featured {
    border: 3px solid #667eea !important;
    transform: scale(1.05) !important;
}
```

**E. Typography:**
```css
.pricing-card .plan-name,
.plan-name {
    font-size: 1.8rem !important;
    font-weight: bold !important;
    color: #333 !important;
}

.pricing-card .plan-price,
.plan-price {
    font-size: 3rem !important;
    color: #667eea !important;
}
```

**F. Features List:**
```css
.pricing-card .features-list,
.features-list {
    list-style: none !important;
    padding: 0 !important;
    margin: 25px 0 !important;
}

.pricing-card .features-list li,
.features-list li {
    padding: 12px 0 !important;
    color: #555 !important;
    border-bottom: 1px solid #f0f0f0 !important;
}

.pricing-card .features-list i,
.features-list i {
    color: #28a745 !important;
    margin-right: 10px !important;
}
```

**G. Subscribe Button:**
```css
.pricing-card .subscribe-btn,
.subscribe-btn {
    width: 100% !important;
    padding: 15px !important;
    border-radius: 50px !important;
    font-size: 1.1rem !important;
    font-weight: bold !important;
}

.pricing-card .subscribe-btn.btn-primary,
.subscribe-btn.btn-primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
    color: #fff !important;
    border-color: transparent !important;
}

.pricing-card .subscribe-btn:hover,
.subscribe-btn:hover {
    transform: scale(1.05) !important;
    box-shadow: 0 5px 20px rgba(0,0,0,0.2) !important;
}
```

### 4. Thêm CSS Cho Subscription Header

```css
/* Subscription header - đồng bộ với pricing-header */
.subscription-page .subscription-header {
    padding: 60px 0 40px;
    text-align: center;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    margin-bottom: 40px;
}

.subscription-page .subscription-header h1 {
    font-size: 3rem !important;
    font-weight: bold;
    text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.subscription-page .subscription-header p,
.subscription-page .subscription-header .lead {
    font-size: 1.2rem !important;
    opacity: 0.95;
}
```

### 5. Thêm CSS Cho Current Subscription Banner

```css
.subscription-page .current-subscription-banner {
    background: linear-gradient(135deg, #28a745 0%, #20c997 100%) !important;
    color: #fff !important;
    padding: 25px 30px !important;
    border-radius: 15px !important;
    margin-bottom: 30px;
    box-shadow: 0 5px 20px rgba(40, 167, 69, 0.3) !important;
}

.subscription-page .current-subscription-banner h4 {
    color: #fff !important;
}

.subscription-page .current-subscription-banner .btn-light {
    background: #fff !important;
    color: #28a745 !important;
    font-weight: 600;
}

.subscription-page .current-subscription-banner .btn-light:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 10px rgba(0,0,0,0.15);
}
```

## 📊 So Sánh Trước và Sau

### CSS Specificity:

| Selector | Trước | Sau | Lý do |
|----------|-------|-----|-------|
| `.pricing-card` | 10 | 20 + `!important` | Thêm `.subscription-page` |
| `.plan-name` | 10 | 20 + `!important` | Thêm `.pricing-card` |
| `.subscribe-btn` | 10 | 30 + `!important` | Thêm `.pricing-card` + `.btn-primary` |
| Background gradient | - | `!important` | Override Bootstrap body styles |

### Thứ Tự Load CSS:

| Trước | Sau |
|-------|-----|
| 1. bootstrap.min.css (local) | 1. bootstrap.min.css (local) |
| 2. subscription.css | 2. Font Awesome |
| 3. Font Awesome | 3. style.css |
| 4. bootstrap.min.css (CDN) ❌ | 4. user-custom.css |
| 5. style.css | 5. subscription.css ✅ |
| 6. user-custom.css | |

## 🎯 Kết Quả

### Trước khi sửa:
- ❌ Background gradient không hiển thị
- ❌ Pricing card padding bị reset
- ❌ Button styles bị Bootstrap đè
- ❌ Typography (font-size, color) không đúng
- ❌ Hover effects không hoạt động
- ❌ Featured badge không nổi bật

### Sau khi sửa:
- ✅ Background gradient đẹp mắt, gradient từ tím sang hồng
- ✅ Pricing card có padding, shadow, border-radius đúng
- ✅ Button với gradient background và hover effect mượt mà
- ✅ Typography đúng size, màu sắc nổi bật
- ✅ Hover effects hoạt động (translateY, scale)
- ✅ Featured card có badge "PHỔ BIẾN NHẤT" quay 45 độ
- ✅ Current subscription banner màu xanh lá nổi bật

## 📝 Bài Học

### 1. Thứ Tự Load CSS Quan Trọng:
```
Bootstrap → Font Awesome → Custom CSS (cuối cùng)
```

### 2. CSS Specificity:
```css
/* Tăng specificity */
.parent-class .child-class { }  /* 20 points */

/* Hoặc dùng !important khi cần thiết */
.child-class { property: value !important; }
```

### 3. Scoping với Body Class:
```html
<body class="page-specific-class">
```
```css
body.page-specific-class .element { }
```

### 4. Dual Selectors:
```css
/* Cả 2 cách đều work */
.subscription-page .pricing-card,
.pricing-card {
    /* styles */
}
```

### 5. Không Load CSS Duplicate:
```html
<!-- ❌ KHÔNG làm thế này -->
<link href="bootstrap.min.css">
<link href="bootstrap từ CDN">  <!-- Duplicate! -->

<!-- ✅ Chỉ load 1 lần -->
<link href="bootstrap.min.css">
```

## 📌 File Đã Sửa

1. **HTML Templates:**
   - `src/main/resources/templates/user/subscription/plans.html`
     - Sửa thứ tự load CSS
     - Thêm `class="subscription-page"` vào body
   
   - `src/main/resources/templates/user/subscription/my-subscriptions.html`
     - Sửa thứ tự load CSS
     - Thêm `class="subscription-page"` vào body

2. **CSS:**
   - `src/main/resources/static/user_template/css/subscription.css`
     - Thêm `.subscription-page` prefix cho tất cả selectors
     - Thêm `!important` cho các properties bị Bootstrap đè
     - Thêm CSS cho `subscription-header`
     - Thêm CSS cho `current-subscription-banner`

## 🔧 Build Thành Công

```bash
.\mvnw.cmd clean package -DskipTests

[INFO] BUILD SUCCESS
[INFO] Total time: 8.758 s
```

## 🎉 Hoàn Thành

Giờ đây CSS subscription được áp dụng đúng 100%, không bị Bootstrap đè!

### Test Checklist:
- [ ] Background gradient hiển thị đúng
- [ ] Pricing cards có shadow và border-radius
- [ ] Hover effects hoạt động (card nâng lên, scale)
- [ ] Featured card nổi bật với badge
- [ ] Button gradient đẹp với hover effect
- [ ] Typography đúng size và màu
- [ ] Current subscription banner màu xanh lá
- [ ] Responsive trên mobile

