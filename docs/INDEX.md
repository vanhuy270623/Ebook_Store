# 📚 EBOOK STORE - DOCUMENTATION QUICK INDEX

> **Quick access to all documentation**  
> **Last Updated**: 30/11/2025  
> **Total Files**: 25

---

## 🚀 START HERE

### For New Developers
1. 📖 **[README_TECHNICAL.md](README_TECHNICAL.md)** - Technical overview
2. 🏗️ **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture
3. 📊 **[README_FLOWS.md](README_FLOWS.md)** - All flows with debugging

### For Testing/QA
1. 📋 **[README_FLOWS.md](README_FLOWS.md)** - Test scenarios
2. 🔐 **[FLOW_01_AUTHENTICATION.md](FLOW_01_AUTHENTICATION.md)** - Auth testing
3. 🛒 **[FLOW_03_SHOPPING_CART_CHECKOUT.md](FLOW_03_SHOPPING_CART_CHECKOUT.md)** - E-commerce testing

### For Project Managers
1. ✅ **[TODO.md](TODO.md)** - Current tasks
2. 📈 **[PROJECT_PROGRESS.md](PROJECT_PROGRESS.md)** - Overall status
3. 📝 **[FINAL_DAY_SUMMARY_30_11_2025_AFTERNOON.md](FINAL_DAY_SUMMARY_30_11_2025_AFTERNOON.md)** - Latest updates

---

## 📂 All Files by Category

### 🔄 Flow Documents (5 files) ⭐ NEW
- **[README_FLOWS.md](README_FLOWS.md)** - Index with debugging guide
- **[FLOW_01_AUTHENTICATION.md](FLOW_01_AUTHENTICATION.md)** - Login, Register, Logout
- **[FLOW_02_ADMIN_BOOK_MANAGEMENT.md](FLOW_02_ADMIN_BOOK_MANAGEMENT.md)** - Book CRUD + Upload
- **[FLOW_03_SHOPPING_CART_CHECKOUT.md](FLOW_03_SHOPPING_CART_CHECKOUT.md)** - Cart, Coupon, Payment
- **[FLOW_04_USER_ACCOUNT_MANAGEMENT.md](FLOW_04_USER_ACCOUNT_MANAGEMENT.md)** - Profile, Orders, History

### 📈 Progress & Summary (7 files)
- **[PROGRESS_REPORT_21_11_2025.md](PROGRESS_REPORT_21_11_2025.md)** - Database & Entities
- **[PROGRESS_REPORT_23_11_2025.md](PROGRESS_REPORT_23_11_2025.md)** - Repository layer
- **[PROGRESS_REPORT_24_11_2025.md](PROGRESS_REPORT_24_11_2025.md)** - Services & DTOs
- **[PROGRESS_REPORT_24_11_2025_DOCS.md](PROGRESS_REPORT_24_11_2025_DOCS.md)** - Documentation
- **[PROGRESS_REPORT_28_11_2025.md](PROGRESS_REPORT_28_11_2025.md)** - Cleanup
- **[FINAL_DAY_SUMMARY_30_11_2025.md](FINAL_DAY_SUMMARY_30_11_2025.md)** - Morning work
- **[FINAL_DAY_SUMMARY_30_11_2025_AFTERNOON.md](FINAL_DAY_SUMMARY_30_11_2025_AFTERNOON.md)** - This afternoon ⭐

### 📚 Core Technical (8 files)
- **[README_TECHNICAL.md](README_TECHNICAL.md)** - Main technical doc
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture
- **[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)** - 19 tables design
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - 50+ endpoints
- **[SECURITY_CONFIG.md](SECURITY_CONFIG.md)** - Spring Security
- **[SERVICE_LAYER.md](SERVICE_LAYER.md)** - Business logic
- **[FRONTEND_STRUCTURE.md](FRONTEND_STRUCTURE.md)** - Thymeleaf templates
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Project layout

### 🗂️ Reference (5 files)
- **[README.md](README.md)** - Main documentation index
- **[TODO.md](TODO.md)** - Tasks & roadmap
- **[PROJECT_PROGRESS.md](PROJECT_PROGRESS.md)** - Overall status
- **[ADMIN_ENDPOINTS_REFERENCE.md](ADMIN_ENDPOINTS_REFERENCE.md)** - Admin API reference
- **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** - Documentation map

---

## 🎯 Quick Links by Task

### Authentication & Security
- [Login Flow](FLOW_01_AUTHENTICATION.md#flow-12-đăng-nhập)
- [Register Flow](FLOW_01_AUTHENTICATION.md#flow-11-đăng-ký)
- [Security Config](SECURITY_CONFIG.md)
- [Debug Auth](FLOW_01_AUTHENTICATION.md#debugging-endpoints)

### Book Management
- [Create Book](FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-22-create-book)
- [Upload Files](FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-25-upload-files)
- [Delete Book](FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-24-delete-book)
- [Debug Books](FLOW_02_ADMIN_BOOK_MANAGEMENT.md#debugging-endpoints)

### Shopping & Checkout
- [Add to Cart](FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-32-add-to-cart)
- [Apply Coupon](FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-35-apply-coupon)
- [Checkout](FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-36-checkout)
- [Payment](FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-37-payment-processing)
- [Debug Cart](FLOW_03_SHOPPING_CART_CHECKOUT.md#debugging-endpoints)

### User Account
- [Update Profile](FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-42-update-profile)
- [Change Password](FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-43-change-password)
- [Order History](FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-45-order-history)
- [Debug Account](FLOW_04_USER_ACCOUNT_MANAGEMENT.md#debugging-endpoints)

### Database
- [Schema Design](DATABASE_SCHEMA.md)
- [ER Diagram](DATABASE_SCHEMA.md#entity-relationship-diagram)
- [Sample Data](DATABASE_SCHEMA.md#sample-data)

### API Reference
- [All Endpoints](API_DOCUMENTATION.md)
- [Admin Endpoints](ADMIN_ENDPOINTS_REFERENCE.md)
- [Authentication APIs](API_DOCUMENTATION.md#authentication-apis)

---

## 🔧 Debug Quick Access

### Common Debug Endpoints
```bash
# Check authentication
curl http://localhost:8080/debug/current-user -H "Cookie: JSESSIONID=xxx"

# Check cart
curl http://localhost:8080/debug/cart -H "Cookie: JSESSIONID=xxx"

# Check book details
curl http://localhost:8080/debug/book/1

# Check user info
curl http://localhost:8080/debug/user-info -H "Cookie: JSESSIONID=xxx"

# Check coupon
curl http://localhost:8080/debug/coupon/SUMMER2024
```

### Debug Guides
- [Flow 01 Debug](FLOW_01_AUTHENTICATION.md#debugging-endpoints) - 8 endpoints
- [Flow 02 Debug](FLOW_02_ADMIN_BOOK_MANAGEMENT.md#debugging-endpoints) - 8 endpoints
- [Flow 03 Debug](FLOW_03_SHOPPING_CART_CHECKOUT.md#debugging-endpoints) - 8 endpoints
- [Flow 04 Debug](FLOW_04_USER_ACCOUNT_MANAGEMENT.md#debugging-endpoints) - 7 endpoints

**Total**: 31 debug endpoints

---

## 📊 Statistics

| Metric | Count |
|--------|-------|
| Total Files | 25 |
| Flow Documents | 5 |
| Progress Reports | 7 |
| Core Documentation | 8 |
| Reference Files | 5 |
| Total Lines | 50,000+ |
| Debug Endpoints | 31 |
| Test Scenarios | 22 |

---

## 🔄 Update History

### 30/11/2025 - Version 3.0 ⭐
- Created 5 new flow documents
- Added 31 debug endpoints
- Added 22 test scenarios
- Deleted 26 redundant files
- Total restructure complete

### 28/11/2025 - Version 2.0
- Documentation cleanup
- Organized structure

### 24/11/2025 - Version 1.x
- Initial documentation

---

## 📞 Need Help?

1. **Can't find something?** Check this index
2. **Need to debug?** See [README_FLOWS.md](README_FLOWS.md)
3. **Want overview?** See [README_TECHNICAL.md](README_TECHNICAL.md)
4. **Current tasks?** See [TODO.md](TODO.md)

---

**Last Updated**: 30/11/2025  
**Maintained by**: Development Team  
**Status**: ✅ Production Ready

---

*Use Ctrl+F to search for specific topics!*

