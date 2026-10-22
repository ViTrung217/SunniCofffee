# Khôi phục mật khẩu qua email

## Luồng sử dụng

Ở `/login`, chọn **Quên mật khẩu?**, nhập email đã đăng ký, mở email và đặt mật khẩu mới. Liên kết dùng một lần, hết hạn sau 15 phút. Yêu cầu mới sau thời gian chờ sẽ thay thế liên kết cũ. Sau khi thành công, đăng nhập lại; các phiên cũ hết hiệu lực. Tài khoản bị khóa không được khôi phục bằng cách này. Tài khoản tạo bằng Google cũng có thể dùng email của mình để đặt mật khẩu nội bộ; liên kết Google vẫn được giữ.

Phản hồi không tiết lộ email có tồn tại hay không. Có giới hạn theo IP (chung với đăng nhập) và một phút giữa hai yêu cầu cho cùng tài khoản. CSDL chỉ lưu SHA-256 của mã ngẫu nhiên 32 byte. Không ghi mã hoặc liên kết vào log. Khi chưa cấu hình dịch vụ email, trang thông báo chưa khả dụng và không nhận yêu cầu gửi.

## Cấu hình local

Tạo `.runtime/mail.properties` (đã nằm trong thư mục bị Git bỏ qua), quyền file chỉ cho chủ sở hữu đọc/ghi (`chmod 600 .runtime/mail.properties`). Điền cấu hình do dịch vụ SMTP của bạn cung cấp:

```properties
app.password-reset.enabled=true
app.password-reset.base-url=http://localhost:8080
app.password-reset.from=EMAIL_GUI_CUA_BAN
spring.mail.host=SMTP_HOST_CUA_BAN
spring.mail.port=587
spring.mail.username=TAI_KHOAN_SMTP
spring.mail.password=MAT_KHAU_UNG_DUNG_HOAC_SMTP
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

Mẫu trên dành cho SMTP dùng STARTTLS cổng 587; nếu nhà cung cấp dùng TLS cổng 465, dùng đúng thông số của họ. Không gửi mật khẩu SMTP vào chat và không commit file này. Không dùng mật khẩu đăng nhập thông thường nếu nhà cung cấp yêu cầu mật khẩu ứng dụng. Địa chỉ `from` phải được dịch vụ cho phép gửi.

Dừng phiên ứng dụng cũ rồi chạy lại `mvn spring-boot:run`. Profile local tự đọc file này. Các profile khác có thể nhận cấu hình Spring qua biến môi trường; không mặc định đọc file local. Base URL do chủ ứng dụng cấu hình, không lấy từ Host header của yêu cầu. `localhost` chỉ mở được trên chính máy đang chạy ứng dụng; mở email trên máy đó. Khi triển khai ngoài local phải dùng URL HTTPS thực tế.

Timeout kết nối/đọc/ghi SMTP mặc định 5 giây. Gửi email thất bại sẽ rollback mã mới; giao diện vẫn trả phản hồi chung để không lộ tài khoản. Chưa có hàng đợi hoặc retry email tự động. SMTP chưa được cấu hình và gửi thật trong lượt triển khai này; kiểm thử dùng mail sender giả lập, không phát sinh email bên ngoài.

## Nghiệm thu

1. Dùng email của chính bạn đã có tài khoản; yêu cầu khôi phục và kiểm tra hộp thư/thư rác.
2. Đặt mật khẩu mới, xác nhận mật khẩu cũ không đăng nhập được; phiên cũ bị thu hồi khi gửi yêu cầu tiếp theo.
3. Mở lại liên kết đã dùng: không được đổi lần nữa. Mã hết hạn hoặc không hợp lệ cũng bị từ chối.
4. Thử email chưa đăng ký: phản hồi chung như tài khoản tồn tại, không gửi thư.
5. Không sửa database hoặc tắt CSRF để thử luồng này. Sao lưu dữ liệu trước khi nạp migration mới V11 theo hướng dẫn bàn giao.
