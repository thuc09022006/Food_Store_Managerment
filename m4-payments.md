# M4: Payments, Vouchers & Customers

Folder: `src/main/java/com/foodstore/payments/` (+ `payments/discount/`) và `src/main/java/com/foodstore/customers/` · Owner: **Owner 4** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                             | Mục đích                                                                                                                                                                  |
|------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `payments/Payment.java`                                          | Model — `id`, `orderId`, `voucherId` (nullable), `subtotal`, `discountAmount`, `vatAmount`, `total`, `method`, `paidAt`                                                    |
| `payments/PaymentMethod.java`                                    | Enum `CASH`, `CARD`, `TRANSFER`, `EWALLET`                                                                                                                                |
| `payments/PaymentDao.java` / `payments/JdbcPaymentDao.java`      | `save(Payment)`, `findByOrderId(long)`, `findByDateRange(LocalDate from, LocalDate to)` (truy vấn `paid_at >= from AND paid_at < to+1 ngày`, không bọc cột trong hàm)       |
| `payments/discount/DiscountStrategy.java`                        | Interface — `BigDecimal computeDiscount(BigDecimal subtotal, DiscountContext ctx)`                                                                                        |
| `payments/discount/DiscountContext.java`                         | Dữ liệu đầu vào cho strategy — `Customer` (nullable), `Voucher` (nullable)                                                                                                |
| `payments/discount/NoDiscount.java`                              | Trả 0                                                                                                                                                                     |
| `payments/discount/TierDiscount.java`                            | Giảm theo hạng khách (`NORMAL` 0%, `SILVER`/`GOLD` theo `store.loyalty.silver-discount-percent` / `gold-discount-percent`)                                                |
| `payments/discount/VoucherDiscount.java`                         | Giảm theo voucher (`PERCENT` hoặc `FIXED`), không vượt quá `subtotal`                                                                                                     |
| `payments/TaxPolicy.java`                                        | `vat(BigDecimal taxable)` = `taxable × store.vat-rate` (qua `StoreProperties`), làm tròn `HALF_UP` về đồng                                                                |
| `payments/PaymentService.java`                                   | `preview(orderId, voucherCode)` — tính subtotal/giảm giá/VAT/total mà chưa ghi; chọn strategy theo quy tắc D2                                                             |
| `payments/CheckoutService.java`                                  | `pay(orderId, method, voucherCode)` — `@Transactional(rollbackFor = Exception.class)`, luồng ở `SPEC.md` mục "Luồng tích hợp"                                             |
| `payments/InvoiceFormatter.java`                                 | Dựng hóa đơn dạng văn bản (dòng món, giảm giá, VAT, tổng) để hiển thị/in                                                                                                  |
| `payments/PaymentPanel.java`                                     | Swing `JPanel` (`@Component`) — chọn đơn `SERVED`, nhập mã voucher, xem preview, chọn phương thức, thanh toán (qua `SwingWorker`), xem hóa đơn                             |
| `customers/Customer.java` / `customers/CustomerTier.java`        | Model và enum hạng (`NORMAL`, `SILVER`, `GOLD`; `CustomerTier.forPoints(...)` đọc ngưỡng từ `StoreProperties`) — Foundation đã tạo bản đầu                                |
| `customers/Voucher.java`                                         | Model — `code`, `discountType`, `discountValue`, `minOrder`, `validFrom`, `validTo`, `usageLimit`, `usedCount` (cột DB `discount_value`)                                  |
| `customers/CustomerDao.java` / `JdbcCustomerDao.java`            | CRUD, `findByPhone(String)`, `updatePointsAndTier`                                                                                                                        |
| `customers/VoucherDao.java` / `JdbcVoucherDao.java`              | CRUD, `findByCode(String)`, `boolean incrementUsed(long id)` — `UPDATE ... SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit` (thêm `OR usage_limit IS NULL` nếu cột cho NULL), trả `false` khi 0 dòng |
| `customers/CustomerService.java`                                 | **Interface đã chốt ở Foundation** (`findById`, `addPoints`); M4 được **thêm** phương thức                                                                                |
| `customers/CustomerServiceImpl.java`                             | `@Service` cài đặt: tìm khách, `addPoints` (điểm = `floor(tổng tiền / store.loyalty.amount-per-point)`), tính hạng theo `store.loyalty.*`                                  |
| `customers/VoucherService.java`                                  | `validate(code, subtotal, today)` — kiểm tồn tại, hạn, lượt dùng, đơn tối thiểu; **ném `BusinessException` kèm lý do cụ thể** khi không hợp lệ                            |
| `customers/CustomerPanel.java` / `customers/VoucherPanel.java`   | Swing — CRUD khách hàng, CRUD voucher                                                                                                                                     |
| `src/test/java/com/foodstore/payments/DiscountStrategyTest.java` | Unit test từng strategy                                                                                                                                                   |
| `src/test/java/com/foodstore/payments/TaxPolicyTest.java`        | Unit test VAT và làm tròn                                                                                                                                                 |
| `src/test/java/com/foodstore/payments/CheckoutServiceTest.java`  | Unit test thứ tự gọi và lan truyền exception (mock)                                                                                                                       |
| `src/test/java/com/foodstore/customers/VoucherServiceTest.java`  | Unit test kiểm tra voucher                                                                                                                                                |

## Quy tắc tiền (SPEC D1, D2)

```
subtotal  = Σ (unit_price × quantity)            -- từ order_items
discount  = min(strategy.computeDiscount(...), subtotal)
taxable   = subtotal − discount
vat       = round_HALF_UP(taxable × store.vat-rate)
total     = taxable + vat                         -- luôn ≥ 0
```

Chọn strategy: có `voucherCode` → `VoucherService.validate` (sai là ném lỗi, **không** chuyển sang giảm theo hạng) → `VoucherDiscount`. Không có mã → đơn có khách thì `TierDiscount`, không thì `NoDiscount`. Không cộng dồn.

## Tasks

- Task: Strategy giảm giá — `DiscountStrategy` + `NoDiscount`, `TierDiscount`, `VoucherDiscount`; `TaxPolicy`

  - Acceptance: cùng một `subtotal` cho ra các mức giảm khác nhau theo từng strategy; voucher `FIXED` lớn hơn subtotal bị chặn ở mức subtotal; voucher `PERCENT` làm tròn về đồng; VAT tính trên `taxable` (sau giảm), làm tròn `HALF_UP`
  - Verify: `mvn test`
  - Files: `payments/discount/*.java`, `payments/TaxPolicy.java`, `src/test/java/com/foodstore/payments/DiscountStrategyTest.java`, `src/test/java/com/foodstore/payments/TaxPolicyTest.java`
- Task: `Customer`, `Voucher`, `CustomerTier` + DAO (interface + Jdbc) + `CustomerService`, `VoucherService`

  - Acceptance: `VoucherService.validate` ném lỗi riêng cho từng trường hợp: không tồn tại, chưa tới/hết hạn, hết lượt, đơn dưới mức tối thiểu; `addPoints` làm khách lên hạng đúng ngưỡng cấu hình; `incrementUsed` trả `false` khi voucher đã hết lượt
  - Verify: `mvn test`
  - Files: toàn bộ `customers/*.java` (gồm `CustomerServiceImpl.java`), `src/test/java/com/foodstore/customers/VoucherServiceTest.java`
- Task: `Payment` + `PaymentDao` + `PaymentService.preview` — theo "Quy tắc tiền" ở trên

  - Acceptance: preview cho một đơn seed ra số khớp tính tay; `total` không bao giờ âm; mã voucher không hợp lệ làm preview báo lỗi thay vì lặng lẽ bỏ qua
  - Verify: `mvn test`
  - Files: `payments/Payment.java`, `payments/PaymentMethod.java`, `payments/PaymentDao.java`, `payments/JdbcPaymentDao.java`, `payments/PaymentService.java`
- Task: `CheckoutService.pay` — `@Transactional(rollbackFor = Exception.class)`: kiểm đơn `SERVED` → tính tiền → lưu `Payment` → `OrderService.markPaid` → `voucher.used_count + 1` (nếu có) → `InventoryService.deductForOrder` → `CustomerService.addPoints` (nếu có khách) → `TableService.release` (nếu ăn tại chỗ)

  - Acceptance: unit test (mock) xác nhận gọi đúng thứ tự (`InOrder`), và khi một bước ném exception thì **các bước sau không được gọi** và exception lan ra (test cả `RuntimeException` lẫn một exception checked để chứng minh `rollbackFor`); thanh toán đơn không ở trạng thái `SERVED` bị từ chối; thanh toán đơn đã `PAID` lần hai bị từ chối; `incrementUsed` trả `false` thì ném lỗi
  - Verify: `mvn test`; rollback thật kiểm thủ công ở Phase 3
  - Files: `payments/CheckoutService.java`, `src/test/java/com/foodstore/payments/CheckoutServiceTest.java`
- Task: `InvoiceFormatter` + `PaymentPanel`, `CustomerPanel`, `VoucherPanel`

  - Acceptance: thanh toán một đơn seed qua UI (không treo giao diện), hóa đơn hiện đủ dòng món/giảm giá/VAT/tổng; thêm khách và voucher qua UI; nhập mã sai thì hiện lý do
  - Verify: chạy tay trên MySQL thật
  - Files: `payments/InvoiceFormatter.java`, `payments/PaymentPanel.java`, `customers/CustomerPanel.java`, `customers/VoucherPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service + `BusinessException` đã merge (Foundation); `OrderService`, `TableService` (M2) và `InventoryService` (M3) — xây trên seed data và mock nếu hai module kia chưa xong.
- **Được dùng bởi:** M2 (gắn khách vào đơn qua `CustomerService.findById`), M5 (báo cáo doanh thu đọc bảng `payments`, nối panel; Cashier dùng chủ yếu module này).

## Checkpoint B contribution

Demo preview + thanh toán một đơn seed (có và không có voucher, có và không có khách) độc lập trên MySQL thật; `DiscountStrategyTest`, `TaxPolicyTest`, `VoucherServiceTest`, `CheckoutServiceTest` đạt.
