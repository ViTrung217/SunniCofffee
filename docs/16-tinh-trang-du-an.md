# Bảng tình trạng SuniCoffee — 15/10/2026

Phạm vi: bàn giao và bảo vệ đồ án chạy local; QR SePay chạy local; chưa có bằng chứng nghiệm thu giao dịch thật. “Đã có” nghĩa là có mã và luồng sử dụng, không khẳng định không còn lỗi trong mọi tình huống. Dữ liệu sản phẩm phục vụ đồ án đã được chủ dự án xác nhận sử dụng ngày 15/10/2026.

| Nhóm | Chức năng | Trạng thái | Giới hạn / việc còn lại |
| --- | --- | --- | --- |
| Tài khoản | Đăng ký, đăng nhập email/mật khẩu hoặc Google, đăng xuất, hồ sơ, đổi mật khẩu | Đã có | Quên mật khẩu qua email và xác minh email chưa có |
| Phân quyền | Khách/admin, khóa tài khoản, thu hồi phiên khi đổi quyền/mật khẩu | Đã có | Có CSRF và giới hạn lần đăng nhập; chưa đánh giá bảo mật độc lập |
| Danh mục | Quản lý nhóm, tìm theo tên/SKU, lọc nhóm, phân trang | Đã có | Chưa tìm kiếm ngữ nghĩa hoặc gợi ý cá nhân hóa |
| Sản phẩm | Thêm/sửa/ngừng bán, đơn vị tính, giá, ảnh | Đã có | Tối đa 6 ảnh tải lên, chọn ảnh đại diện và xóa từng ảnh; chưa có biến thể chung một sản phẩm |
| Mô tả | 36 mô tả có giới thiệu, quy cách, công dụng và lưu ý | Đã bổ sung đợt này | Phân biệt quy cách giả lập và thông số chưa xác minh |
| Dữ liệu danh mục | 36 SKU: 10 cà phê, 6 máy, 12 dụng cụ, 8 phụ kiện | Đã bổ sung đợt này | Chủ dự án chấp thuận bộ dữ liệu mặc định ngày 15/10/2026; đủ 36 ảnh riêng, ảnh AI là minh họa |
| Nhập dữ liệu | Seed DB trống; nạp bổ sung mẫu, không reset tồn/giá cũ | Đã có | Mẫu JSON hàng thật + validator có sẵn; chưa có màn hình import hàng loạt CSV/Excel |
| Giỏ hàng | Thêm, cập nhật, xóa; giỏ theo tài khoản | Đã có | Kiểm tra tồn khi đặt; không giữ hàng vô thời hạn trong giỏ |
| Checkout | Địa chỉ nhận, tổng tiền, phí giao, chống đặt trùng | Đã có | Phí giao cố định theo cấu hình, chưa tính theo địa chỉ/hãng |
| Thanh toán | COD và xác nhận nhận đủ tiền sau giao thành công | Đã có | Đối soát thủ công; trạng thái giao thành công khác với đã nhận tiền |
| Thanh toán QR | SePay API v2, QR theo đơn, kiểm tra và chống ghi tiền trùng | **Có mã, chờ cấu hình và nghiệm thu SePay** | Polling khi mở trang, không cần public website; thiếu/thừa/tiền đến sau hủy cần đối chiếu. [Hướng dẫn](18-thanh-toan-sepay.md) |
| Cổng khác | VNPay, MoMo, payOS | **Chưa triển khai** | Người dùng chọn SePay gói Free |
| Tiền thanh toán | Đặt cọc, thanh toán một phần, hoàn tiền tự động | **Chưa triển khai** | Đã có sổ tiền vào SePay; chưa có hoàn tiền tự động/webhook |
| Đơn hàng | Khách xem/hủy đơn chờ; admin xác nhận và chuyển trạng thái | Đã có | Lưu diễn biến và lý do; chưa gửi email/SMS thông báo |
| Vận chuyển | Hãng/mã vận đơn, giao thất bại, giao lại, nhận hoàn | Đã có, thủ công | Chưa gọi API hãng vận chuyển, vị trí thực tế hoặc ETA tự động |
| Kho | Tồn đầu, nhập/xuất, trừ khi đặt, hoàn khi hủy/nhận hoàn | Đã có | Nhật ký và khóa giao dịch; chưa có nhà cung cấp/phiếu mua nhiều dòng/đa kho |
| Báo cáo | Doanh thu tiền hàng theo ngày hoàn thành, lọc khoảng ngày | Đã có | Không gồm phí giao; chưa trừ giá vốn/chi phí; không đồng nghĩa tiền COD đã thu |
| Khuyến mại | Mã giảm giá, điểm thưởng | **Chưa triển khai** | Ngoài phạm vi ban đầu |
| Tương tác khách | Đánh giá sản phẩm, yêu thích | **Đã triển khai** | Đánh giá 1–5 sao sau khi nhận hàng, sửa/xóa đánh giá của mình; yêu thích theo tài khoản, có phân trang |
| Chat nhân viên | Khách gửi/nhận tin, admin có hộp thư, số chưa đọc, đóng/mở lại, lịch sử lưu DB | **Đã triển khai** | Cập nhật định kỳ; chưa có ảnh/tệp, phân công, thông báo đẩy; xem [hướng dẫn](17-chat-khach-hang.md) |
| Chatbot | Cửa sổ góc phải, 55 mục/14 nguồn, 30 câu v1 khóa | Đã có | Kho v1 giữ nguyên; dữ liệu mẫu bổ sung không phải khảo sát hàng thật |
| Chatbot hội thoại | Ngữ cảnh, sửa phủ định, số tiền, đổi chủ đề | Đã có, đã có hồi quy | Không bảo đảm hiểu mọi cách nói; lịch sử tối đa 6 lượt, không lưu lâu dài |
| Chatbot dữ liệu | Giá/tồn công khai, đơn thuộc tài khoản đã xác thực | Đã có | Chỉ đọc; không đặt/hủy/thanh toán thay khách |
| Chatbot tìm web | Định tuyến kiến thức liên quan sang Google Search, nguồn tham chiếu | **Có mã, chưa nghiệm thu đầu-cuối** | Lần thử gần nhất: Search 429; Gemini có 503/timeout. Cần quota và thử lại |
| Kiểm thử | Bộ tự động H2; kiểm tra hash bộ đánh giá đã khóa | Đã có | Xem kết quả mới ở mục dưới; đã chạy lại H2/MySQL sau sửa CI; điểm AI cũ không đại diện toàn bộ bản mới |
| Vận hành local | H2/MySQL, Flyway, health/readiness, hướng dẫn backup | Đã có | Chỉ chạy một phiên với cùng file H2; không cần deploy theo yêu cầu |
| Hồ sơ bảo vệ | Tài liệu thiết kế, vận hành, đề cương thuyết trình | Đã có một phần | Cần mẫu khoa, rà báo cáo nộp, chấm AI độc lập và diễn tập trên máy bảo vệ |
| Kiểm tra trải nghiệm | Trang bán hàng và widget có kiểm thử tích hợp | Chưa nghiệm thu toàn diện | Chưa xác nhận mọi kích thước mobile/trình duyệt và tải kéo dài |

## Bằng chứng mới nhất

- **SePay:** đã bổ sung kiểm thử quyền truy cập, đối soát, chuyển thiếu/thừa, nhận tiền sau hủy, chống ghi trùng và nâng cấp dữ liệu; xem [kết quả mới](verification/sepay/README.md). Chưa nghiệm thu giao dịch thật.
- **Sửa CI trước SePay: 127/127 ca đạt ở mỗi lượt H2 và cấu hình MySQL**, Java 21, UTC; có hồi quy thời điểm gửi/retry/đọc tin nhắn. Xem [kết quả](verification/customer-support/ci-fix-tests.json). Ca nâng cấp schema vẫn dùng H2 riêng; kiểm thử đơn vị không cần MySQL.

- **Chat nhân viên: 126/126 kiểm thử H2 đạt**, thử HTTP thật bằng hai tài khoản tổng hợp riêng; xem [kết quả](verification/customer-support/README.md). Schema mới tự áp dụng ở lần khởi động tiếp theo. Chưa nghiệm thu đầy đủ thao tác widget bằng trình duyệt.

## Bằng chứng của đợt bổ sung danh mục

- [Nguồn dữ liệu và quy tắc nạp](../data/README-products.md), [JSON 36 sản phẩm](../data/products-demo.json).
- **Kết quả đợt danh mục: 114/114 kiểm thử H2 đạt**, database local đã tăng từ 12 lên 36 sản phẩm, 12 SKU cũ giữ nguyên ID/giá/tồn; phiên nhập tạm đã dừng.
- [Kết quả kiểm tra danh mục](verification/catalog-expansion/README.md): kiểm thử nạp lặp, giữ giá/tồn/ID, nhật ký tồn đầu, tài nguyên ảnh và trang chi tiết.
- [Hội thoại mở và giới hạn Search](verification/chatbot-web/README.md): bộ trước có 111 kiểm thử H2; tra web chưa thành công với quota hiện tại. Không chạy lại AI trong đợt bổ sung danh mục này.
- [Nghiệm thu local trước đây](14-nghiem-thu-1.0.0.md): 99 kiểm thử H2/MySQL và điểm AI 177/180 là kết quả lịch sử, không suy ra mọi câu trả lời của bản mới đều đạt.

## Thứ tự hoàn thiện đề xuất

1. Rà lại danh mục, ảnh và mô tả trên trang; thay dần số liệu bằng dữ liệu đã xác nhận nếu yêu cầu đồ án cần hàng thật.
2. Cấu hình SePay Free, thử Sandbox rồi xác minh giao dịch Live khi chủ tài khoản sẵn sàng; theo dõi hạn mức 50 giao dịch tiền vào/tháng.
3. Kiểm tra hạn mức Gemini/Search, đánh giá thêm hội thoại tự do và chấm lại phản hồi độc lập.
4. Diễn tập toàn bộ luồng mua → xử lý đơn → kho → COD → báo cáo; rà giao diện trên máy bảo vệ và hoàn thiện hồ sơ theo mẫu khoa.


## Cập nhật giao diện và luồng thanh toán

- Chi tiết sản phẩm: ảnh lớn, khu vực mua hàng và mô tả tách riêng.
- Thanh điều hướng có trạng thái trang hiện tại, cuộn ngang trên màn hình nhỏ.
- Bộ lọc sản phẩm có khung tìm kiếm/danh mục riêng.
- COD và chuyển khoản hiển thị dạng thẻ lựa chọn.
- Đặt đơn chuyển khoản mở thẳng QR; khi xác nhận PAID, tự chuyển trang thành công có liên kết tiếp tục mua sắm/trang chủ.
- Chưa kiểm chứng trực quan bản giao diện mới bằng công cụ trình duyệt; cần rà soát trên thiết bị khi nghiệm thu.


## Ảnh và sản phẩm liên quan — 15/10/2026

- Đã tạo thêm 24 ảnh AI, đủ ảnh riêng cho 36 sản phẩm. Ảnh đã tải lên bởi admin được ưu tiên; 12 ảnh riêng cũ được giữ.
- Trang chi tiết hiển thị tối đa 4 sản phẩm đang bật, cùng danh mục; ưu tiên còn hàng rồi giá gần nhất, loại sản phẩm đang xem. Không có ứng viên thì ẩn khu vực.
- Đây là gợi ý theo quy tắc từ database, không cần đào tạo AI, không gọi API hoặc thu thập lịch sử người dùng.
- Bộ dữ liệu mặc định đã được chủ dự án chấp thuận sử dụng cho đồ án. Nguồn gốc biên soạn và ảnh minh họa vẫn được ghi rõ.
- Kiểm thử H2 đầy đủ: 150 ca đạt, không lỗi. Baseline chatbot giữ nguyên. Chưa xác minh trực quan trên trình duyệt.
- [Prompt và danh sách ảnh](../output/product-images/cream-v2/manifest.json), [kết quả kiểm tra](verification/catalog-images/results.json).

## Nội dung cửa hàng — 15/10/2026

Mô tả 36 sản phẩm chuyển sang nội dung dành cho khách hàng; bỏ lời giới thiệu đồ án/nguồn dữ liệu khỏi mô tả công khai, đổi tên mặc định có chữ “mẫu” sang Suni. Migration V7 chỉ cập nhật nội dung khớp bản mặc định cũ, giữ chỉnh sửa riêng, giá, tồn kho và lịch sử đơn. Chatbot dùng cách giới thiệu danh mục bình thường, vẫn không suy đoán thông số thiếu. Nhãn thanh toán Sandbox và thông báo quyền riêng tư được giữ. Hồ sơ nguồn dữ liệu và baseline đánh giá tiếp tục lưu nội bộ.


## Rà soát hiện trạng ngày 15/10/2026

Kiểm thử lại H2 / Java 21 / UTC: **151/151 đạt**. Công cụ cấu hình SePay **4/4 đạt**; baseline chatbot và kiểm tra diff đạt; đủ file ảnh cho **36/36 sản phẩm**. Kết quả này không thay thế nghiệm thu trên trình duyệt hoặc dịch vụ thật. Cổng localhost:8080 không kết nối được tại thời điểm rà soát.

| Loại | Phát hiện | Ảnh hưởng / việc cần làm |
| --- | --- | --- |
| Đã sửa | Trang QR được khôi phục từ back-forward cache sau khi bấm Back | Trang tải lại trạng thái từ máy chủ và khởi tạo lại polling; kiểm thử JavaScript xác nhận chỉ tải lại khi khôi phục cache, chỉ chuyển sang thành công khi PAID. Chưa kiểm tra trực quan trên trình duyệt. |
| Giới hạn hiện tại | Thanh toán chỉ polling khi mở trang, dừng sau 20 phút; chưa có webhook/đồng bộ nền | Tiền đến khi đóng trang hoặc sau khi đơn đã PAID không tự cập nhật ngay; cần kiểm tra lại để phát hiện tiền đến muộn/chuyển trùng. |
| Chưa triển khai | Xác nhận đã hoàn tiền và xử lý tiền thừa trong admin | Có REFUND_REQUIRED/REVIEW nhưng chưa có quy trình hoàn tất đối soát ngay trên website. |
| Chưa nghiệm thu | SePay thật, Gemini/Search hiện tại, giao diện desktop/mobile | Không kết luận tích hợp thực tế ổn định từ kiểm thử mock. |
| Chưa đồng bộ GitHub | Ảnh mới, sản phẩm liên quan, nội dung cửa hàng, đánh giá/yêu thích/thư viện ảnh, migrations V6–V8 và sửa trang QR còn ở máy local | GitHub CI thành công trước đây chỉ chứng minh commit 195b2b7, không phải toàn bộ mã đang sửa. |
| Tài liệu cần chốt | Các kết quả 99/114/127/148/150 ca phía trên là lịch sử | Lượt mới nhất: 155 ca đạt trên mỗi cấu hình H2 và MySQL; CI bản mới chưa chạy vì chưa push. |

Các chức năng chưa có nhưng không bắt buộc cho phạm vi local: quên mật khẩu/xác minh email, mã giảm giá, biến thể sản phẩm, import CSV/Excel, API vận chuyển, email/SMS. Không tự mở rộng phạm vi trong lượt rà soát này.


## Đánh giá, yêu thích và thư viện ảnh — 15/10/2026

- Trang chi tiết có điểm trung bình, tổng số đánh giá và danh sách 10 nhận xét/trang. Chỉ tài khoản có đơn COMPLETED chứa sản phẩm mới được gửi/sửa nhận xét; mỗi tài khoản một đánh giá/sản phẩm, 1–5 sao, tối đa 1500 ký tự. Có thể xóa đánh giá của mình.
- Danh sách yêu thích nằm ở thanh điều hướng, 12 sản phẩm/trang, không hiển thị sản phẩm ngừng bán. Thêm/bỏ yêu thích không tạo trùng và chỉ tác động tài khoản đang đăng nhập.
- Trang chỉnh sửa sản phẩm cho admin: thêm ảnh vào thư viện, đặt ảnh đại diện, xóa từng ảnh. Tối đa 6 ảnh tải lên/sản phẩm, JPG/PNG, mỗi ảnh 5 MB và 4096 px mỗi chiều. Ảnh được giải mã và mã hóa lại trước khi lưu. Xóa ảnh đại diện tự dùng ảnh bổ sung đầu tiên, hết ảnh tải lên thì trở về ảnh mặc định.
- Khách bấm ảnh nhỏ để đổi ảnh lớn; khi JavaScript tắt, liên kết vẫn mở được ảnh. Ảnh sản phẩm ngừng bán chỉ admin xem được.
- Migration V8 thêm ba bảng; không xóa ảnh đại diện cũ, tài khoản hay đơn hàng. Chưa có ảnh đính kèm đánh giá, phản hồi/kiểm duyệt đánh giá của admin hoặc tải nhiều file trong một lần.
- Khởi động lại ứng dụng để áp dụng schema và chức năng. Lịch sử đánh giá tổng hợp được bổ sung theo yêu cầu ở mục tiếp theo; ảnh bổ sung do admin tải lên.

Kiểm chứng đợt đánh giá/yêu thích/thư viện ảnh: **155/155 kiểm thử đạt ở mỗi cấu hình H2 và MySQL 8.0.44**, Java 21 / UTC; MySQL chạy trên cơ sở dữ liệu kiểm thử riêng. JavaScript thư viện ảnh, hồi quy vòng đời trang QR và baseline đạt. Kiểm thử vòng đời QR đã thêm vào CI. Chưa nghiệm thu trực quan trên trình duyệt hoặc giao dịch SePay thật. [Kết quả](verification/product-engagement/results.json).

## Lịch sử hoạt động cửa hàng — 15/10/2026

Theo yêu cầu bổ sung dữ liệu cho bản local, profile `local` bật `app.seed-activity=true`. Khi khởi động bản mới lần đầu, hệ thống tạo bộ `store-history-v1` trong một transaction:

- 24 tài khoản khách hàng mang tên Việt Nam tự nhiên; nhân vật và địa chỉ hư cấu, email thuộc miền `.example`, số điện thoại giữ giá trị không liên lạc được. Không sử dụng thông tin cá nhân người thật.
- 72 đơn trong khoảng 4 tháng trước ngày khởi tạo: 60 đơn COMPLETED đã thu COD, 12 đơn CANCELLED chưa thu tiền; có chi tiết hàng, phí vận chuyển và mốc lịch sử trạng thái.
- 60 đánh giá 3–5 sao, chỉ trên sản phẩm mà tài khoản tương ứng có đơn hoàn tất; 120 mục yêu thích (5 mục/tài khoản).
- Sổ kho có cặp nhập/xuất lịch sử cho đơn hoàn tất, giữ nguyên tồn kho và giá hiện tại. Không gọi SePay, không tạo biên nhận ngân hàng hoặc gửi thông báo bên ngoài.

Đây là **dữ liệu tổng hợp phục vụ bản local**, không phải giao dịch hay nhận xét khách hàng thật. Báo cáo doanh thu và điểm đánh giá sẽ bao gồm dữ liệu này. Có thể nhận diện tài khoản qua hậu tố `@history.sunicoffee.example`, bộ nhập qua bảng `activity_seed_runs` và ghi chú sổ kho. Không đưa bộ dữ liệu này vào cửa hàng bán thật.

Marker được khóa trong transaction và chỉ ghi hoàn tất sau khi nhập đủ; chạy lại không tạo trùng hoặc phục hồi dữ liệu đã sửa/xóa. Tài khoản hiện có không bị sửa hoặc nhận làm tài khoản mẫu; nếu email dành riêng đã tồn tại nhưng marker chưa hoàn tất, lượt nhập bị rollback để tránh trộn dữ liệu. Mật khẩu tạo ngẫu nhiên, không có mật khẩu chung công khai; quản trị viên có thể đặt lại nếu cần đăng nhập bằng một tài khoản này.

Cần ít nhất 12 sản phẩm mặc định đang bán; nếu chưa đủ, hệ thống bỏ qua lượt nhập và ghi log. Tắt tính năng bằng `APP_SEED_ACTIVITY=false` (không xóa dữ liệu đã nhập). Ứng dụng đang chạy phải khởi động lại để áp dụng V9 và nạp dữ liệu; không chạy thêm một tiến trình cùng file H2.

Kiểm chứng bộ lịch sử: **156/156 kiểm thử H2 đạt**, Java 21 / UTC, baseline và diff đạt. Chưa chạy MySQL cho phần nhập lịch sử V9; chưa nạp vào cơ sở dữ liệu local đang được tiến trình cũ sử dụng. [Kết quả](verification/activity-history/results.json).

## Đăng nhập Google — 15/10/2026

Đã bổ sung OAuth 2.0 / OpenID Connect qua Spring Security. Chỉ bật khi có cấu hình; ứng dụng vẫn hoạt động bằng email/mật khẩu khi chưa cấu hình Google. Migration V10 lưu liên kết Google subject với tài khoản nội bộ. Khách mới được tạo với quyền CUSTOMER; email trùng yêu cầu mật khẩu SuniCoffee để liên kết trong phiên có thời hạn 5 phút. Không tự gộp theo email. Phiên Google vẫn tuân thủ khóa tài khoản và authVersion.

Cấu hình bằng `python3 scripts/configure-google.py`, khởi động lại và thử tại `/login`. Client Secret nằm trong `.runtime/google.properties`, không commit. Hướng dẫn Google Console, callback localhost và nghiệm thu ở [Đăng nhập Google](17-dang-nhap-google.md). Chưa xác thực với Google thật vì chưa có thông tin client của người dùng; chưa có chức năng tự gỡ/đổi liên kết.

Kiểm chứng bản có Google: **161/161 kiểm thử H2 đạt**, Java 21 / UTC; 2 kiểm thử script cấu hình đạt; baseline và diff đạt. Chưa chạy MySQL cho V10. [Kết quả](verification/google-login/results.json).

## Hoàn thiện giao diện mua sắm — 15/10/2026

Trang sản phẩm chuyển sang bố cục ảnh/thông tin mua hàng, breadcrumb, giá nổi bật, điểm đánh giá liên kết tới nhận xét, bảng thông tin gọn và thanh liên kết giữa các phần. Có nút tăng/giảm số lượng theo giới hạn tồn kho, ảnh thu nhỏ có trạng thái chọn, hộp phóng to ảnh đóng bằng Esc và trả focus về nút mở. Thẻ sản phẩm, button, trường nhập và trang đăng nhập được chỉnh độ bo, bóng đổ và trạng thái tương tác; thêm hiệu ứng xuất hiện khi cuộn và chuyển ảnh. Tôn trọng prefers-reduced-motion và tùy chọn giảm chuyển động sẵn có. Không thêm giá khuyến mãi hoặc số đã bán không có dữ liệu.

Kiểm chứng: 35 kiểm thử liên quan (Shop, SampleCatalog, ProductEngagement, GoogleLogin) đạt; JavaScript kiểm tra cú pháp đạt. Chưa nghiệm thu trực quan desktop/mobile do công cụ Chrome không khả dụng. Khởi động lại và tải mới trang để nhận CSS/template mới. Tham khảo cách trình bày thông tin thương mại điện tử từ [Shopee](https://shopee.vn/list/c%C3%A0%20ph%C3%AA), giữ phong cách xanh/kem của SuniCoffee.

## Đồng bộ và dọn repository — 15/10/2026

Bản cập nhật gồm ảnh sản phẩm, đánh giá/yêu thích/thư viện ảnh, lịch sử tổng hợp, Google login và giao diện. Xóa 12 bản PNG trùng cùng ZIP xuất ảnh cũ (ảnh dùng trên website giữ nguyên); giữ manifest nguồn, tài liệu vận hành và baseline. Thêm quy tắc bỏ qua cache Python, cập nhật README. Đã sửa thuộc tính data-reveal trùng ở trang danh mục được phát hiện trong lượt kiểm tra trước push. Kết quả cuối: 161/161 kiểm thử H2 đạt, 6 kiểm thử script cấu hình đạt, hồi quy JavaScript thanh toán và baseline đạt. Không đưa cấu hình bí mật hoặc database local vào Git. Các ghi chú “chưa đẩy GitHub” phía trên là trạng thái lịch sử trước đợt đồng bộ này.

## Vá đối soát thanh toán trên MySQL — 16/10/2026

CI phát hiện ca đối soát đồng thời không ghi nhận tiền. Cột created_at có độ chính xác giây; MySQL làm tròn phần lẻ từ 0,5 giây lên giây tiếp theo, khiến giao dịch đến ngay sau checkout có thể bị coi là xảy ra trước đơn. OrderService chuẩn hóa thời gian tạo đơn xuống giây trước khi lưu. Giữ nguyên điều kiện kiểm tra tài khoản, mã, số tiền và chống ghi nhận trùng. Kiểm thử hồi quy xác nhận thời gian sau đọc DB không thay đổi và hai yêu cầu đồng thời chỉ ghi nhận một biên nhận ở cùng giây tạo đơn.

161/161 kiểm thử đạt trên mỗi cấu hình H2 và MySQL 8.0, Java 21 / UTC; baseline đạt. Không thử chuyển tiền thật. [Bằng chứng](verification/sepay/creation-time-fix.json).

## Trang cá nhân và thanh điều hướng — 16/10/2026

Trang hồ sơ có thẻ nhận diện, menu tài khoản, nhóm thông tin liên hệ/địa chỉ và nút lưu rõ ràng; trang đổi mật khẩu thống nhất phong cách. Header chuyển sang hai hàng với ô tìm sản phẩm, giỏ hàng, menu tài khoản dạng details và điều hướng chính. Menu hỗ trợ bàn phím, Esc và bấm bên ngoài; không có JavaScript vẫn mở được. Có bố cục mobile và hỗ trợ giảm chuyển động. Giữ nguyên xử lý hồ sơ, CSRF, đăng xuất và phân quyền.

51 kiểm thử liên quan đạt (Management, Shop, GoogleLogin, ProductEngagement); JavaScript kiểm tra cú pháp đạt. Chưa nghiệm thu trực quan trình duyệt của bản này. Chưa đẩy thay đổi giao diện này lên GitHub.

## Tinh chỉnh chi tiết sản phẩm — 19/10/2026

Điều chỉnh khoảng cách, tỷ lệ ảnh/thông tin, khung giá và đơn vị bán, nhãn tồn kho, ảnh thu nhỏ cuộn ngang trên màn hình hẹp. Mô tả có khối tư vấn bên cạnh trên desktop và xếp dọc trên mobile. Đánh giá có điểm tổng quan từ dữ liệu thật trong database, trạng thái chưa có nhận xét và lựa chọn sao bằng radio hỗ trợ bàn phím. Ẩn CTA đăng nhập mua hàng khi hết hàng; tiêu đề trình duyệt dùng tên sản phẩm. Không thêm khuyến mãi hay chỉ số bán hàng giả.

30 kiểm thử liên quan đạt (ProductEngagement, SampleCatalog, Shop); diff hợp lệ. Chưa nghiệm thu trực quan desktop/mobile trong đợt này. Thay đổi đang ở local, chưa push GitHub.
