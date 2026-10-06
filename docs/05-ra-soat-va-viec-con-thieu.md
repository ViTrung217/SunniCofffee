# Rà soát hiện trạng SuniCoffee — 06/10/2026

## Cập nhật sau triển khai 0.3

Danh sách dưới đây là kết quả rà soát trước khi sửa, giữ để truy vết. Đã triển khai vận chuyển thủ công, giao thất bại/giao lại/nhận hoàn, COD, lịch sử đơn, tìm/lọc/nhắc xử lý đơn chờ, sửa top sản phẩm và checkout nhiều tab/lỗi khóa; bổ sung giới hạn đăng nhập, Git/CI, script sao lưu, kiểm thử và tài liệu V3. Kết quả mới và các phần còn phụ thuộc dữ liệu/môi trường xem [bàn giao 0.3](08-ban-giao-v03.md).

Người dùng xác nhận chưa có dữ liệu thật/mẫu của khoa; tài liệu bàn giao hiện là tài liệu kỹ thuật và kịch bản demo, không tự coi là báo cáo tốt nghiệp đã được duyệt. Không triển khai AI theo yêu cầu.

## Phạm vi và bằng chứng

Rà soát mã nguồn, migration, giao diện, tài liệu và kiểm thử trong thư mục dự án. Đối chiếu yêu cầu đồ án theo tài liệu 01; chưa đối chiếu lại bản đề cương Word gốc hoặc tài liệu nằm ngoài dự án. Đây là rà soát hiện trạng, chưa sửa các vấn đề nghiệp vụ liệt kê dưới đây.

Chạy lại `mvn -q -Ddebug=false test`: **44 kiểm thử đạt, 0 thất bại, 0 lỗi, 0 bỏ qua** trên H2. Gồm ShopIntegrationTest 21, ManagementIntegrationTest 21, MigrationUpgradeTest 1 và UploadHttpIntegrationTest 1. Báo cáo nằm trong `target/surefire-reports/`; các ca hiện có đạt không đồng nghĩa đã bao phủ mọi tình huống.

Nhật ký `target/verification-v02/mysql-tests.log` ghi nhận 42 ca đạt ngày 03/10/2026, trong đó tài liệu 04 ghi 41 ca ứng dụng dùng MySQL và 1 ca nâng cấp dùng H2. Chưa chạy lại bộ 44 ca hiện tại trên MySQL trong lần rà soát này. Chưa kiểm tra trực quan trình duyệt, tải lớn hoặc triển khai công khai.

## Những phần đã có

- Đăng ký/đăng nhập, phân quyền, hồ sơ, đổi mật khẩu, quản trị tài khoản và thu hồi phiên cũ.
- Danh mục, sản phẩm, tìm kiếm, phân trang, ảnh đại diện và ngừng bán.
- Giỏ hàng, đặt COD, lưu giá tại thời điểm mua, lịch sử đơn, luồng trạng thái và hủy đơn hợp lệ.
- Khóa tồn kho khi đặt hàng; hai khách cùng mua món cuối chỉ một đơn thành công; hoàn tác giỏ nhiều dòng nếu thiếu hàng.
- Phiếu nhập/xuất, nhật ký bán/hoàn kho, chống ghi trùng và hoàn tồn một lần.
- Báo cáo doanh thu theo ngày hoàn thành; migration V1/V2; cấu hình H2/MySQL và hướng dẫn chạy.

## Điểm cần sửa hoặc hoàn thiện trong code

| Ưu tiên | Phát hiện | Căn cứ và tác động | Hướng xử lý |
| --- | --- | --- | --- |
| Cao | Top sản phẩm bị tách khi đổi tên hoặc SKU | `RevenueService.report()` nhóm theo `i.sku,i.product_name`, trong khi `CatalogService.saveProduct()` cho đổi cả hai. Cùng một product_id bán trước và sau khi đổi tên có thể xuất hiện thành hai dòng, ảnh hưởng thứ hạng top 10; tổng doanh thu chung vẫn đúng. Phát hiện qua đọc mã, chưa có ca kiểm thử riêng. | Nhóm theo định danh sản phẩm ổn định; quy định cách hiển thị tên trong báo cáo, giữ nguyên bản chụp chi tiết đơn; thêm kiểm thử đổi tên/SKU giữa hai lần bán. |
| Vừa | Chưa xử lý riêng lỗi chờ khóa/deadlock khi checkout | `OrderController.submit()` chỉ bắt `BusinessException`; bộ xử lý lỗi chung chỉ có trường hợp upload. Khi cơ sở dữ liệu phát sinh lỗi khóa, khách có thể gặp trang lỗi chung thay vì hướng dẫn thử lại. Chưa tái hiện timeout/deadlock trong lần rà soát này. | Xử lý sau khi giao dịch thất bại đã rollback; thông báo thử lại và giữ thông tin biểu mẫu. Nếu có retry tự động, giới hạn số lần và giữ token chống tạo trùng. |
| Vừa | Mở checkout ở tab mới làm token tab cũ mất hiệu lực | Mỗi GET `/checkout` ghi đè một `checkoutToken` duy nhất trong session; POST tab cũ bị từ chối. Không gây bán vượt tồn nhưng gây gián đoạn thao tác. | Chốt chính sách hỗ trợ nhiều tab và quản lý token tương ứng; kiểm thử gửi lại sau khi mở tab khác. |

## Phần còn thiếu so với phạm vi đồ án

1. **Dữ liệu được xác minh:** hiện có 4 danh mục và 12 mặt hàng mô phỏng; chưa có bộ thông số, ảnh và nguồn thực tế được bàn giao trong dự án. Cần xác nhận quy tắc cửa hàng và phân biệt rõ dữ liệu mô phỏng trong báo cáo.
2. **Kho tri thức:** chưa có ít nhất 50 mục có nguồn như tài liệu 01 yêu cầu. Mỗi mục cần nội dung, nguồn và thông tin đủ để kiểm tra lại; không dùng mô tả sản phẩm giả lập làm căn cứ thực tế.
3. **Bộ đánh giá AI:** chưa có 30 câu hỏi và đáp án mong đợi tách riêng; chưa định nghĩa cách chấm và xử lý câu ngoài phạm vi. Mục tiêu ghi trong tài liệu là ít nhất 24/30 câu, chưa có kết quả đo.
4. **Chatbot:** chưa có giao diện, API/service, truy xuất tri thức, cấu hình nhà cung cấp, dẫn nguồn hoặc xử lý khi không đủ căn cứ. Cần lấy giá/tồn từ dữ liệu hiện tại khi trả lời các thông tin này.
5. **Hồ sơ phân tích thiết kế:** đã có ERD và trạng thái đơn bản 0.1; chưa thấy bộ use case, đặc tả luồng chính/ngoại lệ, sơ đồ hoạt động/tuần tự và thiết kế kiến trúc đầy đủ. ERD/từ điển dữ liệu tổng hợp chưa phản ánh đầy đủ V2.
6. **Bộ bàn giao cuối:** chưa thấy báo cáo tốt nghiệp hoàn chỉnh, kết quả đánh giá chatbot và slide bảo vệ trong dự án. README đã có hướng dẫn chạy và kịch bản sử dụng cơ bản; còn cần hướng dẫn minh họa, kịch bản demo tổng thể và bằng chứng kiểm thử gắn với từng yêu cầu.

## Quy tắc nghiệp vụ còn cần chốt

- **Giao hàng thất bại/khách từ chối nhận:** SHIPPING chỉ chuyển được sang COMPLETED. Chưa có trạng thái trả về và quy tắc hoàn kho khi nhận lại hàng. Nếu dùng thực tế, đơn giao thất bại sẽ không có đường kết thúc phù hợp.
- **Đơn PENDING tồn lâu:** tồn đã trừ nhưng chưa có thời hạn xử lý/hủy tự động. Cần quy định xử lý thủ công hay tự động; không tự áp dụng hạn giữ hàng khi chưa chốt nghiệp vụ COD.
- **Lịch sử trạng thái đơn:** hiện lưu trạng thái hiện tại, ngày tạo và ngày hoàn thành; chưa có nhật ký ai xác nhận/giao hàng, thời điểm từng bước và lý do hủy.
- **Vận hành quản trị:** danh sách đơn mới có phân trang, chưa tìm theo mã/khách hoặc lọc trạng thái/ngày. Có thể bổ sung sau các lỗi và phần bắt buộc của đồ án.

## Kiểm thử và vận hành còn thiếu bằng chứng

- Chạy lại bộ hiện tại trên MySQL kiểm thử riêng, đặc biệt tình huống hai giao dịch cùng đọc tồn cũ; không dùng cơ sở dữ liệu cần giữ vì kiểm thử xóa dữ liệu trước mỗi ca.
- Bổ sung tình huống checkout đồng thời với hủy/nhập/xuất kho, hai yêu cầu cùng hủy một đơn, nhiều sản phẩm mua theo thứ tự thêm giỏ khác nhau và timeout khóa.
- Kiểm tra giao diện trên desktop/điện thoại, bàn phím, biểu mẫu lỗi và luồng mua hàng bằng trình duyệt; kiểm thử hiện tại chưa xác nhận bố cục trực quan.
- Chưa có số đo hiệu năng/tải đồng thời và giới hạn đáp ứng được xác định.
- Chưa thấy quy trình sao lưu/khôi phục MySQL được kiểm thử hoặc cấu hình triển khai công khai. Có bản sao lưu cục bộ trong `.backups`, chưa thay thế một quy trình khôi phục.
- Chưa thấy cơ chế hạn chế thử đăng nhập liên tục trong mã/cấu hình dự án; cần xem xét trước khi mở dịch vụ công khai.
- Thư mục hiện tại chưa phải Git repository; chưa thấy CI. Nên quản lý phiên bản và chạy kiểm thử tự động để dễ truy vết thay đổi.

## Tài liệu cần đồng bộ

- Tài liệu 04 ghi 42 ca kiểm thử: đó là kết quả lịch sử ngày 03/10; bộ hiện tại là 44 ca. Giữ nguyên bằng chứng lịch sử và tham chiếu kết quả mới tại tài liệu này.
- Tài liệu 02 là thiết kế 0.1 và còn mô tả sửa tồn trực tiếp; hiện V2 đã bắt buộc dùng phiếu kho cho sản phẩm có sẵn. Cần bản thiết kế tổng hợp hiện hành.
- README đã nói V1/V2 tồn tại nhưng hướng dẫn thêm migration “V2, V3”; migration tiếp theo phải dùng phiên bản chưa tồn tại, hiện là V3.
- Log xác minh đang ở `target/`, có thể mất khi `mvn clean`; nên lưu bằng chứng bàn giao ở nơi bền vững hơn.

## Các tính năng mở rộng không mặc định là bắt buộc

Thanh toán online, kết nối vận chuyển, mã giảm giá, đánh giá sản phẩm, đa chi nhánh, nhà cung cấp/phiếu mua nhiều dòng, giá vốn/lợi nhuận và quên mật khẩu qua email chưa được triển khai. Không cần làm tất cả để hoàn thành phạm vi ban đầu; chỉ bổ sung theo đề cương đã duyệt hoặc yêu cầu cửa hàng. Quên mật khẩu có giá trị sử dụng nhưng không thay thế các hạng mục AI đang thiếu.

## Thứ tự đề xuất

1. Sửa cách nhóm top sản phẩm; hoàn thiện lỗi tranh chấp khi đặt hàng và kiểm thử MySQL hiện tại.
2. Chốt giao thất bại, xử lý đơn chờ lâu và lịch sử trạng thái; triển khai phần được chọn vào phạm vi.
3. Chuẩn bị dữ liệu thật/mô phỏng có nhãn, 50 mục tri thức và 30 câu đánh giá độc lập.
4. Xây dựng chatbot, chạy đánh giá và ghi nhận kết quả thực tế.
5. Hoàn thiện kiểm thử trình duyệt, tài liệu thiết kế hiện hành, báo cáo, hướng dẫn, demo và slide.
