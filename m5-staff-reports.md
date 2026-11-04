# M5: Staff, Auth & Reports

Folder: `src/main/java/com/foodstore/auth/` và `src/main/java/com/foodstore/reports/` · Owner: **Owner 5** (điền tên) · **Phase 0** (song song với Foundation, chặn mọi người) + Phase 1 + Phase 2

## Files

| File                                                      | Phase | Mục đích                                                                                                                                                              |
|-----------------------------------------------------------|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `auth/StaffRole.java`                                     | 0     | Enum `ADMIN, CASHIER, WAITER, CHEF` (khớp ENUM `staff.role`)                                                                                                          |
| `auth/Screen.java`                                        | 0     | Enum `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`                                                                                  |
| `auth/AccessLevel.java`                                   | 0     | Enum `VIEW, EDIT`                                                                                                                                                     |
| `auth/Staff.java`                                         | 0     | **Abstract** — `id`, `username`, `fullName`, `active`; `abstract Map<Screen, AccessLevel> permissions()`; `allowedScreens()`, `canView(Screen)`, `canEdit(Screen)` dẫn xuất từ `permissions()` |
| `auth/Admin.java`, `Cashier.java`, `Waiter.java`, `Chef.java` | 0 | `extends Staff`, override `permissions()` theo bảng phân quyền trong `SPEC.md` (Admin: mọi màn hình `EDIT`)                                                            |
| `auth/StaffAccount.java`                                  | 0     | Bản ghi đọc từ DB (kèm `passwordHash`); **không** để lộ ra ngoài `auth/`                                                                                              |
| `auth/StaffDao.java` / `auth/JdbcStaffDao.java`           | 0     | `findByUsername(String)`; Phase 1 thêm CRUD, `countActiveAdmins()`                                                                                                    |
| `auth/AuthService.java` / `auth/AuthServiceImpl.java`     | 0     | `Staff login(String username, String password)`; dùng `PasswordEncoder.matches`; dựng đúng lớp con qua factory theo `StaffRole`                                       |
| `auth/AuthenticationFailedException.java`                 | 0     | `extends BusinessException`; thông báo chung chung, **không** tiết lộ username có tồn tại hay không                                                                   |
| `auth/CurrentSession.java`                                | 0     | `@Component` giữ `Staff` đang đăng nhập; `login/logout`; Panel hỏi `canEdit(Screen)` qua đây                                                                          |
| `auth/LoginFrame.java`                                    | 0     | Swing đăng nhập; gọi `AuthService` (qua `SwingWorker`); sai thông tin thì báo lỗi, không đóng cửa sổ                                                                  |
| `auth/MainFrame.java`                                     | 0/2   | `JFrame` + `CardLayout`; Phase 0 là khung với `PlaceholderPanel`; Phase 2 nối panel thật. Menu chỉ hiện `allowedScreens()`; có nút Đăng xuất                          |
| `auth/PlaceholderPanel.java`                              | 0     | Panel giữ chỗ                                                                                                                                                         |
| `auth/StaffService.java` / `StaffServiceImpl.java`        | 1     | CRUD nhân viên, đổi mật khẩu, khóa/mở tài khoản                                                                                                                       |
| `auth/StaffPanel.java`                                    | 1     | Swing quản lý nhân viên (Admin)                                                                                                                                       |
| `reports/ReportDao.java` / `reports/JdbcReportDao.java`   | 1     | SQL **chỉ đọc** (`GROUP BY`) trên các bảng của module khác (SPEC D11)                                                                                                 |
| `reports/ReportService.java`                              | 1     | `revenueByDay(from, to)`, `revenueByMonth(year)`, `topProducts(from, to, limit)`, `lowStockIngredients()`                                                             |
| `reports/ReportPanel.java`                                | 1     | Swing — chọn khoảng ngày, bảng kết quả; chạy truy vấn qua `SwingWorker`                                                                                               |
| `src/test/java/com/foodstore/auth/AuthServiceTest.java`   | 0     | Unit test đăng nhập                                                                                                                                                   |
| `src/test/java/com/foodstore/auth/StaffPermissionsTest.java` | 0  | Unit test `permissions()` của 4 vai trò khớp bảng phân quyền                                                                                                          |
| `src/test/java/com/foodstore/auth/StaffServiceTest.java`  | 1     | Unit test quản lý nhân viên                                                                                                                                           |
| `src/test/java/com/foodstore/reports/ReportServiceTest.java` | 1  | Unit test logic khoảng ngày/giới hạn (DAO mock)                                                                                                                       |

## Quy tắc nghiệp vụ

| Chủ đề          | Quy tắc                                                                                                                                                                         |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Đăng nhập       | Username tồn tại + `is_active` + `matches(password, hash)`. Sai bất kỳ điều kiện nào → cùng một thông báo `AuthenticationFailedException`. Không log mật khẩu                     |
| Mật khẩu        | Luôn băm BCrypt khi tạo/đổi; độ dài tối thiểu 8; không lưu/in plaintext                                                                                                         |
| Nhân viên       | Username duy nhất (không phân biệt hoa thường), không rỗng. Không được khóa chính mình, và không được khóa/hạ quyền **Admin cuối cùng** (`countActiveAdmins() > 1`)              |
| Phân quyền      | Chỉ UI kiểm quyền (SPEC D8). `MainFrame` chỉ dựng menu cho `allowedScreens()`; Panel khóa nút sửa khi `canEdit(screen) == false`                                                 |
| Báo cáo ngày    | Lọc theo `paid_at >= from AND paid_at < to + 1 ngày` (không bọc cột bằng `DATE()` để dùng được chỉ mục). Chỉ tính đơn `PAID`                                                     |
| Món bán chạy    | Tổng `quantity` từ `order_items` của các đơn `PAID`, sắp giảm dần, có `LIMIT`                                                                                                   |
| Quyền báo cáo   | Chỉ Admin thấy `REPORTS` và `STAFF`                                                                                                                                              |

## Tasks

### Phase 0 (song song Foundation — cần xong trước Checkpoint A)

- Task: `Staff` abstract + `Admin/Cashier/Waiter/Chef` + `StaffRole`, `Screen`, `AccessLevel`

  - Acceptance: gọi `permissions()` qua tham chiếu `Staff` ra đúng bảng phân quyền của từng vai trò (đa hình); `canEdit(Screen.MENU)` đúng cho Admin (`true`) và Waiter (`false`)
  - Verify: `mvn test` (`StaffPermissionsTest`)
  - Files: `auth/Staff.java`, `auth/Admin.java`, `auth/Cashier.java`, `auth/Waiter.java`, `auth/Chef.java`, `auth/StaffRole.java`, `auth/Screen.java`, `auth/AccessLevel.java`
- Task: `StaffDao` + `AuthService` + `CurrentSession`

  - Acceptance: đăng nhập được bằng 4 tài khoản seed và nhận đúng lớp con; sai mật khẩu, không tồn tại, bị khóa đều bị từ chối bằng cùng thông báo
  - Verify: `mvn test` (`AuthServiceTest`); chạy tay
  - Files: `auth/StaffAccount.java`, `auth/StaffDao.java`, `auth/JdbcStaffDao.java`, `auth/AuthService.java`, `auth/AuthServiceImpl.java`, `auth/AuthenticationFailedException.java`, `auth/CurrentSession.java`
- Task: `LoginFrame` + `MainFrame` khung + `PlaceholderPanel`

  - Acceptance: `mvn spring-boot:run` mở cửa sổ đăng nhập; đăng nhập xong mở `MainFrame` với menu đúng vai trò (panel giữ chỗ); đăng xuất quay về đăng nhập
  - Verify: chạy tay
  - Files: `auth/LoginFrame.java`, `auth/MainFrame.java`, `auth/PlaceholderPanel.java`

### Phase 1

- Task: `StaffService` + `StaffPanel`

  - Acceptance: thêm/sửa/khóa nhân viên qua UI; không khóa được Admin cuối cùng hay chính mình; mật khẩu mới lưu dạng BCrypt
  - Verify: `mvn test` (`StaffServiceTest`); chạy tay
  - Files: `auth/StaffService.java`, `auth/StaffServiceImpl.java`, `auth/StaffPanel.java`
- Task: `ReportDao` + `ReportService` + `ReportPanel`

  - Acceptance: doanh thu theo ngày/tháng khớp tính tay trên 2 đơn seed đã thanh toán; món bán chạy đúng thứ tự; danh sách nguyên liệu tồn thấp đúng; báo cáo không treo giao diện
  - Verify: `mvn test` (`ReportServiceTest`); chạy tay trên MySQL thật
  - Files: `reports/ReportDao.java`, `reports/JdbcReportDao.java`, `reports/ReportService.java`, `reports/ReportPanel.java`

### Phase 2 (sau khi M1–M4 có panel chạy được)

- Task: Nối panel của M1–M4 và `ReportPanel`, `StaffPanel` vào `MainFrame`

  - Acceptance: mỗi vai trò thấy đúng menu; màn hình `VIEW` không sửa được; mọi panel truy cập được từ ít nhất một vai trò
  - Verify: chạy tay với cả 4 tài khoản seed
  - Files: `auth/MainFrame.java`

## Dependencies

- **Cần:** `schema.sql`, `BusinessException`, `StoreProperties` (Foundation). Phase 0 không phụ thuộc module nào khác.
- **Được dùng bởi:** toàn bộ module (login, `CurrentSession`, `Screen`, `AccessLevel`); Phase 2 cần panel của M1–M4.

## Checkpoint contribution

- Checkpoint A: đăng nhập seed hoạt động, `AuthServiceTest` và `StaffPermissionsTest` đạt.
- Checkpoint C: 4 vai trò thấy menu đúng, màn hình `VIEW` không sửa được.
