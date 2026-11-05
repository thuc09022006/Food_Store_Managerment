# Foundation (shared/root — Phase 0, tuần tự, chặn tất cả)

Owner: 1-2 người. Không thuộc 5 module — đây là các file gốc mà mọi module phụ thuộc. Chỉ chỉnh ở đây; owner module không sửa sau đó (xem Boundaries trong `SPEC.md`).

## Files

| File                                                                                                                                                                  | Mục đích                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pom.xml`                                                                                                                                                             | Spring Boot parent 4.0.9, Java 21; `spring-boot-starter-webmvc`, `-thymeleaf`, `-security`, `-validation`, `-jdbc`; `thymeleaf-extras-springsecurity6`; `mysql-connector-j`; test: `spring-boot-starter-test`, `-webmvc-test`, `-security-test`. Bản chuẩn: xem `pom.xml` đã chốt  |
| `.gitignore`                                                                                                                                                          | Loại `target/`, `application-local.properties`, file IDE                                                                                                                                                                                                                  |
| `README.md`                                                                                                                                                           | Cài đặt từ máy trống (bash/cmd/PowerShell), chạy, mở trình duyệt, tài khoản seed, lỗi thường gặp                                                                                                                                                                          |
| `docs/er-diagram.md`                                                                                                                                                  | ER diagram (Mermaid), khớp `schema.sql`                                                                                                                                                                                                                                   |
| `src/main/java/com/foodstore/FoodStoreApplication.java`                                                                                                               | `main()` — `SpringApplication.run` (web server nhúng, không cần cấu hình thêm)                                                                                                                                                                                            |
| `common/BusinessException.java`                                                                                                                                       | `extends RuntimeException` — gốc của mọi lỗi nghiệp vụ; thông điệp tiếng Việt hiển thị được cho người dùng                                                                                                                                                                |
| `common/GlobalExceptionHandler.java`                                                                                                                                  | `@ControllerAdvice` — `BusinessException` → quay lại trang trước (flash attribute `error`); `AccessDeniedException` → trang `403`; lỗi khác → `error.html`, ghi log đầy đủ nhưng **không** hiện stack trace cho người dùng                                                 |
| `config/SecurityConfig.java`                                                                                                                                          | `@Configuration` + `@EnableMethodSecurity`: form login (`/login`), logout (POST `/logout`), CSRF **bật**, `PasswordEncoder` = `BCryptPasswordEncoder`; sinh quy tắc từ `Feature`: `GET <pathPrefix>/**` cần `<FEATURE>_VIEW`, method khác cần `<FEATURE>_EDIT`; `/login`, `/css/**`, `/js/**` công khai; còn lại cần đăng nhập |
| `config/StoreProperties.java`                                                                                                                                         | `@ConfigurationProperties(prefix = "store")` (record) — VAT, ngày cảnh báo hết hạn, ngưỡng hạng, % giảm theo hạng, `loyalty.amount-per-point`                                                                                                                             |
| `src/main/resources/application.properties`                                                                                                                           | Datasource (biến môi trường `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`), `server.port`, `server.servlet.session.timeout`, `spring.thymeleaf.cache=false` (dev), `store.*`                                                                                                       |
| `src/main/resources/schema.sql`                                                                                                                                       | `CREATE TABLE` cho 15 bảng đúng thứ tự khóa ngoại: `staff`, `categories`, `products`, `combo_components`, `dining_tables`, `customers`, `vouchers`, `orders`, `order_items`, `suppliers`, `ingredients`, `recipes`, `stock_imports`, `stock_import_items`, `payments`      |
| `src/main/resources/data.sql`                                                                                                                                         | Seed: 4 staff (BCrypt), danh mục, món gồm ít nhất 1 combo, bàn, nguyên liệu + công thức, voucher (1 còn hạn, 1 hết hạn), khách hàng                                                                                                                                       |
| `templates/layout/base.html`                                                                                                                                          | Khung chung (Thymeleaf layout fragment): thanh menu **chỉ hiện mục mà người dùng có quyền VIEW** (`sec:authorize`), tên người dùng, nút Đăng xuất (form POST có CSRF), vùng hiển thị flash `success`/`error`                                                              |
| `templates/login.html`, `dashboard.html`, `error.html`, `static/css/app.css`                                                                                          | Trang đăng nhập (báo lỗi chung chung), dashboard, trang lỗi, CSS dùng chung                                                                                                                                                                                               |
| `menu/ProductService`, `orders/OrderService`, `orders/TableService`, `inventory/InventoryService`, `customers/CustomerService`                                        | **Interface Service** (chỉ chữ ký — xem Contract). Owner module cài đặt trong `XxxServiceImpl` (`@Service`) cùng package                                                                                                                                                  |
| `menu/Product`, `Food`, `Drink`, `Combo`, `ComboComponent`, `Category`; `orders/Order`, `OrderItem`, `OrderStatus`, `OrderType`; `customers/Customer`, `CustomerTier` | Model tối thiểu mà các interface tham chiếu — Foundation tạo bản đầu để cả dự án biên dịch được; owner module mở rộng sau                                                                                                                                                  |
| `auth/*` (Phase 0 của M5)                                                                                                                                             | `Staff` + 4 vai trò, `Feature`, `AccessLevel`, `StaffRole`, `StaffAccount`, `StaffDao`/`JdbcStaffDao`, `StaffUserDetails`, `StaffUserDetailsService`, `LoginController`, `DashboardController`                                                                              |
| `menu/CategoryDao`, `menu/JdbcCategoryDao`, `menu/CategoryController` + `templates/menu/categories.html`                                                              | **Mẫu hoàn chỉnh** DAO → controller → template (danh sách + thêm, Post-Redirect-Get, `th:action`, hiện flash) làm tham chiếu cho cả nhóm                                                                                                                                   |
| `src/test/java/com/foodstore/auth/StaffUserDetailsServiceTest.java`, `StaffPermissionsTest.java`, `config/SecurityConfigTest.java`                                    | Test: nạp user (có, không có, bị khóa); `permissions()` 4 vai trò; MockMvc: chưa đăng nhập → `/login`, vai trò chỉ VIEW POST → `403`, POST thiếu CSRF → bị từ chối                                                                                                         |

## Contract: chữ ký Service chốt ở Phase 0

Các phương thức cross-module dưới đây có chữ ký cố định từ Phase 0; chỉ **thêm** phương thức mới, không đổi/xóa.

| Interface (owner)       | Phương thức (chữ ký)                                                                                                                                                                       | Ai gọi            |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|
| `ProductService` (M1)   | `Product getById(long id)`, `List<Product> findActive()`, `List<ComboComponent> getComponents(long comboId)`                                                                               | M2, M3            |
| `OrderService` (M2)     | `Order getById(long id)`, `List<OrderItem> getItems(long orderId)`, `BigDecimal calcSubtotal(long orderId)`, `void markPaid(long orderId)`, `void attachCustomer(long orderId, long customerId)` | M3, M4            |
| `TableService` (M2)     | `void release(long tableId)`                                                                                                                                                               | M4                |
| `InventoryService` (M3) | `void deductForOrder(long orderId)`                                                                                                                                                        | M4                |
| `CustomerService` (M4)  | `Optional<Customer> findById(long id)`, `Optional<Customer> findByPhone(String phone)`, `void addPoints(long customerId, BigDecimal orderTotal)`                                           | M2, M4, M5        |
| `Feature` enum (M5)     | `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`, mỗi giá trị có `pathPrefix()`                                                                              | SecurityConfig, layout |
| `Staff` (M5)            | `Map<Feature, AccessLevel> permissions()`, `canView(Feature)`, `canEdit(Feature)`                                                                                                          | UserDetails, layout |

Đăng nhập do **Spring Security** đảm nhiệm qua `StaffUserDetailsService` (không còn `AuthService.login`). Controller lấy người dùng hiện tại bằng `@AuthenticationPrincipal StaffUserDetails`.

Ngữ nghĩa bắt buộc:

- `OrderService.markPaid`: chỉ `SERVED → PAID` bằng UPDATE có điều kiện; không có dòng nào bị đổi thì ném `BusinessException`.
- `OrderService.attachCustomer`: chỉ cho đơn chưa `PAID`/`CANCELLED`.
- `InventoryService.deductForOrder`: mở rộng combo; không bao giờ làm tồn kho âm; không ném lỗi chỉ vì thiếu hàng (SPEC D5).
- Mọi phương thức ném `BusinessException` (hoặc lớp con) cho lỗi nghiệp vụ.

## Tasks

- Task: Khung Maven theo `pom.xml` chuẩn (Boot 4.0.9; Java 21; webmvc, Thymeleaf, Security, Validation, JDBC, MySQL Driver)

  - Acceptance: `mvn clean package` thành công; phiên bản Spring Boot ghim tải được từ Maven Central
  - Verify: `mvn clean package`
  - Files: `pom.xml`
- Task: `FoodStoreApplication` + cấu hình web

  - Acceptance: `mvn spring-boot:run` khởi động server; truy cập http://localhost:8080 bị chuyển tới `/login`; log không có lỗi bean
  - Verify: chạy tay
  - Files: `FoodStoreApplication.java`, `application.properties`
- Task: `BusinessException` + `GlobalExceptionHandler`

  - Acceptance: controller ném `BusinessException` thì người dùng quay lại trang trước và thấy thông báo tiếng Việt; lỗi bất ngờ hiện `error.html` không có stack trace; `403` hiện trang riêng
  - Verify: test MockMvc + chạy tay
  - Files: `common/BusinessException.java`, `common/GlobalExceptionHandler.java`, `templates/error.html`
- Task: `schema.sql` cho 15 bảng (từ mục Data Model của `SPEC.md`)

  - Acceptance: `mysql ... -e "source src/main/resources/schema.sql"` chạy sạch trên DB mới (MySQL 8.0.16+); có `CHECK` cho tồn kho không âm, số lượng đơn > 0, giá ≥ 0; InnoDB, utf8mb4
  - Verify: chạy tay (bash và PowerShell)
  - Files: `schema.sql`
- Task: `data.sql` — 4 staff, 2-3 danh mục, 6-8 sản phẩm (ít nhất 1 combo), 5 bàn, nguyên liệu + công thức, 2 voucher (1 còn hạn, 1 hết hạn), 2 khách hàng

  - Acceptance: `staff.password_hash` là BCrypt (không plaintext); đủ dữ liệu để mỗi module tự demo; tiếng Việt hiển thị đúng
  - Verify: truy vấn tay
  - Files: `data.sql`
- Task: `SecurityConfig` — form login, logout POST, CSRF bật, quy tắc theo `Feature`

  - Acceptance: chưa đăng nhập vào bất kỳ trang nào → `/login`; vai trò chỉ VIEW gửi POST tới feature đó nhận `403`; POST thiếu CSRF token bị từ chối; session hết hạn theo cấu hình
  - Verify: `mvn test` (`SecurityConfigTest`)
  - Files: `config/SecurityConfig.java`, `config/StoreProperties.java`
- Task: Layout chung `base.html` + `login.html` + `dashboard.html` + CSS

  - Acceptance: sau đăng nhập, menu chỉ hiện đúng mục theo vai trò (`sec:authorize`); nút Đăng xuất là form POST; flash `success`/`error` hiện được ở mọi trang dùng layout
  - Verify: chạy tay với 4 tài khoản seed
  - Files: `templates/layout/base.html`, `templates/login.html`, `templates/dashboard.html`, `static/css/app.css`
- Task: Khai báo interface Service theo bảng Contract + model tối thiểu + mẫu Category (DAO → controller → template)

  - Acceptance: toàn bộ dự án biên dịch được khi các interface chưa có cài đặt; app khởi động được (bản `XxxServiceImpl` tạm ném `UnsupportedOperationException` nếu có lớp nào inject interface chưa cài đặt); trang `/menu/categories` chạy đủ thêm/liệt kê làm mẫu
  - Verify: `mvn clean package`; chạy tay
  - Files: các `XxxService.java`, `menu/CategoryDao.java`, `menu/JdbcCategoryDao.java`, `menu/CategoryController.java`, `templates/menu/categories.html`
- Task: `Staff` hierarchy, `Feature`, `AccessLevel`, `StaffUserDetailsService` + test (chi tiết ở `m5-staff-reports.md`, Phase 0)

  - Acceptance: đăng nhập seed đúng vai trò; sai mật khẩu/không tồn tại/bị khóa đều bị từ chối bằng cùng thông báo chung
  - Verify: `mvn test`
  - Files: `auth/*`, `src/test/java/com/foodstore/auth/*`
- Task: `docs/er-diagram.md` khớp `schema.sql`

  - Acceptance: đủ 15 bảng và quan hệ khóa ngoại; render được trên GitHub
  - Verify: xem trên GitHub
  - Files: `docs/er-diagram.md`
- Task: `README.md` cài đặt từ máy trống

  - Acceptance: người chưa từng thấy dự án làm theo là chạy được và đăng nhập được, trên cả bash và PowerShell
  - Verify: Checkpoint D (`integration.md`)
  - Files: `README.md`

## Checkpoint A

Cả 5 người pull nhánh này, chạy `mvn clean package` và `mvn spring-boot:run` thành công, đăng nhập được trên máy mình trước khi Phase 1 bắt đầu. Phần `Staff`/đăng nhập của M5 (Phase 0) chạy song song và cũng là điều kiện của Checkpoint A.
