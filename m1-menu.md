# M1: Menu

Folder: `src/main/java/com/foodstore/menu/` · Owner: **Owner 1** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                       | Mục đích                                                                                                                                                                              |
|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `menu/Product.java`                                        | **Abstract** — `id`, `categoryId`, `name`, `price`, `description`, `active`; phương thức trừu tượng `getType()` (Foundation đã tạo bản đầu, M1 mở rộng)                               |
| `menu/Food.java`                                           | `extends Product` — món ăn                                                                                                                                                            |
| `menu/Drink.java`                                          | `extends Product` — đồ uống                                                                                                                                                           |
| `menu/Combo.java`                                          | `extends Product` — chứa `List<ComboComponent>` (Composite); có giá riêng, `getComponentsTotal()`, `getSavings()`; từ chối combo lồng combo                                           |
| `menu/ComboComponent.java`                                 | `product` + `quantity` (quantity ≥ 1)                                                                                                                                                 |
| `menu/Category.java`                                       | Model — `id`, `name`                                                                                                                                                                  |
| `menu/ProductFactory.java`                                 | `create(...)` — dựng đúng lớp con theo `product_type`; loại không biết thì ném `IllegalArgumentException`                                                                             |
| `menu/CategoryDao.java` / `menu/JdbcCategoryDao.java`      | CRUD `categories` (bản mẫu từ Foundation) + `existsProductIn(long categoryId)`, `existsByName(String)`                                                                                |
| `menu/ProductDao.java`                                     | Interface — xem chữ ký bên dưới                                                                                                                                                       |
| `menu/JdbcProductDao.java`                                 | `JdbcTemplate` — SQL viết tay cho từng phương thức; `findById` với combo thì nạp thêm thành phần bằng `findComponents`                                                                |
| `menu/ProductService.java`                                 | **Interface đã chốt ở Foundation** (`getById`, `findActive`, `getComponents`) — không sửa chữ ký; M1 **thêm** các phương thức ở bên dưới                                              |
| `menu/ProductServiceImpl.java`                             | `@Service` cài đặt; validate, bật/tắt bán, xóa; thao tác ghi dùng `@Transactional(rollbackFor = Exception.class)`                                                                     |
| `menu/CategoryService.java` / `menu/CategoryServiceImpl.java` | Quản lý danh mục: liệt kê, tạo, đổi tên, xóa (có kiểm tra)                                                                                                                         |
| `menu/MenuPanel.java`                                      | Swing `JPanel` (`@Component`) — bảng món, form thêm/sửa, ô tìm kiếm + lọc, quản lý danh mục, chỉnh thành phần combo; khóa nút sửa khi `AccessLevel.VIEW` (Waiter chỉ xem)               |
| `src/test/java/com/foodstore/menu/ProductServiceTest.java` | Unit test — DAO mock                                                                                                                                                                  |
| `src/test/java/com/foodstore/menu/ProductFactoryTest.java` | Unit test factory + đa hình                                                                                                                                                           |

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

## Quy tắc nghiệp vụ (service phải chặn, không dựa vào UI)

| Chủ đề          | Quy tắc                                                                                                                                                                                                  |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tên             | Bắt buộc, không rỗng sau `trim()`; danh mục không trùng tên (không phân biệt hoa thường)                                                                                                                 |
| Giá             | ≥ 0; **không có phần thập phân** (`price.scale() > 0` bị từ chối hoặc làm tròn `HALF_UP` — chọn một cách và test); không vượt giới hạn cột `DECIMAL(12,0)`                                              |
| Combo           | Phải có ít nhất 1 thành phần; mỗi thành phần `quantity ≥ 1`; **không được là combo**; không trùng thành phần (khóa chính `(combo_id, component_id)`); thành phần phải tồn tại. `getSavings()` âm thì UI cảnh báo nhưng vẫn cho lưu |
| Ngừng bán       | `setActive(false)` cho món đang là thành phần của một combo **đang bán** → từ chối, thông báo tên các combo đó. `setActive(true)` cho combo → mọi thành phần phải đang bán                                  |
| Xóa món         | Nếu món có trong `order_items`, là thành phần combo (`existsAsComponent`) hoặc có công thức (`existsInRecipes`) → **không xóa**, chuyển `active=false` (vẫn áp quy tắc "Ngừng bán" ở trên). Ngược lại xóa cứng; với combo thì xóa các dòng `combo_components` của chính nó trước, trong cùng transaction |
| Xóa danh mục    | Từ chối nếu còn món thuộc danh mục (`existsProductIn`)                                                                                                                                                   |
| Lưu combo       | `save`/`update` món + `replaceComponents` nằm trong **một transaction**                                                                                                                                  |
| `findActive()`  | Chỉ trả món `active=TRUE`; combo trả về đã nạp thành phần                                                                                                                                                |
| NULL            | `description` cho phép NULL; đọc/ghi không ném lỗi                                                                                                                                                       |

## Tasks

- Task: Hierarchy `Product` (abstract) → `Food`, `Drink`, `Combo` + `ProductFactory`

  - Acceptance: `ProductFactory.create("COMBO", ...)` trả `Combo`; gọi `getType()` qua tham chiếu kiểu `Product` ra đúng loại của từng lớp con (đa hình); `Combo.getSavings()` đúng (`componentsTotal − price`)
  - Verify: `mvn test` (`ProductFactoryTest`)
  - Files: `menu/Product.java`, `menu/Food.java`, `menu/Drink.java`, `menu/Combo.java`, `menu/ComboComponent.java`, `menu/ProductFactory.java`
- Task: `Category` + `ProductDao` (interface + `JdbcProductDao`): CRUD, tìm kiếm/lọc, bật/tắt bán, đọc/ghi thành phần combo, các hàm kiểm tham chiếu

  - Acceptance: DAO hoạt động trên dữ liệu seed; `save` trả về id vừa sinh; `search` lọc đúng theo tên/danh mục/trạng thái (tham số null = bỏ lọc); đọc `description` NULL không ném lỗi; mọi SQL dùng tham số `?` (tìm theo tên dùng `LIKE ?` với giá trị `%tên%`)
  - Verify: chạy tay / smoke test bằng `main` trên MySQL thật
  - Files: `menu/Category.java`, `menu/ProductDao.java`, `menu/JdbcProductDao.java`
- Task: `ProductService`, `CategoryService` + unit test (DAO mock) cho toàn bộ bảng quy tắc ở trên

  - Acceptance: mỗi quy tắc có cặp test đạt/không đạt, riêng các quy tắc sau bắt buộc có test: combo lồng combo bị từ chối, ngừng bán thành phần của combo đang bán bị từ chối, xóa món đã có trong đơn/công thức/combo chuyển thành ngừng bán, xóa danh mục còn món bị từ chối, giá có phần thập phân
  - Verify: `mvn test`
  - Files: `menu/ProductServiceImpl.java`, `menu/CategoryServiceImpl.java`, `src/test/java/com/foodstore/menu/ProductServiceTest.java`
- Task: `MenuPanel` (bảng món + form + tìm kiếm/lọc + danh mục + chỉnh combo)

  - Acceptance: thêm, sửa, ngừng bán, tìm kiếm một món và tạo một combo qua giao diện trên MySQL thật; lỗi nghiệp vụ hiện bằng hộp thoại tiếng Việt; Waiter (`VIEW`) thấy bảng nhưng các nút Thêm/Sửa/Xóa bị khóa
  - Verify: chạy tay
  - Files: `menu/MenuPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service + `BusinessException` đã merge (Foundation). Không phụ thuộc module nào khác — an toàn để bắt đầu đầu tiên ở Phase 1.
- **Được dùng bởi:** M2 (chọn món khi tạo đơn, kiểm món đang bán), M3 (công thức theo `Product`, mở rộng combo khi trừ kho qua `getComponents`), M5 (báo cáo món bán chạy), M5 Phase 2 (nối panel).

## Checkpoint B contribution

Demo CRUD `MenuPanel` độc lập trên MySQL thật, gồm một combo; `ProductServiceTest` và `ProductFactoryTest` đạt.
