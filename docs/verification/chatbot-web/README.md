# Hội thoại mở và Google Search — 13/10/2026

Phạm vi: bổ sung sau đợt nghiệm thu local 1.0.0. Không sửa bộ tri thức/evaluation v1 đã khóa. Không triển khai Internet, không đổi API key, không thay dữ liệu cửa hàng.

## Thay đổi

- Trích nhu cầu bảo thủ: bỏ lựa chọn bị phủ định, không biến câu so sánh thành sở thích; nhận cách nói “máy pha” và các định dạng tiền có phân cách. Bộ ẩn thông tin liên hệ giữ nguyên số tiền.
- Chỉ dùng câu trả lời máy mẫu cố định cho phần bổ sung nhu cầu đơn giản; câu hỏi và phát biểu phức tạp chuyển cho mô hình đọc ngữ cảnh. Câu tìm hiểu không phải mua hàng; chỉ thêm nhãn danh mục minh họa khi câu trả lời thực sự trích nguồn STORE.
- Mô hình chọn `RESEARCH` khi câu hỏi kiến thức công khai liên quan thiếu nguồn. Lượt Google Search riêng không có lịch sử, dữ liệu cửa hàng, danh tính hay đơn hàng. Có cờ tắt `CHAT_WEB_SEARCH_ENABLED=false`.
- Nguồn lấy từ grounding metadata, kiểm tra URL HTTPS và đoạn hỗ trợ, chèn dấu tham chiếu với chỉ số byte UTF-8. Metadata thiếu/sai thì trả thiếu thông tin. Google Search Suggestions nằm trong iframe sandbox không chạy script và không truy cập trang cha.
- Lỗi Search/quota có thông báo riêng; không ghi đáp án thất bại vào hội thoại. Không lấy kết quả web làm giá/tồn/chính sách riêng. Tối đa hai bước mô hình, mỗi bước vẫn giới hạn thời gian/số lần thử lại.

## Bằng chứng và giới hạn

- [tests.json](tests.json): `mvn -q -Ddebug=false verify`, 111 kiểm thử H2, không lỗi; API Gemini giả lập trong kiểm thử tự động. Có ca hồi quy phủ định, đổi chủ đề, định dạng tiền, bảo vệ tra đơn, không chuyển truy vấn riêng sang web, quota, metadata nguồn và UTF-8. Chưa chạy lại MySQL cho đợt này; không thay schema/truy vấn nghiệp vụ.
- `python3 scripts/verify-chatbot-baseline.py`: 55 mục / 14 nguồn / 30 câu / 10 hash nguyên vẹn.
- [live-isolated.json](live-isolated.json): gọi HTTP ứng dụng mới ở 8087 với H2 bộ nhớ và danh mục tổng hợp; Gemini thật. Ngân sách có dấu phân cách được hiểu đúng; câu phủ định nhận gợi ý dụng cụ thủ công; đơn chưa xác thực bị chặn. Câu so sánh gặp quota, câu rang sáng gặp AI bận và câu ngoài phạm vi gặp timeout. Không tính các ca gián đoạn là ca trả lời đạt.
- [live-known-topic.json](live-known-topic.json): sau tư vấn máy, hỏi Picopresso có tự đun nước không; nhận câu trả lời đúng chủ đề và nguồn hãng. Bằng chứng này thu trước sửa nhãn minh họa thừa; ca hồi quy mới kiểm tra việc bỏ nhãn khi chỉ dùng nguồn kiến thức.
- [search-capability.json](search-capability.json): kiểm tra riêng Google Search với cùng cấu hình nhận HTTP 429 `RESOURCE_EXHAUSTED`. Chỉ lưu mã lỗi, không lưu khóa hoặc nội dung lỗi thô. Không xác định được hạn mức cụ thể từ phản hồi này.
- [local-runtime.json](local-runtime.json): đã sao lưu H2 khi dừng ứng dụng, kiểm tra bản sao và chạy JAR mới tại 8080; readiness UP. [live-8080-final.json](live-8080-final.json): ngân sách có dấu phân cách đúng trên bản cuối; lượt hỏi kiến thức tiếp theo gặp lỗi kết nối Gemini, không coi là thành công.
- JavaScript cửa sổ chat qua kiểm tra cú pháp bằng `node:vm.Script`. Chưa quan sát Google Search Suggestions với phản hồi API thật vì quota; chưa nghiệm thu hiển thị khung này trên mọi trình duyệt/thiết bị.

**Chưa xác nhận tra web thành công đầu-cuối với khóa hiện tại.** Cần kiểm tra hạn mức/quyền Search của dự án Google rồi chạy lại. Không tự đổi gói, bật thanh toán hay tăng quota. Điểm 177/180 trước đây không đại diện cho mã/prompt mới; chưa chạy lại 90 lượt đánh giá AI. Các quy tắc từ khóa vẫn là gợi ý bảo thủ; mô hình có thể hiểu sai câu phức tạp và metadata nguồn không bảo đảm mọi phát biểu đều chính xác.
