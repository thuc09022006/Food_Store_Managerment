# M3: Inventory & Suppliers

Folder: `src/main/java/com/foodstore/inventory/` · Owner: **Owner 3** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                           | Mục đích                                                                                                                                              |
|----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `inventory/Ingredient.java`                                    | Model — `id`, `name`, `unit`, `stockQuantity`, `minQuantity` (đều ≥ 0)                                                                                |
| `inventory/RecipeLine.java`                                    | Model — `productId`, `ingredientId`, `quantityPerServing` (> 0)                                                                                       |
| `inventory/Supplier.java`                                      | Model nhà cung cấp                                                                                                                                    |
| `inventory/StockImport.java` / `inventory/StockImportItem.java` | Phiếu nhập và dòng nhập (`ingredientId`, `quantity`, `unitCost`, `expiryDate` nullable)                                                              |
| `inventory/LowStockEvent.java`                                 | Sự kiện Observer — `ingredientId`, `name`, `stockQuantity`, `minQuantity`, `shortage` (phần thiếu, 0 nếu không thiếu)                                  |
| `inventory/InventoryDao.java` / `JdbcInventoryDao.java`        | CRUD nguyên liệu, công thức, nhà cung cấp, phiếu nhập; `decrease(long ingredientId, BigDecimal qty)` dùng `GREATEST(stock_quantity − ?, 0)`; `findExpiringLots(LocalDate until)` |
| `inventory/InventoryService.java`                              | **Interface đã chốt ở Foundation** (`deductForOrder`); M3 **thêm** phương thức bên dưới                                                               |
| `inventory/InventoryServiceImpl.java`                          | `@Service`; `deductForOrder` và `importStock` là `@Transactional(rollbackFor = Exception.class)`; phát sự kiện qua `ApplicationEventPublisher`          |
| `inventory/LowStockListener.java`                              | `@Component` — `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`; cập nhật UI qua `SwingUtilities.invokeLater`             |
| `inventory/InventoryPanel.java`                                | Swing — tab Nguyên liệu, Công thức, Nhà cung cấp, Nhập kho, Cảnh báo; khóa nút sửa khi `AccessLevel.VIEW` (Chef chỉ xem)                              |
| `src/test/java/com/foodstore/inventory/InventoryServiceTest.java` | Unit test trừ kho, nhập kho, sự kiện — DAO, `ProductService`, `OrderService` mock                                                                 |

### Phương thức M3 thêm vào `InventoryService`

```java
long importStock(long supplierId, List<StockImportItem> items);   // tạo phiếu + dòng + tăng tồn, một transaction
List<Ingredient> findLowStock();                                   // stock ≤ min
List<StockImportItem> findExpiringLots();                          // hạn ≤ hôm nay + store.expiry-warning-days
// CRUD nguyên liệu, nhà cung cấp, công thức (setRecipe(productId, List<RecipeLine>))
```

## Quy tắc nghiệp vụ

| Chủ đề              | Quy tắc                                                                                                                                                                                                                          |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tồn kho             | Không bao giờ âm (service chặn, DB có `CHECK`). Số lượng nhập > 0; giá nhập ≥ 0                                                                                                                                                  |
| Công thức           | Chỉ cho món `FOOD`/`DRINK`; **combo không có công thức riêng**. `quantityPerServing > 0`. Mỗi cặp `(product, ingredient)` một dòng                                                                                               |
| `deductForOrder`    | Với mỗi dòng đơn (`OrderService.getItems`): món thường → nhân `quantityPerServing × quantity`; **combo → mở rộng** bằng `ProductService.getComponents`, mỗi thành phần nhân `component.quantity × item.quantity` rồi áp công thức |
| Thiếu nguyên liệu   | Không chặn thanh toán (SPEC D5): trừ xuống tối đa về 0 bằng `GREATEST`, tính phần thiếu, đưa vào `LowStockEvent.shortage`. Không ném lỗi chỉ vì thiếu hàng                                                                        |
| Cảnh báo tồn thấp   | Phát `LowStockEvent` khi sau trừ kho `stock ≤ min` (chỉ phát cho nguyên liệu vừa bị trừ, tránh lặp). Listener chạy sau commit (SPEC D7)                                                                                           |
| Nhập kho            | Tạo phiếu, dòng nhập (kèm `expiry_date` theo lô) và tăng `stock_quantity` trong **một transaction**                                                                                                                              |
| Cảnh báo hết hạn    | Đọc từ `stock_import_items.expiry_date`. **Hạn chế đã biết:** kho không trừ theo lô, nên lô đã dùng hết vẫn có thể bị báo; UI ghi chú rõ điều này. Trừ kho FIFO theo lô là stretch goal                                          |
| Mẫu số liệu         | Dùng `BigDecimal` cho số lượng nguyên liệu (có phần thập phân như 0.25 kg), không dùng `double`                                                                                                                                  |

## Tasks

- Task: Model + DAO nguyên liệu/công thức/nhà cung cấp/phiếu nhập (+ bản Jdbc)

  - Acceptance: CRUD chạy trên dữ liệu seed; `decrease` không bao giờ cho `stock_quantity` âm; truy vấn lô sắp hết hạn trả đúng theo ngày
  - Verify: chạy tay / smoke test bằng `main`
  - Files: `inventory/Ingredient.java`, `inventory/RecipeLine.java`, `inventory/Supplier.java`, `inventory/StockImport*.java`, `inventory/InventoryDao.java`, `inventory/JdbcInventoryDao.java`
- Task: `InventoryServiceImpl.importStock` + CRUD + unit test

  - Acceptance: nhập kho tăng tồn đúng và tạo lô có hạn; số lượng ≤ 0 bị từ chối; công thức cho món `COMBO` bị từ chối
  - Verify: `mvn test`
  - Files: `inventory/InventoryServiceImpl.java`, `src/test/java/com/foodstore/inventory/InventoryServiceTest.java`
- Task: `deductForOrder` + `LowStockEvent` + `LowStockListener` (Observer)

  - Acceptance: test với đơn có món thường, đơn có combo (thành phần được nhân đúng), đơn thiếu nguyên liệu (tồn về 0, không ném lỗi, `shortage` đúng), đơn làm tồn ≤ min (phát đúng một sự kiện); listener dùng `AFTER_COMMIT`
  - Verify: `mvn test`; kiểm tay trên MySQL thật
  - Files: `inventory/InventoryServiceImpl.java`, `inventory/LowStockEvent.java`, `inventory/LowStockListener.java`
- Task: `InventoryPanel`

  - Acceptance: thêm nguyên liệu, nhập kho, sửa công thức, xem cảnh báo tồn thấp/sắp hết hạn qua UI trên MySQL thật; Chef (`VIEW`) chỉ xem
  - Verify: chạy tay
  - Files: `inventory/InventoryPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge; `ProductService.getComponents` (M1) và `OrderService.getItems` (M2) — dùng seed data và mock nếu chưa xong.
- **Được dùng bởi:** M4 (`deductForOrder` trong `CheckoutService`), M5 (báo cáo nguyên liệu tồn thấp, nối panel).

## Checkpoint B contribution

Demo nhập kho + trừ kho cho một đơn seed (có combo) trên MySQL thật, thấy cảnh báo tồn thấp; `InventoryServiceTest` đạt.
