# Kiểm tra mở rộng danh mục — 14/10/2026

- [tests.json](tests.json): `mvn -q -Ddebug=false verify` đạt **114 kiểm thử, 0 lỗi, 0 bỏ qua** trên H2. Ba ca mới kiểm tra nạp 36 SKU, nhật ký tồn đầu, nguồn/mô tả/ảnh, nạp lặp không trùng, bảo toàn dữ liệu đã chỉnh và hiển thị trang công khai. Không chạy lại MySQL trong đợt này.
- `python3 scripts/verify-chatbot-baseline.py`: 55 mục, 14 nguồn, 30 câu và 10 khóa SHA-256 nguyên vẹn. Không gọi Gemini hoặc chạy lại chấm chất lượng AI trong đợt này.
- [local-import.json](local-import.json): đã sao lưu file H2 khi không có tiến trình giữ khóa, kiểm tra hash bản sao, nạp danh mục vào database local đang dùng. **12 → 36 sản phẩm**, cập nhật mô tả 12 SKU cũ, thêm 24 SKU và 24 nhật ký tồn đầu.
- Đã đối chiếu toàn bộ ID/SKU/tên/giá/tồn/trạng thái bán/danh mục của 12 SKU cũ trước và sau: không đổi. Số tài khoản, đơn hàng, dòng hàng, lịch sử đơn và dòng giỏ giữ nguyên. Chưa coi kiểm tra số lượng bản ghi là so sánh toàn bộ từng trường riêng tư.
- HTTP trên phiên tạm cổng 8087: tìm French press có kết quả, trang thứ ba có sản phẩm mới, trang chi tiết có mô tả nhiều phần; bốn SVG trả HTTP 200. Đây là kiểm tra phản hồi HTML/tài nguyên, chưa phải nghiệm thu hình ảnh trên mọi trình duyệt/mobile.
- Đã dừng phiên tạm sau cập nhật. **Không để ứng dụng chạy nền giữ file H2.** Người dùng có thể chạy `mvn spring-boot:run` bình thường ở 8080, không cần cờ import nữa.

Bản sao database nằm trong `.backups/`, bị loại khỏi Git. Không ghi khóa Gemini, mật khẩu hoặc dữ liệu cá nhân vào bằng chứng này. Dataset có nguồn nội bộ được khai báo là dữ liệu biên soạn mô phỏng, không phải nguồn thông số nhà sản xuất.
