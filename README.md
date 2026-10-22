# SuniCoffee 1.0.0

Ứng dụng bán cà phê và dụng cụ pha chế, bản chạy local để bàn giao và bảo vệ đồ án. Bao gồm cửa hàng, quản trị, kho, vận chuyển COD và chatbot Gemini có nguồn tham chiếu, cửa sổ chat ở góc dưới bên phải.

**Dữ liệu hiện tại phục vụ đồ án, chưa được cửa hàng xác nhận.** Phần mềm 1.0.0 không đồng nghĩa đã có hàng hóa, giá hay chính sách kinh doanh thực tế. [Bảng tình trạng hiện tại](docs/16-tinh-trang-du-an.md) phân biệt kết quả đã kiểm tra và việc cần bổ sung.

## Chạy bản bàn giao

Yêu cầu Java 21–25, Maven 3.6.3 trở lên. Dùng Spring Boot 3.5.16, Thymeleaf, Spring Security, JPA, Flyway. Nếu ứng dụng đang chạy ở 8080, dừng phiên cũ trước khi khởi động bản mới.

```bash
python3 scripts/verify-chatbot-baseline.py
mvn -B verify
bash scripts/run-local.sh
```

Truy cập [http://localhost:8080](http://localhost:8080). Khi phát triển có thể chạy `mvn spring-boot:run`. Profile mặc định nay là **local**, giữ dữ liệu H2 ở `.runtime/sunicoffee`, không tự nạp sản phẩm hoặc tạo tài khoản demo. Khóa Gemini hiện có ở `.runtime/chatbot.properties` tiếp tục dùng được trên máy này, không đóng gói hoặc đưa vào Git.

Máy mới cần tạo quản trị riêng bằng `ADMIN_EMAIL` và `ADMIN_PASSWORD` rồi thêm danh mục/sản phẩm. Xem cách nhập mật khẩu kín, cấu hình MySQL cục bộ và bàn giao trong [hướng dẫn phát hành](docs/13-phat-hanh-va-ban-giao.md). Profile `demo` chỉ dùng khi chủ động dựng dữ liệu minh họa; không phải chế độ mặc định.

## Chức năng

[Bảng đã làm / còn thiếu](docs/16-tinh-trang-du-an.md). Danh mục đồ án hiện có 36 sản phẩm với mô tả chi tiết; xem [nguồn dữ liệu và cách nạp bổ sung](data/README-products.md).

| Nhóm | Nội dung |
| --- | --- |
| Khách hàng | Đăng ký/đăng nhập Google hoặc mật khẩu, yêu thích, đánh giá, hồ sơ, đổi/khôi phục mật khẩu qua email (cần SMTP), tìm/lọc sản phẩm, giỏ riêng, đặt COD/QR SePay, xem và hủy đơn còn chờ xác nhận |
| Sản phẩm | Danh mục, thêm/sửa/ngừng bán, thư viện tối đa 6 ảnh JPG/PNG, mỗi ảnh tối đa 5 MB, kiểm tra nội dung ảnh |
| Tài khoản | Phân quyền, khóa/mở, vô hiệu hóa phiên khi đổi quyền/mật khẩu; giữ ít nhất một admin hoạt động |
| Kho | Nhật ký tồn đầu/nhập/xuất, trừ khi đặt, hoàn khi hủy/nhận hàng hoàn; giao dịch và khóa tồn |
| Đơn hàng | Trạng thái hợp lệ, hãng/mã vận đơn, giao thất bại/giao lại/hoàn, phí giao chụp vào đơn, chống đặt trùng |
| COD và báo cáo | Đối soát riêng với giao thành công, doanh thu tiền hàng theo ngày hoàn thành tại Việt Nam, bộ lọc đơn |
| Chat nhân viên | Khách đăng nhập trao đổi trực tiếp với admin; hộp thư, số chưa đọc, đóng/mở lại và lịch sử lưu DB |
| Chatbot | Gemini, 55 mục tri thức có nguồn, ngữ cảnh hội thoại, luồng tra Google cho kiến thức liên quan (phụ thuộc quota), giá/tồn hiện hành, chỉ tra cứu đơn thuộc tài khoản đã xác thực |
| Vận hành | MySQL hoặc H2 cục bộ, Flyway, health/readiness, graceful shutdown, backup/restore kiểm tra |

QR SePay đã có mã tích hợp, cần cấu hình tài khoản và kiểm chứng với dịch vụ thật; xem [hướng dẫn SePay](docs/18-thanh-toan-sepay.md). Khôi phục mật khẩu qua email đã có luồng xử lý; cần cấu hình SMTP theo [hướng dẫn](docs/20-khoi-phuc-mat-khau.md). API hãng vận chuyển, khuyến mại, đa chi nhánh và giá vốn/lợi nhuận nằm ngoài phạm vi đồ án đã chốt. Không mô tả các mục này là chức năng đã triển khai.

## Kiểm thử và đánh giá

```bash
mvn -B verify
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

Kiểm thử mặc định dùng H2 bộ nhớ riêng. CI chạy lại với MySQL riêng. Không trỏ kiểm thử vào dữ liệu cần giữ: kiểm thử làm trống bảng nghiệp vụ. Bằng chứng phát hành lưu ở [docs/verification/release-1.0.0](docs/verification/release-1.0.0/).

Bộ [30 câu đánh giá](evaluation/chatbot/v1/QUESTIONS.md) được khóa trước khi code chatbot, gồm 8 tư vấn sản phẩm, 8 pha chế, 9 đơn hàng/xác thực và 5 ngoài phạm vi. Không sửa đáp án hoặc hash để khớp kết quả. Bộ thu thập chạy Gemini thật 30 × 3 lượt là bước **chủ động riêng**, không tự phát sinh phí khi `mvn verify`; xem [hướng dẫn chạy/chấm](docs/13-phat-hanh-va-ban-giao.md#đánh-giá-chatbot-có-thể-chạy-lại).

## Tài liệu bàn giao

- [Phát hành local, khởi động và tài khoản mới](docs/13-phat-hanh-va-ban-giao.md)
- [Kết quả nghiệm thu và phần còn thiếu](docs/14-nghiem-thu-1.0.0.md)
- [Thanh toán QR SePay, cấu hình local và hạn mức Free](docs/18-thanh-toan-sepay.md)
- [Chat trực tiếp khách hàng – admin](docs/17-chat-khach-hang.md)
- [Đăng nhập Google và cấu hình localhost](docs/17-dang-nhap-google.md)
- [Chatbot Gemini và cấu hình khóa](docs/12-chatbot-gemini.md)
- [Mẫu thu thập danh mục thật và công cụ kiểm tra](data/catalog/README.md)
- [Thiết kế và nghiệp vụ](docs/06-thiet-ke-va-nghiep-vu-v03.md), [vận hành/sao lưu](docs/13-phat-hanh-va-ban-giao.md)
- [Kho tri thức 55 mục](docs/ai/kho-tri-thuc-v1.md), [quy trình đánh giá đã khóa](evaluation/chatbot/v1/PROTOCOL.md)
- [Đề cương thuyết trình](docs/09-dan-y-thuyet-trinh.md)

Tài liệu 13 hướng dẫn chạy và vận hành; tài liệu 16 theo dõi tình trạng mới nhất; tài liệu 14 lưu kết quả nghiệm thu trước các đợt bổ sung. Các báo cáo tiến độ cũ đã được lược bỏ, có thể tra lại trong lịch sử Git. Tài liệu thiết kế 06 được giữ nguyên vì là nguồn tham chiếu của kho tri thức đã khóa. Không suy số kiểm thử/điểm AI hiện tại từ bằng chứng cũ.
