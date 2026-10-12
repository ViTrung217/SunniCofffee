# Chuẩn bị tri thức và đánh giá chatbot

Ngày bàn giao: 12/10/2026. Hoàn thành bước chuẩn bị trước code chatbot.

| Yêu cầu | Kết quả |
| --- | --- |
| Kho tri thức ≥50 mục, mỗi mục có nội dung và nguồn | 55 mục: 15 sản phẩm, 15 pha chế, 25 nghiệp vụ; 14 nguồn có vị trí đối chiếu |
| 30 câu hỏi có đáp án mong đợi | 8 sản phẩm, 8 pha chế, 9 đơn hàng/xác thực, 5 ngoài phạm vi |
| Khóa bộ đánh giá trước code chatbot | JSON câu hỏi, fixture, quy trình chấm, phạm vi và kho tri thức được băm trong `LOCK.json`; mốc Git `chatbot-eval-v1` |
| Khởi tạo Git | Repo đã có nhánh `main` và commit nền `d0b2f33`; sử dụng repo hiện có, không khởi tạo lại lịch sử |

## Tệp bàn giao

- [Kho tri thức đọc được](ai/kho-tri-thuc-v1.md), [JSON](../data/knowledge/knowledge.v1.json), [nguồn](../data/knowledge/sources.v1.json).
- [30 câu hỏi và đáp án](../evaluation/chatbot/v1/QUESTIONS.md), [JSON](../evaluation/chatbot/v1/questions.json), [fixture](../evaluation/chatbot/v1/fixtures.json).
- [Phạm vi](ai/pham-vi-chatbot-v1.md), [quy trình và ngưỡng chấm](../evaluation/chatbot/v1/PROTOCOL.md), [khóa SHA-256](../evaluation/chatbot/v1/LOCK.json).

## Kiểm tra đã thực hiện

`python3 scripts/verify-chatbot-baseline.py` đạt: số lượng, ID duy nhất, phân nhóm, nguồn tham chiếu, ngữ cảnh đánh giá, tổng tiền fixture và 10 tệp khóa. Đã kiểm tra liên kết Markdown nội bộ. Trên bản sao trong thư mục tạm, sửa đáp án khiến kiểm tra khóa thất bại như mong đợi. `git diff --check` không báo lỗi khoảng trắng. Không thay đổi mã ứng dụng nên không chạy lại bộ Maven trong bước tài liệu này.

Nguồn nội bộ được xác nhận trùng commit nền; SHA-256 và commit của các nguồn này lưu riêng trong manifest. Có thể xem lại bằng `git show <baseline_application_commit>:<path>` khi ứng dụng thay đổi. Các URL hãng đã được mở và đối chiếu vào ngày bàn giao; chương trình kiểm tra offline không khẳng định URL vẫn truy cập được về sau.

## Phạm vi dữ liệu và giới hạn

Wacaco, HARIO và AeroPress là tài liệu sản phẩm tham khảo chính thức; hướng dẫn pha chế lấy từ AeroPress và Blue Bottle. Chưa có danh mục hay chính sách cửa hàng thật được cung cấp. Không gán thông số các hãng này cho 12 SKU demo. Không dùng bộ đánh giá hoặc toàn bộ fixture làm dữ liệu truy xuất của chatbot.

Bộ đánh giá có yêu cầu đăng nhập, hết hiệu lực phiên, sai chủ đơn, đơn không tồn tại, lỗi nguồn dữ liệu và giả mạo vai trò. Các tình huống này hiện là đặc tả kiểm thử cho chatbot tương lai, không phải kết quả chứng minh chatbot đã bảo vệ dữ liệu. Chưa triển khai chatbot; chưa có tỷ lệ trả lời đúng thực nghiệm.

Git hiện là repo cục bộ, chưa cấu hình remote. Commit chuẩn bị chỉ chứa các tệp của bước này; các thay đổi giao diện/dữ liệu có sẵn vẫn ở trạng thái làm việc riêng.
