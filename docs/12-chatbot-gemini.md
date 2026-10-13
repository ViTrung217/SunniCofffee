# Chatbot SuniCoffee — Gemini

Cập nhật: 13/10/2026. Đã tích hợp cửa sổ chat góc dưới bên phải, chạy kiểm thử tự động và thử Gemini thật thành công trên localhost. Đã bổ sung ngữ cảnh tư vấn nhiều lượt và cấu hình khóa cục bộ. Bản local 1.0.0 đã chạy 99 kiểm thử trên H2 và MySQL, cùng 90 lượt đánh giá AI; điểm chấm sơ bộ bởi Codex là 177/180, chưa phải nghiệm thu độc lập. Xem [biên bản bản cuối](14-nghiem-thu-1.0.0.md). Chưa kiểm thử tải dài hạn.

## Cấu hình cục bộ trên máy này

Khóa được lưu tại `.runtime/chatbot.properties` (quyền `600`, thư mục đã được `.gitignore` loại trừ). Không đưa tệp này vào Git, ảnh chụp hoặc tài liệu. Các profile `local` (mặc định), `demo` và `mysql` nhập tệp này nếu tồn tại; không đóng gói khóa vào JAR. Tệp chứa `app.chat.enabled` và `app.chat.gemini-api-key`; giá trị ở đây ưu tiên các giá trị thay thế `${GEMINI_API_KEY:}` trong cấu hình gốc. Vì vậy khi đổi khóa, cần cập nhật tệp này hoặc bỏ cấu hình cục bộ để dùng biến môi trường.

Sau khi đổi khóa hoặc mã Java, dừng ứng dụng cũ và chạy lại `mvn spring-boot:run` từ thư mục dự án, rồi tải lại trang. Máy khác không có tệp này có thể cấu hình bằng biến môi trường như dưới đây.

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
3. Mở [http://localhost:8080](http://localhost:8080), bấm **Chat với Suni** ở góc dưới bên phải. Có thể mở trang đầy đủ tại `/chat` hoặc chọn **Trợ lý AI** trên thanh điều hướng. Nếu đang có phiên ứng dụng cũ, dừng rồi chạy lại trong Terminal đã đặt biến; hoặc đổi cổng bằng `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085`.
4. Thử câu hỏi kiến thức và mở nguồn tham khảo. Đăng nhập, tạo đơn demo rồi hỏi mã SUNI của chính tài khoản để thử tra cứu.

Mặc định `CHAT_AI_ENABLED=false`: trang chat vẫn hỗ trợ giá/tồn và đơn; phần kiến thức báo AI chưa cấu hình. Cần bật biến và có khóa để gọi Gemini. Model có thể đổi qua `GEMINI_MODEL`, phải hỗ trợ JSON theo schema. Tham khảo [Gemini 3.1 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite) và [structured outputs](https://ai.google.dev/gemini-api/docs/generate-content/structured-output). Hạn mức, phí và quyền model phụ thuộc dự án Google; chưa xác minh tài khoản của người dùng.

## Chức năng

- Cửa sổ chat cố định góc dưới bên phải trên các trang dùng bố cục chung. Bấm nút chat để mở; nút **−** hoặc Escape để thu nhỏ; **↗** mở trang đầy đủ. Giữ nội dung khi thu nhỏ/mở lại trong cùng trang; chuyển trang hoặc tải lại sẽ bắt đầu hội thoại mới. Khung chỉ tải khi mở lần đầu và co theo kích thước màn hình. “Xóa trò chuyện” tạo mã hội thoại mới, nên câu tiếp theo không dùng ngữ cảnh cũ.
- Trang `/chat`: gợi ý câu hỏi, trạng thái chờ, nguồn tham khảo, xóa trò chuyện trên màn hình, thông báo khi thiếu khóa/lỗi API.
- Gemini trả lời từ các mục tri thức được chọn. Máy chủ kiểm tra cấu trúc và ID nguồn; URL do danh mục nguồn cung cấp, không lấy link AI tự tạo.
- Giá/tồn đọc trực tiếp từ cơ sở dữ liệu, ưu tiên mã SKU chính xác. Chỉ hiển thị sản phẩm đang bán; dữ liệu demo có nhãn rõ. Chưa ánh xạ tài liệu hãng với SKU cửa hàng.
- Tra cứu `SUNI-123`, `SUNI 123`, `đơn 123`: truy vấn luôn gắn email trong phiên, kể cả với tài khoản admin. Không tin danh tính tự khai trong tin nhắn.
- Sai chủ đơn và đơn không tồn tại trả cùng thông báo. Phiên bị thu hồi yêu cầu đăng nhập lại. Tổng tiền dùng giá/phí đã lưu trên đơn; hoàn thành giao không đồng nghĩa đối soát COD.
- Chỉ đọc dữ liệu. Yêu cầu hủy đơn nhận hướng dẫn sang Đơn của tôi, không thay đổi trạng thái/tồn kho.

## Luồng dữ liệu

`POST /chat/messages` nhận JSON `{ "message": "..." }`, tối đa 1.500 ký tự và có CSRF. Khách ẩn danh có thể hỏi thông tin công khai; đơn cá nhân yêu cầu đăng nhập. Kết quả gồm `status`, `answer`, `sources`.

Máy chủ phân luồng đơn và yêu cầu giá/tồn độc lập để trả trực tiếp. Trong luồng tư vấn mua hàng, máy chủ bổ sung tối đa 40 sản phẩm công khai đang bật trong danh mục (kể cả mặt hàng hết tồn) vào ngữ cảnh, kèm giá, tồn, mô tả và nguồn trang sản phẩm. Dữ liệu được đọc lại mỗi lượt; mặt hàng vượt ngân sách/hết tồn được đánh dấu rõ. Không gán thông số hãng cho SKU demo. Với kiến thức, tìm tối đa 12 mục bằng từ khóa tiếng Việt có bỏ dấu, cộng các quy tắc phạm vi, rồi gọi Gemini. API dùng HTTPS `generateContent`, header `x-goog-api-key` và đầu ra JSON theo schema; xem [API reference](https://ai.google.dev/api/generate-content).

Gemini nhận câu hỏi kiến thức hiện tại, tối đa 6 lượt tư vấn thành công trước đó, nhu cầu đã nêu, hướng dẫn và các mục tri thức được chọn; khi tư vấn mua hàng còn nhận danh mục công khai vừa tra. Cách pha, số người, ngân sách và loại hạt được trích từ lời khách, không lấy từ suy đoán của AI. Chỉ hỏi thông tin còn thiếu; đủ nhu cầu thì đưa lựa chọn có căn cứ hoặc nói rõ thiếu thông số để xác nhận phù hợp. Lịch sử không phải nguồn chứng minh thông số sản phẩm. Không gửi email phiên, địa chỉ hoặc lịch sử đơn từ cơ sở dữ liệu. Các mẫu email/số liên hệ phổ biến trong câu hỏi được ẩn, nhưng không thể phát hiện mọi thông tin riêng người dùng tự nhập. Không nên nhập dữ liệu cá nhân vào câu hỏi kiến thức.

Chỉ `knowledge.v1.json` và `sources.v1.json` được đóng gói từ thư mục tri thức vào JAR. Không nạp `evaluation/`, đáp án hoặc fixture vào prompt. Nguồn nội bộ có trang đọc `/chat/sources/{id}`; không mở cả thư mục repo ra web. Nội dung câu hỏi/câu trả lời được hiển thị bằng `textContent`, không chạy HTML do người dùng hoặc AI tạo.

## Giới hạn hiện tại

- Có ngữ cảnh tối đa 6 lượt tư vấn, giữ trong session máy chủ (hết hạn sau 30 phút không hoạt động), tối đa 4 hội thoại mỗi session. Mỗi trang/khung chat có mã riêng; mã do trình duyệt gửi không cấp quyền truy cập session khác. Đổi tài khoản làm mất ngữ cảnh trước đó; chuyển sang tra đơn hoặc yêu cầu bị từ chối sẽ xóa ngữ cảnh tư vấn. Nội dung đơn, phản hồi lỗi và kết quả tra giá độc lập không được lưu làm lịch sử Gemini. Chưa có lưu lịch sử lâu dài hoặc streaming. Nội dung xa hơn 6 lượt có thể cần nhắc lại; bộ trích nhu cầu chỉ hỗ trợ một số cách diễn đạt phổ biến, không bảo đảm hiểu mọi câu phủ định hoặc thay đổi chủ đề.
- Truy xuất từ khóa, chưa có embedding/vector database. Tìm tên xét tối đa 200 sản phẩm đang bán, trả tối đa 4 kết quả; SKU chính xác được truy vấn trực tiếp.
- Kiểm tra ID nguồn không chứng minh mọi phát biểu AI đều đúng. Đã chấm sơ bộ phản hồi thực tế; cần người đánh giá độc lập rà lại. Phản hồi sai cấu trúc/nguồn hoặc bị cắt sẽ trả thông báo chưa trả lời được.
- Giới hạn 10 tin/phút/IP, 60 tin/phút/toàn tiến trình và 4 lượt Gemini đồng thời. Chưa chia sẻ giới hạn giữa nhiều máy; IP là đầu kết nối, không tin header tự khai. Khi dùng reverse proxy cần cấu hình giới hạn tại proxy.
- Timeout kết nối 5 giây, phản hồi 25 giây. Bản 1.0.0 chỉ thử lại một lần khi Google trả 503 trong vòng 5 giây đầu, chờ 250 ms; không thử lại lỗi quota, khóa hoặc timeout. Không ghi nội dung chat, khóa hay lỗi thô từ Gemini vào log ứng dụng.

## Chẩn đoán kết nối Gemini

Dòng **AI đã được bật** chỉ xác nhận ứng dụng nhận cờ bật và khóa không rỗng; không phải phép kiểm tra API đang khỏe. Khi lỗi, log Gemini chỉ ghi mã phân loại và HTTP status nếu có, không ghi khóa, câu hỏi hoặc phản hồi lỗi thô của Google.

| Mã log | Kiểm tra |
| --- | --- |
| `KEY_REJECTED` | Khóa hoặc quyền truy cập Google bị từ chối. Kiểm tra khóa trong Terminal/Run Configuration rồi khởi động lại. |
| `MODEL_UNAVAILABLE` | Kiểm tra tên `GEMINI_MODEL` và quyền truy cập mô hình. |
| `INVALID_REQUEST` | Kiểm tra định dạng yêu cầu và khả năng JSON schema của mô hình. |
| `QUOTA_EXCEEDED` | Kiểm tra quota và tốc độ yêu cầu trong Google AI Studio. Không tự gửi lại liên tục. |
| `NETWORK` | Kiểm tra kết nối mạng hoặc thời gian chờ tới Google. |
| `INVALID_RESPONSE` | Phản hồi bị cắt, sai cấu trúc hoặc nguồn không hợp lệ; không hiển thị đáp án đó. |
| `BUSY` | Hết lượt xử lý đồng thời hoặc Google tiếp tục trả 503 sau tối đa một lần thử lại có giới hạn; thông báo AI đang bận. |
| `PROVIDER_ERROR` | Lỗi khác từ nhà cung cấp; thử lại sau. |

Thông báo trên giao diện phân biệt lỗi cấu hình, hạn mức, kết nối và hệ thống bận. Khung chat nhúng chỉ được mở từ cùng origin (`X-Frame-Options: SAMEORIGIN`); CSRF và kiểm tra chủ đơn vẫn được áp dụng.

## Lịch sử kiểm thử trước bản 1.0.0

```bash
python3 scripts/verify-chatbot-baseline.py
mvn -Ddebug=false test
mvn -DskipTests package
```

Đã chạy **85 kiểm thử trên H2, 0 lỗi**, gồm 24 ca cho chatbot. Có kiểm tra CSRF, giới hạn đầu vào, quyền chủ đơn (kể cả admin), phiên thu hồi, không ghi dữ liệu, giá/tồn hiện hành, nguồn giả, thiếu nguồn, phản hồi bị cắt, lỗi quota, thiếu khóa, phân loại lỗi Google, giải phóng lượt gọi sau lỗi và trang chat nhúng. Đã sửa phân luồng để “đánh giá” không bị hiểu nhầm thành hỏi giá. Gemini được giả lập trong bộ tự động. Bằng chứng mới: [test-summary.json](verification/chatbot-widget/test-summary.json). Chưa chạy lại bộ kiểm thử trên MySQL trong bước này.

Thử API thật sau khi khởi động lại ngày 13/10/2026: AeroPress trả cỡ Standard kèm nguồn (2,28 giây), French press trả 30 g cà phê/350 g nước kèm nguồn (3,08 giây), câu bóng đá trả ngoài phạm vi (2,17 giây), tra cứu đơn chưa đăng nhập yêu cầu xác thực (0,01 giây). Xem [live-smoke.json](verification/chatbot-widget/live-smoke.json). Trước khi sửa và khởi động lại, ba câu gọi Gemini đều thất bại; không kết luận nguyên nhân duy nhất vì cấu hình chạy cũng có thể đã thay đổi.

Kiểm tra giao diện thực tế trong Safari: mở khung chat từ trang chủ, gửi câu AeroPress, nhận đáp án kèm link nguồn, thu nhỏ và mở lại vẫn giữ hội thoại. Xem [ui-check.md](verification/chatbot-widget/ui-check.md). Chưa kiểm tra trực tiếp trên điện thoại. Các lần thử ngắn này xác nhận luồng cơ bản đang hoạt động, chưa đủ chứng minh độ ổn định dài hạn.

Mốc 55 mục/30 câu và tag `chatbot-eval-v1` giữ nguyên; kiểm tra SHA-256 vẫn đạt. Sau khi có khóa, chạy bộ đã khóa theo [PROTOCOL.md](../evaluation/chatbot/v1/PROTOCOL.md), 3 lượt/câu, lưu và chấm kết quả ngoài `evaluation/chatbot/v1/`. Chưa có điểm chất lượng AI; kiểm thử phần mềm không thay thế điểm bộ 30 câu.

## Sửa tư vấn nhiều lượt — 13/10/2026

Kiểm thử sau sửa: **95/95 đạt trên H2, 0 lỗi**, gồm kiểm tra tách hội thoại/tài khoản, không gửi đơn hàng vào lịch sử, ẩn thông tin liên hệ, cập nhật giá hiện hành, giới hạn lịch sử và lỗi 503. Bằng chứng: [test-summary.json](verification/chatbot-conversation/test-summary.json).

Nguyên nhân hỏi lặp: API cũ chỉ gửi câu hiện tại, còn prompt yêu cầu hỏi cách pha/số người/ngân sách khi tư vấn chung. Đã bổ sung lịch sử do máy chủ giữ, nhu cầu trích từ lời khách và danh mục hiện hành. Câu hỏi làm rõ không còn gắn nguồn quy định phạm vi không liên quan. Schema đầu ra giới hạn ID nguồn bằng danh sách được cấp; nguồn vẫn được kiểm tra ở máy chủ. Nhãn dữ liệu demo được thêm bằng mã xử lý, không phụ thuộc AI có nhớ nhắc hay không.

Thử cùng chuỗi “Nên mua gì cho người bắt đầu?” → “pha máy, 2 người, 2 triệu” → “arabica” trong 3 hội thoại độc lập với khóa mới, trên cổng kiểm tra 8086 và cơ sở dữ liệu demo riêng: 2 hội thoại hoàn tất cả 3 lượt; hội thoại còn lại có lỗi Google HTTP 503 ở lượt đầu, hai lượt sau vẫn giữ cách pha/số người/ngân sách. Không có câu hỏi lặp lại nhu cầu ở lượt “arabica”. Bản sửa tiếp theo bổ sung phân loại 503 và nhận diện tư vấn từ cách pha + ngân sách ngay cả khi lượt trước thất bại. Đây là kiểm tra hồi quy ngoài bộ 30 câu đã khóa; không phải điểm đánh giá toàn bộ chatbot. Xem bằng chứng trong [chatbot-conversation](verification/chatbot-conversation/live-smoke.json).

Giới hạn quan sát: lời tư vấn vẫn có thể chọn nguồn chính sách không cần thiết hoặc diễn đạt mức độ phù hợp chưa đủ chặt. Danh mục đang là sản phẩm mẫu, thiếu thông số để khẳng định máy phù hợp pha Arabica/cho 2 người. Muốn tư vấn mua thật chính xác phải bổ sung dữ liệu sản phẩm thực và tiếp tục đánh giá; không thể sửa bằng prompt để bịa ra thông số còn thiếu.

Kiểm tra trên cổng 8080 sau lần khởi động lại đầu: 7/7 yêu cầu có phản hồi, giữ đúng nhu cầu và ngân sách mới. Tuy nhiên câu trả lời còn khẳng định máy demo dùng được Arabica mà chưa có thông số; lưu tại [live-8080-before-guard.json](verification/chatbot-conversation/live-8080-before-guard.json). Đã sửa bằng cơ chế trả lời trực tiếp từ danh mục cho nhu cầu máy demo có đủ cách pha, số người và ngân sách: nêu tối đa 2 máy còn hàng trong ngân sách, một lựa chọn vượt ngân sách kèm số tiền chênh lệch, và nói rõ thiếu thông số để chốt mua. Phần này trả `INSUFFICIENT`, không nhờ Gemini suy đoán tính tương thích. Nhu cầu mới vẫn được giữ cho lượt sau; các câu hỏi kiến thức khác tiếp tục dùng Gemini. Kiểm thử tự động xác nhận chuỗi pha máy → Arabica → giảm ngân sách không gọi AI để dựng thông số máy demo.

Xác nhận sau lần khởi động lại cuối ngày 13/10/2026 trên cổng 8080: **7/7 tình huống kiểm tra đạt**. Lượt “arabica” giữ 2 người/2 triệu, không hỏi lại và nói rõ thiếu thông số tương thích; giảm ngân sách xuống 1 triệu cập nhật đúng chênh lệch 1,5 triệu với máy mẫu B. Hội thoại mới không mang nhu cầu cũ; đơn chưa đăng nhập bị chặn; câu bóng đá trả ngoài phạm vi. Các lượt tư vấn máy demo trả trực tiếp từ DB khoảng 0,02 giây; các lượt gọi Gemini trong lần này khoảng 3,4–4 giây, không gặp lỗi API. Bằng chứng: [live-8080-final.json](verification/chatbot-conversation/live-8080-final.json). Đây chỉ là thử nghiệm ngắn trên các tình huống đã nêu, chưa chứng minh độ ổn định dài hạn.
