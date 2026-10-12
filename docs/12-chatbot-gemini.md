# Chatbot SuniCoffee — Gemini

Ngày: 12/10/2026. Đã có mã tích hợp, giao diện và kiểm thử tự động. Chưa chạy Gemini thật hoặc chấm bộ 30 câu.

## Bạn cần làm gì?

1. Tạo/chọn API key trong [Google AI Studio](https://aistudio.google.com/apikey). Kiểm tra quyền gọi mô hình và quota của dự án. Không gửi khóa qua chat hoặc đưa vào Git. Xem [hướng dẫn của Google](https://ai.google.dev/gemini-api/docs/api-key).
2. Trong Terminal **zsh** ở thư mục dự án, nhập khóa ở lời nhắc ẩn rồi bật AI:

   ```zsh
   read -rs 'GEMINI_API_KEY?Dán Gemini API key rồi Enter: '
   export GEMINI_API_KEY
   export CHAT_AI_ENABLED=true
   export GEMINI_MODEL=gemini-3.1-flash-lite
   mvn spring-boot:run
   ```

   Khóa nằm trong môi trường của Terminal này và tiến trình ứng dụng con. Nếu chạy từ IDE, đặt các biến tương ứng trong Run Configuration riêng trên máy. Ứng dụng không tự đọc `.env`. Không thêm khóa vào JavaScript, Java hoặc `application.yml`.
3. Mở [http://localhost:8080/chat](http://localhost:8080/chat), hoặc chọn **Trợ lý AI** trên thanh điều hướng. Nếu đang có phiên ứng dụng cũ, dừng rồi chạy lại; hoặc đổi cổng bằng `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085`.
4. Thử câu hỏi kiến thức và mở nguồn tham khảo. Đăng nhập, tạo đơn demo rồi hỏi mã SUNI của chính tài khoản để thử tra cứu.

Mặc định `CHAT_AI_ENABLED=false`: trang chat vẫn hỗ trợ giá/tồn và đơn; phần kiến thức báo AI chưa cấu hình. Cần bật biến và có khóa để gọi Gemini. Model có thể đổi qua `GEMINI_MODEL`, phải hỗ trợ JSON theo schema. Tham khảo [Gemini 3.1 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite) và [structured outputs](https://ai.google.dev/gemini-api/docs/generate-content/structured-output). Hạn mức, phí và quyền model phụ thuộc dự án Google; chưa xác minh tài khoản của người dùng.

## Chức năng

- Trang `/chat`: gợi ý câu hỏi, trạng thái chờ, nguồn tham khảo, xóa trò chuyện trên màn hình, thông báo khi thiếu khóa/lỗi API.
- Gemini trả lời từ các mục tri thức được chọn. Máy chủ kiểm tra cấu trúc và ID nguồn; URL do danh mục nguồn cung cấp, không lấy link AI tự tạo.
- Giá/tồn đọc trực tiếp từ cơ sở dữ liệu, ưu tiên mã SKU chính xác. Chỉ hiển thị sản phẩm đang bán; dữ liệu demo có nhãn rõ. Chưa ánh xạ tài liệu hãng với SKU cửa hàng.
- Tra cứu `SUNI-123`, `SUNI 123`, `đơn 123`: truy vấn luôn gắn email trong phiên, kể cả với tài khoản admin. Không tin danh tính tự khai trong tin nhắn.
- Sai chủ đơn và đơn không tồn tại trả cùng thông báo. Phiên bị thu hồi yêu cầu đăng nhập lại. Tổng tiền dùng giá/phí đã lưu trên đơn; hoàn thành giao không đồng nghĩa đối soát COD.
- Chỉ đọc dữ liệu. Yêu cầu hủy đơn nhận hướng dẫn sang Đơn của tôi, không thay đổi trạng thái/tồn kho.

## Luồng dữ liệu

`POST /chat/messages` nhận JSON `{ "message": "..." }`, tối đa 1.500 ký tự và có CSRF. Khách ẩn danh có thể hỏi thông tin công khai; đơn cá nhân yêu cầu đăng nhập. Kết quả gồm `status`, `answer`, `sources`.

Máy chủ phân luồng đơn/giá/tồn để trả trực tiếp. Với kiến thức, tìm tối đa 12 mục bằng từ khóa tiếng Việt có bỏ dấu, cộng các quy tắc phạm vi, rồi gọi Gemini. API dùng HTTPS `generateContent`, header `x-goog-api-key` và đầu ra JSON theo schema; xem [API reference](https://ai.google.dev/api/generate-content).

Gemini chỉ nhận câu hỏi kiến thức hiện tại, hướng dẫn và các mục tri thức đã chọn. Không gửi email phiên, địa chỉ hoặc lịch sử đơn từ cơ sở dữ liệu. Các mẫu email/số liên hệ phổ biến trong câu hỏi được ẩn, nhưng không thể phát hiện mọi thông tin riêng người dùng tự nhập. Không nên nhập dữ liệu cá nhân vào câu hỏi kiến thức.

Chỉ `knowledge.v1.json` và `sources.v1.json` được đóng gói từ thư mục tri thức vào JAR. Không nạp `evaluation/`, đáp án hoặc fixture vào prompt. Nguồn nội bộ có trang đọc `/chat/sources/{id}`; không mở cả thư mục repo ra web. Nội dung câu hỏi/câu trả lời được hiển thị bằng `textContent`, không chạy HTML do người dùng hoặc AI tạo.

## Giới hạn bản đầu

- Mỗi câu xử lý độc lập. Hội thoại chỉ ở bộ nhớ trang trình duyệt, chưa có ngữ cảnh nhiều lượt, lưu lịch sử hay streaming. Cần nhắc lại dụng cụ hoặc mã đơn trong câu tiếp theo.
- Truy xuất từ khóa, chưa có embedding/vector database. Tìm tên xét tối đa 200 sản phẩm đang bán, trả tối đa 4 kết quả; SKU chính xác được truy vấn trực tiếp.
- Kiểm tra ID nguồn không chứng minh mọi phát biểu AI đều đúng. Cần chấm thực tế sau khi có khóa. Phản hồi sai cấu trúc/nguồn hoặc bị cắt sẽ trả thông báo chưa trả lời được.
- Giới hạn 10 tin/phút/IP, 60 tin/phút/toàn tiến trình và 4 lượt Gemini đồng thời. Chưa chia sẻ giới hạn giữa nhiều máy; IP là đầu kết nối, không tin header tự khai. Khi dùng reverse proxy cần cấu hình giới hạn tại proxy.
- Timeout kết nối 5 giây, phản hồi 30 giây; không tự gọi lại API. Nên đặt quota/cảnh báo chi phí Google trước khi mở công khai. Không ghi nội dung chat, khóa hay lỗi thô từ Gemini vào log ứng dụng.

## Kiểm thử

```bash
python3 scripts/verify-chatbot-baseline.py
mvn -Ddebug=false test
mvn -DskipTests package
```

Đã chạy **81 kiểm thử trên H2, 0 lỗi**, gồm 20 ca mới cho chatbot. Có kiểm tra CSRF, giới hạn đầu vào, quyền chủ đơn (kể cả admin), phiên thu hồi, không ghi dữ liệu, giá/tồn hiện hành, nguồn giả, thiếu nguồn, phản hồi bị cắt, lỗi quota và thiếu khóa. Gemini được giả lập, không gọi API thật. Bằng chứng: [test-summary.json](verification/chatbot-v1/test-summary.json). Chưa chạy lại MySQL trong bước này.

Mốc 55 mục/30 câu và tag `chatbot-eval-v1` giữ nguyên; kiểm tra SHA-256 vẫn đạt. Sau khi có khóa, chạy bộ đã khóa theo [PROTOCOL.md](../evaluation/chatbot/v1/PROTOCOL.md), 3 lượt/câu, lưu và chấm kết quả ngoài `evaluation/chatbot/v1/`. Chưa có điểm chất lượng AI; kiểm thử phần mềm không thay thế điểm bộ 30 câu.
