# M5: Staff, Auth & Reports

Folder: `src/main/java/com/foodstore/auth/` và `.../reports/` · Template: `templates/login.html`, `dashboard.html`, `templates/staff/`, `templates/reports/` · Owner: **Owner 5** (điền tên) · **Phase 0** (song song với Foundation, chặn mọi người) + Phase 1 + Phase 2

## Files

| File                                                              | Phase | Mục đích                                                                                                                                                                              |
|-------------------------------------------------------------------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `auth/StaffRole.java`                                             | 0     | Enum `ADMIN, CASHIER, WAITER, CHEF` (khớp ENUM `staff.role`)                                                                                                                          |
| `auth/Feature.java`                                               | 0     | Enum `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`; mỗi giá trị có `pathPrefix()` (`/menu`, `/tables`, ...) và `viewAuthority()`/`editAuthority()` (`MENU_VIEW`, `MENU_EDIT`) |
| `auth/AccessLevel.java`                                           | 0     | Enum `VIEW, EDIT`                                                                                                                                                                     |
| `auth/Staff.java`                                                 | 0     | **Abstract** — `id`, `username`, `fullName`, `active`; `abstract Map<Feature, AccessLevel> permissions()`; `canView(Feature)`, `canEdit(Feature)` (EDIT bao gồm VIEW); `authorities()` trả danh sách authority cho Spring Security |
| `auth/Admin.java`, `Cashier.java`, `Waiter.java`, `Chef.java`     | 0     | `extends Staff`, override `permissions()` theo bảng phân quyền trong `SPEC.md` (Admin: mọi feature `EDIT`)                                                                            |
| `auth/StaffAccount.java`                                          | 0     | Bản ghi đọc từ DB (kèm `passwordHash`); không lộ ra ngoài `auth/`                                                                                                                     |
| `auth/StaffDao.java` / `JdbcStaffDao.java`                        | 0     | `findByUsername(String)`; Phase 1 thêm CRUD, `countActiveAdmins()`                                                                                                                    |
| `auth/StaffUserDetails.java`                                      | 0     | `implements UserDetails` — bọc một `Staff`; `getAuthorities()` từ `staff.authorities()`; `isEnabled()` = `staff.isActive()`                                                           |
| `auth/StaffUserDetailsService.java`                               | 0     | `implements UserDetailsService` — `loadUserByUsername`: tìm `StaffAccount`, dựng đúng lớp con `Staff` theo `StaffRole`, trả `StaffUserDetails`; không tìm thấy thì ném `UsernameNotFoundException`. Spring Security so mật khẩu BCrypt |
| `auth/LoginController.java`                                       | 0     | GET `/login` hiện `login.html`; báo lỗi **chung chung** ("Sai tên đăng nhập hoặc mật khẩu"), không tiết lộ username có tồn tại hay không                                              |
| `auth/DashboardController.java`                                   | 0/2   | GET `/` — dashboard theo vai trò: lối tắt tới các feature có quyền; Phase 2 thêm số liệu nhanh (số đơn đang mở, nguyên liệu tồn thấp qua `InventoryService.findLowStock()` nếu có quyền xem) |
| `templates/login.html`, `dashboard.html`                          | 0/2   | Trang đăng nhập (form POST `/login` có CSRF token), dashboard dùng `layout/base.html`                                                                                                 |
| `auth/StaffService.java` / `StaffServiceImpl.java`                | 1     | CRUD nhân viên, đổi mật khẩu, khóa/mở tài khoản                                                                                                                                       |
| `auth/StaffForm.java`                                             | 1     | Form: `username` (`@NotBlank`, `@Size`), `fullName`, `role`, `password` (bắt buộc khi tạo, `@Size(min=8)`)                                                                            |
| `auth/StaffController.java`                                       | 1     | `/staff` — danh sách, thêm, sửa, khóa/mở (Admin); không cho khóa chính mình (`@AuthenticationPrincipal`)                                                                              |
| `templates/staff/*.html`                                          | 1     | Trang nhân viên                                                                                                                                                                       |
| `reports/ReportDao.java` / `JdbcReportDao.java`                   | 1     | SQL **chỉ đọc** (`GROUP BY`) trên các bảng module khác (SPEC D12)                                                                                                                     |
| `reports/ReportService.java`                                      | 1     | `revenueByDay(from, to)`, `revenueByMonth(year)`, `topProducts(from, to, limit)`, `lowStockIngredients()`                                                                             |
| `reports/ReportController.java` + `templates/reports/*.html`      | 1     | `/reports` — chọn khoảng ngày (form GET), bảng kết quả; (stretch: biểu đồ bằng Chart.js)                                                                                              |
| Tests                                                             | 0/1   | `StaffUserDetailsServiceTest`, `StaffPermissionsTest` (Phase 0); `StaffServiceTest`, `StaffControllerTest`, `ReportServiceTest`, `ReportControllerTest` (Phase 1)                      |

### Tính authority (để `SecurityConfig` và layout dùng chung)

```java
// Staff.authorities(): với mỗi (feature, level) trong permissions()
//   EDIT → thêm <FEATURE>_VIEW và <FEATURE>_EDIT
//   VIEW → thêm <FEATURE>_VIEW
// Ví dụ Waiter: MENU_VIEW, TABLES_VIEW, TABLES_EDIT, ORDERS_VIEW, ORDERS_EDIT
```

## Quy tắc nghiệp vụ

| Chủ đề          | Quy tắc                                                                                                                                                                            |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Đăng nhập       | Username tồn tại + `is_active` + mật khẩu khớp BCrypt. Sai bất kỳ điều kiện nào → cùng một thông báo chung. Không log mật khẩu                                                      |
| Mật khẩu        | Băm BCrypt khi tạo/đổi; tối thiểu 8 ký tự; không lưu/in plaintext; không bao giờ đưa `passwordHash` vào model của giao diện                                                         |
| Nhân viên       | Username duy nhất (không phân biệt hoa thường), không rỗng. Không khóa chính mình; không khóa/hạ quyền **Admin cuối cùng** (`countActiveAdmins() > 1`)                              |
| Phân quyền      | **Server kiểm quyền** (D8): `SecurityConfig` sinh quy tắc từ `Feature`. Layout chỉ ẩn mục menu/nút bằng `sec:authorize` để tiện dùng, không thay thế kiểm quyền ở server            |
| Phiên           | Có thời hạn session; đăng xuất bằng POST `/logout` (có CSRF); sau đăng nhập thành công về `/`                                                                                      |
| Báo cáo ngày    | `paid_at >= from AND paid_at < to + 1 ngày`; chỉ tính đơn `PAID`; `from > to` bị từ chối                                                                                           |
| Món bán chạy    | Tổng `quantity` từ `order_items` của đơn `PAID`, giảm dần, có `LIMIT` (giới hạn trên của `limit`, ví dụ 100)                                                                       |
| Quyền báo cáo   | Chỉ Admin có `REPORTS` và `STAFF`                                                                                                                                                  |

## Tasks

### Phase 0 (song song Foundation — cần xong trước Checkpoint A)

- Task: `Staff` abstract + `Admin/Cashier/Waiter/Chef` + `StaffRole`, `Feature`, `AccessLevel`

  - Acceptance: `permissions()` qua tham chiếu `Staff` ra đúng bảng phân quyền từng vai trò (đa hình); `canEdit(Feature.MENU)`: Admin `true`, Waiter `false`; `canView(Feature.MENU)` của Waiter `true`; `authorities()` của Waiter đúng ví dụ ở trên
  - Verify: `mvn test` (`StaffPermissionsTest`)
  - Files: `auth/Staff.java`, `Admin.java`, `Cashier.java`, `Waiter.java`, `Chef.java`, `StaffRole.java`, `Feature.java`, `AccessLevel.java`
- Task: `StaffDao` + `StaffUserDetails` + `StaffUserDetailsService`

  - Acceptance: đăng nhập được bằng 4 tài khoản seed và nhận đúng quyền; sai mật khẩu, không tồn tại, bị khóa đều bị từ chối với cùng thông báo
  - Verify: `mvn test` (`StaffUserDetailsServiceTest`); chạy tay
  - Files: `auth/StaffAccount.java`, `StaffDao.java`, `JdbcStaffDao.java`, `StaffUserDetails.java`, `StaffUserDetailsService.java`
- Task: `LoginController` + `login.html` + `DashboardController` khung

  - Acceptance: http://localhost:8080 chuyển tới `/login`; đăng nhập đúng vào dashboard với menu đúng vai trò; đăng nhập sai báo lỗi chung và ở lại trang; đăng xuất quay về `/login`
  - Verify: chạy tay với 4 tài khoản seed
  - Files: `auth/LoginController.java`, `DashboardController.java`, `templates/login.html`, `templates/dashboard.html`

### Phase 1

- Task: `StaffService` + `StaffForm` + `StaffController` + template

  - Acceptance: Admin thêm/sửa/khóa nhân viên trên trình duyệt; không khóa được Admin cuối cùng hay chính mình; mật khẩu mới lưu dạng BCrypt; vai trò khác vào `/staff` nhận `403`
  - Verify: `mvn test` (`StaffServiceTest`, `StaffControllerTest`); chạy tay
  - Files: `auth/StaffService.java`, `StaffServiceImpl.java`, `StaffForm.java`, `StaffController.java`, `templates/staff/*`
- Task: `ReportDao` + `ReportService` + `ReportController` + template

  - Acceptance: doanh thu theo ngày/tháng khớp tính tay trên 2 đơn seed đã thanh toán; món bán chạy đúng thứ tự; nguyên liệu tồn thấp đúng; khoảng ngày sai (`from > to`) hiện lỗi; vai trò khác Admin nhận `403`
  - Verify: `mvn test` (`ReportServiceTest`, `ReportControllerTest`); chạy tay trên MySQL thật
  - Files: `reports/ReportDao.java`, `JdbcReportDao.java`, `ReportService.java`, `ReportController.java`, `templates/reports/*`

### Phase 2 (sau khi M1–M4 có trang chạy được)

- Task: Hoàn thiện `layout/base.html` + dashboard + rà soát đường dẫn

  - Acceptance: mỗi vai trò thấy đúng menu và đúng số liệu dashboard; mọi trang truy cập được từ ít nhất một vai trò; **mọi URL của mỗi feature đều bị chặn đúng với vai trò không có quyền** (kiểm bằng cách gõ URL trực tiếp, không chỉ nhìn menu)
  - Verify: chạy tay với cả 4 tài khoản; bảng kiểm trong `integration.md`
  - Files: `templates/layout/base.html` (phối hợp owner Foundation), `auth/DashboardController.java`, `templates/dashboard.html`

## Dependencies

- **Cần:** `schema.sql`, `BusinessException`, `SecurityConfig`, `StoreProperties` (Foundation). Phase 0 không phụ thuộc module nào khác.
- **Được dùng bởi:** toàn bộ module (đăng nhập, `Feature`, `AccessLevel`, authority); Phase 2 cần trang của M1–M4.

## Checkpoint contribution

- Checkpoint A: đăng nhập seed hoạt động; `StaffUserDetailsServiceTest` và `StaffPermissionsTest` đạt.
- Checkpoint C: 4 vai trò thấy menu đúng; gõ URL trái quyền bị `403`.
