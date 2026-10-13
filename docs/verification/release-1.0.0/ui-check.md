# Quan sát giao diện

Ngày 13/10/2026, Safari qua công cụ giao diện, trang cửa hàng trên cổng kiểm tra riêng 8087: trang chủ hiển thị, nút “Chat với Suni” ở góc dưới bên phải. Thử kích hoạt qua cây accessibility gặp lỗi `elementHasNoFrame`; không ghi nhận là đã bấm mở thành công trong lượt này. Tab kiểm tra đã đóng.

Bản local cuối được kiểm tra HTTP: nút/widget trong HTML, trang chat, CSRF, đăng nhập và gửi Gemini thật đều thành công (local-smoke.json). Các bằng chứng mở/thu nhỏ trên Safari ở giai đoạn trước nằm trong ../chatbot-widget/ui-check.md; không coi đó là kiểm thử mọi thiết bị ở bản cuối. Chưa thực hiện đầy đủ kiểm tra mobile ở đợt này.
