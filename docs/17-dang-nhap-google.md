# Đăng nhập Google trên localhost

## 1. Tạo thông tin ứng dụng ở Google

1. Mở https://console.cloud.google.com/ và tạo/chọn project **SuniCoffee**.
2. Vào **Google Auth Platform**. Nếu chưa thiết lập, chọn **Get started**. Điền tên ứng dụng SuniCoffee, email hỗ trợ và email liên hệ của bạn trong **Branding**.
3. Trong **Audience**, chọn **External** nếu dùng tài khoản Gmail cá nhân. Khi ở chế độ **Testing**, thêm email của bạn và người thử vào **Test users**.
4. Trong **Clients**, chọn **Create client**, loại **Web application**, tên `SuniCoffee Local`.
5. Trong **Authorized redirect URIs**, thêm chính xác:

   ```text
   http://localhost:8080/login/oauth2/code/google
   ```

   Nếu bạn cũng mở web bằng địa chỉ IP, thêm riêng:

   ```text
   http://127.0.0.1:8080/login/oauth2/code/google
   ```

6. Tạo client, sao chép **Client ID** và **Client Secret**. Không gửi Secret vào chat hay commit lên GitHub.

Luồng này chạy phía máy chủ, không cần cấu hình JavaScript origin hay bật Google Drive/Gmail API. Chỉ yêu cầu các scope `openid`, `email`, `profile` để đăng nhập. Tên mục trong Google Console có thể thay đổi theo giao diện tài khoản.

Nguồn: [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect), [OAuth cho ứng dụng web](https://developers.google.com/identity/protocols/oauth2/web-server).

## 2. Cấu hình SuniCoffee

Trong Terminal tại thư mục project:

```bash
python3 scripts/configure-google.py
```

Nhập Client ID, sau đó dán Client Secret vào lời nhắc ẩn. Terminal không hiện ký tự khi nhập Secret là bình thường. Script lưu riêng `.runtime/google.properties` với quyền đọc/ghi chỉ cho chủ file; thư mục đã nằm trong `.gitignore`. Cấu hình Google không thay đổi khóa Gemini/SePay.

Dừng phiên đang chạy bằng Ctrl+C rồi chạy lại:

```bash
mvn spring-boot:run
```

Mở **http://localhost:8080/login**, chọn **Tiếp tục với Google**. Nút chỉ xuất hiện sau khi bật cấu hình. Không cần gửi khóa cho người phát triển để hoàn thành bước này.

Profile `local` tự đọc file trên. Với profile khác, có thể đặt các biến môi trường `APP_GOOGLE_ENABLED=true`, `APP_GOOGLE_CLIENT_ID`, `APP_GOOGLE_CLIENT_SECRET` bằng cơ chế quản lý bí mật của môi trường. Không ghi giá trị Secret vào lệnh được chia sẻ hoặc lịch sử shell.

## 3. Cách tài khoản hoạt động

- Email mới: tạo tài khoản CUSTOMER, không có quyền admin; có thể bổ sung số điện thoại và địa chỉ ở hồ sơ trước khi đặt hàng. Mật khẩu nội bộ ngẫu nhiên, không được hiển thị; đăng nhập bằng Google.
- Email đã tồn tại: sau xác thực Google, nhập mật khẩu **SuniCoffee** để liên kết. Yêu cầu liên kết có hiệu lực 5 phút, gắn với phiên trình duyệt và có CSRF/rate limit. Sau đó chuyển qua Google lần nữa để hoàn tất đăng nhập. Không tự gộp tài khoản chỉ vì cùng email.
- Các lần sau nhận diện bằng Google `sub` ổn định; email Google đổi không tạo một tài khoản mới hay chuyển quyền sang email khác. Giữ email nội bộ và lịch sử mua hàng đã liên kết.
- Tài khoản bị khóa không đăng nhập được; authVersion tiếp tục vô hiệu hóa phiên cũ khi quản trị thay đổi tài khoản.
- Chưa có nút tự gỡ/đổi tài khoản Google. Một tài khoản SuniCoffee liên kết tối đa một tài khoản Google.
- Thao tác đăng xuất chỉ kết thúc phiên SuniCoffee, không đăng xuất toàn bộ Google trên trình duyệt.

## 4. Kiểm tra và xử lý lỗi

- `redirect_uri_mismatch`: kiểm tra đủ `http://`, hostname, cổng `8080` và đường dẫn `/login/oauth2/code/google`; không thêm dấu `/` cuối. Dùng cùng hostname khi bắt đầu và quay lại.
- `access_denied`: kiểm tra Audience/Test users hoặc việc người dùng từ chối đăng nhập.
- Không thấy nút Google: kiểm tra đã chạy script, dùng profile local và khởi động lại đúng tiến trình.
- Báo lỗi khi liên kết: dùng mật khẩu của tài khoản SuniCoffee, không phải mật khẩu Google. Quá 5 phút thì bắt đầu lại từ nút Google.
- Xác thực thật cần mạng ra Google và Client ID/Secret đúng. Kiểm thử tự động không chứng minh Google Console đã cấu hình đúng.

Kịch bản nghiệm thu: email mới → đăng nhập → yêu thích/đơn hàng; email đã có → nhập sai mật khẩu không liên kết → nhập đúng giữ nguyên tài khoản; logout; từ chối quyền; khóa tài khoản; thử lại với trình duyệt khác. Không chuyển tiền để kiểm thử đăng nhập.
