# Spec: Hệ thống Quản lý Cửa hàng Đồ ăn (FoodStore)

## Status: DRAFT v2 — các quyết định mặc định đã ghi ở mục "Quyết định thiết kế"; nhóm xác nhận mục "Open Questions" còn lại

Spec này đối chiếu với mô tả bảng ở mục Data Model. **Nguồn duy nhất của schema là `src/main/resources/schema.sql`**; nếu schema thật khác mô tả ở đây thì sửa spec cho khớp schema (hoặc hỏi nhóm trước khi sửa schema).

## Context

- Đồ án môn OOP: thực hành kế thừa, đa hình, đóng gói, trừu tượng và design pattern.
- Nhóm 5 người — mỗi người sở hữu **một module end-to-end** (model + DAO + service + Swing UI).
- Stack: Spring Boot (chỉ làm DI/config/transaction), Swing (desktop), MySQL, SQL viết tay qua `JdbcTemplate` (không dùng ORM) để luyện viết SQL trực tiếp.

## Assumptions

(Sai chỗ nào thì sửa ngay, spec bên dưới viết dựa trên các giả định này.)

1. **Spring Boot** (không phải Spring Framework thuần), **Java 21**, **Maven**, một module Maven duy nhất (một `pom.xml`). Cả nhóm dùng chung một phiên bản Spring Boot, chốt trong Phase 0.
2. **Ứng dụng desktop, không phải web.** `main()` khởi tạo `ApplicationContext` rồi mở Swing `JFrame`. Không có web server, không có REST.
3. **Plain JDBC qua `JdbcTemplate`/`NamedParameterJdbcTemplate`**, không dùng JPA/Hibernate.
4. **MySQL 8.0.16+** (từ bản này MySQL mới thực sự kiểm tra ràng buộc `CHECK`), engine InnoDB, một schema `foodstore_db`.
5. **Tiền VND không có phần thập phân:** `DECIMAL(12,0)` trong DB, `BigDecimal` trong Java. Cấm `double`/`float` cho tiền. Làm tròn `HALF_UP` về đồng.
6. Phạm vi: ăn tại chỗ + mang đi, một chi nhánh, một máy chạy app. Giao hàng là stretch goal.
7. "5 module" là **package/feature-area trong một app**, không phải 5 service triển khai riêng.
8. Repo: https://github.com/thuc09022006/Food_Store_Managerment (công khai).

## Quyết định thiết kế (mặc định — nhóm có thể đổi, nhưng phải sửa spec trước khi code)

| #   | Chủ đề                         | Quyết định                                                                                                                                                                                                                                                         |
|-----|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| D1  | Giá và VAT                     | Giá niêm yết trong menu là giá **chưa gồm VAT**. Giảm giá trừ trước, VAT tính **sau** giảm: `taxable = subtotal − discount`, `vat = round_HALF_UP(taxable × store.vat-rate)`, `total = taxable + vat`.                                                              |
| D2  | Voucher sai/hết hạn            | Thu ngân đã nhập mã mà mã không hợp lệ → **từ chối** thanh toán và báo lý do (không âm thầm chuyển sang giảm theo hạng). Không nhập mã → dùng `TierDiscount` nếu đơn có khách, ngược lại `NoDiscount`. Không cộng dồn.                                              |
| D3  | Exception và rollback          | Mọi exception nghiệp vụ kế thừa `BusinessException extends RuntimeException`. `@Transactional` ở service luôn ghi `rollbackFor = Exception.class` cho luồng nhiều bước. Lý do: mặc định Spring **không** rollback checked exception.                               |
| D4  | Chống ghi trùng                | Đổi trạng thái bằng câu SQL có điều kiện và kiểm số dòng: `UPDATE orders SET status='PAID' WHERE id=? AND status='SERVED'`; `UPDATE vouchers SET used_count = used_count + 1 WHERE id=? AND used_count < usage_limit` (thêm `OR usage_limit IS NULL` nếu cột cho NULL). 0 dòng → ném `BusinessException`. |
| D5  | Kho thiếu nguyên liệu lúc thanh toán | Vẫn cho thanh toán; trừ kho xuống tối thiểu 0 (`GREATEST(stock_quantity − ?, 0)`), ghi nhận phần thiếu và phát cảnh báo. Lý do: món đã bán thật, thu ngân không nên bị chặn.                                                                                       |
| D6  | Thời điểm trừ kho              | Trừ khi đơn `PAID`. Hạn chế đã biết: món nấu xong rồi bị hủy thì kho không phản ánh nguyên liệu hao.                                                                                                                                                              |
| D7  | Sự kiện `LowStockEvent`        | Phát trong transaction nhưng listener dùng `@TransactionalEventListener(phase = AFTER_COMMIT)` (thêm `fallbackExecution = true` nếu cần chạy ngoài transaction). Listener không được làm thanh toán rollback. Cập nhật UI qua `SwingUtilities.invokeLater`.       |
| D8  | Phân quyền                     | Mỗi vai trò có map `Screen → AccessLevel (VIEW / EDIT)`. Chỉ **UI** kiểm quyền (qua `CurrentSession`); service không kiểm vai trò — chấp nhận cho đồ án một máy. Nếu sau này cần, thêm kiểm ở service là việc mở rộng.                                              |
| D9  | Gắn khách vào đơn              | `OrderService.attachCustomer(orderId, customerId)` (thêm vào contract). Đơn chưa `PAID` mới được gắn/đổi khách.                                                                                                                                                  |
| D10 | Swing và Spring                | Mỗi Panel/Frame là `@Component`, nhận service qua constructor. Thao tác chậm (thanh toán, báo cáo) chạy qua `SwingWorker`, không chặn EDT. Truy vấn nhẹ có thể gọi trực tiếp.                                                                                      |
| D11 | Báo cáo đọc dữ liệu module khác | `ReportDao` (M5) được chạy SQL **chỉ đọc** trên mọi bảng. Đây là ngoại lệ có chủ đích của quy tắc "không gọi DAO module khác"; mọi thao tác ghi vẫn đi qua Service.                                                                                                |
| D12 | Combo và kho                   | Combo không có công thức riêng; khi trừ kho, `deductForOrder` mở rộng combo thành các thành phần (`ProductService.getComponents`) nhân số lượng.                                                                                                                   |

## Objective

Xây dựng hệ thống desktop giúp nhân viên cửa hàng đồ ăn:

- Quản lý thực đơn (món ăn, đồ uống, combo, danh mục).
- Quản lý bàn và đơn hàng (tạo đơn, thêm/bớt món, theo dõi trạng thái từ bếp đến phục vụ).
- Quản lý kho nguyên liệu, nhà cung cấp, phiếu nhập, cảnh báo sắp hết và sắp hết hạn.
- Thanh toán, giảm giá/voucher, tính VAT, xuất hóa đơn, tích điểm khách hàng.
- Đăng nhập theo vai trò (Admin, Cashier, Waiter, Chef), xem dashboard và báo cáo theo quyền.

**Tiêu chí thành công:** ứng dụng Swing chạy được trên MySQL; mỗi module có CRUD + ít nhất một nghiệp vụ thật (không chỉ nhập liệu); code thể hiện rõ các khái niệm OOP ghi ở từng module.

**Người dùng:** Admin/quản lý, thu ngân (Cashier), phục vụ (Waiter), bếp (Chef) — do 5 thành viên đóng vai khi demo.

## Yêu cầu chức năng

| Mã    | Yêu cầu                                                                                                  | Module  |
|-------|----------------------------------------------------------------------------------------------------------|---------|
| FR-01 | CRUD món ăn, đồ uống, danh mục; đặt giá; bật/tắt "đang bán"                                              | M1      |
| FR-02 | Combo gồm nhiều món với giá riêng                                                                        | M1      |
| FR-03 | Tìm kiếm/lọc món theo tên, danh mục, trạng thái                                                          | M1      |
| FR-04 | Quản lý bàn (trống / có khách / đã đặt)                                                                  | M2      |
| FR-05 | Tạo đơn: ăn tại chỗ (gắn bàn) hoặc mang đi; thêm/sửa/xóa món; ghi chú món; gắn khách hàng                | M2      |
| FR-06 | Vòng đời đơn NEW → PREPARING → SERVED → PAID, và CANCELLED; chỉ cho phép chuyển trạng thái hợp lệ        | M2      |
| FR-07 | Bếp xem danh sách đơn chờ chế biến và đánh dấu đã xong                                                   | M2      |
| FR-08 | Quản lý nguyên liệu: tồn kho, đơn vị, mức tồn tối thiểu                                                  | M3      |
| FR-09 | Công thức món (mỗi món dùng bao nhiêu nguyên liệu)                                                       | M3      |
| FR-10 | Nhập kho từ nhà cung cấp (phiếu nhập, giá nhập, hạn sử dụng)                                             | M3      |
| FR-11 | Tự trừ kho theo công thức khi thanh toán xong                                                            | M3 + M4 |
| FR-12 | Cảnh báo nguyên liệu sắp hết và lô sắp hết hạn                                                           | M3      |
| FR-13 | Thanh toán: tiền mặt, thẻ, chuyển khoản, ví điện tử                                                      | M4      |
| FR-14 | Giảm giá theo hạng thành viên hoặc voucher (% hoặc số tiền, hạn dùng, giới hạn lượt, đơn tối thiểu)      | M4      |
| FR-15 | Tính VAT theo mức cấu hình được; xuất hóa đơn                                                            | M4      |
| FR-16 | Khách hàng thành viên: tích điểm, lên hạng                                                               | M4      |
| FR-17 | Đăng nhập, phân quyền theo vai trò (xem/sửa), menu riêng cho từng vai trò                                | M5      |
| FR-18 | Quản lý nhân viên (Admin)                                                                                | M5      |
| FR-19 | Báo cáo: doanh thu theo ngày/tháng, món bán chạy, nguyên liệu tồn thấp                                   | M5      |

## Yêu cầu phi chức năng (bắt buộc)

- **Toàn vẹn giao dịch:** thanh toán + đổi trạng thái đơn + trừ kho + cộng điểm + trả bàn nằm trong **một `@Transactional(rollbackFor = Exception.class)`**; lỗi ở bước nào thì rollback tất cả (D3).
- **Chụp giá tại thời điểm đặt:** `order_items.unit_price` lưu giá lúc gọi món; sửa giá sau này không làm đổi đơn cũ.
- **Xóa mềm:** món đã từng được tham chiếu (đơn, combo, công thức) chỉ đặt `is_active = FALSE`, không `DELETE`.
- **Không số âm:** số lượng đơn > 0, tồn kho không âm (chặn ở service, thêm `CHECK` ở DB). Tiền không có phần thập phân.
- **Bảo mật:** mật khẩu băm BCrypt; mọi SQL dùng tham số `?`, cấm nối chuỗi.
- **Cấu hình:** mức VAT (`store.vat-rate`), ngưỡng lên hạng và % giảm theo hạng (`store.loyalty.*`), số ngày cảnh báo hết hạn (`store.expiry-warning-days`) nằm trong `application.properties`, đọc qua `StoreProperties`, không hardcode.
- **Không chặn giao diện:** thao tác chậm chạy ngoài EDT (D10).

## Tech Stack

- Java 21
- Spring Boot 4.0.x (ghim `4.0.8` trong `pom.xml` — xác nhận bản này tải được bằng `mvn clean package` trước khi chốt Phase 0) — `spring-boot-starter` + `spring-boot-starter-jdbc`
- MySQL 8.0.16+ và `mysql-connector-j`
- Java Swing (`javax.swing`), một `JFrame` + `CardLayout`
- `spring-security-crypto` (chỉ dùng `BCryptPasswordEncoder`)
- Maven
- JUnit 5 + Mockito

## Feature Modules (mỗi người một module)

| # | Module                             | Trách nhiệm                                                               | Trọng tâm OOP                                                                                                                          |
|---|------------------------------------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| 1 | **Menu**                           | CRUD món/đồ uống/combo/danh mục, tìm kiếm, bật/tắt bán                    | Kế thừa + đa hình: `abstract Product` → `Food`, `Drink`, `Combo`. **Composite:** `Combo` chứa danh sách thành phần. **Factory:** `ProductFactory` |
| 2 | **Orders & Tables**                | Bàn, tạo/sửa đơn, vòng đời đơn, màn hình bếp                              | **State** (hoặc enum có bảng chuyển trạng thái); composition: `Order` có nhiều `OrderItem`                                             |
| 3 | **Inventory & Suppliers**          | Nguyên liệu, công thức, nhà cung cấp, phiếu nhập, cảnh báo                | **Observer:** tồn ≤ mức tối thiểu thì phát `LowStockEvent` (`ApplicationEventPublisher` của Spring)                                    |
| 4 | **Payments, Vouchers & Customers** | Thanh toán, giảm giá, VAT, hóa đơn, tích điểm                             | **Strategy:** `DiscountStrategy` → `NoDiscount`, `TierDiscount`, `VoucherDiscount`; đóng gói logic tiền                                |
| 5 | **Staff, Auth & Reports**          | Đăng nhập, phân quyền, dashboard theo vai trò, quản lý nhân viên, báo cáo | Kế thừa/đa hình: `abstract Staff` → `Admin`, `Cashier`, `Waiter`, `Chef`, override `permissions()`; truy vấn tổng hợp (`GROUP BY`)    |

Phần Auth của M5 (Phase 0: `Staff`, login) chạy song song với Foundation và chặn mọi người.

> **Lưu ý đặt tên:** lớp cha của món là `Product`, **không** đặt `MenuItem`, vì `java.awt.MenuItem` tồn tại. Một file Swing có `import java.awt.*;` cùng `import com.foodstore.menu.*;` sẽ lỗi biên dịch "reference is ambiguous". Tương tự vai trò đăng nhập đặt tên `Chef`, `Cashier`, `Waiter` (không trùng entity nào).

### Phân quyền theo vai trò

| Vai trò | Màn hình (`Screen`) và mức truy cập                                          |
|---------|-------------------------------------------------------------------------------|
| Admin   | Tất cả — EDIT                                                                 |
| Cashier | ORDERS (EDIT — tạo đơn mang đi, gắn khách), PAYMENTS (EDIT), CUSTOMERS (EDIT) |
| Waiter  | TABLES (EDIT), ORDERS (EDIT), MENU (VIEW)                                     |
| Chef    | KITCHEN (EDIT), INVENTORY (VIEW)                                              |

`Screen`: `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`. Màn hình `CUSTOMERS` gồm cả quản lý voucher. Panel nhận `AccessLevel` và tự khóa nút Thêm/Sửa/Xóa khi là `VIEW`.

## Data Model (MySQL — `foodstore_db`)

15 bảng, khai báo theo đúng thứ tự phụ thuộc khóa ngoại. Sơ đồ quan hệ: [`docs/er-diagram.md`](docs/er-diagram.md).

| #  | Bảng                 | Module | Ghi chú                                                                               |
|----|----------------------|--------|---------------------------------------------------------------------------------------|
| 1  | `staff`              | M5     | `role` ENUM ADMIN/CASHIER/WAITER/CHEF; `password_hash` BCrypt; `is_active`            |
| 2  | `categories`         | M1     |                                                                                       |
| 3  | `products`           | M1     | `product_type` FOOD/DRINK/COMBO; `price DECIMAL(12,0)`; `is_active` (xóa mềm)         |
| 4  | `combo_components`   | M1     | khóa chính `(combo_id, component_id)`                                                 |
| 5  | `dining_tables`      | M2     | `status` FREE/OCCUPIED/RESERVED                                                       |
| 6  | `customers`          | M4     | `points`, `tier` NORMAL/SILVER/GOLD                                                   |
| 7  | `vouchers`           | M4     | `discount_type` PERCENT/FIXED, `discount_value`, `min_order`, hạn dùng, giới hạn lượt |
| 8  | `orders`             | M2     | `table_id`/`customer_id` nullable; `status` NEW/PREPARING/SERVED/PAID/CANCELLED       |
| 9  | `order_items`        | M2     | `unit_price` là giá **chụp** lúc gọi món                                              |
| 10 | `suppliers`          | M3     |                                                                                       |
| 11 | `ingredients`        | M3     | `stock_quantity`, `min_quantity` (đều `CHECK >= 0`)                                   |
| 12 | `recipes`            | M3     | khóa chính `(product_id, ingredient_id)`; lượng cho 1 phần; chỉ cho FOOD/DRINK        |
| 13 | `stock_imports`      | M3     |                                                                                       |
| 14 | `stock_import_items` | M3     | `expiry_date` theo lô (nullable)                                                      |
| 15 | `payments`           | M4     | `order_id` UNIQUE: một đơn thanh toán một lần                                         |

Lưu ý: hạn sử dụng lưu theo lô nhập (`stock_import_items.expiry_date`); `ingredients.stock_quantity` chỉ là tổng tồn. Vì kho không trừ theo từng lô (FIFO là stretch goal), cảnh báo hết hạn có thể báo cả lô đã dùng hết — đây là hạn chế đã biết, ghi rõ trong giao diện cảnh báo.

## Commands

```
Build:   mvn clean package
Run:     mvn spring-boot:run
Test:    mvn test
Package: mvn clean package  →  target/foodstore-management-<version>.jar
```

MySQL setup (mỗi dev chạy một lần) — xem `README.md` (lệnh dùng `-e "source ..."` để chạy được trên PowerShell).

## Project Structure

```
FoodStore-Management-OOP/
├── pom.xml
├── README.md
├── SPEC.md
├── plan.md                                → thứ tự phase, dependency, rủi ro, checkpoint
├── foundation.md                          → Phase 0: file dùng chung (root)
├── m1-menu.md ... m5-staff-reports.md     → task list từng module
├── integration.md                         → Phase 3: tích hợp + demo + stretch goals
├── docs/
│   └── er-diagram.md                      → ER diagram (Mermaid); thêm class diagram khi xong Phase 1
├── src/
│   ├── main/
│   │   ├── java/com/foodstore/
│   │   │   ├── FoodStoreApplication.java  → main(), boot Spring + Swing EDT (shared, root)
│   │   │   ├── common/                    → BusinessException (extends RuntimeException) (shared, root)
│   │   │   ├── config/                    → AppConfig (PasswordEncoder), StoreProperties (store.* trong application.properties) (shared, root)
│   │   │   ├── auth/                      → Foundation + M5: Staff (abstract), Admin, Cashier, Waiter, Chef, StaffRole,
│   │   │   │                                Screen, AccessLevel, StaffAccount, StaffDao(+Jdbc), AuthService,
│   │   │   │                                AuthenticationFailedException, CurrentSession, LoginFrame, MainFrame,
│   │   │   │                                PlaceholderPanel; Phase 1: StaffService, StaffPanel
│   │   │   ├── reports/                   → M5: ReportDao(+Jdbc), ReportService, ReportPanel
│   │   │   ├── menu/                      → M1: Product (abstract), Food, Drink, Combo, ComboComponent,
│   │   │   │                                Category, ProductFactory, DAO(+Jdbc), ProductService, CategoryService, MenuPanel
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

DAO mẫu cho INSERT — **phải trả về id vừa sinh** để các bước sau (ví dụ lưu thành phần combo) dùng được:

```java
@Override
public long save(Product p) {
    String sql = "INSERT INTO products (category_id, name, product_type, price, description, is_active) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(con -> {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getType().name());
        ps.setBigDecimal(4, p.getPrice());
        ps.setString(5, p.getDescription());   // null được chấp nhận
        ps.setBoolean(6, p.isActive());
        return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
}
```

Quy ước:

- **Package-by-module** (`menu/`, `orders/`, `inventory/`, `payments/` + `customers/`, `auth/` + `reports/`), không package-by-layer: mỗi người làm gần như toàn bộ trong thư mục của mình, giữ diff và conflict gọn. Code dùng chung (`FoodStoreApplication`, `common/`, `config/`) chỉ sửa ở Phase 0.
- Trong module vẫn tách vai trò bằng hậu tố tên file: `Xxx.java` (model), `XxxDao`/`JdbcXxxDao`, `XxxService`/`XxxServiceImpl`, `XxxPanel`. `payments/discount/` là ngoại lệ vì nhóm file Strategy đi cùng nhau.
- DAO là interface + một bản Jdbc → service test được bằng Mockito.
- Không đặt nghiệp vụ trong DAO hoặc Panel; Panel chỉ dựng Swing và gọi service.
- Giao tiếp giữa module **qua interface Service**, không gọi DAO của module khác (ngoại lệ chỉ đọc: D11).
- `@Transactional` đặt ở tầng service (bean `@Service`), không đặt ở DAO. Không gọi phương thức `@Transactional` từ chính lớp đó (self-invocation bỏ qua proxy).
- Exception nghiệp vụ: ném `BusinessException` (hoặc lớp con) với thông điệp tiếng Việt hiển thị được cho người dùng; Panel bắt và hiện hộp thoại.
- Swing: một `JFrame` + `CardLayout`; mỗi tính năng một `JPanel` là `@Component` (D10).

## Luồng tích hợp quan trọng nhất: Thanh toán

`CheckoutService.pay(orderId, method, voucherCode)` (M4 sở hữu), chạy trong **một transaction** `@Transactional(rollbackFor = Exception.class)`:

1. Lấy đơn (`OrderService.getById`); phải ở trạng thái `SERVED`, ngược lại ném `BusinessException`.
2. `subtotal = OrderService.calcSubtotal(orderId)` (dùng `unit_price` đã chụp).
3. Chọn strategy (D2): có `voucherCode` → `VoucherService.validate(...)` (ném lỗi nếu không hợp lệ) rồi `VoucherDiscount`; không có mã mà đơn có khách → `TierDiscount`; còn lại `NoDiscount`.
4. `discount = min(strategy.computeDiscount(...), subtotal)`; `taxable = subtotal − discount`; `vat = TaxPolicy.vat(taxable)`; `total = taxable + vat` (D1).
5. Ghi `payments` (cột `order_id` UNIQUE là chốt chặn cuối chống thanh toán đôi).
6. `OrderService.markPaid(orderId)` — câu UPDATE có điều kiện `status='SERVED'`, 0 dòng thì ném lỗi (D4).
7. Nếu có voucher: `VoucherDao.incrementUsed` có điều kiện lượt dùng, 0 dòng thì ném lỗi (D4).
8. `InventoryService.deductForOrder(orderId)` (M3): trừ kho theo `recipes`, mở rộng combo (D12), xử lý thiếu hàng (D5).
9. Nếu đơn có khách: `CustomerService.addPoints(customerId, total)` (M4): cộng điểm, cập nhật hạng.
10. Nếu đơn ăn tại chỗ: `TableService.release(tableId)` (M2): trả bàn về `FREE`.

Bước nào ném exception thì toàn bộ rollback. `LowStockEvent` chỉ được xử lý sau commit (D7).

## Testing Strategy

- **Unit test (JUnit 5 + Mockito):** mỗi `*Service` một test class, DAO mock. Bắt buộc có: từng `DiscountStrategy`, chuyển trạng thái đơn hợp lệ/không hợp lệ, trừ kho theo công thức (kể cả combo và thiếu hàng), `CheckoutService` gọi đúng thứ tự (`InOrder`) và **dừng + ném exception** khi một bước lỗi, `AuthService` (sai mật khẩu, tài khoản bị khóa).
- **Rollback thật** không kiểm chứng được bằng Mockito (mock không có transaction). Kiểm bằng tay trên MySQL thật ở Phase 3 (xem `integration.md`), gồm cả trường hợp exception checked để xác nhận `rollbackFor`.
- **DAO test:** chạy thủ công trên MySQL local, không nằm trong `mvn test`. Testcontainers là stretch goal.
- **Manual QA:** mỗi owner demo CRUD + nghiệp vụ trên DB thật trước khi merge.
- Mục tiêu coverage: logic tầng service, không tính UI và getter/setter.

## Boundaries

- **Luôn làm:** validate ở service (không chỉ ở UI); dùng tham số `?`; chạy `mvn test` trước khi merge.
- **Hỏi nhóm trước (team chat/standup):** sửa `schema.sql` sau Phase 0, đổi tên package người khác đang dùng, thêm dependency Maven, đổi quy ước điều hướng Swing, đổi chữ ký interface Service đã chốt.
- **Không bao giờ:** commit mật khẩu thật hoặc `application.properties` chứa thông tin DB cá nhân (dùng biến môi trường hoặc file override nằm trong `.gitignore`); lưu mật khẩu plaintext; dùng `double` cho tiền; ném checked exception từ luồng giao dịch mà không khai báo `rollbackFor`.

## Success Criteria

- App chạy, có màn hình đăng nhập, xác thực với bảng `staff` (mật khẩu BCrypt), vào đúng dashboard theo vai trò; màn hình `VIEW` không sửa được.
- 5 module đều có CRUD (hoặc tập con phù hợp) trên MySQL thật.
- Luồng tạo đơn → chế biến → phục vụ → thanh toán chạy trọn vẹn và kho được trừ đúng.
- Giảm giá tính bằng Strategy (tối thiểu `NoDiscount` + `VoucherDiscount`, demo được); voucher sai bị từ chối.
- Rollback thanh toán đã kiểm thủ công (cả exception unchecked và checked).
- Mỗi module có ít nhất một JUnit test cho nghiệp vụ lõi.
- Không có SQL nối chuỗi từ input người dùng.
- Máy thành viên khác chạy được chỉ bằng các bước trong README (MySQL mới + `mvn spring-boot:run`).
- Có ER diagram và class diagram trong `docs/`.

## Open Questions (nhóm xác nhận)

1. **Phân công người ↔ module** — các file dùng `Owner 1`–`Owner 5`, thay bằng tên thật.
2. **Xác nhận D1 (VAT):** mức VAT áp dụng cho đồ án và việc giá niêm yết chưa gồm VAT.
3. **Xác nhận D5/D6:** chính sách kho khi thiếu nguyên liệu và thời điểm trừ kho.
4. **Xác nhận bảng phân quyền:** đặc biệt Cashier được EDIT `ORDERS` (để tạo đơn mang đi).
5. Stretch goals (không bắt buộc): giao hàng, tách bill, trừ kho FIFO theo lô, xuất CSV, `FlatLaf`, đặt bàn trước, Testcontainers.
