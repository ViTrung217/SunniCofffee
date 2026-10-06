# Dàn ý thuyết trình SuniCoffee 0.3 — phần ngoài AI

Bản nội dung nháp khoảng 10 phút, chưa theo mẫu khoa. Chưa có tệp PPTX. Không điền tên giảng viên, khoa, người thực hiện hoặc số liệu khảo sát khi chưa có thông tin xác nhận. Nguồn kỹ thuật: docs/02, docs/06, docs/08 và các báo cáo kiểm thử trong docs/verification.

## 1. SuniCoffee: bán hàng và quản lý vận chuyển

Nội dung: ứng dụng quản lý bán cà phê và dụng cụ cà phê phượt; phiên bản 0.3; dữ liệu trình diễn mô phỏng; không trình bày AI trong phần này.

Lời dẫn: “Phần trình diễn tập trung vào đặt hàng, bảo đảm tồn kho và xử lý vận chuyển. Các sản phẩm và giao dịch sử dụng dữ liệu mẫu.”

## 2. Phạm vi và tác nhân

Nội dung: một cửa hàng, một kho; khách hàng và quản trị; đặt COD, phí cố định có thể cấu hình; vận chuyển được admin cập nhật thủ công.

Lời dẫn: “Khách xem sản phẩm, đặt và theo dõi đơn của mình. Quản trị xử lý đơn, kho, tài khoản và báo cáo. Phiên bản này chưa kết nối hãng vận chuyển thật.”

## 3. Kiến trúc và dữ liệu

Nội dung: Spring Boot MVC + Thymeleaf; Spring Security; service giao dịch; JPA/JDBC; MySQL; Flyway V1–V3. Dùng sơ đồ kiến trúc ở docs/06 và ERD hiện hành ở docs/02 khi dựng slide.

Lời dẫn: “Quy tắc nghiệp vụ nằm ở service. Các thay đổi liên quan đơn và tồn kho cùng nằm trong giao dịch; migration cập nhật cấu trúc và giữ dữ liệu cũ.”

## 4. Hai khách tranh sản phẩm cuối

Nội dung: giỏ chưa giữ hàng; khóa và đọc lại tồn khi checkout; chỉ một đơn thành công; thiếu hàng rollback toàn bộ và giữ giỏ. Chèn sơ đồ tuần tự đã có trong docs/06.

Lời dẫn: “Thứ tự lấy khóa cơ sở dữ liệu quyết định giao dịch xử lý trước. Đơn thứ hai phải đọc tồn mới sau khi giao dịch đầu kết thúc, không dùng số tồn cũ đã hiển thị.”

## 5. Giao hàng thất bại và nhận hoàn

Nội dung: chờ → xác nhận → đang giao → hoàn thành; thất bại → giao lại hoặc chuyển hoàn → nhận hoàn. Chỉ cộng lại số còn bán được khi cửa hàng nhận kiện.

Lời dẫn: “Giao thất bại không có nghĩa hàng đã về kho. Ví dụ gửi hai túi, nhận về chỉ một túi còn bán được thì chỉ cộng lại một túi, còn lại ghi kết quả kiểm tra.”

## 6. COD, nhật ký và doanh thu

Nội dung: tổng thanh toán gồm phí; nhận tiền COD tách khỏi giao thành công; nhật ký người/thời điểm/lý do; doanh thu tiền hàng không gồm phí; top theo product_id dù đổi tên/SKU.

Lời dẫn: “Đơn hoàn thành có thể chưa được hãng trả tiền COD. Báo cáo doanh thu và việc nhận tiền vì vậy được diễn giải riêng, chưa phải báo cáo lợi nhuận.”

## 7. Kiểm thử và phục hồi

Nội dung: 61/61 ca đạt ở mỗi lượt H2/MySQL; lượt MySQL có 58 ca ứng dụng dùng MySQL, 2 ca không database, 1 ca nâng cấp H2 riêng. Sao lưu/phục hồi giữ tồn, đơn và hash ảnh. GET cục bộ 100 yêu cầu/10 luồng đạt 100%; p95 21,57 ms, không phải cam kết tải sản xuất.

Lời dẫn: “Các số liệu được lấy từ báo cáo chạy thật. Kiểm thử đồng thời bao gồm tranh món cuối, hủy cùng lúc và đặt hàng với xuất kho. Phép đo phản hồi chỉ là kiểm tra ngắn trên localhost.”

## 8. Demo và giới hạn

Nội dung: demo đặt → bàn giao → thất bại → nhận hoàn; kiểm tra tồn/nhật ký; demo COD và báo cáo. Còn cần dữ liệu thật, kiểm tra trực quan trên thiết bị, môi trường triển khai và mẫu báo cáo khoa. AI để ngoài phạm vi theo yêu cầu.

Lời dẫn: “Kết quả hiện tại là phần mềm và tài liệu kỹ thuật có thể chạy lại. Không coi dữ liệu demo là kết quả khảo sát, và chưa khẳng định hệ thống đã được triển khai công khai.”

## Câu hỏi dự kiến

- Vì sao không giữ tồn ngay khi thêm giỏ? Giỏ bỏ quên không nên giữ hàng vô thời hạn; tồn được kiểm tra lại khi đặt.
- Vì sao không hoàn tồn khi giao thất bại? Hàng còn nằm ngoài cửa hàng; chỉ nhận hoàn khi kiểm đếm thực tế.
- Vì sao cần token checkout ngoài CSRF? CSRF bảo vệ nguồn yêu cầu; token checkout chống tạo trùng do gửi lại.
- Vì sao dữ liệu đơn không lấy tên/giá sản phẩm hiện tại? Để lịch sử mua không thay đổi khi quản trị sửa sản phẩm.
- Vì sao không tự hủy đơn quá 48 giờ? Đây là bộ lọc nhắc xử lý đã chốt cho COD, admin cần liên hệ trước khi quyết định.
