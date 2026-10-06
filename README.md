# SuniCoffee

Ứng dụng bán cà phê và dụng cụ cà phê phượt cho đồ án tốt nghiệp. Bản 0.3 bổ sung vận chuyển thủ công, giao thất bại/giao lại/nhận hoàn, đối soát COD, nhật ký và bộ lọc đơn hàng. Phần AI chưa triển khai.

## Chạy ngay trên máy

Yêu cầu Java 21–25 và Maven 3.6.3 trở lên. Dự án dùng Spring Boot 3.5.16, Thymeleaf, Spring Security, Spring Data JPA và Flyway. Phiên bản được đối chiếu với [yêu cầu hệ thống Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

Mở Terminal tại thư mục có `pom.xml` rồi chạy:

```bash
mvn spring-boot:run
```

Truy cập [http://localhost:8080](http://localhost:8080). Nếu cổng đã được sử dụng:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085
```

Chế độ mặc định là `demo`, dùng H2 lưu trên đĩa tại `.runtime/`, tự tạo 4 danh mục, 12 sản phẩm và hai tài khoản thử nghiệm:

| Quyền | Email | Mật khẩu demo |
| --- | --- | --- |
| Quản trị | admin@sunicoffee.local | SuniDemo!2026 |
| Khách hàng | khach@sunicoffee.local | SuniDemo!2026 |

Mật khẩu trên chỉ dành cho chế độ demo cục bộ. Dữ liệu và đơn hàng giữ lại sau khi tắt và mở lại ứng dụng. Nhấn Ctrl+C trong Terminal đang chạy để dừng ứng dụng. Không chạy hai phiên cùng trỏ vào tệp H2 này.

## Chức năng đã có

- Đăng ký, đăng nhập, đăng xuất; mật khẩu băm BCrypt; khách đăng ký luôn nhận quyền CUSTOMER.
- Danh sách sản phẩm, tìm theo tên/SKU, lọc danh mục, phân trang và trang chi tiết.
- Quản trị thêm, sửa, ngừng bán sản phẩm; thêm, sửa và xóa danh mục trống.
- Giỏ hàng riêng cho từng tài khoản, thêm, đổi số lượng, xóa sản phẩm.
- Đặt hàng COD với tên, điện thoại, địa chỉ và ghi chú; phí vận chuyển bằng 0 trong phạm vi bản đầu.
- Lịch sử và chi tiết đơn hàng; khách hủy đơn chờ xác nhận.
- Quản trị xác nhận, giao, hoàn thành hoặc hủy đơn theo đúng luồng trạng thái.
- Kiểm tra giá/tồn kho tại máy chủ, giao dịch nguyên tử, khóa tồn kho khi mua, hoàn tồn một lần khi hủy.
- Lưu tên, mã và giá tại thời điểm mua; thay đổi sản phẩm không làm thay đổi lịch sử đơn hàng.
- Token đặt hàng gắn với phiên để tránh tạo đơn trùng khi gửi lại cùng yêu cầu.
- API đọc `GET /api/products` và `GET /api/products/{id}` trả dữ liệu sản phẩm công khai.

Spring Security giữ bảo vệ CSRF cho các thao tác thay đổi dữ liệu. Các biểu mẫu Thymeleaf dùng `th:action` để chèn token theo [tài liệu Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## Kịch bản thực hành đầu tiên

1. Mở cửa hàng, lọc danh mục Cà phê và xem một sản phẩm.
2. Đăng nhập bằng tài khoản khách, thêm hai sản phẩm vào giỏ.
3. Nhập thông tin nhận hàng giả lập và đặt đơn COD.
4. Xem đơn trong mục Đơn của tôi; ghi lại mã SUNI của đơn.
5. Đăng xuất, đăng nhập tài khoản quản trị, mở Quản trị → Đơn hàng.
6. Chuyển lần lượt từ Chờ xác nhận → Đã xác nhận → Đang giao → Hoàn thành.
7. Tạo một đơn khác và hủy khi đang chờ xác nhận để quan sát tồn kho được hoàn lại.

## Chạy với MySQL

Chế độ H2 giúp xem ngay ứng dụng. MySQL là cơ sở dữ liệu mục tiêu của đồ án. Hai chế độ dùng chung migration và mã nghiệp vụ; dữ liệu H2 không tự chuyển sang MySQL.

Tạo cơ sở dữ liệu mới, không dùng chung với dự án khác:

```sql
CREATE DATABASE sunicoffee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tạo hoặc chọn tài khoản MySQL riêng có quyền tạo bảng và đọc/ghi trong cơ sở dữ liệu `sunicoffee`. Sau đó đặt biến môi trường trong cùng Terminal (thay các giá trị minh họa):

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_USERNAME=sunicoffee
export DB_PASSWORD='mat-khau-mysql-cua-ban'
export ADMIN_EMAIL='admin@sunicoffee.local'
export ADMIN_PASSWORD='mat-khau-quan-tri-tu-chon'
export SEED_DEMO=true
mvn spring-boot:run
```

`ADMIN_PASSWORD` tối thiểu 8 ký tự, tối đa 72 byte UTF-8. Tài khoản quản trị chỉ được tạo nếu email chưa tồn tại; các biến này không đặt lại mật khẩu của tài khoản đã có. Chế độ MySQL không tạo hai tài khoản demo. `SEED_DEMO=true` chỉ nạp danh mục/sản phẩm khi cả hai bảng đều trống; mặc định biến này là `false`.

Nếu MySQL khác máy hoặc cổng, đặt `DB_URL`, ví dụ:

```bash
export DB_URL='jdbc:mysql://localhost:3306/sunicoffee?connectionTimeZone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8'
```

Flyway tự chạy V1/V2 trong `src/main/resources/db/migration/` và Java migration V3 trong `src/main/java/db/migration/`. Hibernate chỉ kiểm tra cấu trúc (`ddl-auto=validate`), không tự xóa hoặc sửa bảng. Khi phát triển tiếp, thêm migration mới (sau V3 hiện tại là V4) thay vì sửa migration đã chạy trên dữ liệu cần giữ.

## Kiểm thử và đóng gói

```bash
mvn test
mvn package
java -jar target/sunicoffee-0.3.0.jar
```

Kiểm thử mặc định dùng cơ sở dữ liệu H2 riêng trong bộ nhớ, không dùng dữ liệu demo. Xem kết quả ở `target/surefire-reports`.

Có thể chạy cùng bộ kiểm thử với một cơ sở dữ liệu MySQL dành riêng cho kiểm thử bằng cách đặt `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`. Không trỏ bộ kiểm thử vào dữ liệu muốn giữ: trước mỗi ca kiểm thử, các bảng nghiệp vụ trong cơ sở dữ liệu kiểm thử được làm trống.

## Cấu trúc mã nguồn

```text
src/main/java/vn/sunicoffee/
  config/       Cấu hình bảo mật và nạp dữ liệu mẫu
  domain/       Đối tượng tương ứng các bảng
  repository/   Đọc ghi cơ sở dữ liệu
  service/      Quy tắc nghiệp vụ và giao dịch
  web/          Controller trang web và API
  web/form/     Dữ liệu nhập và điều kiện kiểm tra
src/main/resources/
  db/migration/ SQL có phiên bản
  templates/    Giao diện Thymeleaf
  static/       CSS và hình minh họa SVG
src/test/       Kiểm thử tích hợp
data/           Dữ liệu sản phẩm giả lập
docs/           Phạm vi, thiết kế dữ liệu và tiến độ
```

## Phần tiếp theo

Chưa triển khai thanh toán trực tuyến, tích hợp vận chuyển, quên mật khẩu qua email, giá vốn/lợi nhuận hoặc AI. Mỗi quy cách bán hiện là một sản phẩm với SKU riêng. Sản phẩm chưa tải ảnh vẫn dùng SVG minh họa. Phiếu nhập/xuất hiện ghi nhận từng SKU; chưa có nhà cung cấp và phiếu mua hàng nhiều dòng.

Trước phần chatbot cần hoàn thành kho tri thức ít nhất 50 mục có nguồn và bộ 30 câu hỏi đánh giá riêng theo đề cương. Hiện chưa có dữ liệu tri thức được xác minh hoặc kết quả đánh giá AI.


## Sử dụng các chức năng mới của bản 0.2

| Chức năng | Đường dẫn | Cách dùng |
| --- | --- | --- |
| Ảnh sản phẩm | Quản trị → Sản phẩm → Sửa | Tải JPG/PNG, thay ảnh hoặc gỡ ảnh; sản phẩm mới cần lưu trước |
| Hồ sơ | `/account` | Cập nhật họ tên, điện thoại, địa chỉ mặc định; email đăng nhập giữ nguyên |
| Đổi mật khẩu | `/account/password` | Nhập mật khẩu hiện tại và xác nhận mật khẩu mới; đăng nhập lại sau khi đổi |
| Quản trị tài khoản | `/admin/users` | Tìm kiếm, tạo, sửa tên, đổi quyền, khóa/mở khóa; không xóa lịch sử mua |
| Nhập/xuất kho | `/admin/inventory` | Chọn SKU, loại phiếu, số lượng và lý do; xem số dư sau từng biến động |
| Doanh thu | `/admin/revenue` | Chọn khoảng ngày tối đa 366 ngày, xem doanh thu/ngày và 10 sản phẩm doanh thu cao nhất |

Ảnh lưu trong cơ sở dữ liệu, tối đa 5 MB mỗi ảnh JPG/PNG; mỗi chiều tối đa 4096 px, tổng tối đa 16 triệu điểm ảnh. Máy chủ kiểm tra nội dung thật và mã hóa lại ảnh, không dùng tên tệp của người tải lên làm đường dẫn lưu. Một sản phẩm có một ảnh đại diện. Hình mẫu không tự biến thành ảnh hàng thật.

Đổi mật khẩu, đổi quyền và khóa/mở khóa làm hết hiệu lực phiên đăng nhập cũ. Quản trị viên không thể tự khóa hoặc tự hạ quyền, và hệ thống giữ ít nhất một quản trị viên hoạt động. Hồ sơ mặc định được điền sẵn khi checkout; thông tin trên đơn cũ vẫn được giữ nguyên.

Khi sản phẩm đã tạo, thay đổi tồn qua phiếu kho thay vì sửa trực tiếp số tồn trong biểu mẫu sản phẩm. Đặt hàng ghi xuất kho, hủy đơn ghi hoàn kho cùng giao dịch với đơn hàng. Phiếu thủ công hỗ trợ nhập bổ sung hoặc xuất khác, mỗi phiếu một SKU và tối đa 1.000.000 đơn vị. Phiếu đã ghi không sửa/xóa; ghi một phiếu ngược chiều có lý do nếu cần điều chỉnh sai sót.

Doanh thu được ghi nhận khi đơn chuyển sang COMPLETED, theo thời điểm hoàn thành tại Việt Nam, dùng giá đã lưu trên đơn. Chưa tính lợi nhuận hoặc đối soát thanh toán COD tự động. Đơn hoàn thành từ bản 0.1 chưa có ngày hoàn thành sẽ được báo riêng và loại khỏi báo cáo theo ngày, không tự gán một ngày thiếu căn cứ.

Migration V2 giữ dữ liệu cũ và tạo số dư kho đầu kỳ từ tồn hiện tại khi nâng cấp. Đây không phải lịch sử nhập/xuất trước thời điểm nâng cấp. Chi tiết thiết kế và kiểm thử: `docs/04-chuc-nang-ban-0.2.md`.


## Bản 0.3 — vận chuyển và hoàn thiện nghiệp vụ

- Phí giao hàng cố định `SHIPPING_FEE` (mặc định 0 VND), chụp vào đơn và hiển thị trước khi đặt.
- Admin nhập hãng/mã vận đơn; giao thất bại → giao lại hoặc chuyển hoàn; nhận hoàn nhập số lượng còn bán được từng dòng.
- Xác nhận đã nhận đủ tiền COD riêng với giao thành công; nhật ký thời điểm/người thực hiện.
- Tìm mã SUNI-id, người nhận/email, lọc trạng thái/ngày và đơn chờ quá `PENDING_REMINDER_HOURS` (mặc định 48). Không tự hủy COD.
- Checkout hỗ trợ tối đa 20 token/tab trong phiên, xử lý lỗi tranh chấp thân thiện. Top sản phẩm gộp theo product_id dù đổi tên/SKU.
- Giới hạn 20 lần thử đăng nhập/phút/IP trên một tiến trình. Git cục bộ, workflow H2/MySQL và script backup/restore kiểm tra.

Hướng dẫn vận chuyển, sao lưu và demo: [docs/07-huong-dan-van-hanh-va-demo.md](docs/07-huong-dan-van-hanh-va-demo.md). Kiến trúc/use case: [docs/06-thiet-ke-va-nghiep-vu-v03.md](docs/06-thiet-ke-va-nghiep-vu-v03.md). Kết quả và giới hạn bàn giao: [docs/08-ban-giao-v03.md](docs/08-ban-giao-v03.md).

Khi nâng cấp, sao lưu dữ liệu trước và dừng phiên chạy cũ. Không mở JAR 0.2 trên schema đã nâng cấp V3; nếu cần quay lại dùng bản sao lưu trước nâng cấp. Thông tin ở mục bản 0.2 phía trên là lịch sử, bản 0.3 dùng doanh thu tiền hàng không gồm phí giao hàng.
