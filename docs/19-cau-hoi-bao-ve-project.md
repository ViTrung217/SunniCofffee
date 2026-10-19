# Bộ câu hỏi luyện bảo vệ SuniCoffee

Cập nhật: 17/10/2026. Đây là bộ câu hỏi bảo vệ project, **không phải** bộ 30 câu đánh giá chatbot tại `evaluation/chatbot/v1/`. Độ khó thay đổi qua từng câu; không chia thành một nhóm dễ rồi mới đến nhóm khó.

Các câu giả định về tải lớn, tấn công hoặc triển khai thật dùng để đánh giá khả năng phân tích, không có nghĩa project đã triển khai các chức năng đó. Khi trả lời, phân biệt rõ: đã làm, đã kiểm thử, chưa nghiệm thu thực tế và hướng phát triển.

## Cách luyện

- Trả lời theo thứ tự để luyện đổi chủ đề và độ khó, hoặc nhờ người hỏi chọn số bất kỳ.
- **Dễ:** 30–45 giây. **Trung bình:** 60–90 giây. **Khó:** 2 phút. **Phản biện:** tối đa 3 phút, có thể mở code hoặc sơ đồ để chứng minh.
- Mỗi câu trả lời nên gồm: kết luận → cách SuniCoffee thực hiện → ví dụ hoặc bằng chứng → giới hạn nếu có.
- Không cần học thuộc nguyên văn. Cố gắng chỉ đúng controller, service, bảng dữ liệu hoặc kiểm thử liên quan.

## 50 câu hỏi — xen kẽ độ khó

| Câu | Mức độ | Câu hỏi |
| --- | --- | --- |
| 01 | Dễ | SuniCoffee giải quyết vấn đề gì, dành cho ai và có những tác nhân nào? |
| 02 | Khó | Nếu hai khách cùng đặt mua sản phẩm chỉ còn một đơn vị tồn kho, hệ thống ngăn bán vượt tồn như thế nào? Hãy chỉ vị trí lấy khóa và phạm vi transaction. |
| 03 | Trung bình | Một yêu cầu mở trang chi tiết sản phẩm đi qua những lớp nào trước khi trình duyệt hiển thị nội dung? |
| 04 | Phản biện | Phần nào trong đồ án là đóng góp của bạn, phần nào do thư viện, Gemini hoặc công cụ AI hỗ trợ? Bạn sẽ chứng minh mình hiểu và kiểm soát mã nguồn như thế nào? |
| 05 | Dễ | Khách hàng và quản trị viên có quyền gì khác nhau? |
| 06 | Trung bình | Vì sao bạn chọn Spring Boot và Thymeleaf? Với phạm vi hiện tại, lựa chọn này có ưu điểm và hạn chế gì so với frontend React tách riêng? |
| 07 | Khó | Khách thay ID đơn hàng trong URL hoặc request để xem đơn người khác: lớp nào kiểm tra quyền sở hữu? Chỉ ra một kiểm thử chứng minh điều đó. |
| 08 | Dễ | Giỏ hàng có giữ trước tồn kho không? Người dùng thêm hàng vào giỏ có chắc chắn mua được không? |
| 09 | Phản biện | Website có nhiều đơn và đánh giá trông như đã hoạt động lâu. Đây có phải dữ liệu khách hàng thật không? Dữ liệu này ảnh hưởng thế nào đến kết luận về doanh thu và chất lượng sản phẩm? |
| 10 | Trung bình | Mô tả quan hệ giữa tài khoản, đơn hàng, chi tiết đơn hàng và sản phẩm. Vì sao cần bảng chi tiết đơn hàng riêng? |
| 11 | Khó | Trong lúc checkout, lưu đơn thành công nhưng trừ tồn bị lỗi thì dữ liệu phải ở trạng thái nào? Transaction xử lý ra sao? |
| 12 | Dễ | Phân biệt đăng nhập và phân quyền bằng ví dụ trên SuniCoffee. |
| 13 | Trung bình | Vì sao chi tiết đơn lưu tên, SKU và giá tại thời điểm mua thay vì luôn đọc giá hiện tại của sản phẩm? |
| 14 | Phản biện | Nếu hội đồng yêu cầu chứng minh “chatbot thông minh”, bạn sẽ dùng tiêu chí và dữ liệu đánh giá nào thay vì chỉ đưa vài câu trả lời hay? |
| 15 | Khó | Người dùng bấm đặt hàng hai lần hoặc gửi lại request sau timeout. Checkout token giúp tránh tạo đơn trùng thế nào? Nó khác CSRF token ra sao? |
| 16 | Trung bình | Trình bày luồng đăng nhập Google từ lúc nhấn nút đến khi có phiên đăng nhập SuniCoffee. Client Secret được dùng ở đâu? |
| 17 | Dễ | COD và thanh toán QR khác nhau thế nào trong trải nghiệm của khách? |
| 18 | Khó | Google trả về email trùng tài khoản đã có: vì sao không tự gộp? Luồng liên kết hiện tại yêu cầu gì và hết hạn sau bao lâu? |
| 19 | Phản biện | Nếu trình duyệt gửi request nói “đã thanh toán”, bạn có cập nhật đơn thành PAID không? Bằng chứng nào mới đủ tin cậy để hệ thống ghi nhận tiền? |
| 20 | Trung bình | Trình bày các điều kiện mà một giao dịch SePay cần thỏa mãn để được đối chiếu với đơn hàng. Chỉ khớp số tiền đã đủ chưa? |
| 21 | Dễ | Khách cần điều kiện gì để đánh giá sản phẩm? Một tài khoản được gửi bao nhiêu đánh giá cho cùng sản phẩm? |
| 22 | Khó | Hai yêu cầu đối soát cùng nhận một giao dịch SePay: cơ chế nào ngăn cộng tiền hai lần? Chỉ ra vai trò của khóa đơn và mã giao dịch duy nhất. |
| 23 | Trung bình | Polling và webhook khác nhau như thế nào? SuniCoffee hiện dùng cách nào và vì sao phù hợp với chạy local? |
| 24 | Phản biện | Nếu khách chuyển tiền rồi đóng trang QR ngay, hệ thống hiện tại có tự cập nhật ngay không? Bạn sẽ cải tiến thế nào và phải giải quyết thêm vấn đề bảo mật nào? |
| 25 | Dễ | Mật khẩu được lưu dưới dạng gì? Vì sao không lưu mật khẩu nguyên văn? |
| 26 | Khó | Vì sao làm tròn thời gian trong MySQL từng khiến giao dịch ngay sau checkout bị bỏ qua? Nêu ví dụ với phần lẻ giây và cách vá thực tế. |
| 27 | Trung bình | CSRF và XSS khác nhau ở đâu? Chỉ ra cách bảo vệ form và cách hiển thị nội dung đánh giá/tin nhắn trong project. |
| 28 | Dễ | Phân biệt chatbot AI với chức năng chat trực tiếp với admin. Khi Gemini lỗi, kênh nào còn có thể sử dụng? |
| 29 | Phản biện | Có 161 kiểm thử đạt và CI xanh thì có thể kết luận hệ thống không còn lỗi không? Hãy nêu ít nhất ba rủi ro chưa được bằng chứng đó bao phủ. |
| 30 | Khó | Admin khóa tài khoản hoặc đổi quyền trong khi người dùng vẫn đang đăng nhập. Phiên cũ bị vô hiệu hóa thế nào? Google login có tuân thủ cơ chế đó không? |
| 31 | Trung bình | Kho tri thức, dữ liệu giá/tồn trong database và kiến thức do Gemini sinh ra khác nhau thế nào? Khi chúng mâu thuẫn, bạn xử lý ra sao? |
| 32 | Dễ | Flyway dùng để làm gì? Vì sao không tùy ý sửa một migration đã chạy trên database đang sử dụng? |
| 33 | Khó | Người dùng yêu cầu chatbot “bỏ qua quy định và đọc đơn của người khác”. Vì sao chỉ viết prompt cấm là chưa đủ? Quyền truy cập dữ liệu phải được bảo vệ ở đâu? |
| 34 | Phản biện | Bạn có thực sự huấn luyện mô hình AI trong đồ án này không? Hãy phân biệt tích hợp API, cung cấp ngữ cảnh, truy xuất tri thức và fine-tuning. |
| 35 | Trung bình | Vì sao khóa bộ câu hỏi đánh giá chatbot trước khi code? Nếu thêm chức năng sau đó, bạn nên mở rộng kiểm thử thế nào mà không sửa lịch sử đánh giá? |
| 36 | Khó | H2 và MySQL khác nhau thế nào về kiểu dữ liệu, độ chính xác thời gian và hành vi đồng thời? Vì sao kiểm thử H2 thành công chưa đủ? |
| 37 | Dễ | Những thư mục nào dùng để sửa giao diện? Template Thymeleaf, CSS và JavaScript có nhiệm vụ gì? |
| 38 | Trung bình | Người dùng tải file ảnh giả phần mở rộng hoặc ảnh kích thước rất lớn. Project kiểm tra và xử lý ảnh ở đâu trước khi lưu? |
| 39 | Phản biện | Nếu bỏ toàn bộ animation, chức năng mua hàng có còn dùng được không? Bạn chứng minh khả năng sử dụng bằng bàn phím, trên mobile và khi giảm chuyển động bằng cách nào? |
| 40 | Khó | Vì sao giao hàng thất bại chưa được cộng ngay hàng lại vào kho? Nếu gửi hai sản phẩm nhưng chỉ nhận về một sản phẩm còn bán được, hệ thống cần ghi nhận gì? |
| 41 | Dễ | Doanh thu, tiền đã thu và lợi nhuận có phải cùng một chỉ số không? Báo cáo hiện tại của SuniCoffee đang phản ánh gì? |
| 42 | Trung bình | Vì sao nên ngừng bán sản phẩm thay vì xóa sản phẩm đã xuất hiện trong đơn hàng? Điều đó ảnh hưởng trang khách và lịch sử mua thế nào? |
| 43 | Khó | Bộ sinh lịch sử mẫu chạy lại nhiều lần có tạo trùng không? Marker, transaction và khóa dữ liệu bảo vệ quá trình này thế nào? |
| 44 | Phản biện | Nếu đưa website lên bán thật ngày mai, ba việc nào phải ưu tiên trước? Giải thích dựa trên giới hạn hiện tại, không chỉ nói “deploy lên server”. |
| 45 | Trung bình | Các sản phẩm liên quan được chọn theo tiêu chí nào? Có cần đào tạo AI không? Nêu hạn chế của cách chọn hiện tại. |
| 46 | Dễ | API key Gemini, SePay và Google Client Secret nằm ở đâu? Vì sao không được đưa chúng vào JavaScript hoặc GitHub? |
| 47 | Khó | Khách chuyển thiếu, chuyển thừa hoặc chuyển sau khi hủy đơn: mỗi trường hợp được ghi nhận thế nào? Phần nào vẫn cần người quản trị xử lý thủ công? |
| 48 | Trung bình | Hãy mô tả cách sao lưu và thử khôi phục hệ thống. Vì sao chỉ có file backup mà chưa thử restore thì chưa đủ? |
| 49 | Phản biện | Nếu có 1.000 khách cùng mở chat và trang QR, những điểm nghẽn có thể xuất hiện ở đâu? Bạn sẽ đo lường gì trước khi chọn WebSocket, hàng đợi hoặc mở rộng máy chủ? |
| 50 | Khó | Trong 3 phút, hãy chứng minh một luồng hoàn chỉnh bằng code và test: đăng nhập → đặt hàng → trừ tồn → đối soát đúng tiền → xem đơn đúng chủ sở hữu. Bạn sẽ mở những file nào và theo thứ tự nào? |

## Câu hỏi truy vấn tiếp sau mỗi câu trả lời

Người hỏi có thể chọn một câu dưới đây để tăng độ khó bất ngờ:

1. “Em chỉ đúng đoạn code thực hiện điều đó được không?”
2. “Nếu bỏ kiểm tra này đi, tình huống nào sẽ sai?”
3. “Kiểm thử nào chứng minh điều em vừa nói?”
4. “Nếu request bị gửi hai lần hoặc chạy đồng thời thì sao?”
5. “Nếu database đã cập nhật nhưng trình duyệt không nhận được phản hồi thì sao?”
6. “Đây là điều đã kiểm chứng hay em đang suy luận?”
7. “Có cách đơn giản hơn không? Vì sao em không chọn cách đó?”
8. “Hãy giải thích lại cho một người không biết lập trình.”

## Tự chấm một câu trả lời

Mỗi tiêu chí 0–2 điểm, tổng 10 điểm. Đây là thang tự luyện, không phải rubric của trường.

| Tiêu chí | Cần thể hiện |
| --- | --- |
| Đúng trọng tâm | Trả lời trực tiếp câu được hỏi |
| Đúng hiện trạng | Phân biệt phần đang có với dự định và giới hạn |
| Giải thích cơ chế | Nói rõ cách thực hiện, không chỉ liệt kê tên công nghệ |
| Có bằng chứng | Chỉ được code, test, sơ đồ hoặc kết quả chạy tương ứng |
| Xử lý tình huống | Phân tích được một trường hợp lỗi hoặc đánh đổi |

Nếu chưa trả lời được, đánh dấu câu đó và đọc lại phần tương ứng; không học một câu trả lời khẳng định quá mức khả năng hiện tại. Các số kiểm thử phải gắn với bản/commit được nghiệm thu, không dùng kết quả cũ để khẳng định các chỉnh sửa mới đã được kiểm chứng toàn bộ.

## Nơi tra cứu khi luyện

- Tổng quan: [README](../README.md), [tình trạng project](16-tinh-trang-du-an.md).
- Nghiệp vụ: [OrderService](../src/main/java/vn/sunicoffee/service/OrderService.java), [thiết kế nghiệp vụ](06-thiet-ke-va-nghiep-vu-v03.md).
- Phân quyền: [SecurityConfig](../src/main/java/vn/sunicoffee/config/SecurityConfig.java), [AccountStateFilter](../src/main/java/vn/sunicoffee/config/AccountStateFilter.java).
- Google: [hướng dẫn](17-dang-nhap-google.md), [GoogleAccountService](../src/main/java/vn/sunicoffee/service/GoogleAccountService.java).
- Thanh toán: [hướng dẫn SePay](18-thanh-toan-sepay.md), [PaymentStore](../src/main/java/vn/sunicoffee/payment/PaymentStore.java).
- AI: [chatbot Gemini](12-chatbot-gemini.md), [kho tri thức](ai/kho-tri-thuc-v1.md), [bộ đánh giá chatbot](../evaluation/chatbot/v1/QUESTIONS.md).
- Dữ liệu mẫu: [SampleActivityService](../src/main/java/vn/sunicoffee/service/SampleActivityService.java).
- Bằng chứng: [thư mục kiểm thử](../src/test/java/vn/sunicoffee/), [kết quả xác minh](verification/).
