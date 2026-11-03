# Spec: Hệ thống Quản lý Cửa hàng Đồ ăn (FoodStore)

## Status: DRAFT — file Phase 0 (pom, schema, data, config, auth) đã có bản đầu; cần team chốt Open Questions 1, 3, 4

## Context

- Đồ án môn OOP: thực hành kế thừa, đa hình, đóng gói, trừu tượng và design pattern.
- Nhóm 5 người — mỗi người sở hữu **một module end-to-end** (model + DAO + service + Swing UI).
- Stack: Spring Boot (chỉ làm DI/config), Swing (desktop), MySQL, SQL viết tay qua `JdbcTemplate` (không dùng ORM) để luyện viết SQL trực tiếp.

## Assumptions

(Sai chỗ nào thì sửa ngay, spec bên dưới viết dựa trên các giả định này.)

1. **Spring Boot** (không phải Spring Framework thuần), **Java 21**, **Maven**, một module Maven duy nhất (một `pom.xml`). Cả nhóm dùng chung một phiên bản Spring Boot, chốt trong Phase 0.
2. **Ứng dụng desktop, không phải web.** `main()` khởi tạo `ApplicationContext` rồi mở Swing `JFrame`. Không có web server, không có REST.
3. **Plain JDBC qua `JdbcTemplate`/`NamedParameterJdbcTemplate`**, không dùng JPA/Hibernate.
4. **MySQL 8.0.16+** (từ bản này MySQL mới thực sự kiểm tra ràng buộc `CHECK`), một schema `foodstore_db`, tạo bằng một file `schema.sql`.
5. **Tiền VND không có phần thập phân:** `DECIMAL(12,0)` trong DB, `BigDecimal` trong Java. Cấm `double`/`float` cho tiền. Làm tròn `HALF_UP` về đồng.
6. Phạm vi: ăn tại chỗ + mang đi, một chi nhánh. Giao hàng là stretch goal.
7. "5 module" là **package/feature-area trong một app**, không phải 5 service triển khai riêng.
8. Dự án nằm trong repo Git riêng, tạo mới cho nhóm. **Open question 1.**

## Objective

Xây dựng hệ thống desktop giúp nhân viên cửa hàng đồ ăn:

- Quản lý thực đơn (món ăn, đồ uống, combo, danh mục).
- Quản lý bàn và đơn hàng (tạo đơn, thêm/bớt món, theo dõi trạng thái từ bếp đến phục vụ).
- Quản lý kho nguyên liệu, nhà cung cấp, phiếu nhập, cảnh báo sắp hết và sắp hết hạn.
- Thanh toán, giảm giá/voucher, tính VAT, xuất hóa đơn, tích điểm khách hàng.
- Đăng nhập theo vai trò (Admin, Cashier, Waiter, Chef), xem dashboard và báo cáo theo quyền.

**Tiêu chí thành công:** ứng dụng Swing chạy được trên MySQL; mỗi module có CRUD + ít nhất một nghiệp vụ thật (không chỉ nhập liệu); code thể hiện rõ các khái niệm OOP ghi ở từng module.

**Người dùng:** Admin/quản lý, thu ngân (Cashier), phục vụ (Waiter), bếp (Chef) — do 5 thành viên đóng vai khi demo.

## Yêu cầu của một cửa hàng đồ ăn (Functional Requirements)

| Mã | Yêu cầu | Module |
|----|---------|--------|
| FR-01 | CRUD món ăn, đồ uống, danh mục; đặt giá; bật/tắt "đang bán" | M1 |
| FR-02 | Combo gồm nhiều món với giá riêng | M1 |
| FR-03 | Tìm kiếm/lọc món theo tên, danh mục, trạng thái | M1 |
| FR-04 | Quản lý bàn (trống / có khách / đã đặt) | M2 |
| FR-05 | Tạo đơn: ăn tại chỗ (gắn bàn) hoặc mang đi; thêm/sửa/xóa món; ghi chú món | M2 |
| FR-06 | Vòng đời đơn NEW → PREPARING → SERVED → PAID, và CANCELLED; chỉ cho phép chuyển trạng thái hợp lệ | M2 |
| FR-07 | Bếp xem danh sách đơn chờ chế biến và đánh dấu đã xong | M2 |
| FR-08 | Quản lý nguyên liệu: tồn kho, đơn vị, mức tồn tối thiểu | M3 |
| FR-09 | Công thức món (mỗi món dùng bao nhiêu nguyên liệu) | M3 |
| FR-10 | Nhập kho từ nhà cung cấp (phiếu nhập, giá nhập, hạn sử dụng) | M3 |
| FR-11 | Tự trừ kho theo công thức khi thanh toán xong | M3 + M4 |
| FR-12 | Cảnh báo nguyên liệu sắp hết và lô sắp hết hạn | M3 |
| FR-13 | Thanh toán: tiền mặt, thẻ, chuyển khoản, ví điện tử | M4 |
| FR-14 | Giảm giá theo hạng thành viên hoặc voucher (% hoặc số tiền, hạn dùng, giới hạn lượt, đơn tối thiểu) | M4 |
| FR-15 | Tính VAT theo mức cấu hình được; xuất hóa đơn | M4 |
| FR-16 | Khách hàng thành viên: tích điểm, lên hạng | M4 |
| FR-17 | Đăng nhập, phân quyền theo vai trò, menu riêng cho từng vai trò | M5 |
| FR-18 | Quản lý nhân viên (Admin) | M5 |
| FR-19 | Báo cáo: doanh thu theo ngày/tháng, món bán chạy, nguyên liệu tồn thấp | M5 |

## Yêu cầu phi chức năng (bắt buộc)

- **Toàn vẹn giao dịch:** thanh toán + đổi trạng thái đơn + trừ kho + cộng điểm + trả bàn nằm trong **một `@Transactional`**; lỗi ở bước nào thì rollback tất cả.
- **Chụp giá tại thời điểm đặt:** `order_items.unit_price` lưu giá lúc gọi món; sửa giá sau này không làm đổi đơn cũ.
- **Xóa mềm:** món đã từng nằm trong đơn chỉ đặt `is_active = FALSE`, không `DELETE`.
- **Không số âm:** số lượng đơn > 0, tồn kho không âm (chặn ở service, thêm `CHECK` ở DB).
- **Bảo mật:** mật khẩu băm BCrypt; mọi SQL dùng tham số `?`, cấm nối chuỗi.
- **Cấu hình:** mức VAT (`store.vat-rate`), ngưỡng lên hạng và % giảm theo hạng (`store.loyalty.*`), số ngày cảnh báo hết hạn (`store.expiry-warning-days`) nằm trong `application.properties`, đọc qua `StoreProperties`, không hardcode.

## Tech Stack

- Java 21
- Spring Boot 4.0.x (ghim `4.0.8` trong `pom.xml`) — `spring-boot-starter` + `spring-boot-starter-jdbc`
- MySQL 8.0.16+ và `mysql-connector-j`
- Java Swing (`javax.swing`), một `JFrame` + `CardLayout`
- `spring-security-crypto` (chỉ dùng `BCryptPasswordEncoder`)
- Maven
- JUnit 5 + Mockito

## Feature Modules (mỗi người một module)

| # | Module | Trách nhiệm | Trọng tâm OOP |
|---|--------|-------------|---------------|
| 1 | **Menu** | CRUD món/đồ uống/combo/danh mục, tìm kiếm, bật/tắt bán | Kế thừa + đa hình: `abstract Product` → `Food`, `Drink`, `Combo`. **Composite:** `Combo` chứa danh sách `Product` |
| 2 | **Orders & Tables** | Bàn, tạo/sửa đơn, vòng đời đơn, màn hình bếp | **State** (hoặc enum có bảng chuyển trạng thái); composition: `Order` có nhiều `OrderItem` |
| 3 | **Inventory & Suppliers** | Nguyên liệu, công thức, nhà cung cấp, phiếu nhập, cảnh báo | **Observer:** tồn ≤ mức tối thiểu thì phát `LowStockEvent` (`ApplicationEventPublisher` của Spring) |
| 4 | **Payments, Vouchers & Customers** | Thanh toán, giảm giá, VAT, hóa đơn, tích điểm | **Strategy:** `DiscountStrategy` → `NoDiscount`, `TierDiscount`, `VoucherDiscount`; đóng gói logic tiền |
| 5 | **Staff, Auth & Reports** | Đăng nhập, phân quyền, dashboard theo vai trò, quản lý nhân viên, báo cáo | Kế thừa/đa hình: `abstract Staff` → `Admin`, `Cashier`, `Waiter`, `Chef`, override `allowedScreens()`; truy vấn tổng hợp (`GROUP BY`) |

> Khác bản Gym (6 module cho 5 người, module 6 cắt ngang): ở đây phần Auth gộp vào Module 5, nên 5 người = 5 module. Phần Phase 0 của M5 (`Staff`, login) chạy song song với Foundation và chặn mọi người, giống `m6` của repo Gym.

> **Lưu ý đặt tên:** lớp cha của món là `Product`, **không** đặt `MenuItem`, vì `java.awt.MenuItem` tồn tại. Một file Swing có `import java.awt.*;` cùng `import com.foodstore.menu.*;` sẽ lỗi biên dịch "reference is ambiguous". Tương tự vai trò đăng nhập đặt tên `Chef`, `Cashier`, `Waiter` (không trùng entity nào).

### Phân quyền theo vai trò

| Vai trò | Màn hình (`Screen`) được truy cập |
|---------|-----------------------------------|
| Admin | Tất cả |
| Cashier | ORDERS (xem), PAYMENTS, CUSTOMERS |
| Waiter | TABLES, ORDERS, MENU (xem) |
| Chef | KITCHEN, INVENTORY (xem) |

## Data Model (MySQL — `foodstore_db`)

15 bảng, khai báo theo đúng thứ tự phụ thuộc khóa ngoại. **Nguồn duy nhất của schema là `src/main/resources/schema.sql`** (không chép lại SQL ở đây để tránh hai bản lệch nhau).

| # | Bảng | Module | Ghi chú |
|---|------|--------|---------|
| 1 | `staff` | M5 | `role` ENUM ADMIN/CASHIER/WAITER/CHEF; `password_hash` BCrypt; `is_active` |
| 2 | `categories` | M1 | |
| 3 | `products` | M1 | `product_type` FOOD/DRINK/COMBO; `price DECIMAL(12,0)`; `is_active` (xóa mềm) |
| 4 | `combo_components` | M1 | khóa chính `(combo_id, component_id)` |
| 5 | `dining_tables` | M2 | `status` FREE/OCCUPIED/RESERVED |
| 6 | `customers` | M4 | `points`, `tier` NORMAL/SILVER/GOLD |
| 7 | `vouchers` | M4 | `discount_type` PERCENT/FIXED, `discount_value`, `min_order`, hạn dùng, giới hạn lượt |
| 8 | `orders` | M2 | `table_id`/`customer_id` nullable; `status` NEW/PREPARING/SERVED/PAID/CANCELLED |
| 9 | `order_items` | M2 | `unit_price` là giá **chụp** lúc gọi món |
| 10 | `suppliers` | M3 | |
| 11 | `ingredients` | M3 | `stock_quantity`, `min_quantity` (đều `CHECK >= 0`) |
| 12 | `recipes` | M3 | khóa chính `(product_id, ingredient_id)`; lượng cho 1 phần |
| 13 | `stock_imports` | M3 | |
| 14 | `stock_import_items` | M3 | `expiry_date` theo lô (nullable) |
| 15 | `payments` | M4 | `order_id` UNIQUE: một đơn thanh toán một lần |


Lưu ý: hạn sử dụng lưu theo lô nhập (`stock_import_items.expiry_date`); `ingredients.stock_quantity` chỉ là tổng tồn. Cảnh báo hết hạn đọc từ bảng lô. Trừ kho theo FIFO từng lô là stretch goal.

## Commands

```
Build:   mvn clean package
Run:     mvn spring-boot:run
Test:    mvn test
Package: mvn clean package  →  target/foodstore-management-<version>.jar
```

MySQL setup (mỗi dev chạy một lần):

```
mysql -u root -p -e "CREATE DATABASE foodstore_db CHARACTER SET utf8mb4;"
mysql -u root -p foodstore_db < src/main/resources/schema.sql
mysql -u root -p foodstore_db < src/main/resources/data.sql
```

## Project Structure

```
FoodStore-Management-OOP/
├── pom.xml
├── README.md
├── SPEC.md
├── plan.md                                → thứ tự phase, dependency, rủi ro, checkpoint
├── foundation.md                          → Phase 0: file dùng chung (root)
├── m1-menu.md                             → task list Module 1
├── m2-orders.md                           → task list Module 2
├── m3-inventory.md                        → task list Module 3
├── m4-payments.md                         → task list Module 4
├── m5-staff-reports.md                    → task list Module 5 (Phase 0 + Phase 2)
├── integration.md                         → Phase 3: tích hợp + demo + stretch goals
├── src/
│   ├── main/
│   │   ├── java/com/foodstore/
│   │   │   ├── FoodStoreApplication.java  → main(), boot Spring + Swing EDT (shared, root)
│   │   │   ├── config/                    → AppConfig (PasswordEncoder), StoreProperties (store.* trong application.properties) (shared, root)
│   │   │   ├── auth/                      → Foundation + M5: Staff (abstract), Admin, Cashier, Waiter, Chef, StaffRole,
│   │   │   │                                Screen, StaffAccount, StaffDao(+Jdbc), AuthService, AuthenticationFailedException,
│   │   │   │                                CurrentSession, LoginFrame, MainFrame, PlaceholderPanel;
│   │   │   │                                Phase 1: StaffService, StaffPanel
│   │   │   ├── reports/                   → M5: ReportDao(+Jdbc), ReportService, ReportPanel
│   │   │   ├── menu/                      → M1: Product (abstract), Food, Drink, Combo, ComboComponent,
│   │   │   │                                Category, ProductFactory, DAO(+Jdbc), ProductService, MenuPanel
│   │   │   ├── orders/                    → M2: Order, OrderItem, OrderStatus, OrderType, DiningTable,
│   │   │   │                                TableStatus, DAO(+Jdbc), OrderService, TableService,
│   │   │   │                                OrderPanel, TablePanel, KitchenPanel
│   │   │   ├── inventory/                 → M3: Ingredient, RecipeLine, Supplier, StockImport(+Item),
│   │   │   │                                DAO(+Jdbc), InventoryService, LowStockEvent, InventoryPanel
│   │   │   ├── payments/                  → M4: Payment, PaymentMethod, PaymentDao(+Jdbc), PaymentService,
│   │   │   │   └── discount/                CheckoutService, TaxPolicy, InvoiceFormatter, PaymentPanel;
│   │   │   │                                DiscountStrategy family
│   │   │   └── customers/                 → M4: Customer, CustomerTier, Voucher, DAO(+Jdbc),
│   │   │                                    CustomerService, VoucherService, CustomerPanel, VoucherPanel
│   │   └── resources/
│   │       ├── application.properties     → datasource (env vars) + store.vat-rate, store.expiry-warning-days, store.loyalty.*
│   │       ├── schema.sql
│   │       └── data.sql                   → seed: 4 staff (mỗi vai trò 1), danh mục, món, bàn, nguyên liệu, công thức, voucher, khách
│   └── test/java/com/foodstore/           → unit test, mirror cấu trúc main
└── docs/                                  → ER diagram, class diagram
```

## Code Style

DAO mẫu — SQL viết tay + `JdbcTemplate`, **xử lý cột NULL** (`description` cho phép NULL):

```java
@Repository
public class JdbcProductDao implements ProductDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = "SELECT id, category_id, name, product_type, price, description, is_active " +
                     "FROM products WHERE id = ?";
        return jdbcTemplate.query(sql, this::mapRow, id).stream().findFirst();
    }

    private Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ProductFactory.create(
            rs.getLong("id"),
            rs.getLong("category_id"),
            rs.getString("name"),
            rs.getString("product_type"),
            rs.getBigDecimal("price"),
            rs.getString("description"),     // getString trả null an toàn khi cột NULL
            rs.getBoolean("is_active")
        );
    }
}
```

Quy ước:

- **Package-by-module** (`menu/`, `orders/`, `inventory/`, `payments/` + `customers/`, `auth/` + `reports/`), không package-by-layer: mỗi người làm gần như toàn bộ trong thư mục của mình, giữ diff và conflict gọn. Code dùng chung (`FoodStoreApplication`, `config/`) chỉ sửa ở Phase 0.
- Trong module vẫn tách vai trò bằng hậu tố tên file: `Xxx.java` (model), `XxxDao`/`JdbcXxxDao`, `XxxService`, `XxxPanel`. `payments/discount/` là ngoại lệ vì nhóm file Strategy đi cùng nhau.
- DAO là interface + một bản Jdbc → service test được bằng Mockito.
- Không đặt nghiệp vụ trong DAO hoặc Panel; Panel chỉ dựng Swing và gọi service.
- Giao tiếp giữa module **qua interface Service**, không gọi DAO của module khác (ví dụ `CheckoutService` gọi `InventoryService.deductForOrder`). Module là ranh giới sở hữu, không phải tường cấm tham chiếu.
- `@Transactional` đặt ở tầng service, không đặt ở DAO.
- Swing: một `JFrame` + `CardLayout`; mỗi tính năng một `JPanel`.

## Luồng tích hợp quan trọng nhất: Thanh toán

`CheckoutService.pay(orderId, method, voucherCode)` (M4 sở hữu), chạy trong **một transaction**:

1. Kiểm tra đơn đang ở trạng thái `SERVED`.
2. Tính `subtotal` từ `order_items` (dùng `unit_price` đã chụp).
3. Chọn `DiscountStrategy` (mặc định: có voucher hợp lệ thì dùng voucher, ngược lại dùng hạng thành viên, không cộng dồn), tính giảm giá, tính VAT, làm tròn.
4. Ghi `payments`, đơn → `PAID`, tăng `vouchers.used_count`.
5. `InventoryService.deductForOrder(orderId)` (M3): trừ kho theo `recipes`.
6. `CustomerService.addPoints(...)` (M4): cộng điểm, cập nhật hạng.
7. `TableService.release(tableId)` (M2): trả bàn về `FREE`.

Bước nào ném exception thì toàn bộ rollback.

## Testing Strategy

- **Unit test (JUnit 5 + Mockito):** mỗi `*Service` một test class, DAO mock. Bắt buộc có: từng `DiscountStrategy`, chuyển trạng thái đơn hợp lệ/không hợp lệ, trừ kho theo công thức, `CheckoutService` gọi đúng thứ tự và **dừng + ném exception** khi một bước lỗi.
- **Rollback thật** không kiểm chứng được bằng Mockito (mock không có transaction). Kiểm bằng tay trên MySQL thật ở Phase 3 (xem `integration.md`): ép bước trừ kho lỗi, xác nhận `payments` và trạng thái đơn không đổi.
- **DAO test:** chạy thủ công trên MySQL local, không nằm trong `mvn test`. Testcontainers là stretch goal.
- **Manual QA:** mỗi owner demo CRUD + nghiệp vụ trên DB thật trước khi merge.
- Mục tiêu coverage: logic tầng service, không tính UI và getter/setter.

## Boundaries

- **Luôn làm:** validate ở service (không chỉ ở UI); dùng tham số `?`; chạy `mvn test` trước khi merge.
- **Hỏi nhóm trước (team chat/standup):** sửa `schema.sql` sau Phase 0, đổi tên package người khác đang dùng, thêm dependency Maven, đổi quy ước điều hướng Swing, đổi chữ ký interface Service đã chốt.
- **Không bao giờ:** commit mật khẩu thật hoặc `application.properties` chứa thông tin DB cá nhân (dùng biến môi trường hoặc file override nằm trong `.gitignore`); lưu mật khẩu plaintext; dùng `double` cho tiền.

## Success Criteria

- App chạy, có màn hình đăng nhập, xác thực với bảng `staff` (mật khẩu BCrypt), vào đúng dashboard theo vai trò.
- 5 module đều có CRUD (hoặc tập con phù hợp) trên MySQL thật.
- Luồng tạo đơn → chế biến → phục vụ → thanh toán chạy trọn vẹn và kho được trừ đúng.
- Giảm giá tính bằng Strategy (tối thiểu `NoDiscount` + `VoucherDiscount`, demo được).
- Mỗi module có ít nhất một JUnit test cho nghiệp vụ lõi.
- Không có SQL nối chuỗi từ input người dùng.
- Máy thành viên khác chạy được chỉ bằng các bước trong README (MySQL mới + `mvn spring-boot:run`).

## Open Questions

1. **Repo hosting** — còn mở, cần quyết trước khi ai push code. Đề xuất: repo GitHub mới cho nhóm.
2. Điều hướng Swing — **mặc định: một `JFrame` + `CardLayout`.**
3. **Chính sách kho khi không đủ nguyên liệu lúc thanh toán** — còn mở. Hai lựa chọn: (a) chặn thanh toán, (b) vẫn cho thanh toán, trừ kho xuống tối thiểu 0 và phát cảnh báo thiếu hàng. Đề xuất (b), vì món đã bán thật, sổ kho lệch không nên chặn thu ngân [Có khả năng cao là hợp lý hơn với thực tế quán].
4. **Phân công người ↔ module** — còn mở; các file dùng `Owner 1`–`Owner 5`, thay bằng tên thật.
5. **Thời điểm trừ kho** — đề xuất trừ khi `PAID` (đơn giản). Nhược điểm: món đã nấu rồi bị hủy thì kho không phản ánh nguyên liệu đã hao.
6. **Mức VAT** — để cấu hình trong `application.properties`; nhóm tự xác nhận mức áp dụng cho đồ án.
7. Stretch goals (không bắt buộc): giao hàng, tách bill, trừ kho FIFO theo lô, xuất CSV, `FlatLaf`, đặt bàn trước.
