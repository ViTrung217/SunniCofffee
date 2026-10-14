# Bước 1 — Phạm vi nghiệp vụ và dữ liệu ban đầu của SuniCoffee

Tài liệu lưu căn cứ phạm vi ban đầu; tình trạng hiện tại xem [bảng tiến độ](16-tinh-trang-du-an.md).

Trạng thái: bản khởi đầu để trao đổi, chưa phải kết quả khảo sát cửa hàng. Người dùng đã xác nhận chưa có dự án và chưa có dữ liệu thật; bắt đầu từ đầu bằng dữ liệu mẫu.

## Mục tiêu

Xây dựng hệ thống quản lý kinh doanh đồ chuyên dụng cho cà phê phượt SuniCoffee tích hợp chức năng AI. Triển khai lần lượt từ phân tích nghiệp vụ, thiết kế dữ liệu, chức năng bán hàng và quản trị đến chatbot. Mỗi bước cần có sản phẩm bàn giao và cách kiểm tra trước khi chuyển bước.

Tài liệu tham chiếu: DeCuong_ViHoangTrung_2251162192_64HTTT4.docx do người dùng cung cấp. Nội dung đề cương được dùng làm căn cứ phạm vi; những lựa chọn bổ sung dưới đây là đề xuất, chưa phải yêu cầu đã được cửa hàng xác nhận.

## Phạm vi đã có trong yêu cầu và đề cương

- Công nghệ: Java Spring Boot, Thymeleaf, Maven, Spring Security và MySQL theo đề cương.
- Sản phẩm kinh doanh: cà phê, máy pha cà phê, dụng cụ pha chế và thiết bị phục vụ cà phê phượt.
- Khách hàng: đăng ký, đăng nhập, quản lý tài khoản, xem và tìm sản phẩm, giỏ hàng, đặt hàng, theo dõi đơn và lịch sử mua hàng.
- Quản trị viên: quản lý sản phẩm, ảnh sản phẩm, danh mục, tài khoản, quyền và đơn hàng.
- AI triển khai sau chức năng cơ bản: chatbot tư vấn trong phạm vi kho tri thức, xử lý câu hỏi thiếu căn cứ bằng thông báo giới hạn và hướng dẫn liên hệ hỗ trợ.
- Theo đề cương, trước khi phát triển chatbot cần hoàn thành ít nhất 50 mục kiến thức có nguồn và bộ 30 câu hỏi cùng đáp án mong đợi, tách riêng khỏi kho tri thức. Mục tiêu đánh giá là đạt ít nhất 80%, tương đương 24/30 câu; cần định nghĩa cách chấm trước khi chạy đánh giá.
- Kết quả cuối gồm mã nguồn, cơ sở dữ liệu, tài liệu phân tích thiết kế, kết quả kiểm thử, đánh giá chatbot, báo cáo, hướng dẫn sử dụng và slide bảo vệ.

## Phương án nghiệp vụ ban đầu cần xác nhận

Đề xuất xây dựng cho một cửa hàng và một kho hàng. Hai vai trò đăng nhập là khách hàng và quản trị viên; khách chưa đăng nhập có thể xem, tìm kiếm sản phẩm. Đề xuất yêu cầu đăng nhập trước khi đặt hàng và sử dụng thanh toán khi nhận hàng (COD) ở phiên bản đầu.

Đa chi nhánh, phân quyền nhân viên chi tiết, cổng thanh toán trực tuyến, kết nối đơn vị vận chuyển và khuyến mãi phức tạp chưa thuộc phạm vi ban đầu đề xuất. Việc bổ sung tùy yêu cầu thực tế và thời gian làm đồ án.

Luồng bán hàng dự kiến: khách xem sản phẩm → thêm giỏ → nhập thông tin nhận hàng → đặt hàng → quản trị viên xác nhận và cập nhật giao hàng → khách theo dõi kết quả.

Cần làm rõ trước khi chốt thiết kế đơn hàng: thời điểm giữ hoặc trừ tồn kho, điều kiện hủy, cách xử lý tồn kho khi hủy, phí vận chuyển và việc quản lý sản phẩm theo biến thể. Ví dụ, cà phê túi 250 g và 500 g có thể cần hai mã bán hàng riêng dù cùng một dòng sản phẩm.

## Dữ liệu cần chuẩn bị trước

Giai đoạn đầu tập trung vào danh mục và sản phẩm. Các trường dưới đây mô tả dữ liệu nghiệp vụ; chưa khẳng định chúng nằm trong cùng một bảng cơ sở dữ liệu.

| Nhóm | Thông tin cần thu thập | Lưu ý |
| --- | --- | --- |
| Danh mục | Tên, mô tả | Nhóm thử nghiệm có thể gồm cà phê, máy pha, dụng cụ pha chế, thiết bị cà phê phượt |
| Sản phẩm | Mã bán hàng, tên, danh mục, thương hiệu nếu có, mô tả | Mã phải phân biệt được từng mặt hàng bán và quản lý tồn |
| Giá và tồn | Giá bán bằng VND, số lượng tồn, đơn vị bán, thời điểm ghi nhận | Giá, tồn thay đổi theo thời gian; phải đánh dấu dữ liệu thật hay mô phỏng |
| Thuộc tính | Khối lượng, kích thước, dung tích, nguồn điện, chất liệu, tình huống sử dụng tùy loại | Không điền thuộc tính không có căn cứ; không phải sản phẩm nào cũng có mọi thuộc tính |
| Ảnh | Tệp hoặc liên kết ảnh và nguồn | Ưu tiên ảnh cửa hàng cung cấp hoặc ảnh được phép sử dụng |
| Nguồn | Nguồn cung cấp thông tin, ngày thu thập, trạng thái xác minh | Dùng để kiểm tra lại và chuẩn bị tri thức AI sau này |

Đề xuất đợt đầu có khoảng 4 danh mục và 10–20 sản phẩm để thử các chức năng quản lý. Đây là quy mô khởi đầu đề xuất, không phải dữ liệu thật đã thu thập hoặc yêu cầu cố định của đề cương.

Nếu chưa có dữ liệu cửa hàng, có thể dùng dữ liệu mô phỏng được ghi nhãn rõ. Không dùng dữ liệu mô phỏng để khẳng định đã khảo sát thực tế, đã có doanh thu thực tế hoặc đã kiểm chứng đặc tính sản phẩm.

Mẫu mô tả một sản phẩm để người dùng cung cấp:

- Mã sản phẩm:
- Tên sản phẩm:
- Danh mục:
- Thương hiệu nếu có:
- Quy cách hoặc biến thể:
- Giá bán VND:
- Số lượng tồn và đơn vị:
- Mô tả và thông số đã xác minh:
- Ảnh và nguồn ảnh:
- Nguồn thông tin và ngày thu thập:
- Dữ liệu thật hay mô phỏng:

## Thứ tự triển khai và kết quả cần đạt

| Bước | Công việc | Điều kiện hoàn thành |
| --- | --- | --- |
| 1 | Chốt phạm vi, tác nhân, luồng bán hàng và nguồn dữ liệu | Có danh sách chức năng và các giả định được xác nhận |
| 2 | Thiết kế ERD, bảng và ràng buộc; tạo dữ liệu mẫu | Có từ điển dữ liệu, tập lệnh tạo cơ sở dữ liệu và dữ liệu mẫu nhất quán |
| 3 | Khởi tạo ứng dụng, kết nối dữ liệu, đăng nhập và phân quyền | Ứng dụng chạy được; quyền khách hàng và quản trị được kiểm tra |
| 4 | Quản trị danh mục, sản phẩm, ảnh; hiển thị và tìm kiếm sản phẩm | Dữ liệu nhập ở quản trị xuất hiện đúng ở trang khách hàng |
| 5 | Giỏ hàng, đặt hàng, quản trị đơn, lịch sử mua | Luồng mua hàng hoàn chỉnh; kiểm tra giá, tồn kho, hủy đơn và quyền truy cập |
| 6 | Chuẩn bị kho tri thức và bộ đánh giá, sau đó tích hợp AI | Đủ nguồn; bộ đánh giá tách riêng; có kết quả đo thực tế theo tiêu chí |
| 7 | Kiểm thử tổng thể, đo hiệu năng, hoàn thiện báo cáo và slide | Có bằng chứng kiểm thử và bản trình diễn tái lập được |

Ghi chép báo cáo và kiểm thử được thực hiện trong từng bước. Bước cuối tổng hợp kết quả đã làm, không điền trước kết quả thử nghiệm hoặc tỷ lệ đạt.

## Nhóm dữ liệu dự kiến cho bước thiết kế tiếp theo

Tài khoản và vai trò; danh mục và sản phẩm; biến thể nếu nghiệp vụ cần; ảnh; giỏ hàng và các dòng giỏ; đơn hàng và các dòng đơn. Đơn hàng cần giữ thông tin người nhận, địa chỉ và giá tại thời điểm mua để thay đổi hồ sơ hoặc giá sản phẩm sau này không làm thay đổi lịch sử mua.

Kho tri thức và bộ câu hỏi đánh giá được chuẩn bị thành phần riêng khi bước sang AI. Chưa quyết định nhà cung cấp mô hình hoặc cơ chế truy xuất ở bước này.

## Thông tin còn thiếu

- Công cụ lập trình và MySQL hiện có trên máy, cần kiểm tra khi bắt đầu thiết lập môi trường.
- Kết quả xác nhận các giả định nghiệp vụ: một kho, COD, điều kiện đặt hàng, biến thể và xử lý tồn kho.
- Thời hạn nộp và yêu cầu định dạng báo cáo của khoa khi lập lịch chi tiết.

## Cập nhật sau khi viết mã bản 0.1

Đã xây dựng ứng dụng Spring Boot với dữ liệu, tài khoản và luồng bán hàng cơ bản theo yêu cầu tiếp tục tạo code của người dùng. Thiết kế dữ liệu nằm trong `02-thiet-ke-co-so-du-lieu.md`; kết quả bản 0.1 có thể tra lại trong lịch sử Git. Chưa khảo sát cửa hàng và chưa tích hợp AI.

## Xử lý hai khách cùng đặt sản phẩm cuối

Quy tắc đang áp dụng trong mã nguồn: thêm vào giỏ hoặc mở trang thanh toán chưa giữ hàng. Tồn kho chỉ bị trừ khi tạo đơn `PENDING` thành công.

Khi chỉ còn 1 sản phẩm và hai khách cùng xác nhận đặt hàng:

1. Mỗi yêu cầu chạy trong một giao dịch cơ sở dữ liệu. Hệ thống khóa ghi dòng sản phẩm (`PESSIMISTIC_WRITE`) và đọc lại tồn kho trước khi kiểm tra số lượng.
2. Giao dịch lấy được khóa trước được kiểm tra và trừ tồn. Nếu giao dịch này thành công, đơn được tạo và tồn kho về 0.
3. Giao dịch còn lại chờ khóa, sau đó đọc tồn mới là 0 và bị từ chối với thông báo sản phẩm không còn đủ hàng. Giỏ hàng được giữ để khách chỉnh sửa; không tạo đơn hay phiếu xuất kho cho yêu cầu thất bại.

Thứ tự lấy khóa tại cơ sở dữ liệu quyết định yêu cầu được xử lý trước, không đảm bảo theo thời điểm khách bấm nút. Nếu giao dịch trước thất bại và hoàn tác, giao dịch sau vẫn có thể mua số hàng đó.

Với giỏ có nhiều sản phẩm, hệ thống khóa theo ID tăng dần để giảm nguy cơ khóa chéo. Nếu bất kỳ dòng nào thiếu hàng, toàn bộ đơn được hoàn tác, kể cả phần tồn đã trừ ở dòng trước. Tạo đơn, trừ tồn, ghi nhật ký kho và xóa giỏ cùng thuộc một giao dịch. Hủy đơn hợp lệ hoàn tồn đúng một lần.

Kiểm thử trong `src/test/java/vn/sunicoffee/ShopIntegrationTest.java` bao gồm hai giao dịch cùng đọc tồn ban đầu là 1 trước khi đặt hàng, thông báo trên trang thanh toán khi khách khác vừa mua hết hàng, và hoàn tác giỏ nhiều dòng khi dòng sau thiếu hàng. Chạy bằng `mvn -Dtest=ShopIntegrationTest test`; cấu hình kiểm thử sử dụng H2 chế độ MySQL, không thay thế kiểm thử trên MySQL thực tế.

## Tiến độ ban đầu trước khi viết mã

Đã đọc nội dung đề cương, lập bản phạm vi ban đầu và chuẩn bị 4 danh mục cùng 12 sản phẩm mô phỏng trong `../data/products-demo.json`. Chưa khảo sát cửa hàng, chưa tạo cơ sở dữ liệu, chưa viết chức năng ứng dụng và chưa tích hợp AI. Công việc tiếp theo là làm rõ quy tắc nghiệp vụ ảnh hưởng đến thiết kế dữ liệu và xây dựng ERD trong bước 2.


## Cập nhật phạm vi 0.3

Người dùng yêu cầu triển khai vận chuyển và hoàn thiện các phần còn thiếu ngoài AI. Đã xác nhận phí mặc định 0 VND có thể cấu hình, xử lý đơn chờ lâu thủ công, chỉ hoàn tồn khi nhận lại hàng còn bán được. Chưa cung cấp dữ liệu cửa hàng thật hoặc mẫu báo cáo khoa. Thiết kế hiện hành ở tài liệu 02 và 06, hướng dẫn hiện tại ở [tài liệu 13](13-phat-hanh-va-ban-giao.md), kết quả/giới hạn mới nhất ở [tài liệu 16](16-tinh-trang-du-an.md). Các phần ghi “ban đầu”, “0.1” là lịch sử phân tích.
