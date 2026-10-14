# Thanh toán QR SePay — local

Cập nhật 14/10/2026. COD được giữ; SePay là lựa chọn bổ sung. Chức năng QR mặc định tắt đến khi cấu hình tài khoản riêng. Không tạo tài khoản, liên kết ngân hàng hoặc thực hiện chuyển tiền thay người dùng.

## Gói Free và cách tránh phát sinh phí

[Bảng giá SePay](https://sepay.vn/bang-gia.html) hiện ghi 0đ/tháng, 50 giao dịch tiền vào/tháng, có API/Webhook. **Cho phép vượt hạn mức và tính phí phần vượt**; không phải tự dừng ở giao dịch thứ 50. Số lượng SePay tính theo giao dịch tiền vào được ghi nhận trong dịch vụ, không chỉ số đơn SuniCoffee; hãy kiểm tra mức sử dụng trong dashboard, kể cả giao dịch ngoài website trên tài khoản liên kết. Không bật nâng cấp gói hoặc giả định hệ thống này chặn được mọi khoản tiền đến ngân hàng. Phí ngân hàng là khoản riêng.

Kiểm thử phần mềm dùng dữ liệu giả lập, không gọi SePay thật. Để diễn tập tích hợp không chuyển tiền thật, dùng [Sandbox API v2](https://developer.sepay.vn/vi/sepay-api/v2/sandbox). Không dùng tài khoản ngân hàng thật để tạo các giao dịch mô phỏng.

## Chuẩn bị

1. Tạo tài khoản tại [my.sepay.vn](https://my.sepay.vn), kiểm tra đang dùng gói Free.
2. Có thể bật **Test mode** để tạo tài khoản ngân hàng/giao dịch giả lập và API token Sandbox; chưa cần liên kết ngân hàng thật. Khi muốn nhận tiền thật, liên kết ngân hàng trong Live theo hướng dẫn SePay.
3. Lấy API token riêng của đúng môi trường ở **Cấu hình Công ty → API Access**. Token chỉ lưu trên máy chạy backend; không gửi vào chat, không đặt trong JavaScript hoặc commit.
4. Xác định UUID tài khoản từ API v2 `/bank-accounts`, số tài khoản gốc, số tài khoản/VA dùng trong QR và tên ngân hàng. Vào tài liệu API v2 hoặc công cụ gọi API của bạn để tra, với token trong header `Authorization: Bearer …`; không đưa token vào URL.

[Quy tắc QR theo ngân hàng](https://developer.sepay.vn/vi/tien-ich-khac/tao-qr-code): một số ngân hàng yêu cầu VA; VietinBank cá nhân/hộ kinh doanh cần `SEVQR` trong nội dung, VA theo nội dung cần `TKP` + mã VA. Số QR có thể khác số tài khoản gốc trong giao dịch. Điền đúng các trường dưới đây theo cấu hình đã liên kết; không tự lấy mã ngân hàng khác để thay thế.

## Cấu hình trên máy

### Cách nhanh cho MBBank không dùng VA

Ảnh dashboard đã xác nhận tài khoản MBBank kết nối API, Test mode đang tắt. Chưa xác minh token hoặc giao dịch qua ứng dụng.

Chạy trong thư mục dự án:

```bash
python3 scripts/configure-sepay.py
```

Chọn `live` cho tài khoản đang có trong ảnh; chọn `sandbox` nếu dùng tài khoản mô phỏng và token Test mode riêng. Dán token khi Terminal hỏi (ký tự không hiện), chọn tài khoản MBBank rồi nhập `luu`. Công cụ chỉ đọc danh sách tài khoản, tự lấy UUID, lưu file riêng quyền 600 và không ghi đè cấu hình có sẵn. Không thực hiện chuyển tiền. Với VA, dùng cách điền mẫu bên dưới.

Nếu cấu hình lỗi: HTTP 401 thường liên quan token hoặc môi trường; HTTP 403 là truy cập bị từ chối; HTTP 429 cần chờ rồi thử lại. Công cụ có User-Agent riêng để tránh lỗi 403 đã tái hiện với Python-urllib mặc định. Thông báo không in token hoặc nội dung phản hồi của nhà cung cấp. Không tắt xác minh chứng chỉ HTTPS.

### Điền mẫu thủ công

```bash
mkdir -p .runtime
cp -n config/sepay.properties.example .runtime/sepay.properties
chmod 600 .runtime/sepay.properties
```

Mở `.runtime/sepay.properties` trong IDE và điền:

| Thuộc tính `app.sepay.*` | Nội dung |
| --- | --- |
| `enabled` | `true` sau khi đã điền và kiểm tra cấu hình |
| `sandbox` | `true` để mô phỏng; `false` khi chủ động nhận tiền thật |
| `api-token` | Token API v2 của đúng Test mode/Live |
| `bank-account-id` | UUID tài khoản trong SePay API v2 |
| `account-number` | Số tài khoản gốc, khớp `account_number` trong giao dịch |
| `qr-account` | Số tài khoản hoặc VA được phép dùng để nhận tiền qua QR |
| `bank` | Mã/short name theo danh sách ngân hàng SePay, MBBank dùng BIN `970422` |
| `holder` | Tên chủ tài khoản nhận, không dấu |
| `description-prefix` | Chuỗi bắt buộc của ngân hàng/VA, nếu có; ứng dụng nối mã đơn phía sau |
| `va` | Chuỗi VA khớp trường `va` trong giao dịch; để trống khi không dùng VA |

Không đổi cấu hình nhận tiền giữa lúc đang thanh toán. Mỗi đơn giữ bản chụp ngân hàng, tài khoản, mã thanh toán và môi trường; đổi cấu hình không sửa QR của đơn cũ. Khi đổi Sandbox/Live, đơn thuộc môi trường cũ không được kiểm tra bằng token môi trường mới.

Dừng phiên ứng dụng cũ, sao lưu H2 khi đã dừng rồi chạy:

```bash
mvn -B verify
mvn spring-boot:run
```

Migration V5 tự thêm trường thanh toán và bảng giao dịch, giữ đơn cũ là COD. Đơn đã có ngày đối soát COD được chuyển sang trạng thái thanh toán PAID; đơn chưa đối soát không tự coi đã thu tiền.

## Sử dụng

- Checkout chọn **COD** hoặc **QR SePay**. QR chỉ xuất hiện khi đủ cấu hình; Sandbox ghi nhãn riêng.
- Đặt đơn chuyển khoản xong, ứng dụng mở thẳng trang thông tin và QR. Khi backend xác nhận đủ tiền đúng mã đơn, trình duyệt tự chuyển tới trang **Thanh toán thành công**, có nút **Tiếp tục mua sắm**, **Về trang chủ** và **Xem đơn hàng**. Trang thành công kiểm tra quyền chủ đơn và trạng thái PAID ở server; không thể tự xác nhận bằng cách sửa URL. Có thể mở lại trang QR từ chi tiết đơn. Live hiển thị ảnh QR từ dịch vụ SePay; nếu ảnh lỗi, đối chiếu thông tin chuyển khoản bằng chữ. Sandbox không hiển thị QR có thể quét chuyển tiền thật: tạo giao dịch trong Test mode với đúng nội dung/số tiền được hiển thị.
- Khi đang chờ, trang kiểm tra khoảng 15 giây/lần khi hiển thị, tối đa 20 phút. Có nút kiểm tra thủ công. Đóng trang không hủy đơn; mở lại trang hoặc admin kiểm tra để cập nhật giao dịch.
- Luồng tự chuyển trang dùng polling, chưa dùng webhook. Thông báo từ SePay có thể đến chậm; ứng dụng xác minh định kỳ khoảng 15 giây khi mở trang. Backend gọi API v2 SePay qua HTTPS để lấy giao dịch, không cần webhook, tunnel hoặc public website. Token không gửi tới trình duyệt. [API giao dịch](https://developer.sepay.vn/vi/sepay-api/v2/giao-dich/danh-sach), [xác thực và giới hạn](https://developer.sepay.vn/vi/sepay-api/v2/xac-thuc).
- Chỉ nhận giao dịch tiền vào đúng UUID tài khoản, số tài khoản, VA (nếu có), mã thanh toán đầy đủ và thời gian từ khi đặt đơn. Lưu ID giao dịch duy nhất chống ghi tiền hai lần. Tổng tiền do backend tính cả phí giao; không lấy số tiền từ trình duyệt.
- Chuyển đủ tiền: PAID; thiếu/thừa: REVIEW, liên hệ cửa hàng. Nhiều lần chuyển được cộng theo các ID khác nhau. Đơn hủy/đã nhận hoàn có tiền vào: REFUND_REQUIRED; không khôi phục đơn hoặc trừ kho lại.
- Admin chỉ xác nhận/giao đơn QR khi PAID. COD vẫn đối soát riêng sau giao thành công. Khách không tự hủy khi hệ thống đã ghi nhận tiền; admin hủy đơn có tiền phải xử lý hoàn tiền thủ công.

## Giới hạn cần biết

- Chưa tích hợp webhook hoặc tiến trình đồng bộ nền khi không ai mở trang. Admin cần kiểm tra các đơn hủy có khả năng nhận tiền muộn; không suy trạng thái UNPAID là ngân hàng chắc chắn chưa nhận tiền.
- Không tự hết hạn/hủy QR, không tự hoàn tiền. Khi hủy đơn, QR đã lưu/chụp vẫn có thể được dùng; tiền đến muộn cần đối chiếu. Chưa có nút xác nhận hoàn tiền hoặc xử lý phân bổ tiền thừa; theo dõi thủ công với mã tham chiếu ngân hàng.
- Trường hợp sai nội dung, thiếu mã, không có dữ liệu API hoặc quá 100 kết quả cho một mã: không tự đánh dấu đủ tiền, cửa hàng đối chiếu SePay/ngân hàng. Lỗi mạng hoặc 401/429 không được coi là thanh toán thành công.
- Giới hạn gọi API: tối đa một lần bắt đầu yêu cầu/giây trong tiến trình, mỗi đơn cách ít nhất 15 giây. Không tự đăng ký gói trả phí. Đây là giới hạn truy vấn của ứng dụng, không phải hạn mức giao dịch ngân hàng.
- Cấu hình đủ chỉ cho phép hiển thị phương thức, chưa chứng minh token/ngân hàng hoạt động. Cần thử Sandbox và một giao dịch Live được chủ tài khoản chủ động thực hiện trước khi công bố đã nghiệm thu nhận tiền thật.

Xem [bằng chứng kiểm thử](verification/sepay/README.md). Chức năng hiện có mã tích hợp; chưa có token được cấu hình để xác nhận đầu-cuối với SePay thật.
