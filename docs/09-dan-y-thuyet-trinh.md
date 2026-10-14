# Dàn ý thuyết trình SuniCoffee 1.0.0 — chạy local

Bản nội dung nháp khoảng 10 phút, chưa theo mẫu khoa. Chưa có tệp PPTX. Không điền tên giảng viên, khoa, người thực hiện hoặc số liệu khảo sát khi chưa có thông tin xác nhận. Cập nhật 14/10/2026. Nguồn: [thiết kế](06-thiet-ke-va-nghiep-vu-v03.md), [chatbot](12-chatbot-gemini.md), [tình trạng hiện tại](16-tinh-trang-du-an.md), [chat nhân viên](17-chat-khach-hang.md) và bằng chứng trong `docs/verification`.

## 1. SuniCoffee: bán hàng và quản lý vận chuyển

Nội dung: ứng dụng quản lý bán cà phê và dụng cụ cà phê phượt; phiên bản 1.0.0; 36 sản phẩm minh họa, chatbot có nguồn và kênh chat trực tiếp với admin. Dữ liệu trình diễn mô phỏng, chưa phải khảo sát cửa hàng.

Lời dẫn: “Phần trình diễn tập trung vào đặt hàng, bảo đảm tồn kho và xử lý vận chuyển. Các sản phẩm và giao dịch sử dụng dữ liệu mẫu.”

## 2. Phạm vi và tác nhân

Nội dung: một cửa hàng, một kho; khách hàng và quản trị; đặt COD, phí cố định có thể cấu hình; vận chuyển được admin cập nhật thủ công.

Lời dẫn: “Khách xem sản phẩm, đặt và theo dõi đơn của mình. Quản trị xử lý đơn, kho, tài khoản và báo cáo. Phiên bản này chưa kết nối hãng vận chuyển thật.”

## 3. Kiến trúc và dữ liệu

Nội dung: Spring Boot MVC + Thymeleaf; Spring Security; service giao dịch; JPA/JDBC; H2 local hoặc MySQL; Flyway V1–V4. Sơ đồ nghiệp vụ nền ở docs/06 và ERD V3 ở docs/02; V4 bổ sung bảng hội thoại/tin nhắn, mô tả ở docs/17.

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

## 7. Chatbot AI và chat nhân viên

Nội dung: 55 mục tri thức/14 nguồn; 30 câu đánh giá khóa trước khi code. Chatbot tư vấn theo ngữ cảnh, đọc giá/tồn và chỉ tra đơn của tài khoản đã xác thực. Google Search có mã nhưng chưa nghiệm thu đầu-cuối do quota/lỗi dịch vụ. Chat nhân viên độc lập Gemini, lưu lịch sử DB và tự cập nhật khi cửa sổ hiển thị.

Trình diễn một hội thoại nhiều lượt, mở nguồn dẫn; sau đó dùng hai phiên khách/admin để gửi nhận tin, xem số chưa đọc và đóng/mở hội thoại. Không đưa API key hoặc thông tin khách thật lên màn hình.

## 8. Kiểm thử và phục hồi

Nội dung: [126/126 kiểm thử H2 đạt](verification/customer-support/tests.json), gồm kiểm thử quyền chat, phân trang, gửi lặp và đồng thời. HTTP thật dùng hai tài khoản tổng hợp đã được kiểm tra. Chưa chạy lại MySQL cho toàn bộ phần bổ sung mới và chưa nghiệm thu đầy đủ widget trên trình duyệt.

Bằng chứng [nghiệm thu trước đây](14-nghiem-thu-1.0.0.md) gồm 99 ca H2/MySQL, backup/restore và điểm AI sơ bộ 177/180 do Codex hỗ trợ chấm. Các số cũ gắn với phiên bản được ghi trong bằng chứng, không dùng làm điểm chất lượng của mã mới. Cần chấm AI độc lập và diễn tập khôi phục trước buổi bảo vệ.

## 9. Demo và giới hạn

Nội dung: đặt → bàn giao → thất bại → nhận hoàn; kiểm tra tồn/nhật ký; COD và báo cáo; chatbot và chat admin. Chạy theo [hướng dẫn bàn giao](13-phat-hanh-va-ban-giao.md). Chỉ chạy local theo phạm vi đã chốt.

Chưa có thanh toán online, API vận chuyển hoặc quên mật khẩu qua email. Còn cần dữ liệu xác minh, kiểm tra giao diện trên thiết bị và hoàn thiện báo cáo/slide theo mẫu khoa. Không trình bày dữ liệu mẫu thành doanh thu hay khảo sát thật.

## Câu hỏi dự kiến

- Vì sao không giữ tồn ngay khi thêm giỏ? Giỏ bỏ quên không nên giữ hàng vô thời hạn; tồn được kiểm tra lại khi đặt.
- Vì sao không hoàn tồn khi giao thất bại? Hàng còn nằm ngoài cửa hàng; chỉ nhận hoàn khi kiểm đếm thực tế.
- Vì sao cần token checkout ngoài CSRF? CSRF bảo vệ nguồn yêu cầu; token checkout chống tạo trùng do gửi lại.
- Vì sao dữ liệu đơn không lấy tên/giá sản phẩm hiện tại? Để lịch sử mua không thay đổi khi quản trị sửa sản phẩm.
- Vì sao không tự hủy đơn quá 48 giờ? Đây là bộ lọc nhắc xử lý đã chốt cho COD, admin cần liên hệ trước khi quyết định.
