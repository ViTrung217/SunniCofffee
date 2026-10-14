# Kiểm chứng thanh toán QR SePay

Kiểm thử dùng DB riêng và phản hồi API giả lập; không chuyển tiền, không gọi SePay hoặc Gemini thật. Chưa cấu hình tài khoản ngân hàng/API token của người dùng; chưa có kết quả thử Sandbox hoặc Live trên dịch vụ SePay.

Phạm vi: COD giữ nguyên, QR đúng tổng tiền và bản chụp tài khoản, quyền chủ đơn/admin, CSRF, chống gửi lặp và đối soát đồng thời, thiếu/thừa tiền, tiền đến sau hủy, chặn giao hàng trước thanh toán, không đối soát QR như COD, lỗi API không lộ bí mật/không ghi tiền, phân biệt Sandbox/Live, bảo toàn COD cũ khi nâng cấp V5. Kiểm thử HTML không thay thế kiểm tra giao diện trên trình duyệt/điện thoại thật.

## Kết quả ngày 14/10/2026

- Java 21, JVM UTC: **146/146 ca đạt trên H2**, **146/146 ca đạt với cấu hình MySQL 8.0**. Các ca nâng cấp schema vẫn dùng H2 riêng; kiểm thử đơn vị không cần MySQL.
- Công cụ cấu hình MBBank: **3/3 ca đạt**, kiểm tra file quyền 600, không in token, chỉ gọi GET và không ghi đè file hiện có.
- Baseline chatbot giữ nguyên: 55 mục, 14 nguồn, 30 câu hỏi, 10 khóa SHA-256.
- Cú pháp `payment.js` và `git diff --check` đạt.
- Trong kiểm thử MySQL đã phát hiện lệch cách bind LocalDateTime giữa Hibernate và JdbcTemplate khi JVM UTC. Đã bật Hibernate direct JDBC cho java.time và chạy lại cả hai cấu hình thành công.
- Chưa chạy nghiệm thu trên SePay Sandbox/Live thật hoặc kiểm tra QR bằng ứng dụng ngân hàng. Công cụ cấu hình chưa được chạy bằng token của người dùng.

[Số liệu](results.json). Các kiểm thử chạy trên database riêng; không khởi động thêm ứng dụng trên database H2 của người dùng.

### Sửa công cụ cấu hình MBBank

Đã tái hiện bằng GET không có token: User-Agent mặc định Python-urllib trả HTTP 403; SuniCoffee-Setup/1.0 trả HTTP 401 (đến bước xác thực). Đã thêm User-Agent và thông báo an toàn cho lỗi HTTP, mạng, timeout, chứng chỉ. Bộ kiểm thử công cụ hiện đạt 4/4 ca (gồm các trường hợp con cho lỗi), không dùng token thật. Kết quả này chưa xác nhận token của người dùng hợp lệ.

### Luồng QR trực tiếp và trang thành công

Kiểm thử sau thay đổi: 34/34 ca đạt (SepayPaymentIntegrationTest 13, ShopIntegrationTest 21), Java 21 / H2. Xác nhận đặt đơn QR chuyển thẳng trang thanh toán; gửi lại không tạo đơn mới; trang thành công chỉ cho chủ đơn đã PAID, không mở khi thiếu tiền hoặc cần hoàn tiền. JavaScript được kiểm tra với bốn trạng thái: chỉ PAID chuyển trang, UNPAID/REVIEW/REFUND_REQUIRED không chuyển. Chưa xác minh luồng bằng giao dịch SePay thật.

Trước commit bàn giao: toàn bộ H2 verify đạt 148/148 ca, Java 21 / UTC; công cụ cấu hình 4/4 ca. MySQL đầy đủ cho commit mới được chạy trên GitHub Actions.
