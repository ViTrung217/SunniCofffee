# Rà soát và nghiệm thu SuniCoffee 1.0.0 — bản local

Ngày: 13/10/2026. Phạm vi được người thực hiện đồ án xác nhận: **chạy local để bàn giao và bảo vệ, không deploy**. Chưa có danh mục, giá/tồn và chính sách được cửa hàng xác nhận. Bản 1.0.0 là phiên bản phần mềm, không biến dữ liệu mô phỏng thành dữ liệu kinh doanh thật.

## Kết luận rà soát

Các luồng chính trong phạm vi đồ án đã có mã và kiểm thử: tài khoản/phân quyền, danh mục/sản phẩm/ảnh, giỏ hàng, checkout COD, tồn kho, vận chuyển thủ công, giao thất bại/giao lại/hoàn hàng, đối soát, doanh thu và chatbot. Không còn mặc định tự tạo sản phẩm và tài khoản demo khi khởi động bản bàn giao.

| Hạng mục phát hiện | Xử lý trong bản cuối |
| --- | --- |
| Mặc định chạy demo, tự nạp dữ liệu | Chuyển mặc định sang `local`, không tự seed; giữ DB và khóa hiện có |
| Tài liệu chính còn mô tả 0.3 | Nâng phiên bản Maven/JAR 1.0.0, viết lại README và hướng dẫn chạy local |
| Không có lệnh chạy bản đóng gói rõ ràng | `bash scripts/run-local.sh`, bind 127.0.0.1; không cần máy chủ ngoài |
| Dữ liệu mẫu có thể mất nhãn khi đổi profile | Nhãn dựa thêm trên SKU DEMO đang bán; nội dung ghi dữ liệu phục vụ đồ án, chưa được cửa hàng xác nhận |
| Chưa có mẫu thu thập hàng thật | Mẫu JSON, quy trình nhập qua quản trị và validator chỉ đọc, không ghi DB |
| Chưa chạy đủ đánh giá AI đã khóa | Có bộ thu thập 30 × 3, phản hồi nguyên văn, nguồn, trace, hash cấu hình và phiếu chấm riêng |
| Gemini thỉnh thoảng trả 503 | Chỉ thử lại một lần cho 503 trả nhanh; vẫn giới hạn thời gian và số yêu cầu |
| Câu cam kết đổi mới bị trả lời ngoài phạm vi chung | Trả lời rõ chưa có chính sách cửa hàng được xác minh; không hứa bảo hành/đổi trả |
| Chưa có trạng thái kiểm tra ứng dụng | Health/readiness có kiểm tra DB; chỉ công khai trạng thái, chặn các endpoint quản trị Actuator |
| CI chưa kiểm tra khóa câu hỏi | Thêm kiểm tra SHA-256 baseline trước kiểm thử H2/MySQL |

Profile `demo` vẫn giữ để tái tạo dữ liệu minh họa khi chủ động yêu cầu. Sự tồn tại của công cụ tạo dữ liệu không làm bản bàn giao thành bản thử nghiệm; phải công bố đúng nguồn dữ liệu được sử dụng. Những tài khoản demo đã có không tự bị xóa hoặc đổi mật khẩu. Khi cần quản trị riêng, tạo tài khoản mới qua Quản trị rồi khóa tài khoản công khai.

## Bằng chứng kiểm thử

| Kiểm tra | Kết quả / bằng chứng |
| --- | --- |
| Build sạch `mvn -B -Ddebug=false clean verify` | 99 kiểm thử, không lỗi trên H2; [h2-tests.json](verification/release-1.0.0/h2-tests.json) |
| Cùng bộ kiểm thử trên MySQL 8.0.44 riêng | [mysql-tests.json](verification/release-1.0.0/mysql-tests.json); không dùng DB của người dùng |
| Kho tri thức và bộ đánh giá | 55 mục, 14 nguồn, 30 câu chia 8/8/9/5; 10 khóa SHA-256 còn nguyên |
| JAR bàn giao | [artifact.json](verification/release-1.0.0/artifact.json); kiểm tra không đóng gói khóa cục bộ, fixture hoặc runner đánh giá |
| Khởi động bản local | [local-smoke.json](verification/release-1.0.0/local-smoke.json); DB kiểm tra riêng, không chiếm tệp đang dùng ở 8080 |
| Giao dịch và backup/restore bổ sung | [bằng chứng trước khi chốt phạm vi local](verification/release-1.0.0/earlier-profile-checks/README.md); đơn giả lập COMPLETED, 240.000 VND, đã đối soát COD, tồn 8, 2 biến động kho, 5 sự kiện khớp sau restore |

Các kiểm tra bổ sung với profile máy chủ được lưu lịch sử, nhưng profile đó và mẫu deploy **không nằm trong bản cuối**. Không dùng các số 106/108 kiểm thử hoặc hash JAR trước đó làm số liệu bản local cuối cùng. Giao dịch/backup nói trên dùng dữ liệu tổng hợp ở MySQL riêng; chưa chạy quy trình khôi phục trên bản dữ liệu cá nhân của người dùng.

## Đánh giá chatbot

Mô hình: `gemini-3.1-flash-lite`; prompt/kho tri thức/mã nguồn ghi hash trong metadata. Thu thập 30 câu × 3 lần, hội thoại mới từng lượt, backend fixture riêng. Không đưa đáp án hoặc toàn bộ fixture vào Gemini.

| Nhóm | Điểm chấm sơ bộ | Tỷ lệ điểm |
| --- | --- | --- |
| Tư vấn sản phẩm | 45/48 | 93,75% |
| Kiến thức pha chế | 48/48 | 100% |
| Dữ liệu đơn hàng/xác thực | 54/54 | 100% |
| Ngoài phạm vi | 30/30 | 100% |
| Tổng | **177/180** | **98,33%** |

87/90 lượt đạt đủ 2 điểm, tức **96,67% câu trả lời đầy đủ**, khác với tỷ lệ điểm 98,33%. Ba lượt Q01 thiếu hướng dẫn chuẩn bị nước nóng riêng, dù trả lời đúng rằng Picopresso không tự đun; mỗi lượt chấm 1 điểm. Không ghi nhận lỗi nghiêm trọng theo phiếu chấm của đợt này. Lỗi dữ liệu đơn Q25 là ca cố ý mô phỏng dịch vụ không khả dụng, không phải lỗi nhà cung cấp AI.

**Người chấm: Codex hỗ trợ phát triển, chưa phải con người độc lập.** Đã đọc phản hồi, nguồn và dấu vết tra cứu theo nghĩa. Kết quả đạt các ngưỡng đã khóa dưới cách chấm này, cần người hướng dẫn/người đánh giá rà lại trước khi ghi là nghiệm thu chính thức trong báo cáo. Đây là bộ cố định đã biết trước, không phải đánh giá mù; kết quả không bảo đảm mọi câu ngoài bộ hoặc dịch vụ Gemini luôn ổn định.

Bộ thu thập kiểm tra tầng dịch vụ với danh tính/đơn fixture; việc kiểm tra phiên HTTP, CSRF và truy vấn DB đúng chủ nằm trong kiểm thử tích hợp riêng. Không gọi việc giả lập backend là đã thử đơn hàng thật.

- [Kết quả cuối và cấu hình](../evaluation/results/2026-10-13-release-1.0.0-final/metadata.json)
- [90 phản hồi nguyên văn và trace](../evaluation/results/2026-10-13-release-1.0.0-final/responses.json)
- [Phiếu chấm từng lượt](../evaluation/results/2026-10-13-release-1.0.0-final/grading.json), [tổng hợp điểm](../evaluation/results/2026-10-13-release-1.0.0-final/summary.json)
- [Lần đầu trước sửa](../evaluation/results/2026-10-13-release-1.0.0/summary.json): 162/180, giữ lại để không lựa chọn chỉ công bố kết quả đẹp; có 5 lượt lỗi 503 và 3 lượt trả lời cam kết đổi mới quá chung.

## Phần cần chốt trước buổi bảo vệ

1. **Dữ liệu:** có thể bảo vệ bằng dữ liệu mô phỏng nếu được người hướng dẫn chấp nhận; ghi rõ trong báo cáo. Nếu yêu cầu khảo sát cửa hàng thật, còn thiếu người xác nhận, danh mục, giá, kiểm kê và chính sách. Đã chuẩn bị [mẫu thu thập](../data/catalog/README.md), không tự tạo kết quả khảo sát.
2. **Báo cáo theo mẫu khoa:** dự án có thiết kế, tài liệu vận hành, bằng chứng kiểm thử và đề cương thuyết trình; chưa có mẫu bìa/bố cục/biểu mẫu nghiệm thu của khoa để chốt bản nộp. Không coi README là báo cáo tốt nghiệp đã hoàn tất.
3. **Chấm AI độc lập:** người hướng dẫn rà phiếu chấm 90 lượt, ghi người chấm và lý do khác biệt nếu có. Không sửa v1 để phù hợp phản hồi.
4. **Diễn tập trên máy bảo vệ:** dùng JAR local, tài khoản riêng, kiểm tra Internet/hạn mức Gemini, đăng nhập → đặt COD → quản trị → tra cứu chatbot. Dự phòng trường hợp AI gián đoạn bằng chức năng bán hàng và tra cứu có sẵn; không trình chiếu phản hồi lưu như thể vừa sinh trực tiếp.
5. **Kiểm tra giao diện trên thiết bị sử dụng:** HTML/widget có kiểm thử tích hợp; vị trí nút góc phải đã quan sát trên Safari. Chưa tuyên bố nghiệm thu toàn bộ thiết bị di động hoặc mọi trình duyệt. Cần diễn tập trên kích thước màn hình thực tế khi bảo vệ.

Không cần triển khai Internet để hoàn thành phạm vi local. Thanh toán trực tuyến, API hãng vận chuyển, quên mật khẩu qua email, khuyến mại, đa chi nhánh, nhà cung cấp/giá vốn/lợi nhuận là hướng phát triển ngoài phạm vi đã chốt, không phải những chức năng đã được triển khai.

## Sử dụng bản mới trên máy hiện tại

Đã sao lưu tệp H2 khi ứng dụng dừng, kiểm tra hash bản sao và khởi động bản local 1.0.0 tại http://localhost:8080. Danh mục 12 sản phẩm, giá và tồn công khai khớp trước/sau; khóa hiện có tiếp tục dùng. Chuỗi “người bắt đầu” → “pha máy, 2 người, 2 triệu” → “arabica” đã kiểm tra lại trên cổng 8080: giữ nhu cầu, không hỏi lặp lại, không khẳng định thông số chưa có căn cứ. Xem [live-8080-local.json](verification/release-1.0.0/live-8080-local.json).

Phiên hiện tại chạy nền; PID ở `.runtime/local-app.pid`, log ở `.runtime/local-app.log`. Bản sao DB trước nâng cấp nằm trong `.backups/` và bị loại khỏi Git. Khi chạy lại bằng Terminal, dừng đúng tiến trình Java hiện tại rồi dùng `bash scripts/run-local.sh`; sau các lần sửa mã cần `mvn -B verify` trước để tạo JAR mới. Nếu phiên chạy ở Terminal thì Ctrl+C dừng ứng dụng. Không chạy hai phiên cùng tệp H2.
