# Fix: Ngăn chặn xóa thiết bị đang đăng nhập

## Vấn đề
Hệ thống đang cho phép người dùng xóa thiết bị mà họ đang sử dụng để đăng nhập, dẫn đến lỗi phiên làm việc và trải nghiệm người dùng không tốt.

## Nguyên nhân
1. **Controller không kiểm tra thiết bị hiện tại**: Method `removeDevice()` trong `UserController` đang gọi `userService.removeDevice()` thay vì `removeDeviceWithCurrentCheck()`
2. **Template hiển thị nút xóa cho thiết bị hiện tại**: Nút xóa xuất hiện cho tất cả thiết bị không phải trusted, kể cả thiết bị đang sử dụng

## Giải pháp đã triển khai

### 1. Backend - UserController.java
**File**: `src/main/java/stu/datn/ebook_store/controller/user/UserController.java`

**Thay đổi**:
```java
// TRƯỚC
@PostMapping("/devices/{deviceId}/remove")
@ResponseBody
public Map<String, Object> removeDevice(
        @PathVariable String deviceId,
        Authentication authentication) {
    // ...
    userService.removeDevice(currentUser.getUserId(), deviceId);
    // ...
}

// SAU
@PostMapping("/devices/{deviceId}/remove")
@ResponseBody
public Map<String, Object> removeDevice(
        @PathVariable String deviceId,
        Authentication authentication,
        jakarta.servlet.http.HttpSession session) {
    // ...
    String currentDeviceId = (String) session.getAttribute("currentDeviceId");
    userService.removeDeviceWithCurrentCheck(currentUser.getUserId(), deviceId, currentDeviceId);
    // ...
}
```

**Lợi ích**:
- Thêm `HttpSession` parameter để lấy `currentDeviceId`
- Sử dụng `removeDeviceWithCurrentCheck()` thay vì `removeDevice()` để kiểm tra thiết bị hiện tại
- Trả về thông báo lỗi rõ ràng khi cố xóa thiết bị đang sử dụng

### 2. Frontend - manage.html
**File**: `src/main/resources/templates/user/devices/manage.html`

**Thay đổi**:
```html
<!-- TRƯỚC -->
<button th:if="${!device.isTrusted}"
        class="btn btn-sm btn-outline-danger btn-remove-device"
        th:attr="data-device-id=${device.deviceId}, data-device-name=${device.deviceName}">
    <i class="fas fa-trash me-1"></i>Xóa
</button>

<!-- SAU -->
<button th:if="${!device.isTrusted && !device.isCurrentDevice}"
        class="btn btn-sm btn-outline-danger btn-remove-device"
        th:attr="data-device-id=${device.deviceId}, data-device-name=${device.deviceName}">
    <i class="fas fa-trash me-1"></i>Xóa
</button>
<button th:if="${device.isCurrentDevice}"
        class="btn btn-sm btn-outline-secondary"
        disabled
        title="Không thể xóa thiết bị đang sử dụng">
    <i class="fas fa-ban me-1"></i>Đang sử dụng
</button>
```

**Lợi ích**:
- Ẩn nút xóa cho thiết bị đang sử dụng
- Hiển thị nút disabled với thông báo rõ ràng cho người dùng
- Cải thiện UX bằng cách ngăn chặn hành động không hợp lệ ngay từ giao diện

### 3. Logic Backend - UserServiceImpl.java
**File**: `src/main/java/stu/datn/ebook_store/service/impl/UserServiceImpl.java`

Logic đã có sẵn (không cần sửa):
```java
@Transactional
public void removeDeviceWithCurrentCheck(String userId, String deviceId, String currentDeviceId) throws Exception {
    // Kiểm tra không cho xóa thiết bị hiện tại
    if (deviceId.equals(currentDeviceId)) {
        throw new Exception("Không thể xóa thiết bị đang sử dụng. Vui lòng đăng xuất trước khi xóa thiết bị này.");
    }
    
    // Gọi logic xóa thông thường
    removeDevice(userId, deviceId);
}
```

## Luồng xử lý mới

### Khi người dùng truy cập trang quản lý thiết bị:
1. Controller lấy `currentDeviceId` từ session
2. Truyền `currentDeviceId` vào `DeviceResponseDto.fromEntity()` để đánh dấu thiết bị hiện tại
3. Template kiểm tra `device.isCurrentDevice`:
   - Nếu `true`: Hiển thị nút disabled "Đang sử dụng"
   - Nếu `false` và `!isTrusted`: Hiển thị nút "Xóa"
   - Nếu `isTrusted`: Hiển thị nút disabled "Không thể xóa"

### Khi người dùng cố xóa thiết bị:
1. JavaScript gửi AJAX request đến `/user/devices/{deviceId}/remove`
2. Controller:
   - Lấy `currentDeviceId` từ session
   - Gọi `removeDeviceWithCurrentCheck(userId, deviceId, currentDeviceId)`
3. Service:
   - Kiểm tra `deviceId.equals(currentDeviceId)`
   - Nếu trùng: Throw exception với message rõ ràng
   - Nếu không: Tiếp tục xóa thiết bị
4. Controller trả về JSON response với success/error
5. JavaScript hiển thị SweetAlert thông báo kết quả

## Các trường hợp kiểm tra

### ✅ Trường hợp 1: Xóa thiết bị khác (không phải current)
- **Kết quả**: Xóa thành công
- **Thông báo**: "Xóa thiết bị thành công"

### ✅ Trường hợp 2: Cố xóa thiết bị hiện tại (via API)
- **Kết quả**: Thất bại
- **Thông báo**: "Không thể xóa thiết bị đang sử dụng. Vui lòng đăng xuất trước khi xóa thiết bị này."

### ✅ Trường hợp 3: Cố xóa thiết bị tin cậy
- **Kết quả**: Thất bại
- **Thông báo**: "Không thể xóa thiết bị tin cậy (thiết bị đăng ký đầu tiên)"

### ✅ Trường hợp 4: Giao diện ẩn nút xóa cho thiết bị hiện tại
- **Kết quả**: Nút "Xóa" không hiển thị, thay bằng nút disabled "Đang sử dụng"

## Lợi ích

### 1. Bảo mật
- Ngăn chặn người dùng vô tình xóa phiên đăng nhập của chính họ
- Tránh lỗ hổng session hijacking

### 2. UX/UI
- Giao diện rõ ràng, người dùng biết thiết bị nào đang sử dụng
- Thông báo lỗi cụ thể, dễ hiểu
- Ngăn chặn hành động không hợp lệ ngay từ giao diện

### 3. Tính toàn vẹn dữ liệu
- Đảm bảo session luôn hợp lệ
- Tránh trường hợp user bị logout đột ngột

## Files đã thay đổi
1. ✅ `src/main/java/stu/datn/ebook_store/controller/user/UserController.java`
2. ✅ `src/main/resources/templates/user/devices/manage.html`

## Files liên quan (không thay đổi)
- `src/main/java/stu/datn/ebook_store/service/impl/UserServiceImpl.java` (logic đã có sẵn)
- `src/main/java/stu/datn/ebook_store/dto/DeviceResponseDto.java` (logic đã có sẵn)
- `src/main/resources/static/user_template/js/device-management.js` (không cần thay đổi)

## Kết luận
✅ **Hoàn thành**: Đã fix lỗi cho phép xóa thiết bị đang đăng nhập
- Backend: Kiểm tra `currentDeviceId` trước khi xóa
- Frontend: Ẩn nút xóa cho thiết bị hiện tại
- UX: Hiển thị badge "Thiết bị hiện tại" và nút disabled rõ ràng

---
**Ngày hoàn thành**: 18/12/2025
**Trạng thái**: ✅ Ready for testing

