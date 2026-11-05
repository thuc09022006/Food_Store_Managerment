# M4: Payments, Vouchers & Customers

Folder: `src/main/java/com/foodstore/payments/` (+ `payments/discount/`) và `.../customers/` · Template: `templates/payments/`, `templates/customers/` · Owner: **Owner 4** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                              | Mục đích                                                                                                                                                                  |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `payments/Payment.java`                                           | Model — `id`, `orderId`, `voucherId` (nullable), `subtotal`, `discountAmount`, `vatAmount`, `total`, `method`, `paidAt`                                                    |
| `payments/PaymentMethod.java`                                     | Enum `CASH`, `CARD`, `TRANSFER`, `EWALLET`                                                                                                                                |
| `payments/PaymentDao.java` / `JdbcPaymentDao.java`                | `save(Payment)`, `findByOrderId(long)`, `findByDateRange(LocalDate from, LocalDate to)` (`paid_at >= from AND paid_at < to + 1 ngày`, không bọc cột trong hàm)             |
| `payments/discount/DiscountStrategy.java`                         | Interface — `BigDecimal computeDiscount(BigDecimal subtotal, DiscountContext ctx)`                                                                                        |
| `payments/discount/DiscountContext.java`                          | Đầu vào strategy — `Customer` (nullable), `Voucher` (nullable)                                                                                                            |
| `payments/discount/NoDiscount.java`                               | Trả 0                                                                                                                                                                     |
| `payments/discount/TierDiscount.java`                             | Giảm theo hạng (`NORMAL` 0%, `SILVER`/`GOLD` theo `store.loyalty.silver-discount-percent` / `gold-discount-percent`)                                                      |
| `payments/discount/VoucherDiscount.java`                          | Giảm theo voucher (`PERCENT` hoặc `FIXED`), không vượt `subtotal`                                                                                                         |
| `payments/TaxPolicy.java`                                         | `vat(BigDecimal taxable)` = `taxable × store.vat-rate` (qua `StoreProperties`), làm tròn `HALF_UP` về đồng                                                                |
| `payments/PaymentService.java`                                    | `preview(orderId, voucherCode)` — tính subtotal/giảm giá/VAT/total mà chưa ghi; chọn strategy theo D2                                                                     |
| `payments/CheckoutService.java`                                   | `pay(orderId, method, voucherCode)` — `@Transactional(rollbackFor = Exception.class)`; luồng ở `SPEC.md` mục "Luồng tích hợp"; trả về `Payment` đã lưu                    |
| `payments/PaymentForm.java`                                       | Form: `method` (`@NotNull`), `voucherCode` (tùy chọn, `@Size(max=50)`)                                                                                                    |
| `payments/InvoiceView.java`                                       | Đối tượng dữ liệu cho trang hóa đơn: dòng món, giảm giá, VAT, tổng (dựng từ `Payment` + `OrderService.getItems`)                                                          |
| `payments/PaymentController.java`                                 | `/payments` — danh sách đơn `SERVED`; GET `/payments/{orderId}` xem preview (kèm `voucherCode` trên query); POST `/payments/{orderId}/pay`; GET `/payments/{id}/invoice` |
| `templates/payments/list.html`, `checkout.html`, `invoice.html`   | Danh sách đơn chờ thanh toán, trang thanh toán (preview + form), trang hóa đơn có CSS `@media print`; form thanh toán vô hiệu nút Submit sau lần bấm đầu                   |
| `customers/Customer.java` / `CustomerTier.java`                   | Model và enum hạng (`NORMAL`, `SILVER`, `GOLD`; `forPoints(...)` đọc ngưỡng từ `StoreProperties`) — Foundation tạo bản đầu                                                |
| `customers/Voucher.java`                                          | Model — `code`, `discountType`, `discountValue`, `minOrder`, `validFrom`, `validTo`, `usageLimit`, `usedCount`                                                            |
| `customers/CustomerDao.java` / `JdbcCustomerDao.java`             | CRUD, `findByPhone(String)`, `updatePointsAndTier` (cộng điểm bằng UPDATE tương đối `points = points + ?`, không đọc rồi ghi lại)                                         |
| `customers/VoucherDao.java` / `JdbcVoucherDao.java`               | CRUD, `findByCode(String)`, `boolean incrementUsed(long id)` — `UPDATE ... SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit` (`usage_limit` NOT NULL; DB còn có `CHECK (used_count <= usage_limit)` làm chốt chặn cuối), `false` khi 0 dòng |
| `customers/CustomerService.java` / `CustomerServiceImpl.java`     | Interface đã chốt (`findById`, `findByPhone`, `addPoints`); M4 thêm CRUD. `addPoints`: điểm = `floor(total / store.loyalty.amount-per-point)`, cập nhật hạng                 |
| `customers/VoucherService.java`                                   | `validate(code, subtotal, today)` — kiểm tồn tại, hạn, lượt dùng, đơn tối thiểu; **ném `BusinessException` kèm lý do cụ thể** khi không hợp lệ                            |
| `customers/CustomerForm.java`, `VoucherForm.java`                 | Form có Bean Validation (`@NotBlank` tên; số điện thoại đúng định dạng; `discountValue` > 0; `validFrom` ≤ `validTo`; `PERCENT` trong khoảng (0, 100])                       |
| `customers/CustomerController.java` / `VoucherController.java`    | `/customers` và `/customers/vouchers` — CRUD, PRG                                                                                                                         |
| `templates/customers/*.html`                                      | Trang khách hàng, voucher                                                                                                                                                 |
| `src/test/java/com/foodstore/payments/DiscountStrategyTest.java`, `TaxPolicyTest.java`, `CheckoutServiceTest.java` | Unit test chiến lược giảm giá, VAT/làm tròn, thứ tự gọi và lan truyền exception (mock)                                                                  |
| `src/test/java/com/foodstore/customers/VoucherServiceTest.java`   | Unit test kiểm tra voucher                                                                                                                                                |
| `src/test/java/com/foodstore/payments/PaymentControllerTest.java` | MockMvc: Cashier thanh toán → redirect sang hóa đơn; mã voucher sai → quay lại trang thanh toán kèm lý do; Waiter POST thanh toán nhận `403`; POST thiếu CSRF bị từ chối |

## Quy tắc tiền (SPEC D1, D2)

```
subtotal  = Σ (unit_price × quantity)            -- từ order_items
discount  = min(strategy.computeDiscount(...), subtotal)
taxable   = subtotal − discount
vat       = round_HALF_UP(taxable × store.vat-rate)
total     = taxable + vat                         -- luôn ≥ 0
```

Chọn strategy: có `voucherCode` → `VoucherService.validate` (sai thì ném lỗi, **không** chuyển sang giảm theo hạng) → `VoucherDiscount`. Không có mã → đơn có khách thì `TierDiscount`, không thì `NoDiscount`. Không cộng dồn.

**Server tự tính lại mọi số tiền khi thanh toán.** Form chỉ gửi `method` và `voucherCode`; không tin bất kỳ số tiền nào từ trình duyệt (kể cả trường ẩn). DB có `CHECK (total = subtotal − discount_amount + vat_amount)` và `CHECK (discount_amount <= subtotal)`, nên `CheckoutService` tính sai công thức D1 thì lệnh INSERT bị DB từ chối.

## Đường dẫn chính

| Method | URL                        | Quyền           | Việc                                                         |
|--------|----------------------------|-----------------|--------------------------------------------------------------|
| GET    | `/payments`                | `PAYMENTS_VIEW` | Đơn `SERVED` chờ thanh toán                                  |
| GET    | `/payments/{orderId}`      | `PAYMENTS_VIEW` | Preview (tham số `voucherCode` tùy chọn)                     |
| POST   | `/payments/{orderId}/pay`  | `PAYMENTS_EDIT` | Thanh toán → redirect `/payments/{paymentId}/invoice`         |
| GET    | `/payments/{id}/invoice`   | `PAYMENTS_VIEW` | Hóa đơn in được                                              |
| GET/POST | `/customers/...`         | VIEW/EDIT       | Khách hàng, voucher                                          |

## Tasks

- Task: Strategy giảm giá — `DiscountStrategy` + `NoDiscount`, `TierDiscount`, `VoucherDiscount`; `TaxPolicy`

  - Acceptance: cùng `subtotal` cho mức giảm khác nhau theo strategy; voucher `FIXED` lớn hơn subtotal bị chặn ở subtotal; `PERCENT` làm tròn về đồng; VAT tính trên `taxable` (sau giảm), `HALF_UP`
  - Verify: `mvn test`
  - Files: `payments/discount/*.java`, `payments/TaxPolicy.java`, `DiscountStrategyTest.java`, `TaxPolicyTest.java`
- Task: `Customer`, `Voucher`, `CustomerTier` + DAO + `CustomerService`, `VoucherService`

  - Acceptance: `validate` ném lỗi riêng cho: không tồn tại, chưa tới/hết hạn, hết lượt, đơn dưới mức tối thiểu; `addPoints` lên hạng đúng ngưỡng cấu hình; `incrementUsed` trả `false` khi hết lượt; `addPoints` dùng UPDATE tương đối
  - Verify: `mvn test`
  - Files: `customers/*.java`, `VoucherServiceTest.java`
- Task: `Payment` + `PaymentDao` + `PaymentService.preview`

  - Acceptance: preview một đơn seed ra số khớp tính tay; `total` không bao giờ âm; mã voucher không hợp lệ làm preview báo lỗi thay vì lặng lẽ bỏ qua
  - Verify: `mvn test`
  - Files: `payments/Payment.java`, `PaymentMethod.java`, `PaymentDao.java`, `JdbcPaymentDao.java`, `PaymentService.java`
- Task: `CheckoutService.pay` — `@Transactional(rollbackFor = Exception.class)`: kiểm `SERVED` → tính tiền → lưu `Payment` → `OrderService.markPaid` → `used_count + 1` (nếu có voucher) → `InventoryService.deductForOrder` → `CustomerService.addPoints` (nếu có khách) → `TableService.release` (nếu ăn tại chỗ)

  - Acceptance: test mock xác nhận thứ tự (`InOrder`) và khi một bước ném exception thì **các bước sau không được gọi**, exception lan ra (test cả `RuntimeException` lẫn exception checked để chứng minh `rollbackFor`); đơn không `SERVED` bị từ chối; thanh toán đơn đã `PAID` lần hai bị từ chối; `incrementUsed` trả `false` thì ném lỗi
  - Verify: `mvn test`; rollback thật và thanh toán đồng thời kiểm tay ở Phase 3
  - Files: `payments/CheckoutService.java`, `CheckoutServiceTest.java`
- Task: `PaymentController` + form + template (checkout, hóa đơn), `CustomerController`, `VoucherController`

  - Acceptance: trên trình duyệt thanh toán một đơn seed, hóa đơn hiện đủ dòng món/giảm giá/VAT/tổng và in được; nhập mã sai thì hiện lý do và vẫn ở trang thanh toán; sau thanh toán redirect (F5 không thanh toán lại); bấm đúp nút không tạo hai thanh toán; Waiter không thanh toán được kể cả gửi POST thủ công
  - Verify: `mvn test` (`PaymentControllerTest`); chạy tay với `cashier`
  - Files: `payments/PaymentForm.java`, `InvoiceView.java`, `PaymentController.java`, `customers/*Form.java`, `CustomerController.java`, `VoucherController.java`, `templates/payments/*`, `templates/customers/*`, `PaymentControllerTest.java`

## Dependencies

- **Cần:** Foundation; `OrderService`, `TableService` (M2) và `InventoryService` (M3) — xây trên seed và mock nếu hai module kia chưa xong.
- **Được dùng bởi:** M2 (`CustomerService.findByPhone/findById` khi gắn khách), M5 (báo cáo doanh thu đọc bảng `payments`).

## Checkpoint B contribution

Demo preview + thanh toán một đơn seed (có/không voucher, có/không khách) trên MySQL thật qua trình duyệt; `DiscountStrategyTest`, `TaxPolicyTest`, `VoucherServiceTest`, `CheckoutServiceTest`, `PaymentControllerTest` đạt.
