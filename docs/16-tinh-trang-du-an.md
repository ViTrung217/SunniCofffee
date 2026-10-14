# Bảng tình trạng SuniCoffee — 14/10/2026

Phạm vi: bàn giao và bảo vệ đồ án chạy local; chưa triển khai thanh toán mới trong đợt này. “Đã có” nghĩa là có mã và luồng sử dụng, không khẳng định không còn lỗi trong mọi tình huống. Dữ liệu sản phẩm vẫn phục vụ đồ án, chưa được cửa hàng xác nhận.

| Nhóm | Chức năng | Trạng thái | Giới hạn / việc còn lại |
| --- | --- | --- | --- |
| Tài khoản | Đăng ký, đăng nhập, đăng xuất, hồ sơ, đổi mật khẩu | Đã có | Quên mật khẩu qua email và xác minh email chưa có |
| Phân quyền | Khách/admin, khóa tài khoản, thu hồi phiên khi đổi quyền/mật khẩu | Đã có | Có CSRF và giới hạn lần đăng nhập; chưa đánh giá bảo mật độc lập |
| Danh mục | Quản lý nhóm, tìm theo tên/SKU, lọc nhóm, phân trang | Đã có | Chưa tìm kiếm ngữ nghĩa hoặc gợi ý cá nhân hóa |
| Sản phẩm | Thêm/sửa/ngừng bán, đơn vị tính, giá, ảnh | Đã có | Một ảnh tải lên mỗi sản phẩm; chưa có bộ ảnh/biến thể chung một sản phẩm |
| Mô tả | 36 mô tả có giới thiệu, quy cách, công dụng và lưu ý | Đã bổ sung đợt này | Phân biệt quy cách giả lập và thông số chưa xác minh |
| Dữ liệu danh mục | 36 SKU: 10 cà phê, 6 máy, 12 dụng cụ, 8 phụ kiện | Đã bổ sung đợt này | Giá/tồn/danh mục chưa được cửa hàng xác nhận; ảnh là minh họa |
| Nhập dữ liệu | Seed DB trống; nạp bổ sung mẫu, không reset tồn/giá cũ | Đã có | Mẫu JSON hàng thật + validator có sẵn; chưa có màn hình import hàng loạt CSV/Excel |
| Giỏ hàng | Thêm, cập nhật, xóa; giỏ theo tài khoản | Đã có | Kiểm tra tồn khi đặt; không giữ hàng vô thời hạn trong giỏ |
| Checkout | Địa chỉ nhận, tổng tiền, phí giao, chống đặt trùng | Đã có | Phí giao cố định theo cấu hình, chưa tính theo địa chỉ/hãng |
| Thanh toán | COD và xác nhận nhận đủ tiền sau giao thành công | Đã có | Đối soát thủ công; trạng thái giao thành công khác với đã nhận tiền |
| Thanh toán mới | Chuyển khoản/VietQR, VNPay, MoMo, payOS | **Chưa triển khai** | Mới tư vấn lựa chọn; cần chốt phương án trước khi thực hiện |
| Tiền thanh toán | Đặt cọc, thanh toán một phần, hoàn tiền tự động | **Chưa triển khai** | Chưa có sổ giao dịch cổng thanh toán/webhook |
| Đơn hàng | Khách xem/hủy đơn chờ; admin xác nhận và chuyển trạng thái | Đã có | Lưu diễn biến và lý do; chưa gửi email/SMS thông báo |
| Vận chuyển | Hãng/mã vận đơn, giao thất bại, giao lại, nhận hoàn | Đã có, thủ công | Chưa gọi API hãng vận chuyển, vị trí thực tế hoặc ETA tự động |
| Kho | Tồn đầu, nhập/xuất, trừ khi đặt, hoàn khi hủy/nhận hoàn | Đã có | Nhật ký và khóa giao dịch; chưa có nhà cung cấp/phiếu mua nhiều dòng/đa kho |
| Báo cáo | Doanh thu tiền hàng theo ngày hoàn thành, lọc khoảng ngày | Đã có | Không gồm phí giao; chưa trừ giá vốn/chi phí; không đồng nghĩa tiền COD đã thu |
| Khuyến mại | Mã giảm giá, điểm thưởng | **Chưa triển khai** | Ngoài phạm vi ban đầu |
| Tương tác khách | Đánh giá sản phẩm, yêu thích | **Chưa triển khai** | Chưa có giao diện và nghiệp vụ tương ứng |
| Chat nhân viên | Khách gửi/nhận tin, admin có hộp thư, số chưa đọc, đóng/mở lại, lịch sử lưu DB | **Đã triển khai** | Cập nhật định kỳ; chưa có ảnh/tệp, phân công, thông báo đẩy; xem [hướng dẫn](17-chat-khach-hang.md) |
| Chatbot | Cửa sổ góc phải, 55 mục/14 nguồn, 30 câu v1 khóa | Đã có | Kho v1 giữ nguyên; dữ liệu mẫu bổ sung không phải khảo sát hàng thật |
| Chatbot hội thoại | Ngữ cảnh, sửa phủ định, số tiền, đổi chủ đề | Đã có, đã có hồi quy | Không bảo đảm hiểu mọi cách nói; lịch sử tối đa 6 lượt, không lưu lâu dài |
| Chatbot dữ liệu | Giá/tồn công khai, đơn thuộc tài khoản đã xác thực | Đã có | Chỉ đọc; không đặt/hủy/thanh toán thay khách |
| Chatbot tìm web | Định tuyến kiến thức liên quan sang Google Search, nguồn tham chiếu | **Có mã, chưa nghiệm thu đầu-cuối** | Lần thử gần nhất: Search 429; Gemini có 503/timeout. Cần quota và thử lại |
| Kiểm thử | Bộ tự động H2; kiểm tra hash bộ đánh giá đã khóa | Đã có | Xem kết quả mới ở mục dưới; kết quả MySQL/điểm AI cũ không đại diện toàn bộ bản mới |
| Vận hành local | H2/MySQL, Flyway, health/readiness, hướng dẫn backup | Đã có | Chỉ chạy một phiên với cùng file H2; không cần deploy theo yêu cầu |
| Hồ sơ bảo vệ | Tài liệu thiết kế, vận hành, đề cương thuyết trình | Đã có một phần | Cần mẫu khoa, rà báo cáo nộp, chấm AI độc lập và diễn tập trên máy bảo vệ |
| Kiểm tra trải nghiệm | Trang bán hàng và widget có kiểm thử tích hợp | Chưa nghiệm thu toàn diện | Chưa xác nhận mọi kích thước mobile/trình duyệt và tải kéo dài |

## Bằng chứng mới nhất

- **Chat nhân viên: 126/126 kiểm thử H2 đạt**, thử HTTP thật bằng hai tài khoản tổng hợp riêng; xem [kết quả](verification/customer-support/README.md). Schema mới tự áp dụng ở lần khởi động tiếp theo. Chưa nghiệm thu đầy đủ thao tác widget bằng trình duyệt.

## Bằng chứng của đợt bổ sung danh mục

- [Nguồn dữ liệu và quy tắc nạp](../data/README-products.md), [JSON 36 sản phẩm](../data/products-demo.json).
- **Kết quả đợt danh mục: 114/114 kiểm thử H2 đạt**, database local đã tăng từ 12 lên 36 sản phẩm, 12 SKU cũ giữ nguyên ID/giá/tồn; phiên nhập tạm đã dừng.
- [Kết quả kiểm tra danh mục](verification/catalog-expansion/README.md): kiểm thử nạp lặp, giữ giá/tồn/ID, nhật ký tồn đầu, tài nguyên ảnh và trang chi tiết.
- [Hội thoại mở và giới hạn Search](verification/chatbot-web/README.md): bộ trước có 111 kiểm thử H2; tra web chưa thành công với quota hiện tại. Không chạy lại AI trong đợt bổ sung danh mục này.
- [Nghiệm thu local trước đây](14-nghiem-thu-1.0.0.md): 99 kiểm thử H2/MySQL và điểm AI 177/180 là kết quả lịch sử, không suy ra mọi câu trả lời của bản mới đều đạt.

## Thứ tự hoàn thiện đề xuất

1. Rà lại danh mục, ảnh và mô tả trên trang; thay dần số liệu bằng dữ liệu đã xác nhận nếu yêu cầu đồ án cần hàng thật.
2. Chốt phương thức thanh toán bổ sung và tách trạng thái thanh toán khỏi trạng thái giao hàng; giữ COD hiện có.
3. Kiểm tra hạn mức Gemini/Search, đánh giá thêm hội thoại tự do và chấm lại phản hồi độc lập.
4. Diễn tập toàn bộ luồng mua → xử lý đơn → kho → COD → báo cáo; rà giao diện trên máy bảo vệ và hoàn thiện hồ sơ theo mẫu khoa.
