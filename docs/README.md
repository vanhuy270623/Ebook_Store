# 📚 TÀI LIỆU DỰ ÁN EBOOK STORE

**Cập nhật:** 23/11/2025  
**Trạng thái:** Đang phát triển

---

## 📁 CẤU TRÚC TÀI LIỆU

### 🔄 Tài Liệu Luồng Xử Lý

#### 1. **SYSTEM_FLOWS.md** 🆕
> Tài liệu tổng hợp toàn bộ các luồng xử lý của hệ thống

**Nội dung:**
- ✅ Kiến trúc tổng quan (Layered Architecture)
- ✅ Luồng Authentication (Register, Login, Logout)
- ✅ Luồng Book Management (CRUD đầy đủ)
- ✅ Luồng File Upload (Image, PDF, EPUB)
- ✅ Luồng Order & Payment (Cart, Checkout, VNPay)
- ✅ Luồng Review & Rating
- ✅ Luồng Reading (Đọc sách, lưu tiến độ)
- ✅ Luồng Security & Authorization

**Sử dụng khi:**
- Cần hiểu cách hệ thống hoạt động
- Implement tính năng mới
- Debug vấn đề
- Onboarding developer mới

---

#### 2. **FLOW_AUTHENTICATION.md**
> Mô tả chi tiết luồng xác thực người dùng

**Nội dung:**
- Sequence diagrams cho Register/Login/Logout
- Code examples chi tiết
- Security configuration
- Session management

**Sử dụng khi:**
- Implement authentication features
- Troubleshoot login issues
- Integrate with OAuth2/JWT

---

### 📊 Tài Liệu Tiến Độ

#### 3. **PROJECT_PROGRESS.md**
> Báo cáo tổng quan tiến độ dự án

**Nội dung:**
- Tổng quan các module đã hoàn thành
- Checklist chi tiết (Entities, Repositories, Services, Controllers)
- Tỷ lệ hoàn thành từng phần
- Roadmap phát triển

**Cập nhật:** Khi hoàn thành một module mới

---

#### 4. **PROGRESS_REPORT_21_11_2025.md**
> Snapshot tiến độ tại ngày 21/11/2025

**Nội dung:**
- Trạng thái backend (40%)
- Trạng thái security (25%)
- Trạng thái frontend (25%)
- Chi tiết từng layer

**Lưu ý:** File snapshot, không cập nhật thường xuyên

---

#### 5. **TODO.md**
> Danh sách công việc cần làm

**Nội dung:**
- Phase 1: Core Backend (DTOs, Services, Controllers)
- Phase 2: Advanced Features (Payment, Email, Search)
- Phase 3: Frontend Enhancement
- Phase 4: Testing & Deployment
- Priority markers: 🔴 High | 🟡 Medium | 🟢 Low

**Cập nhật:** Daily basis

---

## 🎯 HƯỚNG DẪN SỬ DỤNG

### Cho Developer Mới
1. Đọc **SYSTEM_FLOWS.md** để hiểu kiến trúc tổng quan
2. Đọc **FLOW_AUTHENTICATION.md** để hiểu authentication
3. Xem **PROJECT_PROGRESS.md** để biết module nào đã sẵn sàng
4. Check **TODO.md** để chọn task

### Cho Team Lead
1. Review **PROJECT_PROGRESS.md** để track tiến độ
2. Update **TODO.md** để assign tasks
3. Review **PROGRESS_REPORT_*.md** để so sánh tiến độ

### Khi Implement Tính Năng Mới
1. Check **SYSTEM_FLOWS.md** xem flow có sẵn chưa
2. Nếu chưa có, thêm flow mới vào **SYSTEM_FLOWS.md**
3. Update **PROJECT_PROGRESS.md** khi hoàn thành
4. Update **TODO.md** để đánh dấu task completed

### Khi Gặp Lỗi
1. Check **SYSTEM_FLOWS.md** để hiểu expected behavior
2. Review **FLOW_AUTHENTICATION.md** nếu liên quan authentication
3. Check code implementation vs documented flow

---

## 📝 QUY TẮC CẬP NHẬT TÀI LIỆU

### SYSTEM_FLOWS.md
- ✅ **CẬP NHẬT KHI:** Implement tính năng mới có flow phức tạp
- ✅ **ĐỊNH DẠNG:** Sequence diagram + Chi tiết từng bước + Code example
- ❌ **KHÔNG:** Thêm implementation details tạm thời

### PROJECT_PROGRESS.md
- ✅ **CẬP NHẬT KHI:** Hoàn thành một module/layer
- ✅ **ĐỊNH DẠNG:** Checklist + Percentage + Status
- ✅ **TẦN SUẤT:** Mỗi khi có milestone mới

### TODO.md
- ✅ **CẬP NHẬT KHI:** Daily standup, assign task mới
- ✅ **ĐỊNH DẠNG:** Checkbox list + Priority + Timeline
- ✅ **TẦN SUẤT:** Daily

### PROGRESS_REPORT_*.md
- ✅ **TẠO MỚI:** Cuối mỗi tuần/sprint
- ✅ **ĐỊNH DẠNG:** Snapshot toàn bộ tiến độ
- ❌ **KHÔNG:** Cập nhật file cũ (tạo file mới)

---

## 🗑️ FILES ĐÃ XÓA (Lý do)

| File | Lý do xóa | Nội dung đã chuyển sang |
|------|-----------|-------------------------|
| `BOOK_CRUD_SUMMARY.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `CRUD_BOOK_GUIDE.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `FILE_UPLOAD_GUIDE.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `FILE_UPLOAD_QUICK_REF.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `FILE_UPLOAD_STRUCTURE.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `ICON_FIX_GUIDE.md` | Hướng dẫn tạm thời | N/A (đã fix) |
| `LOGIC_VERIFICATION_REPORT.md` | Báo cáo tạm thời | N/A (đã verify) |
| `QUICK_PROGRESS_SUMMARY.md` | Trùng lặp | PROJECT_PROGRESS.md |
| `SQL_ENTITY_VERIFICATION.md` | Báo cáo tạm thời | N/A (đã verify) |
| `SQL_PATH_UPDATE_SUMMARY.md` | Trùng lặp | SYSTEM_FLOWS.md |
| `UPLOAD_ANALYSIS_COMPLETE.md` | Phân tích tạm thời | SYSTEM_FLOWS.md |

**Tổng số file đã xóa:** 11 files  
**Lý do chính:**
- Tránh trùng lặp thông tin
- Tập trung vào tài liệu có giá trị dài hạn
- Dễ dàng maintain và update

---

## 🔄 LUỒNG LÀM VIỆC VỚI TÀI LIỆU

```
┌──────────────────────────────────────────────────────────┐
│              BẮT ĐẦU SPRINT MỚI                          │
└──────────────────────────────────────────────────────────┘
    ↓
1. Review TODO.md → Pick tasks
    ↓
2. Check SYSTEM_FLOWS.md → Understand requirements
    ↓
3. Implement feature
    ↓
4. Update SYSTEM_FLOWS.md (if new flow)
    ↓
5. Update PROJECT_PROGRESS.md (mark completed)
    ↓
6. Update TODO.md (check done)
    ↓
┌──────────────────────────────────────────────────────────┐
│              KẾT THÚC SPRINT                             │
│     Tạo PROGRESS_REPORT_{date}.md (snapshot)             │
└──────────────────────────────────────────────────────────┘
```

---

## 📞 LIÊN HỆ & SUPPORT

- **GitHub Issues:** [Link to issues]
- **Documentation Questions:** [Contact info]
- **Update Requests:** Submit PR to docs folder

---

**Lưu ý:** Tài liệu này được maintain actively. Nếu phát hiện thông tin lỗi thời hoặc thiếu, vui lòng cập nhật hoặc thông báo team.

**Last updated:** 23/11/2025

