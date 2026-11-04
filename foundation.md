# Foundation (shared/root — Phase 0, tuần tự, chặn tất cả)

Owner: 1-2 người. Không thuộc 5 module — đây là các file gốc mà mọi module phụ thuộc. Chỉ chỉnh ở đây; owner module không sửa sau đó (xem Boundaries trong `SPEC.md`).

## Files

| File                                                                                                                                                                  | Mục đích                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pom.xml`                                                                                                                                                             | Build Maven — Spring Boot parent, Java 21, `spring-boot-starter`, `spring-boot-starter-jdbc`, `mysql-connector-j`, `spring-security-crypto`, JUnit 5 + Mockito                                                                                                            |
| `.gitignore`                                                                                                                                                          | Loại `target/`, file override `application-local.properties`, file IDE                                                                                                                                                                                                    |
| `README.md`                                                                                                                                                           | Các bước cài đặt chạy từ máy trống, dùng được trên bash/cmd/PowerShell                                                                                                                                                                                                    |
| `docs/er-diagram.md`                                                                                                                                                  | ER diagram dạng Mermaid, khớp `schema.sql`                                                                                                                                                                                                                                |
| `src/main/java/com/foodstore/FoodStoreApplication.java`                                                                                                               | `main()` — khởi tạo Spring `ApplicationContext` (`headless(false)`), rồi mở Swing UI trên EDT                                                                                                                                                                             |
| `src/main/java/com/foodstore/common/BusinessException.java`                                                                                                           | `extends RuntimeException` — gốc của mọi lỗi nghiệp vụ; thông điệp tiếng Việt hiển thị được cho người dùng                                                                                                                                                                |
| `src/main/java/com/foodstore/config/AppConfig.java`                                                                                                                   | `@Configuration` — bean `PasswordEncoder` (`BCryptPasswordEncoder`)                                                                                                                                                                                                       |
| `src/main/java/com/foodstore/config/StoreProperties.java`                                                                                                             | `@ConfigurationProperties(prefix = "store")` (record) — VAT, ngày cảnh báo hết hạn, ngưỡng hạng, % giảm theo hạng, `loyalty.amount-per-point`                                                                                                                             |
| `src/main/resources/application.properties`                                                                                                                           | Datasource URL/username/password (qua biến môi trường `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, hoặc file `application-local.properties` nằm trong `.gitignore`) + `store.vat-rate`, `store.expiry-warning-days`, `store.loyalty.*`                                           |
| `src/main/resources/schema.sql`                                                                                                                                       | `CREATE TABLE` cho cả 15 bảng, đúng thứ tự khóa ngoại: `staff`, `categories`, `products`, `combo_components`, `dining_tables`, `customers`, `vouchers`, `orders`, `order_items`, `suppliers`, `ingredients`, `recipes`, `stock_imports`, `stock_import_items`, `payments` |
| `src/main/resources/data.sql`                                                                                                                                         | Seed: 4 staff (mỗi vai trò 1, mật khẩu BCrypt), danh mục, món gồm ít nhất 1 combo, bàn, nguyên liệu + công thức, voucher (1 còn hạn, 1 hết hạn), khách hàng                                                                                                               |
| `menu/ProductService`, `orders/OrderService`, `orders/TableService`, `inventory/InventoryService`, `customers/CustomerService`                                        | **Interface Service** (chỉ khai báo chữ ký — xem mục Contract). Owner module cài đặt trong lớp `XxxServiceImpl` (`@Service`) cùng package                                                                                                                                 |
| `menu/Product`, `Food`, `Drink`, `Combo`, `ComboComponent`, `Category`; `orders/Order`, `OrderItem`, `OrderStatus`, `OrderType`; `customers/Customer`, `CustomerTier` | Model tối thiểu mà các interface Service ở trên tham chiếu — Foundation tạo bản đầu để cả dự án biên dịch được; owner module mở rộng sau                                                                                                                                  |
| `auth/*` (Phase 0 của M5)                                                                                                                                             | `Staff` + 4 vai trò, `Screen`, `AccessLevel`, `StaffRole`, `StaffAccount`, `StaffDao`/`JdbcStaffDao`, `AuthService`, `AuthenticationFailedException`, `CurrentSession`, `LoginFrame`, `MainFrame` (khung, panel giữ chỗ), `PlaceholderPanel`                                 |
| `src/main/java/com/foodstore/menu/JdbcCategoryDao.java`                                                                                                               | DAO mẫu hoàn chỉnh (cùng `CategoryDao`) làm tham chiếu cho cả nhóm                                                                                                                                                                                                        |
| `src/test/java/com/foodstore/auth/AuthServiceTest.java`                                                                                                               | Unit test đăng nhập: đúng mật khẩu, sai mật khẩu, không tồn tại, tài khoản bị khóa                                                                                                                                                                                       |

## Contract: chữ ký Service chốt ở Phase 0

Để 5 người làm song song mà không chờ nhau, các phương thức cross-module dưới đây phải có chữ ký cố định từ Phase 0. Phần cài đặt do owner module viết sau. Chỉ **thêm** phương thức mới, không đổi/xóa phương thức đã chốt.

| Interface (owner)       | Phương thức (chữ ký)                                                                                                                                                                       | Ai gọi        |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `ProductService` (M1)   | `Product getById(long id)`, `List<Product> findActive()`, `List<ComboComponent> getComponents(long comboId)`                                                                               | M2, M3        |
| `OrderService` (M2)     | `Order getById(long id)`, `List<OrderItem> getItems(long orderId)`, `BigDecimal calcSubtotal(long orderId)`, `void markPaid(long orderId)`, `void attachCustomer(long orderId, long customerId)` | M3, M4        |
| `TableService` (M2)     | `void release(long tableId)`                                                                                                                                                               | M4            |
| `InventoryService` (M3) | `void deductForOrder(long orderId)`                                                                                                                                                        | M4            |
| `CustomerService` (M4)  | `Optional<Customer> findById(long id)`, `void addPoints(long customerId, BigDecimal orderTotal)`                                                                                           | M2, M4, M5    |
| `AuthService` (M5)      | `Staff login(String username, String password)`                                                                                                                                            | LoginFrame    |
| `Screen` enum (M5)      | `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`                                                                                                            | MainFrame     |

Ngữ nghĩa bắt buộc của một số phương thức (để các module hiểu giống nhau):

- `OrderService.markPaid(orderId)`: chỉ chuyển `SERVED → PAID`, bằng câu UPDATE có điều kiện; ném `BusinessException` nếu không có dòng nào bị đổi.
- `OrderService.attachCustomer`: chỉ cho đơn chưa `PAID`/`CANCELLED`.
- `InventoryService.deductForOrder`: mở rộng combo; không bao giờ làm tồn kho âm; không ném lỗi chỉ vì thiếu hàng (SPEC D5).
- Mọi phương thức ném `BusinessException` (hoặc lớp con) cho lỗi nghiệp vụ.

## Tasks

- Task: Tạo khung Maven (Spring Boot parent, Java 21, các starter, `mysql-connector-j`, `spring-security-crypto`, JUnit 5 + Mockito)

  - Acceptance: `mvn clean package` thành công chỉ với lớp `FoodStoreApplication` placeholder; phiên bản Spring Boot ghim trong `pom.xml` tải được từ Maven Central
  - Verify: `mvn clean package`
  - Files: `pom.xml`
- Task: `FoodStoreApplication` boot Spring rồi mở Swing UI trên EDT (không có web server)

  - Acceptance: `mvn spring-boot:run` mở cửa sổ đăng nhập "FoodStore Management System - Đăng nhập" (cần `headless(false)` — Spring Boot mặc định headless sẽ làm Swing ném `HeadlessException`); đóng cửa sổ thì tiến trình tắt sạch
  - Verify: chạy tay
  - Files: `src/main/java/com/foodstore/FoodStoreApplication.java`
- Task: Tạo `common/BusinessException` và quy ước exception (SPEC D3)

  - Acceptance: `BusinessException extends RuntimeException`; `AuthenticationFailedException` kế thừa nó
  - Verify: `mvn clean package`
  - Files: `common/BusinessException.java`, `auth/AuthenticationFailedException.java`
- Task: Viết `schema.sql` cho 15 bảng, đúng thứ tự khóa ngoại (lấy từ mục Data Model của `SPEC.md`)

  - Acceptance: `mysql ... -e "source src/main/resources/schema.sql"` chạy không lỗi trên DB mới (MySQL 8.0.16+); có `CHECK` cho tồn kho không âm, số lượng đơn > 0, giá ≥ 0; engine InnoDB, charset utf8mb4
  - Verify: chạy tay trên MySQL local (bash và PowerShell)
  - Files: `src/main/resources/schema.sql`
- Task: Viết `data.sql` — 4 staff (ADMIN, CASHIER, WAITER, CHEF), 2-3 danh mục, 6-8 sản phẩm gồm ít nhất 1 combo, 5 bàn, nguyên liệu với công thức, 2 voucher (1 còn hạn, 1 hết hạn), 2 khách hàng

  - Acceptance: `SELECT * FROM staff` ra 4 dòng với `password_hash` dạng BCrypt (không plaintext); dữ liệu đủ để mỗi module tự demo mà không cần module khác; tiếng Việt hiển thị đúng
  - Verify: truy vấn tay
  - Files: `src/main/resources/data.sql`
- Task: Cấu hình `application.properties` cho datasource MySQL (biến môi trường hoặc file override nằm trong `.gitignore`) + các thuộc tính `store.vat-rate`, `store.expiry-warning-days`, `store.loyalty.*`

  - Acceptance: app kết nối MySQL local khi khởi động, không có credential nào trong git; thiếu biến môi trường thì có thông báo lỗi dễ hiểu
  - Verify: `git diff` không có mật khẩu; log báo khởi tạo datasource thành công
  - Files: `src/main/resources/application.properties`, `.gitignore`, `config/AppConfig.java`, `config/StoreProperties.java`
- Task: Khai báo interface Service theo bảng Contract + model tối thiểu; thêm `JdbcCategoryDao` mẫu và `PlaceholderPanel`

  - Acceptance: toàn bộ dự án biên dịch được khi các interface chưa có cài đặt. Phase 0 chưa có lớp nào `@Autowired` các interface này nên app vẫn khởi động được; từ khi một module bắt đầu inject interface của module khác, module kia phải có `XxxServiceImpl` (kể cả bản tạm ném `UnsupportedOperationException`) để không thiếu bean
  - Verify: `mvn clean package`
  - Files: các `XxxService.java`, `menu/CategoryDao.java`, `menu/JdbcCategoryDao.java`
- Task: `Staff` hierarchy, `Screen`, `AccessLevel`, `AuthService` + `AuthServiceTest` (chi tiết ở `m5-staff-reports.md`, phần Phase 0)

  - Acceptance: đăng nhập seed đúng vai trò; sai mật khẩu/không tồn tại/bị khóa đều bị từ chối
  - Verify: `mvn test`
  - Files: `auth/*`, `src/test/java/com/foodstore/auth/AuthServiceTest.java`
- Task: `docs/er-diagram.md` khớp `schema.sql`

  - Acceptance: đủ 15 bảng và các quan hệ khóa ngoại; render được trên GitHub
  - Verify: xem trên GitHub
  - Files: `docs/er-diagram.md`
- Task: Viết `README.md` các bước chạy từ máy trống (tạo DB, nạp schema + data, đặt biến môi trường, `mvn spring-boot:run`, tài khoản seed, lỗi thường gặp)

  - Acceptance: người chưa từng thấy dự án làm theo là chạy được, trên cả bash và PowerShell
  - Verify: Checkpoint D (`integration.md`)
  - Files: `README.md`

## Checkpoint A (cuối phần việc của file này)

Cả 5 người pull nhánh này, chạy `mvn clean package` và `mvn spring-boot:run` thành công trên máy mình trước khi Phase 1 (M1-M5) bắt đầu. Xem thêm `m5-staff-reports.md`, phần Phase 0 (`Staff` hierarchy, login) chạy song song và cũng là điều kiện của Checkpoint A.
