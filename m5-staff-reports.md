# M5: Staff, Auth & Reports

Folder: `src/main/java/com/foodstore/auth/` và `src/main/java/com/foodstore/reports/` · Owner: **Owner 5** (điền tên) · Trải qua nhiều phase: `Staff` hierarchy + login ở **Phase 0** (chạy song song với Foundation, chặn tất cả), `StaffService`/`ReportService` ở **Phase 1**, dashboard wiring ở **Phase 2** (sau khi panel M1-M4 tồn tại).

> **Lưu ý đặt tên:** các lớp vai trò là `Admin`, `Cashier`, `Waiter`, `Chef` (kế thừa `Staff`). Không dùng tên trùng với entity ở module khác; nếu `MainFrame` cần import cả hai, trùng tên sẽ gây lỗi biên dịch.

## Files

| File | Phase | Mục đích |
|------|-------|----------|
| `auth/Staff.java` | 0 | Abstract — `id`, `username`, `fullName`; phương thức trừu tượng `Set<Screen> allowedScreens()` |
| `auth/Screen.java` | 0 | Enum `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS` |
| `auth/Admin.java` | 0 | `extends Staff` — toàn bộ `Screen` |
| `auth/Cashier.java` | 0 | `extends Staff` — `ORDERS`, `PAYMENTS`, `CUSTOMERS` |
| `auth/Waiter.java` | 0 | `extends Staff` — `TABLES`, `ORDERS`, `MENU` |
| `auth/Chef.java` | 0 | `extends Staff` — `KITCHEN`, `INVENTORY` |
| `auth/StaffRole.java` / `auth/StaffAccount.java` / `auth/AuthenticationFailedException.java` | 0 | Enum vai trò (khớp ENUM `staff.role`); record `StaffAccount(staff, passwordHash, active)` — hash chỉ nằm ở đây, không nằm trong `Staff`; exception đăng nhập thất bại |
| `auth/StaffDao.java` | 0 | Interface — Phase 0: `Optional<StaffAccount> findByUsername(String)`; Phase 1 **thêm** `findAll()`, `save`, `update`, `setActive(long, boolean)`, `countActiveAdmins()` |
| `auth/JdbcStaffDao.java` | 0 | `JdbcTemplate` — SQL viết tay; ánh xạ cột `role` sang đúng lớp con `Staff` |
| `auth/AuthService.java` | 0 | `login(username, password)` — nạp qua `StaffDao`, kiểm bằng `PasswordEncoder`, từ chối tài khoản `is_active = FALSE` |
| `auth/CurrentSession.java` | 0 | Giữ `Staff` đang đăng nhập suốt vòng đời app |
| `auth/LoginFrame.java` | 0 | Swing `JFrame` — form đăng nhập, gọi `AuthService`, mở `MainFrame` khi thành công |
| `src/test/java/com/foodstore/auth/AuthServiceTest.java` | 0 | Unit test — đúng/sai mật khẩu, tài khoản bị khóa; `StaffDao` mock |
| `auth/StaffService.java` | 1 | CRUD nhân viên: băm BCrypt khi tạo/đổi mật khẩu, chặn tắt/xóa Admin cuối cùng, chặn tự khóa chính mình |
| `auth/StaffPanel.java` | 1 | Swing `JPanel` (Admin) — bảng nhân viên, thêm/sửa/khóa |
| `src/test/java/com/foodstore/auth/StaffServiceTest.java` | 1 | Unit test — mật khẩu lưu dạng hash, không tắt được Admin cuối |
| `reports/ReportDao.java` / `reports/JdbcReportDao.java` | 1 | Truy vấn tổng hợp (`GROUP BY`): doanh thu theo ngày/tháng, top món bán chạy, nguyên liệu tồn thấp |
| `reports/ReportService.java` | 1 | Gọi `ReportDao`, kiểm tra khoảng ngày hợp lệ |
| `reports/ReportPanel.java` | 1 | Swing `JPanel` (Admin) — chọn khoảng ngày, bảng doanh thu, top món, cảnh báo kho |
| `auth/PlaceholderPanel.java` | 0 | Panel giữ chỗ cho màn hình chưa có module thật |
| `auth/MainFrame.java` | 0 khung / 2 nối panel | Khung `CardLayout`, menu bên trái dựng **đa hình** từ `Staff.allowedScreens()`. Phase 0 đã có bản chạy được với `PlaceholderPanel`; Phase 2 chỉ sửa `createPanel(Screen)` để trả panel thật |

## Tasks — Phase 0 (chặn tất cả, làm song song với `foundation.md`)

- Task: `Staff` abstract + `Screen` + `Admin`, `Cashier`, `Waiter`, `Chef`; `StaffDao` (interface + `JdbcStaffDao`) với `findByUsername`
  - Acceptance: unit test nạp một dòng staff seed và nhận về đúng lớp con theo `role`; `allowedScreens()` của từng lớp khớp bảng phân quyền trong `SPEC.md`
  - Verify: `mvn test`
  - Files: `auth/Staff.java`, `auth/Screen.java`, `auth/Admin.java`, `auth/Cashier.java`, `auth/Waiter.java`, `auth/Chef.java`, `auth/StaffDao.java`, `auth/JdbcStaffDao.java`
- Task: `AuthService` + `CurrentSession` — kiểm username/password với bảng `staff` bằng `BCryptPasswordEncoder`, giữ `Staff` đang đăng nhập; `LoginFrame` (form Swing cơ bản, chưa cần style)
  - Acceptance: đăng nhập bằng tài khoản Admin seed thành công và đọc được từ `CurrentSession`; sai mật khẩu hoặc tài khoản bị khóa hiện thông báo lỗi, không crash
  - Verify: chạy tay + `mvn test` (`StaffDao` mock, đúng/sai mật khẩu)
  - Files: `auth/AuthService.java`, `auth/CurrentSession.java`, `auth/LoginFrame.java`, `src/test/java/com/foodstore/auth/AuthServiceTest.java`

## Tasks — Phase 1 (song song với M1-M4)

- Task: `StaffService` + `StaffPanel`
  - Acceptance: tạo nhân viên mới lưu `password_hash` dạng BCrypt (không plaintext); không khóa được Admin hoạt động cuối cùng; không tự khóa chính mình; có test đạt/không đạt cho từng quy tắc
  - Verify: `mvn test` + chạy tay
  - Files: `auth/StaffService.java`, `auth/StaffPanel.java`, `src/test/java/com/foodstore/auth/StaffServiceTest.java`
- Task: `ReportDao` + `ReportService` + `ReportPanel` — doanh thu theo ngày/tháng (từ `payments`), top món bán chạy (từ `order_items` của đơn `PAID`), nguyên liệu tồn thấp
  - Acceptance: trên dữ liệu seed, tổng doanh thu theo ngày khớp tổng `payments.total` của ngày đó; khoảng ngày đảo ngược bị từ chối
  - Verify: `mvn test` (service) + chạy tay đối chiếu với truy vấn SQL trực tiếp
  - Files: `reports/ReportDao.java`, `reports/JdbcReportDao.java`, `reports/ReportService.java`, `reports/ReportPanel.java`
- Task: `MainFrame` khung rỗng (CardLayout + menu theo `allowedScreens()` với panel giữ chỗ) để Phase 2 không bị chặn
  - Acceptance: đăng nhập từng vai trò thấy menu khác nhau (panel bên trong còn là giữ chỗ)
  - Verify: chạy tay mỗi vai trò một lần
  - Files: `auth/MainFrame.java`

## Tasks — Phase 2 (sau khi panel M1-M4 tồn tại)

- Task: Nối panel thật của M1-M4 + `StaffPanel` + `ReportPanel` vào `MainFrame`
  - Acceptance: chuyển được giữa mọi panel trong một cửa sổ, không có cửa sổ phụ bật ra
  - Verify: chạy tay
  - Files: `auth/MainFrame.java`
- Task: Hiển thị menu theo vai trò — Admin thấy tất cả; Cashier thấy Orders/Payments/Customers; Waiter thấy Tables/Orders/Menu; Chef thấy Kitchen/Inventory
  - Acceptance: đăng nhập với từng trong 4 vai trò seed cho thấy menu khác nhau rõ ràng; vai trò không có quyền không truy cập được màn hình đó
  - Verify: chạy tay, mỗi vai trò một lần
  - Files: `auth/MainFrame.java` (logic dựng menu rẽ nhánh theo lớp con `Staff` — đây là phần "đa hình" được thể hiện)

## Dependencies

- **Phần Phase 0 cần:** bảng `staff` trong `schema.sql` (Foundation) — làm song song với `foundation.md`.
- **Phần Phase 1 cần:** bảng `payments`, `order_items`, `ingredients` (đã có từ schema) để viết truy vấn báo cáo trên seed data.
- **Phần Phase 2 cần:** panel của M1-M4 tồn tại (kể cả chưa hoàn chỉnh) trước khi nối vào `MainFrame`.
- **Được dùng bởi:** mọi người — đây là điểm vào của cả ứng dụng.

## Checkpoints

- **Checkpoint A:** xong Phase 0 — đăng nhập chạy, `AuthServiceTest` đạt.
- **Checkpoint B contribution:** `StaffService` và báo cáo doanh thu chạy độc lập trên MySQL thật; `StaffServiceTest` đạt.
- **Checkpoint C:** xong Phase 2 — cả 4 vai trò đăng nhập và thấy dashboard đúng, nối được các panel.
