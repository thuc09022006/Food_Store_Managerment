# M3: Inventory & Suppliers

Folder: `src/main/java/com/foodstore/inventory/` · Owner: **Owner 3** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File | Mục đích |
|------|----------|
| `inventory/Ingredient.java` | Model — `id`, `name`, `unit`, `stockQuantity`, `minQuantity`; `isLow()` |
| `inventory/RecipeLine.java` | Model — `productId`, `ingredientId`, `quantity` |
| `inventory/Supplier.java` | Model — `id`, `name`, `phone`, `address` |
| `inventory/StockImport.java` / `inventory/StockImportItem.java` | Phiếu nhập và dòng nhập — `ingredientId`, `quantity`, `unitCost`, `expiryDate` (nullable) |
| `inventory/IngredientDao.java` / `JdbcIngredientDao.java` | CRUD, `findLow()`, `adjustStock(long id, BigDecimal delta)` |
| `inventory/RecipeDao.java` / `JdbcRecipeDao.java` | `findByProduct(long)`, `saveForProduct(long, List<RecipeLine>)` |
| `inventory/SupplierDao.java` / `JdbcSupplierDao.java` | CRUD `suppliers` |
| `inventory/StockImportDao.java` / `JdbcStockImportDao.java` | `save(StockImport)`, `findAll()`, `findExpiringWithin(int days)` |
| `inventory/InventoryService.java` | **Interface đã chốt ở Foundation** (`deductForOrder`); M3 được **thêm** phương thức (`importStock`, CRUD nguyên liệu/công thức, danh sách sắp hết/sắp hết hạn) |
| `inventory/InventoryServiceImpl.java` | `@Service` cài đặt `InventoryService` |
| `inventory/LowStockEvent.java` | Event (Observer) phát khi tồn ≤ `minQuantity` sau khi trừ kho |
| `inventory/LowStockListener.java` | `@EventListener` — gom cảnh báo để `InventoryPanel` hiển thị |
| `inventory/InventoryPanel.java` | Swing `JPanel` (nhiều tab) — nguyên liệu, công thức theo món, nhà cung cấp, nhập kho, cảnh báo |
| `src/test/java/com/foodstore/inventory/InventoryServiceTest.java` | Unit test — DAO, `OrderService`, `ProductService` mock |

## Tasks

- Task: Model + DAO cho nguyên liệu, công thức, nhà cung cấp, phiếu nhập (interface + Jdbc)
  - Acceptance: CRUD hoạt động trên dữ liệu seed; `findExpiringWithin(days)` trả đúng các lô sắp hết hạn
  - Verify: chạy tay / smoke test
  - Files: toàn bộ Model + `*Dao` + `Jdbc*Dao` trong bảng trên
- Task: `InventoryService.importStock` — trong một transaction: lưu phiếu + dòng nhập và cộng tồn từng nguyên liệu
  - Acceptance: nhập 5kg một nguyên liệu làm `stock_quantity` tăng đúng 5; lỗi ở dòng thứ hai thì không dòng nào được ghi
  - Verify: `mvn test` + chạy tay trên MySQL
  - Files: `inventory/InventoryServiceImpl.java`, `src/test/java/com/foodstore/inventory/InventoryServiceTest.java`
- Task: `InventoryService.deductForOrder` — lấy dòng đơn từ `OrderService.getItems`, mở rộng combo thành các thành phần (qua `ProductService.getComponents`), nhân `quantity` với công thức từng món, trừ tồn; phát `LowStockEvent` khi tồn ≤ mức tối thiểu
  - Acceptance: đơn có 2 phần món A (công thức 0.2kg thịt/phần) trừ đúng 0.4kg; đơn có combo trừ theo từng thành phần; món chưa có công thức thì bỏ qua (không lỗi); tồn không bao giờ âm
  - Verify: `mvn test`
  - Files: `inventory/InventoryServiceImpl.java`, `inventory/LowStockEvent.java`, `inventory/LowStockListener.java`
- Task: Xử lý thiếu nguyên liệu theo quyết định ở Open Question 3 của `SPEC.md` (mặc định: trừ xuống tối thiểu 0 và phát cảnh báo thiếu hàng, **không** chặn thanh toán)
  - Acceptance: test có tồn 0.1kg mà cần 0.4kg → tồn về 0, có cảnh báo, không ném exception
  - Verify: `mvn test`
  - Files: `inventory/InventoryServiceImpl.java`, `src/test/java/com/foodstore/inventory/InventoryServiceTest.java`
- Task: `InventoryPanel` (nguyên liệu, công thức, nhà cung cấp, nhập kho, cảnh báo sắp hết/sắp hết hạn)
  - Acceptance: nhập kho một phiếu qua UI, tồn đổi theo; nguyên liệu dưới mức tối thiểu và lô sắp hết hạn hiện trong tab cảnh báo
  - Verify: chạy tay
  - Files: `inventory/InventoryPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge (Foundation); `ProductService` (M1) cho công thức/combo và `OrderService.getItems` (M2) cho `deductForOrder` — xây trên seed data và mock nếu hai module kia chưa xong.
- **Được dùng bởi:** M4 (`CheckoutService` gọi `deductForOrder`), M5 (báo cáo nguyên liệu tồn thấp, nối panel; vai trò Chef xem tồn kho).

## Checkpoint B contribution

Demo nhập kho + trừ kho theo công thức cho một đơn mẫu + cảnh báo sắp hết độc lập trên MySQL thật; `InventoryServiceTest` đạt.
