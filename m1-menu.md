# M1: Menu 

**Folder:** `src/main/java/com/foodstore/menu/` · **Template:** `src/main/resources/templates/menu/` · **Owner:** Owner 1 (điền tên) · **Phase 1** (song song, sau khi Foundation merge)

**Stack:** Spring Web MVC, Thymeleaf, JdbcTemplate, MySQL, `mvn test`.


| # | Thay đổi | Lý do |
| --- | --- | --- |
| 1 | Xóa món là thành phần của combo **đang bán** → từ chối, nêu tên combo |
| 2 | `type` của sản phẩm bất biến sau khi tạo | Tránh combo lồng combo và dòng `combo_components` mồ côi |
| 3 | `create/update` combo đang bán cũng kiểm thành phần đang bán | Giữ cùng bất biến với `setActive(true)` |
| 4 | Bắt `DataIntegrityViolationException` / `DuplicateKeyException`, thêm UNIQUE index | Check-rồi-ghi không an toàn khi chạy đồng thời |
| 5 | Giá có phần thập phân → **từ chối** (không làm tròn) | Làm tròn âm thầm đổi số tiền |
| 6 | `type` lạ → `BusinessException`, `ProductForm.type` thêm `@NotBlank` | Tránh lỗi 500 khi gửi tay |
| 7 | Tách `ProductAdminService`, giữ nguyên `ProductService` của Foundation | Không làm gãy fake/stub của M2, M3, M5 |
| 8 | `MenuControllerTest` dùng `csrf()` | Nếu không, 403 có thể do thiếu token chứ không do thiếu quyền |
| 9 | Thêm `JdbcProductDaoTest` (integration) | DAO chứa nhiều SQL nhất|
| 10 | `LIKE ... ESCAPE '\\'`; `findComponentsByComboIds` để tránh N+1 | Chính xác và hiệu năng |

Các mục 2, 5, 7 là **quyết định thiết kế do bản sửa này chọn**;

## Files

| File | Mục đích |
| --- | --- |
| `menu/Product.java` | Abstract: id, categoryId, name, price, description, active; trừu tượng `getType()` (Foundation tạo bản đầu, M1 mở rộng) |
| `menu/Food.java`, `menu/Drink.java` | `extends Product` |
| `menu/Combo.java` | `extends Product`, chứa `List<ComboComponent>` (Composite); `getComponentsTotal()`, `getSavings()`; từ chối combo lồng combo |
| `menu/ComboComponent.java` | product + quantity (≥ 1) |
| `menu/Category.java` | Model: id, name |
| `menu/ProductFactory.java` | `create(...)` dựng đúng lớp con theo `product_type`; loại không biết thì ném `IllegalArgumentException` |
| `menu/CategoryDao.java`, `JdbcCategoryDao.java` | CRUD categories + `existsProductIn(long)`, `existsByName(String)` |
| `menu/ProductDao.java`, `JdbcProductDao.java` | Xem chữ ký bên dưới; `findById` với combo nạp thêm thành phần |
| `menu/ProductService.java`, `ProductServiceImpl.java` | **Giữ nguyên** interface Foundation: `getById`, `findActive`, `getComponents` |
| `menu/ProductAdminService.java`, `ProductAdminServiceImpl.java` | **Mới.** Tìm kiếm và thao tác ghi; ghi dùng `@Transactional(rollbackFor = Exception.class)` |
| `menu/CategoryService.java`, `CategoryServiceImpl.java` | Liệt kê, tạo, đổi tên, xóa danh mục (có kiểm tra) |
| `menu/ProductForm.java` | name (`@NotBlank`), categoryId (`@NotNull`), type (`@NotBlank`), price (`@NotNull @PositiveOrZero`), description, danh sách thành phần combo |
| `menu/CategoryForm.java` | name (`@NotBlank`) |
| `menu/MenuController.java` | `@Controller @RequestMapping("/menu")`: danh sách + tìm kiếm/lọc, form thêm/sửa, bật/tắt bán, xóa; POST theo Post-Redirect-Get |
| `menu/CategoryController.java` | `/menu/categories`: danh sách/thêm/đổi tên/xóa |
| `templates/menu/list.html`, `form.html`, `categories.html` | Dùng `layout/base.html`; nút Thêm/Sửa/Xóa bọc trong `sec:authorize="hasAuthority('MENU_EDIT')"` |
| `src/test/.../ProductAdminServiceTest.java` | Unit test, DAO mock |
| `src/test/.../ProductFactoryTest.java` | Unit test factory + đa hình |
| `src/test/.../JdbcProductDaoTest.java` | Integration trên MySQL thật (Testcontainers) hoặc H2 chế độ MySQL |
| `src/test/.../MenuControllerTest.java` | MockMvc, xem mục Kiểm thử controller |

## Chữ ký

```java
public interface ProductDao {
    List<Product> findAll();
    Optional<Product> findById(long id);
    List<Product> search(String name, Long categoryId, Boolean active); // null = không lọc
    long save(Product p);                       // trả id vừa sinh (KeyHolder)
    void update(Product p);                     // KHÔNG đổi active, KHÔNG đổi type
    void setActive(long id, boolean active);
    void delete(long id);                       // xóa cứng, chỉ gọi khi không còn tham chiếu
    List<ComboComponent> findComponents(long comboId);
    Map<Long, List<ComboComponent>> findComponentsByComboIds(Collection<Long> comboIds); // gom 1 truy vấn IN
    void replaceComponents(long comboId, List<ComboComponent> components); // xóa cũ rồi chèn mới
    boolean existsInOrders(long productId);     // order_items
    boolean existsAsComponent(long productId);  // combo_components.component_id (mọi combo)
    boolean existsInRecipes(long productId);    // recipes
    List<Product> findActiveCombosContaining(long productId);
}

// Giữ nguyên như Foundation đã chốt
public interface ProductService {
    Product getById(long id);
    List<Product> findActive();                 // combo trả về đã nạp thành phần
    List<ComboComponent> getComponents(long comboId);
}

// Mới trong M1
public interface ProductAdminService {
    List<Product> search(String name, Long categoryId, Boolean active);
    long create(Product product, List<ComboComponent> components);  // components chỉ dùng cho Combo
    void update(Product product, List<ComboComponent> components);
    void setActive(long id, boolean active);
    void delete(long id);   // xóa cứng nếu không bị tham chiếu; ngược lại ngừng bán
}

public interface CategoryService {
    List<Category> findAll();
    long create(String name);
    void rename(long id, String name);
    void delete(long id);
}
```

## Đường dẫn (URL)

| Method | URL | Quyền | Việc |
| --- | --- | --- | --- |
| GET | `/menu` | MENU\_VIEW | Danh sách, tham số `name`, `categoryId`, `active` |
| GET | `/menu/new`, `/menu/{id}/edit` | MENU\_VIEW | Hiện form (nút Lưu chỉ hiện khi có MENU\_EDIT) |
| POST | `/menu` | MENU\_EDIT | Tạo mới → redirect `/menu` |
| POST | `/menu/{id}` | MENU\_EDIT | Cập nhật → redirect `/menu` |
| POST | `/menu/{id}/active` | MENU\_EDIT | Bật/tắt bán |
| POST | `/menu/{id}/delete` | MENU\_EDIT | Xóa (hoặc ngừng bán nếu còn tham chiếu) |
| GET/POST | `/menu/categories/...` | VIEW/EDIT | Quản lý danh mục |

Quyền do `SecurityConfig` sinh từ `Feature.MENU` (GET → VIEW, method khác → EDIT). M1 không tự viết quy tắc quyền; chỉ kiểm bằng test 403.

## Quy tắc nghiệp vụ (service phải chặn, không dựa vào form)

| Chủ đề | Quy tắc |
| --- | --- |
| Tên món | Bắt buộc, không rỗng sau `trim()`. Spec không yêu cầu tên món duy nhất (nếu muốn, thêm quy tắc riêng) |
| Tên danh mục | Không trùng tên. Nguồn sự thật là DB: dùng UNIQUE index trên cột `name`, service kiểm `existsByName` để báo lỗi sớm, đồng thời bắt `DuplicateKeyException` → `BusinessException`. Collation mặc định của MySQL không phân biệt hoa/thường và thường cả dấu;  phải chốt "Cà phê" và "Ca phe" có trùng không và dùng collation tương ứng |
| Giá | `price >= 0` (so bằng `compareTo`). `price.scale() > 0` (sau `stripTrailingZeros` nếu muốn chấp nhận `10000.00`) → **từ chối**. Vượt `DECIMAL(12,0)`: `price.precision() - price.scale() > 12` → từ chối |
| Loại (`type`) | Bắt buộc, thuộc `FOOD/DRINK/COMBO`. Giá trị lạ → `BusinessException` (bắt `IllegalArgumentException` từ factory). **Bất biến sau khi tạo**: `update` với `type` khác loại hiện tại → từ chối |
| Trạng thái khi tạo | `active = TRUE` mặc định. Nếu là combo thì áp quy tắc Combo bên dưới |
| Combo | Ít nhất 1 thành phần; `quantity >= 1`; thành phần không được là combo; không trùng thành phần (khóa chính `(combo_id, component_id)`); thành phần phải tồn tại. **Combo đang bán (`create` hoặc `update`) thì mọi thành phần phải đang bán.** `getSavings()` âm → trang cảnh báo nhưng vẫn cho lưu |
| Ngừng bán | `setActive(false)` cho món đang là thành phần của combo **đang bán** → từ chối, nêu tên các combo đó. `setActive(true)` cho combo → mọi thành phần phải đang bán |
| Xóa món | Xử lý theo thứ tự: (1) là thành phần của combo đang bán → **từ chối**, nêu tên các combo (cùng quy tắc Ngừng bán); (2) còn tham chiếu khác (`order_items`, thành phần của combo không bán, `recipes`) → chuyển `active=false`, không xóa; (3) không tham chiếu → xóa cứng, với combo thì xóa dòng `combo_components` của nó trước, trong cùng transaction. Nếu xóa cứng gặp `DataIntegrityViolationException` (tham chiếu phát sinh đồng thời) → rơi về nhánh (2) |
| Xóa danh mục | Từ chối nếu còn món thuộc danh mục |
| Lưu combo | `save/update` + `replaceComponents` trong một transaction |
| `findActive()` | Chỉ món `active=TRUE`; combo trả về đã nạp thành phần, gom bằng `findComponentsByComboIds` (không N+1) |
| NULL | `description` cho phép NULL |
| Tìm kiếm | `LIKE ? ESCAPE '\\'`; giá trị `%tên%` tạo trong code sau khi escape `\`, `%`, `_` người dùng gõ; không nối chuỗi vào SQL |

## Kiểm thử controller

`MenuControllerTest` (MockMvc):

- `GET /menu` trả view `menu/list`.
- Waiter (MENU\_VIEW) xem được; `POST` kèm `.with(csrf())` nhận 403. **Bắt buộc có `csrf()`** để 403 chỉ do thiếu quyền.
- Admin `POST` kèm `.with(csrf())` thành công và được redirect.
- Form thiếu tên bị trả lại kèm lỗi.
- (Nên có) `POST` không kèm `csrf()` cũng bị từ chối, để tách riêng hai lý do 403.

## Tasks

### Task 1: Hierarchy Product + ProductFactory

- **Acceptance:** `ProductFactory.create("COMBO", ...)` trả `Combo`; `getType()` qua tham chiếu `Product` ra đúng loại; `Combo.getSavings() = componentsTotal − price`; combo lồng combo bị từ chối.
- **Verify:** `mvn test` (`ProductFactoryTest`).
- **Files:** `Product.java`, `Food.java`, `Drink.java`, `Combo.java`, `ComboComponent.java`, `ProductFactory.java`.

### Task 2: Category + ProductDao (interface + Jdbc)

- **Acceptance:** DAO chạy trên seed; `save` trả id vừa sinh; `search` lọc đúng (null = bỏ lọc, `%` và `_` được escape đúng); đọc `description` NULL không lỗi; mọi SQL dùng `?`; `findComponentsByComboIds` trả đúng theo combo.
- **Verify:** `mvn test` (`JdbcProductDaoTest`) và chạy tay trên seed.
- **Files:** `Category.java`, `ProductDao.java`, `JdbcProductDao.java`, `JdbcProductDaoTest.java`.

### Task 3: Service + unit test cho toàn bộ bảng quy tắc

- **Acceptance:** mỗi quy tắc có cặp test đạt/không đạt. Bắt buộc có:
  - combo lồng combo bị từ chối;
  - ngừng bán thành phần của combo đang bán bị từ chối;
  - xóa thành phần của combo đang bán bị từ chối (nêu tên combo);
  - xóa món đã có trong đơn/công thức/combo không bán chuyển thành ngừng bán;
  - xóa cứng gặp `DataIntegrityViolationException` rơi về ngừng bán;
  - đổi `type` khi sửa bị từ chối; `type` lạ bị từ chối bằng `BusinessException`;
  - tạo combo đang bán với thành phần đang ngừng bán bị từ chối;
  - xóa danh mục còn món bị từ chối; tên danh mục trùng bị từ chối;
  - giá có phần thập phân bị từ chối; giá vượt `DECIMAL(12,0)` bị từ chối.
- **Verify:** `mvn test`.
- **Files:** `ProductAdminServiceImpl.java`, `ProductServiceImpl.java`, `CategoryServiceImpl.java`, `ProductAdminServiceTest.java`.

### Task 4: Form, Controller, Template

- **Acceptance:** trên trình duyệt thêm, sửa, ngừng bán, tìm kiếm một món và tạo một combo; form sai (thiếu tên, giá âm, thiếu type) hiện lỗi cạnh ô nhập; lỗi nghiệp vụ hiện thông báo tiếng Việt; sau POST thành công redirect (F5 không gửi lại form); Waiter thấy bảng nhưng không có nút sửa, và POST thủ công (có `csrf()`) bị 403.
- **Verify:** `mvn test` (`MenuControllerTest`); chạy tay với tài khoản admin và waiter.
- **Files:** `ProductForm.java`, `CategoryForm.java`, `MenuController.java`, `CategoryController.java`, `templates/menu/*.html`, `MenuControllerTest.java`.

## Dependencies

**Cần:** `schema.sql` (thêm UNIQUE index cho `categories.name`), interface Service, `BusinessException`, `GlobalExceptionHandler`, `SecurityConfig`, `layout/base.html` (Foundation). Không phụ thuộc module nào khác.

**Được dùng bởi:** M2 (chọn món khi tạo đơn), M3 (công thức + `getComponents` khi trừ kho), M5 (báo cáo món bán chạy). Vì `ProductService` giữ nguyên, các module này không bị ảnh hưởng.

## Checkpoint B contribution

Demo trang Thực đơn trên MySQL thật gồm một combo; `ProductAdminServiceTest`, `ProductFactoryTest`, `JdbcProductDaoTest`, `MenuControllerTest` đạt.
