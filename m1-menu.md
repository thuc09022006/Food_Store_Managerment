# M1: Menu

Folder: `src/main/java/com/foodstore/menu/` · Template: `src/main/resources/templates/menu/` · Owner: **Owner 1** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                              | Mục đích                                                                                                                                                                   |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `menu/Product.java`                                               | **Abstract** — `id`, `categoryId`, `name`, `price`, `description`, `active`; trừu tượng `getType()` (Foundation tạo bản đầu, M1 mở rộng)                                   |
| `menu/Food.java`, `menu/Drink.java`                               | `extends Product`                                                                                                                                                          |
| `menu/Combo.java`                                                 | `extends Product` — chứa `List<ComboComponent>` (Composite); `getComponentsTotal()`, `getSavings()`; từ chối combo lồng combo                                              |
| `menu/ComboComponent.java`                                        | `product` + `quantity` (≥ 1)                                                                                                                                               |
| `menu/Category.java`                                              | Model — `id`, `name`                                                                                                                                                       |
| `menu/ProductFactory.java`                                        | `create(...)` — dựng đúng lớp con theo `product_type`; loại không biết thì ném `IllegalArgumentException`                                                                  |
| `menu/CategoryDao.java` / `JdbcCategoryDao.java`                  | CRUD `categories` (bản mẫu từ Foundation) + `existsProductIn(long)`, `existsByName(String)`                                                                                |
| `menu/ProductDao.java` / `JdbcProductDao.java`                    | Xem chữ ký bên dưới; `findById` với combo nạp thêm thành phần bằng `findComponents`                                                                                        |
| `menu/ProductService.java` / `ProductServiceImpl.java`            | Interface đã chốt ở Foundation (`getById`, `findActive`, `getComponents`) — không đổi; M1 **thêm** phương thức bên dưới. Thao tác ghi: `@Transactional(rollbackFor = Exception.class)` |
| `menu/CategoryService.java` / `CategoryServiceImpl.java`          | Liệt kê, tạo, đổi tên, xóa danh mục (có kiểm tra)                                                                                                                          |
| `menu/ProductForm.java`                                           | Đối tượng nhận dữ liệu form: `name` (`@NotBlank`), `categoryId` (`@NotNull`), `type`, `price` (`@NotNull @PositiveOrZero`), `description`, danh sách thành phần combo         |
| `menu/CategoryForm.java`                                          | `name` (`@NotBlank`)                                                                                                                                                       |
| `menu/MenuController.java`                                        | `@Controller @RequestMapping("/menu")`: danh sách + tìm kiếm/lọc, form thêm/sửa, bật/tắt bán, xóa; POST theo Post-Redirect-Get                                             |
| `menu/CategoryController.java`                                    | `/menu/categories` — danh sách/thêm/đổi tên/xóa (mở rộng từ bản mẫu Foundation)                                                                                           |
| `templates/menu/list.html`, `form.html`, `categories.html`        | Trang danh sách (bảng + ô tìm + bộ lọc), form thêm/sửa (có phần chọn thành phần combo), trang danh mục. Dùng `layout/base.html`; nút Thêm/Sửa/Xóa bọc trong `sec:authorize="hasAuthority('MENU_EDIT')"` |
| `src/test/java/com/foodstore/menu/ProductServiceTest.java`        | Unit test — DAO mock                                                                                                                                                       |
| `src/test/java/com/foodstore/menu/ProductFactoryTest.java`        | Unit test factory + đa hình                                                                                                                                                |
| `src/test/java/com/foodstore/menu/MenuControllerTest.java`        | MockMvc: danh sách trả view `menu/list`; Waiter (`MENU_VIEW`) xem được nhưng POST nhận `403`; Admin POST thành công và được redirect; form thiếu tên bị trả lại kèm lỗi    |

### Chữ ký đề xuất

```java
public interface ProductDao {
    List<Product> findAll();
    Optional<Product> findById(long id);
    List<Product> search(String name, Long categoryId, Boolean active);   // tham số null = không lọc
    long save(Product p);                          // trả về id vừa sinh (KeyHolder)
    void update(Product p);
    void setActive(long id, boolean active);
    void delete(long id);                          // xóa cứng, chỉ gọi khi không còn tham chiếu
    List<ComboComponent> findComponents(long comboId);
    void replaceComponents(long comboId, List<ComboComponent> components);  // xóa cũ rồi chèn mới
    boolean existsInOrders(long productId);        // order_items
    boolean existsAsComponent(long productId);     // combo_components.component_id
    boolean existsInRecipes(long productId);       // recipes
    List<Product> findActiveCombosContaining(long productId);
}

public interface ProductService {
    // --- đã chốt ở Foundation (không đổi) ---
    Product getById(long id);
    List<Product> findActive();
    List<ComboComponent> getComponents(long comboId);
    // --- M1 thêm ---
    List<Product> search(String name, Long categoryId, Boolean active);
    long create(Product product, List<ComboComponent> components);   // components chỉ dùng cho Combo
    void update(Product product, List<ComboComponent> components);
    void setActive(long id, boolean active);
    void delete(long id);                         // xóa cứng nếu không bị tham chiếu, ngược lại ngừng bán
}

public interface CategoryService {
    List<Category> findAll();
    long create(String name);
    void rename(long id, String name);
    void delete(long id);
}
```

### Đường dẫn (URL)

| Method | URL                         | Quyền       | Việc                                    |
|--------|-----------------------------|-------------|-----------------------------------------|
| GET    | `/menu`                     | `MENU_VIEW` | Danh sách, tham số `name`, `categoryId`, `active` |
| GET    | `/menu/new`, `/menu/{id}/edit` | `MENU_VIEW` | Hiện form (nút Lưu chỉ hiện khi có `MENU_EDIT`) |
| POST   | `/menu`                     | `MENU_EDIT` | Tạo mới → redirect `/menu`              |
| POST   | `/menu/{id}`                | `MENU_EDIT` | Cập nhật → redirect `/menu`             |
| POST   | `/menu/{id}/active`         | `MENU_EDIT` | Bật/tắt bán                             |
| POST   | `/menu/{id}/delete`         | `MENU_EDIT` | Xóa (hoặc ngừng bán nếu còn tham chiếu) |
| GET/POST | `/menu/categories/...`    | VIEW/EDIT   | Quản lý danh mục                        |

> Quyền do `SecurityConfig` sinh từ `Feature.MENU` (GET → VIEW, method khác → EDIT). M1 **không** tự viết quy tắc quyền; chỉ kiểm bằng test `403`.

## Quy tắc nghiệp vụ (service phải chặn, không dựa vào form)

| Chủ đề          | Quy tắc                                                                                                                                                                                                   |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tên             | Bắt buộc, không rỗng sau `trim()`; danh mục không trùng tên (không phân biệt hoa thường)                                                                                                                  |
| Giá             | ≥ 0; **không có phần thập phân** (`price.scale() > 0` bị từ chối hoặc làm tròn `HALF_UP` — chọn một cách và test); không vượt giới hạn `DECIMAL(12,0)`                                                    |
| Combo           | Ít nhất 1 thành phần; `quantity ≥ 1`; **không được là combo**; không trùng thành phần (khóa chính `(combo_id, component_id)`); thành phần phải tồn tại. `getSavings()` âm thì trang cảnh báo nhưng vẫn cho lưu |
| Ngừng bán       | `setActive(false)` cho món đang là thành phần của combo **đang bán** → từ chối, nêu tên các combo đó. `setActive(true)` cho combo → mọi thành phần phải đang bán                                           |
| Xóa món         | Có trong `order_items`, là thành phần combo hoặc có công thức → **không xóa**, chuyển `active=false` (vẫn áp quy tắc "Ngừng bán"). Ngược lại xóa cứng; với combo thì xóa các dòng `combo_components` của nó trước, trong cùng transaction |
| Xóa danh mục    | Từ chối nếu còn món thuộc danh mục                                                                                                                                                                        |
| Lưu combo       | `save`/`update` + `replaceComponents` trong **một transaction**                                                                                                                                           |
| `findActive()`  | Chỉ món `active=TRUE`; combo trả về đã nạp thành phần                                                                                                                                                     |
| NULL            | `description` cho phép NULL                                                                                                                                                                               |
| Tìm kiếm        | `LIKE ?` với giá trị `%tên%` được tạo trong code; **không** nối chuỗi vào SQL. Dấu `%`/`_` người dùng gõ vào cần được escape nếu muốn tìm đúng ký tự đó                                                    |

## Tasks

- Task: Hierarchy `Product` (abstract) → `Food`, `Drink`, `Combo` + `ProductFactory`

  - Acceptance: `ProductFactory.create("COMBO", ...)` trả `Combo`; `getType()` qua tham chiếu `Product` ra đúng loại (đa hình); `Combo.getSavings()` = `componentsTotal − price`
  - Verify: `mvn test` (`ProductFactoryTest`)
  - Files: `menu/Product.java`, `Food.java`, `Drink.java`, `Combo.java`, `ComboComponent.java`, `ProductFactory.java`
- Task: `Category` + `ProductDao` (interface + Jdbc)

  - Acceptance: DAO chạy trên seed; `save` trả id vừa sinh; `search` lọc đúng (tham số null = bỏ lọc); đọc `description` NULL không lỗi; mọi SQL dùng `?`
  - Verify: chạy tay / smoke test
  - Files: `menu/Category.java`, `ProductDao.java`, `JdbcProductDao.java`
- Task: `ProductService`, `CategoryService` + unit test cho toàn bộ bảng quy tắc

  - Acceptance: mỗi quy tắc có cặp test đạt/không đạt; bắt buộc có: combo lồng combo bị từ chối; ngừng bán thành phần của combo đang bán bị từ chối; xóa món đã có trong đơn/công thức/combo chuyển thành ngừng bán; xóa danh mục còn món bị từ chối; giá có phần thập phân
  - Verify: `mvn test`
  - Files: `menu/ProductServiceImpl.java`, `CategoryServiceImpl.java`, `ProductServiceTest.java`
- Task: `ProductForm`, `CategoryForm`, `MenuController`, `CategoryController` + template

  - Acceptance: trên trình duyệt thêm, sửa, ngừng bán, tìm kiếm một món và tạo một combo; form sai (thiếu tên, giá âm) hiện lỗi cạnh ô nhập; lỗi nghiệp vụ hiện thông báo tiếng Việt; sau POST thành công redirect (F5 không gửi lại form); Waiter thấy bảng nhưng không có nút sửa, và gửi POST thủ công bị `403`
  - Verify: `mvn test` (`MenuControllerTest`); chạy tay với tài khoản `admin` và `waiter`
  - Files: `menu/ProductForm.java`, `CategoryForm.java`, `MenuController.java`, `CategoryController.java`, `templates/menu/*.html`, `MenuControllerTest.java`

## Dependencies

- **Cần:** `schema.sql`, interface Service, `BusinessException`, `GlobalExceptionHandler`, `SecurityConfig`, `layout/base.html` (Foundation). Không phụ thuộc module nào khác — an toàn để bắt đầu đầu tiên ở Phase 1.
- **Được dùng bởi:** M2 (chọn món khi tạo đơn), M3 (công thức + `getComponents` khi trừ kho), M5 (báo cáo món bán chạy).

## Checkpoint B contribution

Demo trang Thực đơn trên MySQL thật gồm một combo; `ProductServiceTest`, `ProductFactoryTest`, `MenuControllerTest` đạt.
