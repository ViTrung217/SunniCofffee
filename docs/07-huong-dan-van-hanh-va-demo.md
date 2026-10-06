# Hướng dẫn vận hành và trình diễn bản 0.3

## Chạy bản mới

Yêu cầu Java 21+ và Maven. Tại thư mục dự án:

```bash
mvn verify
mvn spring-boot:run
```

Hoặc `java -jar target/sunicoffee-0.3.0.jar` sau khi đóng gói. Chế độ demo giữ tài khoản đã mô tả ở README, dữ liệu giả lập. Chế độ MySQL cần cấu hình riêng trong README; không dùng mật khẩu demo khi triển khai. Sao lưu trước nâng cấp. Flyway chạy V1/V2/V3; không sửa migration đã áp dụng. V3 là Java migration trong `src/main/java/db/migration/` để xử lý cú pháp CHECK khác nhau giữa H2 và MySQL.

Biến môi trường tùy chọn: `SHIPPING_FEE=0`, `PENDING_REMINDER_HOURS=48`. Khởi động lại ứng dụng sau khi đổi cấu hình. Không thay giá trị phí trên đơn đã đặt.

## Khách hàng

1. Tìm sản phẩm, xem chi tiết và thêm giỏ.
2. Mở giỏ, sửa số lượng, vào thanh toán. Giỏ chưa giữ hàng.
3. Kiểm tra thông tin nhận hàng, phí giao hàng và tổng tiền; xác nhận đặt COD.
4. Nếu món vừa hết, sửa giỏ. Nếu báo dữ liệu bận, thử lại sau; không mở hàng loạt yêu cầu.
5. Vào Đơn của tôi → Chi tiết để xem mã vận đơn và lịch sử. Chỉ hủy khi chờ xác nhận.

## Quản trị vận chuyển

1. Quản trị → Đơn hàng: tìm SUNI-id, người nhận hoặc email. Có thể lọc trạng thái/ngày và đơn chờ quá 48 giờ.
2. Mở đơn PENDING → Xác nhận. Khi hủy, nhập lý do.
3. Với đơn CONFIRMED, nhập hãng và mã vận đơn → Bàn giao. Tồn không bị trừ thêm.
4. Giao được: chọn Giao thành công. Khi thực nhận đủ COD, bấm Đã nhận đủ tiền COD riêng.
5. Giao không được: chọn Giao thất bại và nhập lý do; tiếp tục chọn giao lại hoặc chuyển hoàn.
6. Khi đang hoàn, chỉ xác nhận sau khi nhận đủ kiện. Nhập số còn bán được của từng dòng (có thể 0), ghi kết quả kiểm tra và xác nhận. Không nhập lại hàng hỏng vào tồn bán.

## Kho và doanh thu

Nhập/xuất qua trang Kho; không sửa tồn trên biểu mẫu sản phẩm cũ. Phiếu sai phải được bù bằng phiếu ngược chiều có lý do. Nhật ký giữ người thực hiện, số dư sau và liên kết đơn.

Báo cáo doanh thu dùng ngày hoàn thành, loại phí giao hàng, không phải số tiền COD đã về. Top sản phẩm vẫn gộp khi đổi tên/SKU. Không diễn giải số liệu demo thành doanh thu cửa hàng thật.

## Kịch bản demo 10–15 phút

| Bước | Thao tác | Kết quả cần trình bày |
| --- | --- | --- |
| 1 | Đăng nhập khách, mua 2 sản phẩm | Phí/tổng rõ ràng, đơn PENDING, tồn giảm |
| 2 | Admin xác nhận, nhập mã vận đơn, giao thành công | Nhật ký đầy đủ, chưa tự nhận COD |
| 3 | Admin xác nhận tiền COD | Ngày nhận tiền tách ngày giao |
| 4 | Đặt đơn khác, báo giao thất bại rồi giao lại | Tồn không bị trừ lần nữa |
| 5 | Đơn khác: thất bại → hoàn về → nhận 1/2 món còn tốt | Chỉ cộng lại 1, món hỏng có ghi chú |
| 6 | Tạo sản phẩm tồn 1, hai khách cùng mua | Chỉ một đơn; khách kia giữ giỏ |
| 7 | Đổi tên sản phẩm đã bán, bán tiếp và xem báo cáo | Top vẫn gộp; đơn cũ giữ tên/giá cũ |
| 8 | Lọc đơn chờ và xem nhật ký kho | Chứng minh truy vết và xử lý đơn tồn lâu |

Các tài khoản, hãng và mã vận đơn trình diễn đều dùng thông tin giả lập. Không gửi đơn đến hãng vận chuyển thật.

## Sao lưu và kiểm tra khôi phục MySQL

Tạo tệp cấu hình client ở ngoài repository, phân quyền `chmod 600`, ví dụ:

```ini
[client]
host=127.0.0.1
port=3306
user=backup_user
password=YOUR_LOCAL_PASSWORD
```

Chạy:

```bash
scripts/backup-mysql.sh /absolute/private/client.cnf sunicoffee /absolute/backups/sunicoffee.sql
scripts/restore-mysql-check.sh /absolute/private/client.cnf sunicoffee_restore_check /absolute/backups/sunicoffee.sql
```

Script backup không ghi đè tệp có sẵn. Nếu backup lỗi, tệp dở có thể tồn tại: không sử dụng, tạo bản mới sau khi xử lý lỗi. Không thực hiện thay đổi schema trong lúc backup single-transaction. Cần quyền đọc bảng và quyền tương ứng nếu có routines/triggers.

Script restore chỉ chấp nhận tên kết thúc `_restore_check`, tạo database mới; nếu đã có database thì dừng. Không tự ghi đè database sản xuất. So sánh số bản ghi, tồn, tổng đơn và kiểm tra ảnh/đăng nhập trên bản phục hồi. Chỉ khôi phục dump đáng tin cậy do mình tạo. Bản backup chứa thông tin khách hàng: giữ ngoài Git và phân quyền hạn chế.

## Triển khai và quản lý phiên bản

Dự án có Git cục bộ và workflow `.github/workflows/verify.yml`. Workflow chạy H2 và MySQL trên push/PR sau khi đưa lên GitHub; chưa tự tạo remote hoặc đẩy mã. Không commit mật khẩu, database hoặc dump. Log kiểm chứng lưu ở `docs/verification`, không phụ thuộc `mvn clean`.

Trước triển khai công khai: chọn máy chủ/domain, reverse proxy HTTPS, cookie Secure, tài khoản database riêng, tắt seed-demo, cấu hình backup và giám sát. Giới hạn login trong bộ nhớ dành cho một tiến trình; nếu dùng nhiều instance hoặc proxy cần cấu hình rate limit tại proxy tin cậy. Không tự tin các header X-Forwarded-For từ Internet. Hiện chưa triển khai công khai vì chưa có môi trường đích.

## Danh sách kiểm tra giao diện thủ công

- Desktop và màn hình 390 px: danh sách đơn không làm tràn toàn trang; bảng được cuộn ngang.
- Dùng Tab/Shift+Tab để đi qua bộ lọc, biểu mẫu và nút gửi; focus nhìn thấy.
- Thử để trống hãng/mã khi giao, lý do khi thất bại và số lượng khi nhận hoàn; thông báo dễ hiểu.
- Khách không thấy nút quản trị; truy cập đơn khác trả 404.
- Thử hai tab thanh toán; cả hai token vẫn hợp lệ trong giới hạn 20 tab.

Kết quả tự động được ghi trong tài liệu bàn giao; checklist không đồng nghĩa mọi mục đã được kiểm tra trực quan trên thiết bị thật.
