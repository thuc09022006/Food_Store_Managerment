# M3: Inventory & Suppliers

Folder: `src/main/java/com/foodstore/inventory/` · Template: `templates/inventory/` · Owner: **Owner 3** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                              | Mục đích                                                                                                                                                              |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `inventory/Ingredient.java`                                       | Model — `id`, `name`, `unit`, `stockQuantity`, `minQuantity` (≥ 0)                                                                                                    |
| `inventory/RecipeLine.java`                                       | Model — `productId`, `ingredientId`, `quantityPerServing` (> 0)                                                                                                       |
| `inventory/Supplier.java`                                         | Model nhà cung cấp                                                                                                                                                    |
| `inventory/StockImport.java` / `StockImportItem.java`             | Phiếu nhập và dòng nhập (`ingredientId`, `quantity`, `unitCost`, `expiryDate` nullable)                                                                               |
| `inventory/LowStockEvent.java`                                    | Sự kiện Observer — `ingredientId`, `name`, `stockQuantity`, `minQuantity`, `shortage` (0 nếu không thiếu)                                                             |
| `inventory/InventoryDao.java` / `JdbcInventoryDao.java`           | CRUD nguyên liệu/công thức/nhà cung cấp/phiếu nhập; `decrease(long ingredientId, BigDecimal qty)` dùng `GREATEST(stock_quantity − ?, 0)` (một câu UPDATE nguyên tử); `findExpiringLots(LocalDate until)` |
| `inventory/InventoryService.java` / `InventoryServiceImpl.java`   | Interface đã chốt ở Foundation (`deductForOrder`) — không đổi; M3 **thêm** phương thức bên dưới. `deductForOrder`, `importStock`: `@Transactional(rollbackFor = Exception.class)`; phát sự kiện qua `ApplicationEventPublisher` |
| `inventory/LowStockListener.java`                                 | `@Component` — `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`; **chỉ ghi log** (SPEC D7). Cảnh báo cho người dùng đến từ truy vấn khi mở trang |
| `inventory/IngredientForm.java`, `SupplierForm.java`, `ImportForm.java`, `RecipeForm.java` | Form có Bean Validation (`@NotBlank`, `@Positive`, `@PositiveOrZero`, ngày hạn dùng không ở quá khứ khi nhập lô mới) |
| `inventory/InventoryController.java`                              | `/inventory` — tab Nguyên liệu, Công thức, Nhà cung cấp, Nhập kho, Cảnh báo; POST theo Post-Redirect-Get                                                              |
| `templates/inventory/*.html`                                      | Các trang trên; nút sửa bọc trong `sec:authorize="hasAuthority('INVENTORY_EDIT')"`; trang Cảnh báo gọi `findLowStock()` và `findExpiringLots()`                       |
| `src/test/java/com/foodstore/inventory/InventoryServiceTest.java` | Unit test trừ kho, nhập kho, sự kiện — DAO, `ProductService`, `OrderService` mock                                                                                     |
| `src/test/java/com/foodstore/inventory/InventoryControllerTest.java` | MockMvc: Chef (`INVENTORY_VIEW`) xem được, POST nhận `403`; Admin nhập kho thành công; số lượng ≤ 0 bị trả lại kèm lỗi                                            |

### Phương thức M3 thêm vào `InventoryService`

```java
long importStock(long supplierId, long staffId, List<StockImportItem> items);   // staffId = người lập phiếu (stock_imports.staff_id NOT NULL, lấy từ người đang đăng nhập); tạo phiếu + dòng + tăng tồn, một transaction
List<Ingredient> findLowStock();                                   // stock ≤ min
List<StockImportItem> findExpiringLots();                          // hạn ≤ hôm nay + store.expiry-warning-days
// CRUD nguyên liệu, nhà cung cấp; setRecipe(long productId, List<RecipeLine> lines)
```

### Đường dẫn chính

| Method | URL                                                 | Quyền            | Việc                          |
|--------|-----------------------------------------------------|------------------|-------------------------------|
| GET    | `/inventory`, `/inventory/recipes`, `/inventory/suppliers`, `/inventory/alerts` | `INVENTORY_VIEW` | Các trang xem |
| POST   | `/inventory/ingredients`, `/recipes`, `/suppliers`, `/imports` | `INVENTORY_EDIT` | Thêm/sửa/nhập kho |

## Quy tắc nghiệp vụ

| Chủ đề            | Quy tắc                                                                                                                                                                                                                        |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tồn kho           | Không bao giờ âm (service chặn, DB có `CHECK`). Số lượng nhập > 0; giá nhập ≥ 0                                                                                                                                                |
| Công thức         | Chỉ cho món `FOOD`/`DRINK`; **combo không có công thức riêng**. `quantityPerServing > 0`; mỗi cặp `(product, ingredient)` một dòng                                                                                              |
| `deductForOrder`  | Mỗi dòng đơn (`OrderService.getItems`): món thường → `quantityPerServing × quantity`; **combo → mở rộng** bằng `ProductService.getComponents`, mỗi thành phần nhân `component.quantity × item.quantity` rồi áp công thức        |
| Thiếu nguyên liệu | Không chặn thanh toán (D5): trừ về tối đa 0 bằng `GREATEST`, tính phần thiếu, đưa vào `LowStockEvent.shortage`. Không ném lỗi chỉ vì thiếu hàng                                                                                  |
| Cảnh báo tồn thấp | Phát `LowStockEvent` khi sau trừ kho `stock ≤ min` (chỉ cho nguyên liệu vừa bị trừ). Listener chạy sau commit và chỉ ghi log (D7)                                                                                               |
| Đồng thời         | Hai thanh toán cùng lúc trừ cùng nguyên liệu: dùng câu UPDATE tương đối (`stock_quantity = GREATEST(stock_quantity − ?, 0)`), **không** đọc giá trị ra Java rồi ghi lại (sẽ mất cập nhật)                                        |
| Nhập kho          | Phiếu + dòng nhập (kèm `expiry_date` theo lô) + tăng `stock_quantity` trong **một transaction**                                                                                                                                |
| Cảnh báo hết hạn  | Đọc từ `stock_import_items.expiry_date`. **Hạn chế đã biết:** kho không trừ theo lô nên lô đã dùng hết vẫn có thể bị báo; trang ghi chú rõ. FIFO theo lô là stretch goal                                                        |
| Số liệu           | `BigDecimal` cho số lượng (có thể 0.25 kg), không dùng `double`                                                                                                                                                                |

## Tasks

- Task: Model + DAO nguyên liệu/công thức/nhà cung cấp/phiếu nhập (+ Jdbc)

  - Acceptance: CRUD chạy trên seed; `decrease` không bao giờ cho `stock_quantity` âm và là một câu UPDATE tương đối; truy vấn lô sắp hết hạn trả đúng theo ngày
  - Verify: chạy tay / smoke test
  - Files: `inventory/Ingredient.java`, `RecipeLine.java`, `Supplier.java`, `StockImport*.java`, `InventoryDao.java`, `JdbcInventoryDao.java`
- Task: `importStock` + CRUD + unit test

  - Acceptance: nhập kho tăng tồn đúng và tạo lô có hạn; số lượng ≤ 0 bị từ chối; công thức cho món `COMBO` bị từ chối
  - Verify: `mvn test`
  - Files: `inventory/InventoryServiceImpl.java`, `InventoryServiceTest.java`
- Task: `deductForOrder` + `LowStockEvent` + `LowStockListener` (Observer)

  - Acceptance: test đơn món thường, đơn có combo (nhân đúng thành phần), đơn thiếu nguyên liệu (tồn về 0, không ném lỗi, `shortage` đúng), đơn làm tồn ≤ min (phát đúng một sự kiện); listener dùng `AFTER_COMMIT` và không ném exception ra ngoài
  - Verify: `mvn test`; kiểm tay trên MySQL thật
  - Files: `inventory/InventoryServiceImpl.java`, `LowStockEvent.java`, `LowStockListener.java`
- Task: Form + `InventoryController` + template

  - Acceptance: trên trình duyệt thêm nguyên liệu, nhập kho, sửa công thức, xem cảnh báo tồn thấp/sắp hết hạn; form sai hiện lỗi cạnh ô; sau POST redirect; Chef chỉ xem, POST thủ công bị `403`
  - Verify: `mvn test` (`InventoryControllerTest`); chạy tay với `chef` và `admin`
  - Files: `inventory/*Form.java`, `InventoryController.java`, `templates/inventory/*`, `InventoryControllerTest.java`

## Dependencies

- **Cần:** Foundation; `ProductService.getComponents` (M1) và `OrderService.getItems` (M2) — dùng seed + mock nếu chưa xong.
- **Được dùng bởi:** M4 (`deductForOrder` trong `CheckoutService`), M5 (báo cáo nguyên liệu tồn thấp).

## Checkpoint B contribution

Demo nhập kho + trừ kho cho một đơn seed (có combo) trên MySQL thật, thấy cảnh báo tồn thấp trên trang; `InventoryServiceTest`, `InventoryControllerTest` đạt.
