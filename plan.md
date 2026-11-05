# Implementation Plan: Hệ thống Quản lý Cửa hàng Đồ ăn (FoodStore) — Spring Web MVC

Source spec: `SPEC.md`

## Components & Dependencies

```
Foundation + M5 Phase 0 (sequential, chặn tất cả)
    │
    ├── schema.sql / data.sql (đóng băng sau khi merge)
    ├── Maven skeleton + Spring Boot (web MVC, Thymeleaf, Security, Validation, JDBC)
    ├── common/BusinessException + GlobalExceptionHandler
    ├── SecurityConfig (form login, CSRF, quy tắc quyền theo Feature)
    ├── templates/layout/base.html + login.html + dashboard.html + error.html
    ├── Khai báo interface Service của 5 module (chữ ký, chưa cài đặt)
    ├── Staff abstract + Admin/Cashier/Waiter/Chef + Feature + AccessLevel
    ├── StaffUserDetailsService + LoginController (cơ bản)
    └── docs/er-diagram.md
    │
    ▼
Feature modules (song song, model + DAO + service + controller + template)
    ├── M1 Menu ───────────────┐
    ├── M2 Orders & Tables      │ M2 đọc Product (M1) khi thêm món vào đơn
    ├── M3 Inventory            │ M3 đọc công thức theo Product (M1) và order_items (M2)
    ├── M4 Payments/Customers   │ M4 đọc Order (M2), gọi M3 + M2 khi thanh toán
    └── M5 Staff/Reports (phần Phase 1: StaffService, ReportService)
    │
    ▼
M5 Phase 2: hoàn thiện layout/menu theo quyền, dashboard, kiểm tra toàn bộ đường dẫn
    │
    ▼
Integration & Demo Prep
```

**Dependency cứng, tuần tự:** chưa module nào bắt đầu được khi Foundation chưa merge — nó định nghĩa schema, khung package, `SecurityConfig`, layout chung và **chữ ký interface Service** mà mọi module phụ thuộc. Giữ Phase 0 ngắn

**Dependency mềm giữa các module:** M2 cần `products` (M1); M3 cần `products` (M1) và `order_items` (M2); M4 cần `orders` (M2) và gọi `InventoryService` (M3). Mỗi owner làm DAO/service/controller/template trên **seed data** và **interface Service đã chốt**, không cần chờ module bạn xong. Khi module khác chưa có `XxxServiceImpl`, dùng bản tạm ném `UnsupportedOperationException` để Spring không thiếu bean.

> Luồng thanh toán chạm 4 module cùng lúc nên chữ ký interface Service phải chốt từ Phase 0. Đổi chữ ký sau đó đi qua boundary "Hỏi nhóm trước".

## Per-Module Files

| Module                              | File                  | Thư mục (Java / template)                                                                                       |
|-------------------------------------|-----------------------|-----------------------------------------------------------------------------------------------------------------|
| Foundation (shared/root)            | `foundation.md`       | `pom.xml`, `FoodStoreApplication.java`, `common/`, `config/`, `templates/layout/`, `src/main/resources/`        |
| M1 — Menu                           | `m1-menu.md`          | `menu/` · `templates/menu/`                                                                                     |
| M2 — Orders & Tables                | `m2-orders.md`        | `orders/` · `templates/orders/`, `tables/`, `kitchen/`                                                          |
| M3 — Inventory & Suppliers          | `m3-inventory.md`     | `inventory/` · `templates/inventory/`                                                                           |
| M4 — Payments, Vouchers & Customers | `m4-payments.md`      | `payments/` (+ `discount/`), `customers/` · `templates/payments/`, `customers/`                                 |
| M5 — Staff, Auth & Reports          | `m5-staff-reports.md` | `auth/`, `reports/` · `templates/staff/`, `reports/` (+ `login.html`, `dashboard.html`)                         |
| Phase 3 — Integration & Demo Prep   | `integration.md`      |                                                                                                      |

## Implementation Order

1. **Phase 0 — Foundation + M5 Phase 0** (tuần tự). Phải xong và merge trước Phase 1.
2. **Phase 1 — M1–M5** (song song, mỗi người một module).
3. **Phase 2 — M5 hoàn thiện layout/dashboard** khi M1–M4 đã có trang chạy được.
4. **Phase 3 — Integration & Demo Prep** : chạy luồng thanh toán xuyên 4 module, kiểm rollback và tranh chấp đồng thời, duyệt Success Criteria, vẽ class diagram, tập demo. Bắt đầu khi 3-4 module sẵn sàng, không đợi đủ cả 5.

## Risks & Mitigations

| Rủi ro                                                                                    | Giải pháp                                                                                                                       |
|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Đổi schema sau khi Phase 1 bắt đầu làm gãy DAO của nhiều người                            | Đóng băng `schema.sql` cuối Phase 0; sau đó hỏi nhóm trước khi sửa                                                              |
| Đổi chữ ký interface Service làm gãy module khác                                          | Chốt ở Phase 0 trong `foundation.md`; chỉ thêm phương thức, không đổi/xóa                                                       |
| Conflict khi nhiều người sửa file chung (`pom.xml`, `SecurityConfig`, `base.html`, `schema.sql`) | Chỉ owner Foundation sửa sau Phase 0; owner module chỉ thêm file trong package và thư mục template của mình                |
| Luồng thanh toán lỗi giữa chừng làm kho/đơn/điểm lệch nhau                                | `CheckoutService` là một `@Transactional(rollbackFor = Exception.class)`; Phase 3 kiểm rollback tay (cả exception checked)       |
| Exception checked làm transaction **không** rollback                                      | Mọi lỗi nghiệp vụ extends `BusinessException extends RuntimeException`; khai báo `rollbackFor`                                  |
| Hai nhân viên thao tác cùng một đơn/bàn/voucher cùng lúc (web thật sự đa người dùng)      | UPDATE có điều kiện + kiểm số dòng; `UNIQUE(order_id)`; kiểm tay bằng hai trình duyệt ở Phase 3                                 |
| Bấm đúp nút thanh toán/F5 sau POST gửi lại form                                           | Post-Redirect-Get + vô hiệu nút khi submit; chốt chặn thật là UPDATE có điều kiện                                               |
| Chỉ ẩn nút mà không kiểm quyền ở server → ai cũng gọi được URL                            | Quy tắc quyền sinh từ `Feature` trong `SecurityConfig`; test MockMvc `403` cho vai trò chỉ VIEW                                 |
| Ai đó tắt CSRF hoặc dùng `th:utext` để "cho dễ"                                           | Ghi trong Boundaries "Không bao giờ"; review PR kiểm hai điểm này                                                               |
| Lỗi nghiệp vụ hiện trang lỗi/stack trace                                                  | `GlobalExceptionHandler` chuẩn hóa cách báo lỗi; trang `error.html` không lộ chi tiết                                           |
| Khoảng trống đặc tả (VAT, voucher sai, phân quyền xem/sửa)                                | Đã chốt ở mục "Quyết định thiết kế" của `SPEC.md`; nhóm xác nhận Open Questions trước Phase 1                                   |
| Mỗi module một kiểu giao diện, nhìn rời rạc                                               | Dùng chung `layout/base.html` và một file CSS; module chỉ viết phần nội dung                                                    |
| M5 chậm làm kẹt layout/đăng nhập                                                          | Owner M5 làm login + layout khung sớm ở Phase 0/1                                                                               |
| Lặp code DAO                                                                 | Chấp nhận, vì mục tiêu là luyện JDBC/SQL viết tay                                                                               |
| Có người chưa quen Spring MVC/Thymeleaf                                                   | Foundation dựng sẵn một controller + template mẫu (danh mục: `JdbcCategoryDao` + một trang danh sách) làm tham chiếu            |
| Seed password công khai trong repo public                                                 | Ghi rõ trong README; đổi mật khẩu trước khi demo ra ngoài nhóm                                                                  |

## Parallel vs. Sequential

- **Tuần tự, chặn:** Phase 0.
- **Song song:** M1–M5 Phase 1 — 5 luồng độc lập sau khi Phase 0 merge.
- **Song song một phần:** M5 Phase 2 chờ trang của M1–M4.
- **Tuần tự, cả nhóm:** Phase 3.

## Verification Checkpoints

- **Checkpoint A (cuối Phase 0):** `mvn clean package` thành công; `mvn spring-boot:run` mở được http://localhost:8080 và chuyển về trang đăng nhập; `schema.sql` + `data.sql` chạy sạch trên MySQL mới (cả PowerShell); đăng nhập bằng tài khoản seed hoạt động; test phân quyền cơ bản đạt (chưa đăng nhập → `/login`, vai trò chỉ VIEW → `403` khi POST); `docs/er-diagram.md` khớp `schema.sql`; cả 5 người build và chạy được trên máy mình.
- **Checkpoint B (giữa Phase 1):** mỗi owner demo trang của mình chạy độc lập trên MySQL thật; mỗi module có ít nhất một test service và một test controller đạt.
- **Checkpoint C (xong Phase 2):** đăng nhập 4 vai trò thấy menu đúng; trang `VIEW` không sửa được kể cả gửi request thủ công; luồng tạo đơn → thanh toán chạy qua trình duyệt.
- **Checkpoint D (Phase 3 / cuối):** mọi mục Success Criteria được đánh dấu; clone mới + MySQL mới + `mvn spring-boot:run` chạy end-to-end trên máy sạch; rollback và tranh chấp đồng thời đã kiểm tay; class diagram đã có.

Xem file riêng từng module để biết task chi tiết.
