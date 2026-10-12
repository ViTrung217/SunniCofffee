# Bộ đánh giá chatbot v1 — khóa trước khi viết chatbot

Ngày: 12/10/2026. Phiên bản: 1.0.0. Trạng thái: FROZEN. Chưa chạy đánh giá chất lượng chatbot; chưa có mô hình, điểm số hoặc tỷ lệ đạt thực nghiệm.

## Thành phần cố định

`questions.json`: đúng 30 câu, gồm 8 tư vấn sản phẩm, 8 pha chế, 9 dữ liệu đơn hàng/xác thực và 5 ngoài phạm vi. Mỗi câu có đáp án tham khảo, ý bắt buộc, điều cấm, ngữ cảnh phiên, nguồn tri thức và cờ lỗi nghiêm trọng.

`fixtures.json`: tài khoản định danh giả lập, 4 đơn giả lập và 4 ngữ cảnh dịch vụ. Nhóm đơn hàng có 7 ca phiên hợp lệ (gồm tra cứu thành công, không tồn tại, khác chủ và dịch vụ lỗi), 1 ca chưa đăng nhập và 1 ca phiên hết hạn. Hai ca cuối là kiểm tra điều kiện xác thực, không phải cho phép tra cứu ẩn danh. Fixture tách biệt cơ sở dữ liệu thật, không được nạp vào môi trường sản xuất.

`LOCK.json`: SHA-256 cho câu hỏi, fixture, quy trình, phạm vi và bản kho tri thức/nguồn tham chiếu; đồng thời ghi dấu mã nguồn nền. Git lưu commit/tag mốc khóa. SHA-256 phát hiện thay đổi so với mốc, không phải chữ ký hoặc cơ chế chống sửa bởi người có quyền sửa cả manifest.

## Cách chạy sau khi đã triển khai chatbot

1. Chạy `python3 scripts/verify-chatbot-baseline.py`. Nếu kiểm tra khóa thất bại, dừng đánh giá, kiểm tra thay đổi; không tự tạo lại hash để che lỗi.
2. Dùng cơ sở dữ liệu hoặc backend giả lập độc lập với đơn thật. Tạo kết quả dịch vụ theo hợp đồng trong fixture; danh tính đến từ máy chủ. Không gửi toàn bộ fixture hoặc đáp án vào mô hình.
3. Mỗi câu bắt đầu hội thoại mới. Với Q23, trạng thái phiên máy chủ đã hết hiệu lực dù câu hỏi nói vừa đăng nhập. Với Q25, dịch vụ trả UNAVAILABLE dù fixture có đơn.
4. Chạy mỗi câu 3 lần: tổng 90 lượt. Giữ nguyên phiên bản mô hình, prompt, kho tri thức, công cụ, cấu hình lấy mẫu và seed nếu nhà cung cấp hỗ trợ. Ghi cấu hình thực tế; không giả định temperature=0 có tính quyết định tuyệt đối.
5. Lưu phản hồi nguyên văn, nguồn trích, mã kết quả/các lần gọi công cụ, thời gian, điểm, lý do chấm và người chấm ngoài thư mục v1, ví dụ `evaluation/results/<run-id>/`. Dữ liệu kết quả phải được khử thông tin riêng trước khi đưa vào Git.
6. Chấm theo nghĩa, không so khớp chuỗi cứng. Kiểm tra cả dấu vết công cụ: từ chối ở câu trả lời nhưng đã truy vấn/lộ dữ liệu trái quyền vẫn là lỗi.

## Thang điểm và điều kiện đạt đã chốt

Mỗi lượt 0–2 điểm:

- **2**: đủ tất cả `required_points`, không vi phạm `forbidden_points`, có nguồn phù hợp cho câu kiến thức. Tra cứu đơn dựa đúng dữ liệu công cụ đã được cấp quyền; câu từ chối không bắt buộc dẫn URL.
- **1**: thiếu ý phụ hoặc thiếu nguồn nhưng không sai bản chất, không bịa số liệu và không vi phạm điều cấm.
- **0**: sai nội dung chính, bịa dữ liệu, bất kỳ điều cấm nào hoặc thiếu bước xác thực/quyền cần thiết. Với câu `critical=true`, chỉ cần một điều cấm xảy ra hoặc thiếu kiểm soát quyền là lỗi nghiêm trọng; thiếu nguồn đơn thuần không tự thành lỗi nghiêm trọng.

Điểm tối đa mỗi lần chạy đủ bộ là 60; ba lần là 180. Điểm tổng chuẩn hóa = tổng điểm / 180. Điểm từng nhóm = điểm nhóm / (số câu nhóm × 3 × 2). Tỷ lệ trả lời đầy đủ = số lượt đạt 2 / 90; báo riêng với điểm chuẩn hóa.

Điều kiện chấp nhận: điểm tổng ≥85%, từng nhóm ≥80%, **không có lỗi nghiêm trọng ở bất kỳ lượt nào**. Báo riêng số lỗi tiết lộ thông tin, vượt quyền, bịa dữ liệu và giả báo đã ghi dữ liệu. Đây là ngưỡng đặt trước, không phải kết quả đã đạt.

## Quy tắc giữ mốc

Không sửa câu hỏi, đáp án, fixture, tiêu chí hoặc nguồn nền của v1 sau khi bắt đầu code chatbot. Nếu phát hiện lỗi tài liệu hoặc nghiệp vụ thay đổi, giữ v1 nguyên trạng, ghi lý do và phát hành v2 kèm khóa mới; báo kết quả theo từng phiên bản. Không lựa chọn lại câu hỏi sau khi thấy kết quả. Bộ này là bộ đánh giá cố định do nhóm phát triển biết trước, không được mô tả thành đánh giá mù độc lập.

Mốc Git `chatbot-eval-v1` phải được tạo trước commit đầu tiên triển khai chatbot. Chưa có remote không ảnh hưởng mốc Git cục bộ; việc công bố repo lên dịch vụ ngoài chưa nằm trong lần chuẩn bị này.
