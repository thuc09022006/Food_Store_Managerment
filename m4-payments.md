# M4: Payments, Vouchers & Customers

Folder: `src/main/java/com/foodstore/payments/` (+ `payments/discount/`) và `src/main/java/com/foodstore/customers/` · Owner: **Owner 4** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File | Mục đích |
|------|----------|
| `payments/Payment.java` | Model — `id`, `orderId`, `voucherId` (nullable), `subtotal`, `discountAmount`, `vatAmount`, `total`, `method`, `paidAt` |
| `payments/PaymentMethod.java` | Enum `CASH`, `CARD`, `TRANSFER`, `EWALLET` |
| `payments/PaymentDao.java` / `payments/JdbcPaymentDao.java` | `save(Payment)`, `findByOrderId(long)`, `findByDateRange(LocalDate, LocalDate)` |
| `payments/discount/DiscountStrategy.java` | Interface — `BigDecimal computeDiscount(BigDecimal subtotal, DiscountContext ctx)` |
| `payments/discount/DiscountContext.java` | Dữ liệu đầu vào cho strategy — `Customer` (nullable), `Voucher` (nullable) |
| `payments/discount/NoDiscount.java` | Trả 0 |
| `payments/discount/TierDiscount.java` | Giảm theo hạng khách (`NORMAL` 0%, `SILVER`/`GOLD` theo `store.loyalty.silver-discount-percent` / `gold-discount-percent`) |
| `payments/discount/VoucherDiscount.java` | Giảm theo voucher (`PERCENT` hoặc `FIXED`), không vượt quá `subtotal` |
| `payments/TaxPolicy.java` | Tính VAT từ `store.vat-rate` (qua `StoreProperties`), làm tròn `HALF_UP` về đồng |
| `payments/PaymentService.java` | `preview(orderId, voucherCode)` — tính subtotal/giảm giá/VAT/total mà chưa ghi; chọn strategy |
| `payments/CheckoutService.java` | `pay(orderId, method, voucherCode)` — `@Transactional`, luồng ở `SPEC.md` mục "Luồng tích hợp" |
| `payments/InvoiceFormatter.java` | Dựng hóa đơn dạng văn bản (dòng món, giảm giá, VAT, tổng) để hiển thị/in |
| `payments/PaymentPanel.java` | Swing `JPanel` — chọn đơn `SERVED`, nhập mã voucher, xem preview, chọn phương thức, thanh toán, xem hóa đơn |
| `customers/Customer.java` / `customers/CustomerTier.java` | Model và enum hạng (`NORMAL`, `SILVER`, `GOLD`; `CustomerTier.forPoints(...)` đọc ngưỡng từ `StoreProperties`) — Foundation đã tạo bản đầu |
| `customers/Voucher.java` | Model — `code`, `discountType`, `discountValue`, `minOrder`, `validFrom`, `validTo`, `usageLimit`, `usedCount` (cột DB `discount_value`) |
| `customers/CustomerDao.java` / `JdbcCustomerDao.java` | CRUD, `findByPhone(String)`, `updatePointsAndTier` |
| `customers/VoucherDao.java` / `JdbcVoucherDao.java` | CRUD, `findByCode(String)`, `incrementUsed(long)` |
| `customers/CustomerService.java` | **Interface đã chốt ở Foundation** (`findById`, `addPoints`); M4 được **thêm** phương thức |
| `customers/CustomerServiceImpl.java` | `@Service` cài đặt: tìm khách, `addPoints` (điểm = tổng tiền / `store.loyalty.amount-per-point`), tính hạng theo `store.loyalty.*` |
| `customers/VoucherService.java` | `validate(code, subtotal, today)` — kiểm hạn, lượt dùng, đơn tối thiểu |
| `customers/CustomerPanel.java` / `customers/VoucherPanel.java` | Swing — CRUD khách hàng, CRUD voucher |
| `src/test/java/com/foodstore/payments/DiscountStrategyTest.java` | Unit test từng strategy |
| `src/test/java/com/foodstore/payments/CheckoutServiceTest.java` | Unit test thứ tự gọi và lan truyền exception (mock) |
| `src/test/java/com/foodstore/customers/VoucherServiceTest.java` | Unit test kiểm tra voucher |

## Tasks

- Task: Strategy giảm giá — `DiscountStrategy` + `NoDiscount`, `TierDiscount`, `VoucherDiscount`; `TaxPolicy`
  - Acceptance: cùng một `subtotal` cho ra các mức giảm khác nhau theo từng strategy; voucher `FIXED` lớn hơn subtotal bị chặn ở mức subtotal; kết quả làm tròn về đồng
  - Verify: `mvn test`
  - Files: `payments/discount/*.java`, `payments/TaxPolicy.java`, `src/test/java/com/foodstore/payments/DiscountStrategyTest.java`
- Task: `Customer`, `Voucher`, `CustomerTier` + DAO (interface + Jdbc) + `CustomerService`, `VoucherService`
  - Acceptance: `VoucherService.validate` từ chối voucher hết hạn, hết lượt, hoặc đơn dưới mức tối thiểu; `addPoints` làm khách lên hạng đúng ngưỡng cấu hình
  - Verify: `mvn test`
  - Files: toàn bộ `customers/*.java` (gồm `CustomerServiceImpl.java`), `src/test/java/com/foodstore/customers/VoucherServiceTest.java`
- Task: `Payment` + `PaymentDao` + `PaymentService.preview` — quy tắc mặc định: có voucher hợp lệ thì dùng `VoucherDiscount`, ngược lại dùng `TierDiscount`, **không cộng dồn**; `total = subtotal − discount + VAT`
  - Acceptance: preview cho một đơn seed ra số khớp tính tay; `total` không bao giờ âm
  - Verify: `mvn test`
  - Files: `payments/Payment.java`, `payments/PaymentMethod.java`, `payments/PaymentDao.java`, `payments/JdbcPaymentDao.java`, `payments/PaymentService.java`
- Task: `CheckoutService.pay` — `@Transactional`: kiểm đơn `SERVED` → tính tiền → lưu `Payment`, `OrderService.markPaid` → `voucher.used_count + 1` → `InventoryService.deductForOrder` → `CustomerService.addPoints` → `TableService.release`
  - Acceptance: unit test (mock) xác nhận gọi đúng thứ tự, và khi một bước ném exception thì **các bước sau không được gọi** và exception lan ra; thanh toán đơn không ở trạng thái `SERVED` bị từ chối; thanh toán đơn đã `PAID` lần hai bị từ chối
  - Verify: `mvn test`; rollback thật kiểm thủ công ở Phase 3
  - Files: `payments/CheckoutService.java`, `src/test/java/com/foodstore/payments/CheckoutServiceTest.java`
- Task: `InvoiceFormatter` + `PaymentPanel`, `CustomerPanel`, `VoucherPanel`
  - Acceptance: thanh toán một đơn seed qua UI, hóa đơn hiện đủ dòng món/giảm giá/VAT/tổng; thêm khách và voucher qua UI
  - Verify: chạy tay trên MySQL thật
  - Files: `payments/InvoiceFormatter.java`, `payments/PaymentPanel.java`, `customers/CustomerPanel.java`, `customers/VoucherPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge (Foundation); `OrderService`, `TableService` (M2) và `InventoryService` (M3) — xây trên seed data và mock nếu hai module kia chưa xong.
- **Được dùng bởi:** M5 (báo cáo doanh thu đọc bảng `payments`, nối panel; Cashier dùng chủ yếu module này).

## Checkpoint B contribution

Demo preview + thanh toán một đơn seed (có và không có voucher) độc lập trên MySQL thật; `DiscountStrategyTest`, `VoucherServiceTest`, `CheckoutServiceTest` đạt.
