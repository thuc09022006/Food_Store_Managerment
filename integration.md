# Phase 3: Integration & Demo Prep (cả nhóm)

Không có owner riêng — mọi người cùng làm khi M1-M5 xong (Checkpoint C đạt).

## Tasks

- Task: Kiểm tra fresh-machine — một người chưa đụng vào code clone repo, tạo MySQL mới, chỉ làm theo `README.md` và chạy được app
  - Acceptance: thành công mà không cần ai trong nhóm hỗ trợ
  - Verify: chạy tay
  - Files: `README.md` (viết/cập nhật các bước cài đặt nếu thiếu)
- Task: Chạy luồng end-to-end xuyên 4 module bằng đúng thao tác của từng vai trò: Waiter tạo đơn cho bàn → Chef nhận và đánh dấu xong → Waiter chuyển `SERVED` → Cashier thanh toán (có voucher) → xem hóa đơn
  - Acceptance: sau thanh toán, đơn `PAID`, bàn `FREE`, có đúng một dòng trong `payments`, tồn kho giảm đúng theo công thức, `vouchers.used_count` +1, điểm khách tăng
  - Verify: chạy tay + đối chiếu bằng truy vấn SQL trực tiếp
  - Files: không (nhiệm vụ quy trình)
- Task: **Kiểm rollback thủ công** — tạm làm `InventoryService.deductForOrder` ném exception (ví dụ bằng cờ cấu hình hoặc sửa tạm nhánh), rồi thanh toán một đơn `SERVED`
  - Acceptance: sau lỗi, đơn vẫn `SERVED`, **không** có dòng mới trong `payments`, `vouchers.used_count` và điểm khách không đổi, bàn vẫn `OCCUPIED`; gỡ đoạn sửa tạm sau khi kiểm
  - Verify: chạy tay + truy vấn SQL trước/sau (Mockito không kiểm được rollback nên bước này bắt buộc)
  - Files: không (nhiệm vụ quy trình)
- Task: Kiểm các ca biên của kho và giá — sửa giá món sau khi đã có đơn; thanh toán khi nguyên liệu không đủ; xóa món đã nằm trong đơn
  - Acceptance: đơn cũ giữ nguyên `unit_price`; thiếu kho theo đúng chính sách ở Open Question 3 (mặc định: vẫn thanh toán, kho về 0, có cảnh báo); xóa món đã có trong đơn chuyển thành ngừng bán
  - Verify: chạy tay
  - Files: không
- Task: Duyệt toàn bộ checklist Success Criteria trong `SPEC.md` từ trên xuống, đánh dấu từng mục hoặc ghi một task bổ sung
  - Acceptance: mọi ô được đánh dấu, hoặc khoảng hở được ghi rõ ràng
  - Verify: chạy tay
  - Files: `SPEC.md`
- Task: Tập kịch bản demo bao gồm cả 4 vai trò và cả 5 module
  - Acceptance: một lần chạy thử hoàn tất không crash, trong thời gian demo cho phép
  - Verify: tập dượt thủ công
  - Files: không (nhiệm vụ quy trình)

## Checkpoint D (cuối)

Mọi mục trong Success Criteria của `SPEC.md` được đánh dấu; clone mới + MySQL mới + `mvn spring-boot:run` chạy end-to-end trên máy chưa từng có dự án; rollback thanh toán đã được kiểm thủ công.

---

## Stretch Goals (chỉ làm nếu Phase 3 xong sớm)

- Giao hàng (`DELIVERY`): địa chỉ, phí ship, trạng thái giao
- Tách bill / thanh toán nhiều lần cho một đơn
- Trừ kho theo lô FIFO (dùng `stock_import_items`)
- Đặt bàn trước (`RESERVED` có giờ đặt)
- Xuất CSV báo cáo doanh thu
- Dashboard doanh thu cho Admin (tổng `payments` theo tháng)
- `FlatLaf` look-and-feel cho giao diện đẹp hơn
