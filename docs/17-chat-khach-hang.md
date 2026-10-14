# Chat trực tiếp khách hàng – quản trị

Cập nhật 14/10/2026. Kênh này lưu tin nhắn trong database của SuniCoffee và **không gọi Gemini**. Trợ lý AI vẫn hoạt động ở kênh riêng.

## Khách hàng sử dụng

1. Đăng nhập bằng tài khoản khách hàng.
2. Chọn **Chat với cửa hàng** trên thanh điều hướng, hoặc mở cửa sổ góc phải rồi chọn **Nhân viên cửa hàng**.
3. Nhập nội dung và bấm **Gửi tin nhắn**. Mỗi tin tối đa 1.500 ký tự, mỗi tài khoản tối đa 20 tin mới/phút.
4. Giữ cửa sổ mở để nhận phản hồi. Tin tự cập nhật khoảng 3 giây/lần khi kênh đang hiển thị; đây là cập nhật định kỳ, không phải WebSocket hoặc cam kết nhân viên đang trực tuyến.
5. Đăng nhập lại vẫn xem được lịch sử. Ban đầu tải 50 tin gần nhất; bấm **Xem tin nhắn cũ hơn** để đọc tiếp. Khách gửi tin mới sau khi hội thoại đóng sẽ mở lại chính hội thoại đó.

Khách chưa đăng nhập được dẫn đến trang đăng nhập. Mỗi tài khoản khách có một cuộc trò chuyện hỗ trợ; chỉ được xem cuộc trò chuyện của mình. Không gửi mật khẩu hoặc thông tin thẻ vào chat.

## Quản trị sử dụng

1. Đăng nhập tài khoản admin → **Quản trị → Chat khách hàng**, hoặc mở `/admin/support`.
2. Tìm theo tên/email, lọc **Tất cả / Chưa đọc / Đang mở / Đã đóng**. Danh sách phân trang 20 cuộc trò chuyện và tự cập nhật khoảng 5 giây/lần khi trang hiển thị.
3. Mở khách cần hỗ trợ, đọc lịch sử và gửi trả lời. Tin hiển thị tên người gửi và thời điểm theo múi giờ Việt Nam.
4. Chọn **Đóng hội thoại** sau khi xử lý; có thể **Mở lại hội thoại** để trả lời tiếp. Đóng không xóa lịch sử.

Đây là **hộp thư chung cho các admin**: một admin đọc thì số chưa đọc chung được cập nhật. Chưa có phân công nhân viên hoặc trạng thái đọc riêng cho mỗi admin. Admin trả lời các hội thoại do khách bắt đầu; chưa có chức năng chủ động tạo cuộc trò chuyện mới với khách chưa gửi tin.

## Lưu trữ, quyền truy cập và độ tin cậy

- Flyway `V4__customer_support.sql` thêm `support_conversations` và `support_messages`; không sửa hoặc xóa bảng nghiệp vụ cũ. Lần khởi động tiếp theo tự áp dụng migration.
- Tài khoản gửi và quyền admin lấy từ phiên đăng nhập, không nhận từ nội dung JSON. Đường dẫn khách không nhận ID hội thoại tùy chọn để đọc tài khoản khác. Tài khoản bị khóa/thu hồi phiên không tiếp tục truy cập được.
- API ghi yêu cầu CSRF; phản hồi chat/hộp thư dùng `Cache-Control: no-store`. Văn bản render bằng `th:text` hoặc `textContent`, không chạy HTML trong tin nhắn. Lỗi kiểm tra nội dung trả thông báo chung, tránh ghi nguyên tin bị từ chối vào log mặc định.
- Mã gửi tin riêng giúp bấm/gửi lại sau lỗi mạng không tạo tin trùng. Máy chủ khóa tài khoản trước khi tạo hội thoại/gửi tin; có kiểm thử hai yêu cầu đầu tiên cùng lúc dùng một mã gửi.
- Đánh dấu đọc qua POST với ID tin đã tải, không tự đánh dấu từ GET. Mốc đọc chỉ tăng và không vượt tin thuộc hội thoại, nên phản hồi đọc chậm không nuốt tin mới đến sau.
- Giới hạn truy vấn: 50 tin/lượt; polling dùng ID sau cùng đã nhận từ GET. Tin mới trong khi gửi tin khác vẫn được tải qua cursor cũ, không nhảy qua chỉ vì POST trả ID lớn hơn.
- Đổi tài khoản phải tải lại trang. Khi phiên không hợp lệ hoặc rời trang, giao diện xóa phần lịch sử đang hiển thị; khi quay lại bằng lịch sử trình duyệt sẽ tải lại từ máy chủ.

## Chạy trên máy local

```bash
mvn -B verify
mvn spring-boot:run
```

Nếu ứng dụng đang chạy, Ctrl+C phiên cũ trước khi chạy lại. Không mở hai phiên cùng tệp `.runtime/sunicoffee.mv.db`. Đợt kiểm tra tính năng dùng database bộ nhớ và tài khoản tổng hợp riêng; không thêm hội thoại thử vào dữ liệu cá nhân của người dùng và không để tiến trình thử giữ khóa H2.

## Kiểm tra và giới hạn

[Biên bản kiểm tra](verification/customer-support/README.md) ghi kết quả 126 kiểm thử H2, kiểm tra nâng cấp schema, hai yêu cầu đồng thời, và luồng HTTP thật hai phiên đăng nhập. Không chạy lại MySQL hoặc chấm chatbot AI trong đợt này. Kiểm tra giao diện bằng trình duyệt bị gián đoạn khi tab thay đổi; chưa tuyên bố đã nghiệm thu đầy đủ thao tác widget/mobile.

Chưa có tệp đính kèm, ảnh/âm thanh, trạng thái đang gõ, thông báo đẩy/email, thông báo khi đóng trình duyệt, phân công nhân viên hoặc sửa/xóa tin đã gửi. Tin nhắn lưu cùng database; sao lưu database bao gồm lịch sử chat. Phản hồi từ nhân viên tùy thời điểm xử lý, không tự tạo câu trả lời thay nhân viên.
