# Spec: Hệ thống Quản lý Cửa hàng Đồ ăn (FoodStore) — Spring Web MVC

## Status: DRAFT v3 — chuyển từ Swing sang ứng dụng web Spring MVC; các quyết định mặc định ở mục "Quyết định thiết kế", nhóm xác nhận mục "Open Questions"

Spec đối chiếu với mô tả bảng ở mục Data Model. **Nguồn duy nhất của schema là `src/main/resources/schema.sql`**; khác nhau thì sửa spec cho khớp schema (hoặc hỏi nhóm trước khi sửa schema).

## Context

- Đồ án môn OOP: thực hành kế thừa, đa hình, đóng gói, trừu tượng và design pattern.
- Nhóm 5 người — mỗi người sở hữu **một module end-to-end** (model + DAO + service + controller + template giao diện).
- Stack: Spring Boot + **Spring Web MVC** + Thymeleaf (render phía server) + Spring Security; MySQL; SQL viết tay qua `JdbcTemplate` (không dùng ORM) để luyện viết SQL trực tiếp.

## Assumptions

(Sai chỗ nào thì sửa ngay, spec dưới đây viết dựa trên các giả định này.)

1. **Spring Boot + Spring Web MVC**, Java 21, Maven, một module Maven (một `pom.xml`). Cả nhóm dùng chung một phiên bản Spring Boot, chốt ở Phase 0.
2. **Ứng dụng web render phía server bằng Thymeleaf** (`@Controller` trả tên view). Không phải SPA, không bắt buộc REST. *Nếu nhóm dùng JSP hoặc frontend riêng (React/Vue gọi REST) thì báo để sửa: phần model/DAO/service giữ nguyên, chỉ đổi controller và view.*
3. **Spring Security** cho đăng nhập (form login, session), băm mật khẩu BCrypt, CSRF và phân quyền theo URL.
4. **Plain JDBC qua `JdbcTemplate`**, không dùng JPA/Hibernate.
5. **MySQL 8.0.16+** (từ bản này mới kiểm tra ràng buộc `CHECK`), InnoDB, một schema `foodstore_db`.
6. **Tiền VND không có phần thập phân:** `DECIMAL(12,0)` trong DB, `BigDecimal` trong Java; cấm `double`/`float`; làm tròn `HALF_UP` về đồng.
7. Phạm vi: ăn tại chỗ + mang đi, một chi nhánh, **nhiều nhân viên dùng đồng thời qua trình duyệt** (nên xử lý tranh chấp dữ liệu thật, xem D4). Giao hàng là stretch goal.
8. "5 module" là **package/feature-area trong một app**, không phải 5 service triển khai riêng.
9. Repo: https://github.com/thuc09022006/Food_Store_Managerment (công khai).

## Quyết định thiết kế (mặc định — nhóm có thể đổi, nhưng sửa spec trước khi code)

| #   | Chủ đề                         | Quyết định                                                                                                                                                                                                                                                                                       |
|-----|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| D1  | Giá và VAT                     | Giá niêm yết **chưa gồm VAT**. Giảm giá trừ trước, VAT tính **sau** giảm: `taxable = subtotal − discount`, `vat = round_HALF_UP(taxable × store.vat-rate)`, `total = taxable + vat`.                                                                                                             |
| D2  | Voucher sai/hết hạn            | Đã nhập mã mà mã không hợp lệ → **từ chối** thanh toán và báo lý do (không âm thầm chuyển sang giảm theo hạng). Không nhập mã → `TierDiscount` nếu đơn có khách, ngược lại `NoDiscount`. Không cộng dồn.                                                                                         |
| D3  | Exception và rollback          | Mọi lỗi nghiệp vụ kế thừa `BusinessException extends RuntimeException`. `@Transactional` ở service luôn ghi `rollbackFor = Exception.class` cho luồng nhiều bước (Spring mặc định **không** rollback checked exception).                                                                         |
| D4  | Tranh chấp đồng thời           | Nhiều người dùng cùng lúc nên mọi chuyển trạng thái dùng câu SQL có điều kiện và kiểm số dòng: `UPDATE orders SET status='PAID' WHERE id=? AND status='SERVED'`; `UPDATE dining_tables SET status='OCCUPIED' WHERE id=? AND status='FREE'`; `UPDATE vouchers SET used_count = used_count + 1 WHERE id=? AND used_count < usage_limit` (`usage_limit` là NOT NULL trong schema; DB còn có `CHECK (used_count <= usage_limit)` làm chốt chặn cuối). 0 dòng → ném `BusinessException`. `UNIQUE(payments.order_id)` là chốt chặn cuối chống thanh toán đôi. |
| D5  | Kho thiếu nguyên liệu lúc thanh toán | Vẫn cho thanh toán; trừ kho tối thiểu về 0 (`GREATEST(stock_quantity − ?, 0)`), ghi nhận phần thiếu và phát cảnh báo. Lý do: món đã bán thật.                                                                                                                                         |
| D6  | Thời điểm trừ kho              | Trừ khi đơn `PAID`. Hạn chế đã biết: món nấu xong rồi bị hủy thì kho không phản ánh nguyên liệu hao.                                                                                                                                                                                            |
| D7  | `LowStockEvent`                | Phát trong transaction; listener dùng `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` và **chỉ ghi log**, không đụng giao diện (web không đẩy được sang trình duyệt). Người dùng thấy cảnh báo khi mở trang Kho/Dashboard (truy vấn `findLowStock()`).               |
| D8  | Phân quyền                     | Mỗi vai trò có map `Feature → AccessLevel (VIEW / EDIT)`; EDIT bao gồm VIEW. **Server bắt buộc kiểm quyền** (Spring Security), ẩn nút trên giao diện chỉ để tiện dùng chứ không phải bảo mật: quy tắc `GET /<feature>/**` cần `<FEATURE>_VIEW`, mọi method khác cần `<FEATURE>_EDIT`. Service không kiểm vai trò. |
| D9  | Gắn khách vào đơn              | `OrderService.attachCustomer(orderId, customerId)` (thêm vào contract); chỉ đơn chưa `PAID`/`CANCELLED`.                                                                                                                                                                                         |
| D10 | Form và điều hướng             | Mọi form POST dùng **Post-Redirect-Get** (redirect sau khi xử lý, báo kết quả bằng flash attribute) để F5 không gửi lại; form có CSRF token (Thymeleaf `th:action` tự thêm); nút thanh toán chống bấm đúp bằng cách vô hiệu nút khi submit (D4 vẫn là chốt chặn thật).                          |
| D11 | Validate hai lớp               | Form object có Bean Validation (`@NotBlank`, `@PositiveOrZero`...) để báo lỗi từng ô; **service vẫn validate lại** vì service là nơi giữ luật nghiệp vụ (có thể được gọi từ nơi khác).                                                                                                           |
| D12 | Báo cáo đọc dữ liệu module khác | `ReportDao` (M5) được chạy SQL **chỉ đọc** trên mọi bảng; ngoại lệ có chủ đích của quy tắc "không gọi DAO module khác". Mọi thao tác ghi vẫn đi qua Service.                                                                                                                                      |
| D13 | Combo và kho                   | Combo không có công thức riêng; `deductForOrder` mở rộng combo thành thành phần (`ProductService.getComponents`) nhân số lượng.                                                                                                                                                                  |
| D14 | Lỗi nghiệp vụ trên web         | `@ControllerAdvice` (`GlobalExceptionHandler`) bắt `BusinessException` → quay lại trang trước kèm thông báo lỗi tiếng Việt; lỗi khác → trang lỗi chung, **không** lộ stack trace.                                                                                                                |
| D15 | Người thao tác                | `orders.staff_id` và `stock_imports.staff_id` là NOT NULL: controller lấy người đang đăng nhập (`@AuthenticationPrincipal StaffUserDetails`) và truyền `staffId` xuống service; service không tự đoán người dùng.                                                                                                                                  |

## Objective

Xây dựng hệ thống web giúp nhân viên cửa hàng đồ ăn:

- Quản lý thực đơn (món ăn, đồ uống, combo, danh mục).
- Quản lý bàn và đơn hàng (tạo đơn, thêm/bớt món, theo dõi trạng thái từ bếp đến phục vụ).
- Quản lý kho nguyên liệu, nhà cung cấp, phiếu nhập, cảnh báo sắp hết và sắp hết hạn.
- Thanh toán, giảm giá/voucher, tính VAT, hóa đơn, tích điểm khách hàng.
- Đăng nhập theo vai trò (Admin, Cashier, Waiter, Chef), xem dashboard và báo cáo theo quyền.

**Tiêu chí thành công:** ứng dụng chạy trên MySQL, truy cập qua trình duyệt; mỗi module có CRUD + ít nhất một nghiệp vụ thật; code thể hiện rõ các khái niệm OOP ghi ở từng module.

**Người dùng:** Admin/quản lý, thu ngân (Cashier), phục vụ (Waiter), bếp (Chef) — do 5 thành viên đóng vai khi demo.

## Yêu cầu chức năng

| Mã    | Yêu cầu                                                                                                 | Module  |
|-------|---------------------------------------------------------------------------------------------------------|---------|
| FR-01 | CRUD món ăn, đồ uống, danh mục; đặt giá; bật/tắt "đang bán"                                             | M1      |
| FR-02 | Combo gồm nhiều món với giá riêng                                                                       | M1      |
| FR-03 | Tìm kiếm/lọc món theo tên, danh mục, trạng thái                                                         | M1      |
| FR-04 | Quản lý bàn (trống / có khách / đã đặt)                                                                 | M2      |
| FR-05 | Tạo đơn: ăn tại chỗ (gắn bàn) hoặc mang đi; thêm/sửa/xóa món; ghi chú món; gắn khách hàng               | M2      |
| FR-06 | Vòng đời đơn NEW → PREPARING → SERVED → PAID, và CANCELLED; chỉ cho phép chuyển trạng thái hợp lệ       | M2      |
| FR-07 | Bếp xem danh sách đơn chờ chế biến và đánh dấu đã xong                                                  | M2      |
| FR-08 | Quản lý nguyên liệu: tồn kho, đơn vị, mức tồn tối thiểu                                                 | M3      |
| FR-09 | Công thức món (mỗi món dùng bao nhiêu nguyên liệu)                                                      | M3      |
| FR-10 | Nhập kho từ nhà cung cấp (phiếu nhập, giá nhập, hạn sử dụng)                                            | M3      |
| FR-11 | Tự trừ kho theo công thức khi thanh toán xong                                                           | M3 + M4 |
| FR-12 | Cảnh báo nguyên liệu sắp hết và lô sắp hết hạn                                                          | M3      |
| FR-13 | Thanh toán: tiền mặt, thẻ, chuyển khoản, ví điện tử                                                     | M4      |
| FR-14 | Giảm giá theo hạng thành viên hoặc voucher (% hoặc số tiền, hạn dùng, giới hạn lượt, đơn tối thiểu)     | M4      |
| FR-15 | Tính VAT theo mức cấu hình được; trang hóa đơn in được                                                  | M4      |
| FR-16 | Khách hàng thành viên: tích điểm, lên hạng                                                              | M4      |
| FR-17 | Đăng nhập/đăng xuất, phân quyền theo vai trò (xem/sửa), menu riêng cho từng vai trò                     | M5      |
| FR-18 | Quản lý nhân viên (Admin)                                                                               | M5      |
| FR-19 | Báo cáo: doanh thu theo ngày/tháng, món bán chạy, nguyên liệu tồn thấp                                  | M5      |

## Yêu cầu phi chức năng (bắt buộc)

- **Toàn vẹn giao dịch:** thanh toán + đổi trạng thái đơn + trừ kho + cộng điểm + trả bàn nằm trong **một `@Transactional(rollbackFor = Exception.class)`** (D3).
- **Đồng thời:** nhiều nhân viên thao tác cùng lúc; chuyển trạng thái bằng UPDATE có điều kiện (D4).
- **Chụp giá tại thời điểm đặt:** `order_items.unit_price` lưu giá lúc gọi món.
- **Xóa mềm:** món đã được tham chiếu (đơn, combo, công thức) chỉ đặt `is_active = FALSE`.
- **Không số âm:** số lượng đơn > 0, tồn kho không âm (chặn ở service, `CHECK` ở DB).
- **Bảo mật web:** mật khẩu BCrypt; mọi SQL dùng tham số `?`; CSRF bật cho mọi POST; xuất dữ liệu ra HTML bằng `th:text` (escape tự động), **cấm `th:utext`** với dữ liệu người dùng nhập; kiểm quyền ở server (D8); session hết hạn sau thời gian cấu hình; trang lỗi không lộ stack trace.
- **Cấu hình:** `store.vat-rate`, `store.loyalty.*`, `store.expiry-warning-days` trong `application.properties`, đọc qua `StoreProperties`.

## Tech Stack

- Java 21
- Spring Boot 4.0.9 (ghim trong `pom.xml`): `spring-boot-starter-webmvc` (Boot 4 đổi tên từ `-web`), `-thymeleaf`, `-security`, `-validation`, `-jdbc`; test: `spring-boot-starter-test`, `-webmvc-test`, `-security-test`. Dòng 4.0 được hỗ trợ đến hết 2026-12-31 (theo trang theo dõi bên thứ ba); sau đó cân nhắc 4.1.x.
- Thymeleaf + `thymeleaf-extras-springsecurity` (ẩn/hiện phần giao diện theo quyền)
- MySQL 8.0.16+ và `mysql-connector-j`
- CSS: một file tự viết hoặc Bootstrap qua WebJars/CDN (nhóm chọn một cách, ghi vào `foundation.md`)
- Maven
- JUnit 5 + Mockito + MockMvc (test controller và phân quyền)

## Feature Modules (mỗi người một module)

| # | Module                             | Trách nhiệm                                                               | Trọng tâm OOP                                                                                                                          |
|---|------------------------------------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| 1 | **Menu**                           | CRUD món/đồ uống/combo/danh mục, tìm kiếm, bật/tắt bán                    | Kế thừa + đa hình: `abstract Product` → `Food`, `Drink`, `Combo`. **Composite:** `Combo` chứa thành phần. **Factory:** `ProductFactory` |
| 2 | **Orders & Tables**                | Bàn, tạo/sửa đơn, vòng đời đơn, trang bếp                                 | **State** (hoặc enum có bảng chuyển trạng thái); composition: `Order` có nhiều `OrderItem`                                             |
| 3 | **Inventory & Suppliers**          | Nguyên liệu, công thức, nhà cung cấp, phiếu nhập, cảnh báo                | **Observer:** tồn ≤ mức tối thiểu thì phát `LowStockEvent` (`ApplicationEventPublisher`)                                               |
| 4 | **Payments, Vouchers & Customers** | Thanh toán, giảm giá, VAT, hóa đơn, tích điểm                             | **Strategy:** `DiscountStrategy` → `NoDiscount`, `TierDiscount`, `VoucherDiscount`                                                     |
| 5 | **Staff, Auth & Reports**          | Đăng nhập, phân quyền, layout/menu theo vai trò, quản lý nhân viên, báo cáo | Kế thừa/đa hình: `abstract Staff` → `Admin`, `Cashier`, `Waiter`, `Chef`, override `permissions()`; truy vấn tổng hợp (`GROUP BY`)     |

Phần Auth của M5 (Phase 0) chạy song song với Foundation và chặn mọi người.

> **Đặt tên:** lớp cha của món là `Product` (không đặt `MenuItem` để khỏi nhầm với `java.awt.MenuItem` và lớp menu của UI). Vai trò đặt `Chef`, `Cashier`, `Waiter`, `Admin`.

### Phân quyền theo vai trò

| Vai trò | Tính năng (`Feature`) và mức truy cập                                          |
|---------|---------------------------------------------------------------------------------|
| Admin   | Tất cả — EDIT                                                                   |
| Cashier | ORDERS (EDIT — tạo đơn mang đi, gắn khách), PAYMENTS (EDIT), CUSTOMERS (EDIT)   |
| Waiter  | TABLES (EDIT), ORDERS (EDIT), MENU (VIEW)                                       |
| Chef    | KITCHEN (EDIT), INVENTORY (VIEW)                                                |

`Feature`: `MENU, TABLES, ORDERS, KITCHEN, INVENTORY, PAYMENTS, CUSTOMERS, STAFF, REPORTS`. Mỗi `Feature` có một `pathPrefix` (`/menu`, `/tables`, `/orders`, `/kitchen`, `/inventory`, `/payments`, `/customers` (gồm cả `/customers/vouchers`), `/staff`, `/reports`). `SecurityConfig` sinh quy tắc từ danh sách này: `GET` cần authority `<FEATURE>_VIEW`, method khác cần `<FEATURE>_EDIT`; vai trò có EDIT được cấp cả VIEW. Controller của một feature chỉ nằm dưới `pathPrefix` của nó. Khi controller cần dữ liệu của feature khác (ví dụ trang tạo đơn cần danh sách món), gọi **service** của module kia, không gọi URL của họ.

## Data Model (MySQL — `foodstore_db`)

15 bảng, đúng thứ tự khóa ngoại. Sơ đồ: [`docs/er-diagram.md`](docs/er-diagram.md).

| #  | Bảng                 | Module | Ghi chú                                                                               |
|----|----------------------|--------|---------------------------------------------------------------------------------------|
| 1  | `staff`              | M5     | `role` ENUM ADMIN/CASHIER/WAITER/CHEF; `password_hash` BCrypt; `is_active`            |
| 2  | `categories`         | M1     |                                                                                       |
| 3  | `products`           | M1     | `product_type` FOOD/DRINK/COMBO; `price DECIMAL(12,0)`; `is_active` (xóa mềm)         |
| 4  | `combo_components`   | M1     | khóa chính `(combo_id, component_id)`                                                 |
| 5  | `dining_tables`      | M2     | `status` FREE/OCCUPIED/RESERVED                                                       |
| 6  | `customers`          | M4     | `points`, `tier` NORMAL/SILVER/GOLD                                                   |
| 7  | `vouchers`           | M4     | `discount_type` PERCENT/FIXED, `discount_value`, `min_order`, hạn dùng, giới hạn lượt |
| 8  | `orders`             | M2     | `table_id`/`customer_id` nullable; `staff_id` NOT NULL (người tạo đơn); `status` NEW/PREPARING/SERVED/PAID/CANCELLED       |
| 9  | `order_items`        | M2     | `unit_price` là giá **chụp** lúc gọi món                                              |
| 10 | `suppliers`          | M3     |                                                                                       |
| 11 | `ingredients`        | M3     | `stock_quantity`, `min_quantity` (đều `CHECK >= 0`)                                   |
| 12 | `recipes`            | M3     | khóa chính `(product_id, ingredient_id)`; lượng cho 1 phần; chỉ cho FOOD/DRINK        |
| 13 | `stock_imports`      | M3     | `staff_id` NOT NULL (người lập phiếu)                                                  |
| 14 | `stock_import_items` | M3     | `expiry_date` theo lô (nullable)                                                      |
| 15 | `payments`           | M4     | `order_id` UNIQUE: một đơn thanh toán một lần                                         |

Hạn sử dụng lưu theo lô nhập; `ingredients.stock_quantity` chỉ là tổng tồn. Kho không trừ theo lô (FIFO là stretch goal) nên cảnh báo hết hạn có thể báo cả lô đã dùng hết — hạn chế đã biết, ghi rõ trên trang cảnh báo.

## Commands

```
Build:   mvn clean package
Run:     mvn spring-boot:run      →  mở http://localhost:8080
Test:    mvn test
Package: mvn clean package        →  target/foodstore-management-<version>.jar
```

Cài MySQL (mỗi dev một lần): xem `README.md` (lệnh `-e "source ..."` chạy được trên PowerShell).

## Project Structure

```
FoodStore-Management-OOP/
├── pom.xml
├── README.md · SPEC.md · plan.md · foundation.md · integration.md
├── m1-menu.md ... m5-staff-reports.md
├── docs/er-diagram.md                      → thêm class diagram khi xong Phase 1
├── src/
│   ├── main/
│   │   ├── java/com/foodstore/
│   │   │   ├── FoodStoreApplication.java   → main(), SpringApplication.run (shared, root)
│   │   │   ├── common/                     → BusinessException, GlobalExceptionHandler (@ControllerAdvice) (shared, root)
│   │   │   ├── config/                     → SecurityConfig, StoreProperties (store.*) (shared, root)
│   │   │   ├── auth/                       → Foundation + M5: Staff (abstract), Admin, Cashier, Waiter, Chef, StaffRole,
│   │   │   │                                 Feature, AccessLevel, StaffAccount, StaffDao(+Jdbc), StaffUserDetails,
│   │   │   │                                 StaffUserDetailsService, LoginController, DashboardController;
│   │   │   │                                 Phase 1: StaffService, StaffController, StaffForm
│   │   │   ├── reports/                    → M5: ReportDao(+Jdbc), ReportService, ReportController
│   │   │   ├── menu/                       → M1: Product (abstract), Food, Drink, Combo, ComboComponent, Category,
│   │   │   │                                 ProductFactory, DAO(+Jdbc), ProductService, CategoryService,
│   │   │   │                                 MenuController, ProductForm, CategoryForm
│   │   │   ├── orders/                     → M2: Order, OrderItem, OrderStatus, OrderType, DiningTable, TableStatus,
│   │   │   │                                 DAO(+Jdbc), OrderService, TableService, OrderController,
│   │   │   │                                 TableController, KitchenController, forms
│   │   │   ├── inventory/                  → M3: Ingredient, RecipeLine, Supplier, StockImport(+Item), DAO(+Jdbc),
│   │   │   │                                 InventoryService, LowStockEvent, LowStockListener, InventoryController, forms
│   │   │   ├── payments/                   → M4: Payment, PaymentMethod, PaymentDao(+Jdbc), PaymentService, CheckoutService,
│   │   │   │   └── discount/                 TaxPolicy, InvoiceView, PaymentController; DiscountStrategy family
│   │   │   └── customers/                  → M4: Customer, CustomerTier, Voucher, DAO(+Jdbc), CustomerService,
│   │   │                                     VoucherService, CustomerController, VoucherController, forms
│   │   └── resources/
│   │       ├── application.properties      → datasource (env vars), server.port, session timeout, store.*
│   │       ├── schema.sql · data.sql
│   │       ├── templates/
│   │       │   ├── layout/base.html        → khung chung + menu theo quyền (shared, root)
│   │       │   ├── login.html · dashboard.html · error.html   (shared, root / M5)
│   │       │   └── menu/ orders/ tables/ kitchen/ inventory/ payments/ customers/ staff/ reports/   → mỗi module một thư mục template
│   │       └── static/                     → css/, js/ dùng chung
│   └── test/java/com/foodstore/            → unit test + test controller, mirror cấu trúc main
```

## Code Style

DAO mẫu — SQL viết tay + `JdbcTemplate`, xử lý cột NULL, INSERT trả về id:

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

    private Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ProductFactory.create(
            rs.getLong("id"), rs.getLong("category_id"), rs.getString("name"),
            rs.getString("product_type"), rs.getBigDecimal("price"),
            rs.getString("description"),          // getString trả null an toàn khi cột NULL
            rs.getBoolean("is_active"));
    }
}
```

Controller mẫu — mỏng, Post-Redirect-Get, lỗi nghiệp vụ do `GlobalExceptionHandler` xử lý:

```java
@Controller
@RequestMapping("/menu")
public class MenuController {

    private final ProductService productService;

    public MenuController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String name, Model model) {
        model.addAttribute("products", productService.search(name, null, null));
        return "menu/list";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable long id, @RequestParam boolean active, RedirectAttributes ra) {
        productService.setActive(id, active);
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái bán");
        return "redirect:/menu";
    }
}
```

Quy ước:

- **Package-by-module** (`menu/`, `orders/`, `inventory/`, `payments/` + `customers/`, `auth/` + `reports/`), template của module nằm trong `templates/<module>/`. Code dùng chung (`FoodStoreApplication`, `common/`, `config/`, `layout/base.html`) chỉ sửa ở Phase 0.
- Hậu tố tên file: `Xxx` (model), `XxxDao`/`JdbcXxxDao`, `XxxService`/`XxxServiceImpl`, `XxxController`, `XxxForm` (đối tượng nhận dữ liệu form, có Bean Validation).
- DAO là interface + một bản Jdbc → service test được bằng Mockito.
- **Controller mỏng:** chỉ nhận request, validate form, gọi service, chọn view/redirect. Không đặt nghiệp vụ trong controller, DAO hay template.
- Giao tiếp giữa module **qua interface Service**, không gọi DAO của module khác (ngoại lệ chỉ đọc: D12).
- `@Transactional` ở tầng service, không ở DAO/controller; không gọi phương thức `@Transactional` từ chính lớp đó (self-invocation bỏ qua proxy).
- Không trả entity trực tiếp ra form khi sửa: dùng `XxxForm`, map thủ công trong controller/service.
- Template dùng `th:text`; link dùng `@{...}`; form dùng `th:action` (có CSRF token).

## Luồng tích hợp quan trọng nhất: Thanh toán

`POST /payments/{orderId}/pay` → `PaymentController` → `CheckoutService.pay(orderId, method, voucherCode)` (M4), chạy trong **một transaction** `@Transactional(rollbackFor = Exception.class)`:

1. Lấy đơn (`OrderService.getById`); phải `SERVED`, ngược lại ném `BusinessException`.
2. `subtotal = OrderService.calcSubtotal(orderId)`.
3. Chọn strategy (D2): có `voucherCode` → `VoucherService.validate(...)` (sai thì ném lỗi) rồi `VoucherDiscount`; không có mã mà đơn có khách → `TierDiscount`; còn lại `NoDiscount`.
4. `discount = min(strategy, subtotal)`; `taxable = subtotal − discount`; `vat = TaxPolicy.vat(taxable)`; `total = taxable + vat` (D1).
5. Ghi `payments` (`order_id` UNIQUE chặn thanh toán đôi).
6. `OrderService.markPaid(orderId)` — UPDATE có điều kiện `status='SERVED'`, 0 dòng thì ném lỗi (D4).
7. Có voucher: `VoucherDao.incrementUsed` có điều kiện lượt dùng, 0 dòng thì ném lỗi (D4).
8. `InventoryService.deductForOrder(orderId)` (M3): trừ kho theo `recipes`, mở rộng combo, xử lý thiếu hàng (D5, D13).
9. Có khách: `CustomerService.addPoints(customerId, total)` (M4).
10. Đơn ăn tại chỗ: `TableService.release(tableId)` (M2).

Bước nào ném exception thì rollback tất cả; controller bắt lỗi qua `GlobalExceptionHandler`, trả về trang thanh toán kèm thông báo. Thành công thì redirect sang trang hóa đơn (PRG). `LowStockEvent` chỉ xử lý sau commit (D7).

## Testing Strategy

- **Unit test (JUnit 5 + Mockito):** mỗi `*Service` một test class, DAO mock. Bắt buộc: từng `DiscountStrategy`, chuyển trạng thái đơn, trừ kho (kể cả combo và thiếu hàng), `CheckoutService` gọi đúng thứ tự (`InOrder`) và **dừng + ném exception** khi một bước lỗi, `StaffUserDetailsService` (không tồn tại, bị khóa).
- **Test controller (MockMvc):** mỗi module ít nhất một test cho (a) trang danh sách trả đúng view, (b) **phân quyền**: vai trò chỉ có VIEW gửi POST nhận `403`, người chưa đăng nhập bị chuyển về `/login`, POST thiếu CSRF token bị từ chối. Dùng người dùng giả có authority cần thiết.
- **Rollback thật** không kiểm chứng được bằng Mockito. Kiểm tay trên MySQL thật ở Phase 3 (xem `integration.md`), gồm cả exception checked để xác nhận `rollbackFor`.
- **Tranh chấp đồng thời:** kiểm tay bằng hai trình duyệt/hai tài khoản (xem `integration.md`).
- **DAO test:** chạy thủ công trên MySQL local; Testcontainers là stretch goal.
- Mục tiêu coverage: logic tầng service; không tính template và getter/setter.

## Boundaries

- **Luôn làm:** validate ở service (không chỉ ở form); dùng tham số `?`; kiểm quyền ở server; chạy `mvn test` trước khi merge.
- **Hỏi nhóm trước:** sửa `schema.sql` sau Phase 0; đổi tên package người khác đang dùng; thêm dependency Maven; sửa `layout/base.html`, `SecurityConfig` hoặc `GlobalExceptionHandler`; đổi chữ ký interface Service đã chốt.
- **Không bao giờ:** commit mật khẩu thật hoặc `application.properties` chứa thông tin DB cá nhân; lưu mật khẩu plaintext; dùng `double` cho tiền; tắt CSRF để "cho dễ làm"; dùng `th:utext` với dữ liệu người dùng nhập; nối chuỗi SQL từ input; chỉ ẩn nút mà không kiểm quyền ở server.

## Success Criteria

- App chạy, truy cập được bằng trình duyệt, đăng nhập bằng bảng `staff` (BCrypt), vào đúng dashboard theo vai trò.
- Vai trò chỉ có VIEW không sửa được **kể cả khi gửi request thủ công** (test `403`).
- 5 module đều có CRUD (hoặc tập con phù hợp) trên MySQL thật.
- Luồng tạo đơn → chế biến → phục vụ → thanh toán chạy trọn vẹn và kho được trừ đúng.
- Giảm giá bằng Strategy (tối thiểu `NoDiscount` + `VoucherDiscount`); voucher sai bị từ chối.
- Rollback thanh toán đã kiểm tay (unchecked và checked); thanh toán đồng thời cùng một đơn chỉ thành công một lần.
- Mỗi module có ít nhất một JUnit test nghiệp vụ và một test controller.
- Không có SQL nối chuỗi từ input người dùng; mọi form POST có CSRF token.
- Máy thành viên khác chạy được chỉ bằng các bước trong README.
- Có ER diagram và class diagram trong `docs/`.

## Open Questions (nhóm xác nhận)

1. **Công nghệ view:** Thymeleaf (spec này), JSP, hay frontend riêng gọi REST? Đổi thì chỉ sửa controller/view.
2. **Phân công người ↔ module** — thay `Owner 1`–`Owner 5` bằng tên thật.
3. **Xác nhận D1 (VAT):** mức VAT và việc giá niêm yết chưa gồm VAT.
4. **Xác nhận D5/D6:** chính sách kho khi thiếu nguyên liệu và thời điểm trừ kho.
5. **Xác nhận bảng phân quyền:** đặc biệt Cashier được EDIT `ORDERS` (để tạo đơn mang đi).
6. **CSS:** tự viết hay Bootstrap?
7. Stretch goals (không bắt buộc): giao hàng, tách bill, trừ kho FIFO theo lô, xuất CSV, biểu đồ (Chart.js), AJAX tìm kiếm, đặt bàn trước, Testcontainers, kiểm quyền ở tầng service.
