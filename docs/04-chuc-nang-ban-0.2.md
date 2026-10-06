# Năm nhóm chức năng bổ sung trong SuniCoffee 0.2

Ngày thực hiện: 03/10/2026, giờ Việt Nam. Bản này tiếp tục nền tảng 0.1; chưa tích hợp AI.

## Ảnh sản phẩm

Quản trị vào Sản phẩm → Sửa → Ảnh sản phẩm để tải, thay hoặc gỡ ảnh. Mỗi sản phẩm có một ảnh đại diện. Sản phẩm mới được lưu trước khi tải ảnh. JPG/PNG tối đa 5 MB, mỗi chiều không quá 4096 px và tổng không quá 16 triệu điểm ảnh.

Máy chủ đọc nội dung ảnh, kiểm tra định dạng thật và mã hóa lại để bỏ dữ liệu đi kèm. Tệp SVG, GIF, nội dung giả ảnh và ảnh quá giới hạn bị từ chối. Không dùng tên tệp người tải lên làm đường dẫn. Nội dung được lưu ở bảng `product_images`, giúp ảnh và dữ liệu sản phẩm có thể sao lưu cùng cơ sở dữ liệu.

Ảnh của sản phẩm đang bán được truy cập công khai tại `/product-images/{id}`. Ảnh sản phẩm ngừng bán chỉ được xem bởi quản trị viên. Cập nhật cờ ảnh và dữ liệu ảnh nằm cùng giao dịch; tải ảnh lỗi không làm mất ảnh cũ.

Tham khảo cơ chế nhận tệp: [Spring MultipartFile](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/multipart/MultipartFile.html).

## Hồ sơ và mật khẩu

Khách và quản trị viên đều có trang `/account` để sửa họ tên, điện thoại và địa chỉ mặc định. Email đăng nhập giữ nguyên ở bản này. Checkout tự điền hồ sơ mới nhất; thông tin người nhận trong các đơn đã đặt không thay đổi.

Trang `/account/password` yêu cầu mật khẩu hiện tại, mật khẩu mới và xác nhận. Mật khẩu mới cần 8–64 ký tự, tối đa 72 byte UTF-8, khác mật khẩu hiện tại. Mật khẩu lưu bằng BCrypt. Sau khi đổi thành công, phiên hiện tại đăng xuất và những phiên cũ khác bị từ chối ở yêu cầu tiếp theo.

## Quản trị tài khoản

Trang `/admin/users` hỗ trợ tìm theo tên/email, phân trang, tạo tài khoản, sửa tên, đổi vai trò và khóa/mở khóa. Không xóa tài khoản để giữ lịch sử đơn hàng và lịch sử kho. Đăng ký công khai vẫn chỉ tạo khách hàng.

`auth_version` tăng khi đổi mật khẩu, đổi quyền hoặc khóa/mở khóa. Thông tin này được so sánh với phiên đăng nhập trên mỗi yêu cầu. Phiên giữ quyền cũ sẽ hết hiệu lực. Bộ lọc nằm trong [chuỗi bảo mật Spring Security](https://docs.spring.io/spring-security/reference/servlet/architecture.html).

Quản trị viên không tự khóa hoặc tự hạ quyền. Thao tác quản trị khóa một dòng điều phối trong cơ sở dữ liệu để tuần tự hóa thay đổi quyền/trạng thái, đồng thời kiểm tra phải còn ít nhất một quản trị viên hoạt động. Trường `version` phát hiện biểu mẫu sửa đã cũ.

## Nhập xuất kho

Trang `/admin/inventory` cho phép ghi phiếu cho một SKU: nhập bổ sung hoặc xuất khác như hỏng, hao hụt. Bắt buộc số lượng dương và lý do; không xuất quá tồn. Phiếu hiển thị người thực hiện, thời điểm, mức tăng/giảm và số dư sau giao dịch. Lọc theo sản phẩm và phân trang lịch sử.

| Loại biến động | Khi được tạo |
| --- | --- |
| OPENING | Tồn đầu khi nâng cấp, nạp dữ liệu demo hoặc tạo sản phẩm có tồn |
| RECEIPT | Phiếu nhập thủ công |
| ISSUE | Phiếu xuất thủ công |
| SALE | Đặt hàng thành công, trừ tồn |
| CANCEL_RETURN | Hủy đơn hợp lệ, hoàn tồn |

Phiếu thủ công dùng token chống ghi trùng. Đặt hàng, hủy đơn và phiếu kho đều cập nhật tồn trong giao dịch có khóa sản phẩm. Nhật ký và số dư cùng thành công hoặc cùng hoàn tác. Phiếu đã ghi không sửa/xóa; sửa sai bằng phiếu ngược chiều có ghi lý do.

Biểu mẫu sửa sản phẩm không còn cho thay đổi trực tiếp tồn kho. API nghiệp vụ cũng kiểm tra điều này để tránh bỏ qua nhật ký kho. Bản này chưa quản lý nhà cung cấp, giá mua hoặc phiếu mua nhiều dòng.

## Báo cáo doanh thu

Trang `/admin/revenue` lọc khoảng ngày tối đa 366 ngày, tính bao gồm toàn bộ ngày kết thúc theo giờ Việt Nam. Báo cáo có tổng doanh thu, số đơn hoàn thành, số lượng bán, giá trị đơn trung bình, bảng/biểu đồ doanh thu theo ngày và 10 sản phẩm có doanh thu cao nhất.

Chỉ tính đơn `COMPLETED` theo `completed_at`, không dùng ngày đặt đơn. Đơn PENDING, CONFIRMED, SHIPPING và CANCELLED bị loại. Giá và tên sản phẩm lấy từ chi tiết đơn lúc mua, không lấy giá hiện tại. Doanh thu chưa trừ giá vốn/chi phí và chưa được đối soát với dịch vụ thu hộ COD.

Các đơn COMPLETED tồn tại từ bản 0.1 chưa có ngày hoàn thành. Migration giữ `completed_at` trống cho những đơn này. Giao diện thông báo số đơn thiếu ngày, không đưa vào tổng theo khoảng ngày và không tự gán thời điểm lịch sử.

## Thay đổi dữ liệu

Migration mới: `V2__accounts_images_inventory_reports.sql`. Không sửa V1 đã triển khai.

| Bảng | Thay đổi |
| --- | --- |
| app_users | Thêm phone, address, auth_version, version |
| account_admin_guard | Dòng khóa điều phối thay đổi quyền/trạng thái |
| products | Thêm has_image |
| product_images | Ảnh đại diện, MIME và nội dung ảnh |
| customer_orders | Thêm completed_at và chỉ mục báo cáo |
| stock_movements | Nhật ký tăng/giảm, người thực hiện, liên kết đơn, token và thời gian |

V2 tạo số dư đầu kỳ theo tồn hiện có tại lúc nâng cấp. Dòng này không thể hiện những giao dịch lịch sử chưa từng được ghi nhận. Ảnh và nhật ký nằm trong cơ sở dữ liệu, không có thư mục ảnh tải lên riêng.

## Kiểm thử

`ShopIntegrationTest` kiểm tra 19 tình huống nền tảng. `ManagementIntegrationTest` kiểm tra 21 tình huống mới về hồ sơ, mật khẩu, thu hồi phiên, quyền tài khoản, ảnh, kho và doanh thu. `MigrationUpgradeTest` kiểm tra nâng cấp V1 → V2 giữ dữ liệu và không nhân đôi tồn đầu. `UploadHttpIntegrationTest` kiểm tra đăng nhập và tải ảnh qua HTTP thật, gồm biểu mẫu multipart có CSRF và phản hồi 413 khi ảnh vượt dung lượng. Tổng cộng 42 ca.

Kết quả xác minh ngày 03/10/2026:

- Chạy cấu hình H2: 42 kiểm thử, không lỗi, không bỏ qua.
- Chạy cấu hình MySQL 8.0.44: 42 kiểm thử, không lỗi, không bỏ qua. 41 ca ứng dụng dùng MySQL; ca nâng cấp độc lập dùng một H2 riêng để kiểm soát dữ liệu V1.
- Đã kiểm tra HTTP thật: đăng nhập, biểu mẫu multipart có CSRF, nhận ảnh PNG và phản hồi 413 với thông báo khi ảnh trên 5 MB.
- Đã đóng gói `target/sunicoffee-0.2.0.jar`.

Nhật ký được bàn giao tại `target/verification-v02/h2-tests.log` và `target/verification-v02/mysql-tests.log`. Trước khi cập nhật thư mục dự án, mã và cơ sở dữ liệu demo được sao lưu trong `.backups/before-v02-<thời điểm>/`.

Kiểm thử trang bằng MockMvc xác nhận xử lý Thymeleaf và phản hồi HTTP, chưa thay thế kiểm tra hình ảnh trên trình duyệt. Chưa đo Lighthouse, thử tải lớn hoặc triển khai công khai.
