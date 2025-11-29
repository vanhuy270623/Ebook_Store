# Admin Controllers - Complete Endpoint Reference
**Date:** 30/11/2025

---

## 📍 All Admin Controller Endpoints

### 🔹 AdminAuthorController (`/admin/authors`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/authors` | List all authors with search | View: `admin/authors/list` |
| GET | `/admin/authors/add` | Show add author form | View: `admin/authors/form` |
| POST | `/admin/authors/create` | Create new author | Redirect or form with errors |
| GET | `/admin/authors/view/{id}` | View author details | View: `admin/authors/view` |
| GET | `/admin/authors/edit/{id}` | Show edit author form | View: `admin/authors/form` |
| POST | `/admin/authors/update` | Update author | Redirect or form with errors |
| POST | `/admin/authors/delete/{id}` | Delete author | JSON: `{success, message}` |
| POST | `/admin/authors/upload-avatar` | Upload author avatar | JSON: `{success, url}` |
| GET | `/admin/authors/statistics` | View author statistics | View: `admin/authors/statistics` |

**ID Format:** `author_01`, `author_02`, ...  
**Validation:** Name (NotBlank, max 255), Biography (max 5000), Avatar URL (max 500)

---

### 🔹 AdminCategoryController (`/admin/categories`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/categories` | List all categories | View: `admin/categories/list` |
| GET | `/admin/categories/add` | Show add category form | View: `admin/categories/form` |
| POST | `/admin/categories/create` | Create new category | Redirect or form with errors |
| GET | `/admin/categories/view/{id}` | View category details | View: `admin/categories/view` |
| GET | `/admin/categories/edit/{id}` | Show edit category form | View: `admin/categories/form` |
| POST | `/admin/categories/update` | Update category | Redirect or form with errors |
| POST | `/admin/categories/delete/{id}` | Delete category | JSON: `{success, message}` |
| GET | `/admin/categories/statistics` | View category statistics | View: `admin/categories/statistics` |

**ID Format:** `category_01`, `category_02`, ...  
**Validation:** Category name (NotBlank, max 255), unique name check

---

### 🔹 AdminCouponController (`/admin/coupons`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/coupons` | List all coupons with search | View: `admin/coupons/list` |
| GET | `/admin/coupons/add` | Show add coupon form | View: `admin/coupons/form` |
| POST | `/admin/coupons/create` | Create new coupon | Redirect or form with errors |
| GET | `/admin/coupons/view/{id}` | View coupon details | View: `admin/coupons/view` |
| GET | `/admin/coupons/edit/{id}` | Show edit coupon form | View: `admin/coupons/form` |
| POST | `/admin/coupons/update` | Update coupon | Redirect or form with errors |
| POST | `/admin/coupons/delete/{id}` | Delete coupon | JSON: `{success, message}` |
| GET | `/admin/coupons/statistics` | View coupon statistics | View: `admin/coupons/statistics` |

**ID Format:** `coupon_01`, `coupon_02`, ...  
**Validation:** Code (NotBlank, unique), Discount type (PERCENT/FIXED), Date validation (validTo > validFrom)

---

### 🔹 AdminBannerController (`/admin/banners`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/banners` | List user banners | View: `admin/banners/list` |
| GET | `/admin/banners/add` | Show add banner form | View: `admin/banners/form` |
| POST | `/admin/banners/create` | Create new banner | Redirect or form with errors |
| GET | `/admin/banners/view/{id}` | View banner details | View: `admin/banners/view` |
| GET | `/admin/banners/edit/{id}` | Show edit banner form | View: `admin/banners/form` |
| POST | `/admin/banners/update` | Update banner | Redirect or form with errors |
| POST | `/admin/banners/delete/{id}` | Delete banner | JSON: `{success, message}` |
| POST | `/admin/banners/upload-image` | Upload banner image | JSON: `{success, url}` |
| POST | `/admin/banners/toggle-status/{id}` | Toggle banner active/inactive | Redirect |
| GET | `/admin/banners/statistics` | View banner statistics | View: `admin/banners/statistics` |

**ID Format:** `banner_01`, `banner_02`, ...  
**Validation:** Title (NotBlank, max 255), Image URL (NotBlank), Position (HOME/CATEGORY/DETAIL)  
**Special:** User-scoped (queries by current user)

---

### 🔹 AdminPostController (`/admin/posts`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/posts` | List all posts with search | View: `admin/posts/list` |
| GET | `/admin/posts/add` | Show add post form | View: `admin/posts/form` |
| POST | `/admin/posts/create` | Create new post | Redirect or form with errors |
| GET | `/admin/posts/view/{id}` | View post details | View: `admin/posts/view` |
| GET | `/admin/posts/edit/{id}` | Show edit post form | View: `admin/posts/form` |
| POST | `/admin/posts/update` | Update post | Redirect or form with errors |
| POST | `/admin/posts/delete/{id}` | Delete post | JSON: `{success, message}` |
| POST | `/admin/posts/toggle-publish/{id}` | Toggle post published/unpublished | Redirect |
| GET | `/admin/posts/statistics` | View post statistics | View: `admin/posts/statistics` |

**ID Format:** `post_01`, `post_02`, ...  
**Validation:** Title (NotBlank, max 500), Slug (NotBlank, unique), Content (NotBlank)  
**Special:** User-scoped (created by current user), slug duplicate check

---

### 🔹 AdminReviewController (`/admin/reviews`)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/admin/reviews` | List reviews (filter: unapproved/approved/rejected/verified) | View: `admin/reviews/list` |
| GET | `/admin/reviews/view/{id}` | View review details | View: `admin/reviews/view` |
| POST | `/admin/reviews/approve/{id}` | Approve review | Redirect |
| POST | `/admin/reviews/reject/{id}` | Reject review | Redirect |
| POST | `/admin/reviews/delete/{id}` | Delete review | JSON: `{success, message}` |
| POST | `/admin/reviews/bulk-approve` | Bulk approve multiple reviews | JSON: `{success, message}` |
| POST | `/admin/reviews/bulk-reject` | Bulk reject multiple reviews | JSON: `{success, message}` |
| GET | `/admin/reviews/statistics` | View review statistics | View: `admin/reviews/statistics` |

**Filter Types:** `unapproved` (default), `approved`, `rejected`, `verified`  
**Bulk Parameters:** `reviewIds[]` (array of review IDs)

---

## 📊 Request/Response Formats

### Form Submission Pattern
```
GET  /admin/{resource}/add        → Show form
POST /admin/{resource}/create     → Submit form
                                  ↓
                                  Validation
                                  ↓
                        Success: redirect
                        Error: return form with errors
```

### JSON Response Pattern (Delete, Upload, Bulk)
```json
{
  "success": true/false,
  "message": "Operation result message",
  "url": "uploaded file url (if applicable)"
}
```

### Common Model Attributes
- `{resource}`: Entity object (for view)
- `{resource}Request`: DTO (for form)
- `error`: Error messages
- `success`: Success messages
- `is{Resource}`: Boolean flags
- `totalCount`: Statistics
- `recentItems`: Recent records

---

## 🔐 Security & Authentication

- ✅ All endpoints require Spring Security authentication
- ✅ Role-based access via @PreAuthorize (if configured)
- ✅ CSRF protection enabled
- ✅ File upload validation
- ✅ Input validation via DTOs

---

## 📝 Common Query Parameters

| Controller | Parameter | Values | Description |
|-----------|-----------|--------|-------------|
| Authors | `search` | String | Search by author name |
| Coupons | `search` | String | Search by coupon code |
| Posts | `search` | String | Search by post title/content |
| Reviews | `filter` | unapproved/approved/rejected/verified | Filter reviews by status |
| Banners | - | - | No special filtering |
| Categories | - | - | No special filtering |

---

## 🚀 URL Patterns Summary

```
POST   /admin/{resource}/create              ← Create
GET    /admin/{resource}                     ← List
GET    /admin/{resource}/view/{id}           ← View
GET    /admin/{resource}/edit/{id}           ← Edit form
POST   /admin/{resource}/update              ← Update
POST   /admin/{resource}/delete/{id}         ← Delete
POST   /admin/{resource}/upload-*            ← Upload (varied)
POST   /admin/{resource}/toggle-*            ← Toggle (varied)
GET    /admin/{resource}/statistics          ← Statistics
```

---

## 💾 Flash Attributes (Redirects)

### Success Messages
- "Thêm {resource} thành công! ID: {id}, Tên: {name}"
- "Cập nhật {resource} thành công!"
- "Xóa {resource} thành công!"
- "Duyệt đánh giá thành công!"

### Error Messages
- "Không tìm thấy {resource} với ID: {id}"
- "Tên {resource} đã tồn tại!"
- "Mã {resource} đã tồn tại!"
- "Slug bài viết đã được sử dụng!"
- "Lỗi: {exception message}"

---

## 📌 Important Notes

1. **ID Generation:** Automatically based on current count + 1
2. **Duplicate Checks:** Exclude current entity when updating
3. **File Uploads:** Use FileStorageService methods
4. **User Context:** Some controllers (Banner, Post) are user-scoped
5. **Bulk Operations:** Only in ReviewController
6. **Toggle Operations:** Banner status, Post publish status
7. **Search/Filter:** Available in Author, Coupon, Post, Review

---

## 🎯 Testing Checklist

For each controller, test:
- [ ] List endpoint (GET)
- [ ] Add form endpoint (GET)
- [ ] Create endpoint with valid data (POST)
- [ ] Create endpoint with validation errors (POST)
- [ ] View endpoint (GET)
- [ ] Edit form endpoint (GET)
- [ ] Update endpoint with valid data (POST)
- [ ] Update endpoint with validation errors (POST)
- [ ] Delete endpoint (POST)
- [ ] Special endpoints (upload, toggle, etc.)
- [ ] Statistics endpoint (GET)

---

**Last Updated:** 30/11/2025  
**Total Endpoints:** 52+  
**Status:** ✅ All endpoints implemented


