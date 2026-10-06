# Kết quả thực hiện SuniCoffee bản 0.1

Ngày kiểm tra: 03/10/2026, giờ Việt Nam.

## Kết quả đã xác minh

- Mã nguồn biên dịch thành công với Java 25.0.1 và Maven 3.9.9; mức Java mục tiêu trong dự án là 21.
- 19 ca kiểm thử tích hợp đạt trên H2, không lỗi, không bỏ qua.
- Cùng 19 ca kiểm thử đạt trên MySQL Community Server 8.0.44, không lỗi, không bỏ qua.
- Migration Flyway V1 chạy thành công trên cơ sở dữ liệu mới ở cả hai hệ quản trị; Hibernate xác thực ánh xạ thành công.
- Đóng gói thành công `target/sunicoffee-0.1.0.jar`.
- Khởi động chế độ demo tại cổng 8085; nạp dữ liệu mẫu và tài khoản demo. Yêu cầu HTTP đến trang chủ trả trạng thái 200, nội dung HTML UTF-8 và ngôn ngữ vi-VN.

MySQL kiểm thử được khởi tạo trong thư mục tạm riêng, cổng 13316, cơ sở dữ liệu `sunicoffee_test`. Không sử dụng cơ sở dữ liệu có sẵn của người dùng. Bộ kiểm thử mặc định vẫn dùng H2; cấu hình MySQL kiểm thử chỉ được truyền cho lần chạy xác minh.

## Phạm vi 19 ca kiểm thử

| Ca | Nội dung |
| --- | --- |
| 1 | Trang công khai, chi tiết sản phẩm, tìm kiếm trống và API hoạt động |
| 2 | Đăng ký luôn tạo CUSTOMER, email chuẩn hóa, mật khẩu băm |
| 3 | Từ chối đăng ký sai dữ liệu hoặc trùng email |
| 4 | Đăng nhập thật thành công với mật khẩu đúng, thất bại với mật khẩu sai |
| 5 | Chặn khách vào quản trị và chặn POST thiếu CSRF |
| 6 | Render các trang quản trị, giỏ, checkout, lịch sử và chi tiết đơn |
| 7 | Quản trị tạo danh mục/sản phẩm và xử lý biểu mẫu không hợp lệ |
| 8 | Không xóa danh mục đang có sản phẩm |
| 9 | Sản phẩm ngừng bán bị ẩn khỏi cửa hàng/API và không thể thêm giỏ |
| 10 | Kiểm tra số lượng giỏ và phân tách giỏ theo tài khoản |
| 11 | Checkout dùng giá từ dữ liệu hiện tại, gửi lại không nhân đôi đơn; đơn cũ giữ tên/giá |
| 12 | Từ chối giỏ trống hoặc thiếu tồn; không tạo đơn khi thất bại |
| 13 | Từ chối token checkout không khớp phiên |
| 14 | Luồng HTTP checkout với phiên hợp lệ tạo đơn thành công |
| 15 | Khách không đọc hoặc hủy được đơn của người khác |
| 16 | Hủy đơn hoàn tồn đúng một lần |
| 17 | Chỉ chuyển trạng thái theo quy tắc nghiệp vụ |
| 18 | Biểu mẫu quản trị cũ không ghi đè tồn sau khi có đơn mới |
| 19 | Hai khách cùng mua món cuối chỉ một người đặt được; tồn không âm |

Mã kiểm thử: `src/test/java/vn/sunicoffee/ShopIntegrationTest.java`. Nhật ký các lần chạy nằm tại `target/test-run.log` và `target/mysql-test-run.log`; báo cáo Surefire phản ánh lần chạy gần nhất.

## Giới hạn xác minh

Render ở đây là kiểm thử xử lý Thymeleaf và phản hồi HTTP bằng MockMvc; chưa thay thế kiểm tra hình ảnh thực tế trên máy tính và điện thoại. Công cụ điều khiển trình duyệt đang thiếu quyền để hoàn tất kiểm tra trực quan. Chưa đo Lighthouse, chưa thử tải lớn và chưa chạy trên máy chủ triển khai.

## Phạm vi còn lại trước khi hoàn tất đồ án

Ưu tiên tiếp theo là quản lý hồ sơ/tài khoản, tải ảnh sản phẩm và dữ liệu sản phẩm thật. Sau đó bổ sung nghiệp vụ nhập kho, báo cáo doanh thu và các phần khảo sát, UML, kiểm thử giao diện theo đề cương.

AI chưa được triển khai. Cần chuẩn bị kho tri thức có nguồn và bộ đánh giá tách biệt trước khi phát triển chatbot. Kết quả kiểm thử phần mềm hiện tại không phải kết quả đánh giá AI hoặc tuyên bố đã hoàn thành toàn bộ đồ án.
