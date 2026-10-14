# Kiểm tra chat khách hàng – admin, 14/10/2026

- [tests.json](tests.json): `mvn -q -Ddebug=false verify`, **126 kiểm thử H2, 0 lỗi**. Có 11 ca tích hợp mới và một ca gửi đồng thời. Kiểm thử migration hiện có chạy từ schema V1 lên V4 và xác nhận giữ dữ liệu cũ.
- Các ca mới bao phủ trao đổi hai chiều, số chưa đọc/mốc đọc, GET không đánh dấu đọc, CSRF, yêu cầu đăng nhập, quyền admin, không đọc chéo tài khoản, giả mạo sender, tài khoản bị khóa, giới hạn nội dung/tốc độ, chống gửi trùng, hai yêu cầu cùng lúc, đóng/mở lại, phân trang lịch sử, tìm/lọc inbox và render HTML an toàn.
- [http-smoke.json](http-smoke.json): chạy JAR tại cổng 8087 với **H2 bộ nhớ, tài khoản mẫu, AI tắt**. Hai phiên cookie riêng đăng nhập khách/admin, gửi và nhận tin, retry cùng token, cập nhật đọc, đóng và khách mở lại đều đạt. Không gửi tin cho khách thật.
- `support.js`, `support-inbox.js`, `chat-widget.js` qua kiểm tra cú pháp JavaScript bằng `node:vm.Script`.
- `verify-chatbot-baseline.py` vẫn đạt 55 mục / 14 nguồn / 30 câu / 10 hash. Không thay đổi kho/bộ đánh giá v1.
- Kiểm tra giao diện trực quan qua Chrome bị công cụ ngắt do người dùng thay đổi tab trong khi thao tác. Chưa xác nhận hoàn chỉnh widget, cuộn lịch sử và giao diện di động bằng trình duyệt. Kiểm thử HTML/API không thay thế kiểm thử trực quan.
- Không chạy lại MySQL, kiểm thử tải dài hạn hoặc chấm Gemini trong đợt này. Phiên thử 8087 đã dừng sau kiểm tra; migration dữ liệu local sẽ tự chạy khi người dùng khởi động ứng dụng mới.

## Sửa sai khác thời điểm trên CI

Run [38527497938](https://github.com/ViTrung217/SunniCofffee/actions/runs/38527497938) thất bại 1/126 ca: thời điểm Java có nanosecond, H2 làm tròn xuống độ chính xác cột TIMESTAMP. Tin vừa gửi và tin đọc lại khác phần lẻ của giây; đây không phải lỗi phân quyền. API gửi tin nay trả bản ghi đã lưu để lần gửi đầu, retry và đọc lịch sử nhất quán. Không sửa migration hoặc bỏ kiểm tra quyền truy cập.

Kiểm thử hồi quy dùng thời điểm cố định có 9 chữ số thập phân, đã xác nhận thất bại trước sửa và đạt sau sửa. Kết quả chạy lại Java 21, UTC xem [ci-fix-tests.json](ci-fix-tests.json). Workflow tách H2/MySQL thành hai job độc lập và đặt tên artifact theo database; nâng các action sang runtime Node 24, cố định Ubuntu 24.04.
