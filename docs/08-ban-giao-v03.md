# Bàn giao kỹ thuật SuniCoffee 0.3 — phần ngoài AI

Ngày: 06/10/2026. Người dùng đã xác nhận chưa có dữ liệu cửa hàng thật hoặc mẫu báo cáo của khoa. Bản bàn giao này trình bày kết quả phần mềm và tài liệu kỹ thuật, không giả định đã hoàn thành khảo sát hoặc báo cáo tốt nghiệp theo mẫu chưa được cung cấp.

## Kết quả triển khai

| Hạng mục từ rà soát | Kết quả |
| --- | --- |
| Vận chuyển thủ công | Hãng, mã vận đơn, ngày bàn giao; giao thành công/thất bại/giao lại/chuyển hoàn/nhận hoàn |
| Phí giao hàng | Cấu hình cố định, mặc định 0; hiển thị checkout và lưu riêng trên đơn |
| Hoàn kho | Chỉ sau khi nhận kiện; từng dòng 0..số đã mua; lưu số còn bán được và lý do phần không nhập tồn; khóa đơn chống hoàn lặp |
| COD | Xác nhận nhận đủ tiền riêng với hoàn thành giao; ghi ngày và người thực hiện |
| Nhật ký đơn | Đặt, xác nhận, bàn giao/mã vận đơn, thất bại, giao lại, hoàn/hủy và đối soát |
| Đơn chờ lâu | Lọc chờ quá 48 giờ, cấu hình được; admin xử lý thủ công |
| Tìm/lọc đơn | SUNI-id/id, tên người nhận/email, trạng thái, khoảng ngày và quá hạn chờ |
| Top sản phẩm | Gộp theo product_id, giữ giá lịch sử; tên/SKU từ lần mua mới nhất trong kỳ |
| Checkout | Tối đa 20 token/tab/phiên; gửi lại cùng token không tạo trùng; lỗi tranh chấp có thông báo thử lại |
| Bảo vệ đăng nhập | 20 POST/phút/IP trong một tiến trình; 429 + Retry-After; kiểm thử hết cửa sổ thời gian |
| Schema | V3 giữ đơn cũ, không dựng ngày vận chuyển/đối soát hoặc lịch sử không rõ |
| Git/CI | Khởi tạo Git cục bộ; workflow H2 + MySQL cho push/PR; chưa tạo remote/push |
| Sao lưu | Hai script backup và restore vào database mới có hậu tố `_restore_check`; đã thử trên dữ liệu tổng hợp |
| Hồ sơ | ERD/từ điển dữ liệu hiện hành, kiến trúc, use case, luồng ngoại lệ, sơ đồ tuần tự/trạng thái, hướng dẫn vận hành và demo |

## Ma trận yêu cầu — kiểm thử

| Yêu cầu | Bằng chứng chính |
| --- | --- |
| Hai khách tranh món cuối / rollback nhiều dòng | ShopIntegrationTest |
| Quyền, token, giỏ và lịch sử mua | ShopIntegrationTest |
| Hồ sơ, ảnh, kho và doanh thu theo ngày | ManagementIntegrationTest |
| Phí, trạng thái giao, nhận hoàn, COD | ShippingIntegrationTest |
| Đổi tên/SKU không tách top | ShippingIntegrationTest.topProductsRemainGroupedAfterRenameAndSkuChange |
| Nhiều tab, retry khi lỗi khóa | ShippingIntegrationTest.checkoutTabsAndReplayRemainValid / lockFailureGivesRetryMessageAndKeepsCart |
| Hủy đồng thời, đặt với hủy/xuất kho, thứ tự nhiều sản phẩm | ShippingIntegrationTest |
| Giới hạn đăng nhập và hết thời hạn | LoginThrottleTest |
| Nâng cấp có dữ liệu cũ | MigrationUpgradeTest (H2 riêng) |
| Upload và lỗi quá dung lượng qua HTTP thật | UploadHttpIntegrationTest |

Báo cáo chạy lưu tại `docs/verification/v03-h2/` và `v03-mysql/`, log cùng thư mục. **Kết quả cuối: 61/61 ca đạt ở cả lượt H2 và lượt MySQL, không lỗi hoặc bỏ qua.** Bộ hiện tại gồm 61 ca: Shop 21, Management 21, Shipping 15, LoginThrottle 2, Migration 1, Upload HTTP 1. Trong lượt MySQL, 58 ca ứng dụng dùng MySQL, 2 ca giới hạn login không dùng database, 1 ca migration độc lập dùng H2. Các kiểm thử dùng dữ liệu riêng và có dọn bảng; không chạy vào database muốn giữ.

Kiểm thử lỗi khóa trên giao diện dùng lỗi mô phỏng từ service để kiểm tra thông báo và giữ giỏ; không phải số đo timeout/deadlock thật dưới tải kéo dài. Những ca tranh tồn chạy giao dịch thật với luồng đồng thời.

## Sao lưu/khôi phục đã kiểm chứng

Dùng MySQL 8.0.44 riêng tại localhost:13317. Sau kiểm thử tạo fixture tổng hợp có 1 đơn, 1 phiếu kho, 1 ảnh và 1 sự kiện. Backup rồi restore vào database mới. So sánh tồn 7, tổng đơn 120000, phí 25000 và SHA-256 của ảnh: khớp. Log ở `verification/v03-restore.log`. Không sử dụng dữ liệu cửa hàng. Đây là kiểm tra dữ liệu và blob, chưa phải diễn tập khôi phục hệ thống sản xuất có downtime.

## Đo phản hồi cục bộ

`scripts/load-smoke.py`: 100 GET `/api/products`, 10 luồng, trên demo H2 riêng sau warm-up. Kết quả: 100 thành công, median 9,20 ms, p95 21,57 ms, max 27,05 ms, tổng thời gian 0,117 giây. Dữ liệu gốc ở `verification/v03-load.json`.

Phép đo ngắn trên localhost với danh mục nhỏ, không gồm đặt hàng, đăng nhập hay mạng Internet. Không suy ra sức chịu tải sản xuất hoặc SLA từ phép đo này.

## Phần chưa thể xác nhận hoặc còn phụ thuộc đầu vào

- **Kiểm tra trực quan desktop/điện thoại:** đã có render HTTP/Thymeleaf tự động và CSS responsive, nhưng chưa hoàn tất quan sát màn hình. Công cụ trình duyệt báo người dùng đang thay đổi ứng dụng nhiều lần; dừng thao tác để tránh tác động phiên làm việc. Checklist bàn giao nằm trong tài liệu 07.
- **Dữ liệu thật và khảo sát:** người dùng chưa có. Đã tạo phiếu thu thập `data/thu-thap-du-lieu-that.md`, không tự bịa nguồn/thông số.
- **Báo cáo/slide theo khoa:** chưa có mẫu, thông tin bìa, thời lượng hoặc hướng dẫn giảng viên. Đã có nội dung kỹ thuật, kịch bản demo và dàn ý thuyết trình ở tài liệu 09; chưa coi là báo cáo/slide bảo vệ hoàn chỉnh. Công cụ runtime bắt buộc của kỹ năng Presentations (`load_workspace_dependencies`) không có trong phiên, nên chưa xuất PPTX.
- **Triển khai công khai:** chưa có máy chủ/domain/tài khoản triển khai. Không tự xuất bản hoặc đưa dữ liệu ra ngoài. CI chưa chạy trên GitHub vì chưa có remote.
- **Tải lớn/thiết bị thật:** cần môi trường và bộ dữ liệu mục tiêu; số đo hiện tại chỉ là smoke test cục bộ.
- **AI:** toàn bộ chatbot, kho tri thức và đánh giá AI để ngoài lần triển khai này theo yêu cầu.

Thanh toán online, API vận chuyển, khuyến mãi, đa chi nhánh, nhà cung cấp/giá vốn và quên mật khẩu email vẫn là tính năng mở rộng, không tự thêm vào phạm vi đã chốt.

## Cách tiếp nhận

1. Đọc `07-huong-dan-van-hanh-va-demo.md`, chạy bản demo và thử kịch bản giao thành công/hoàn hàng.
2. Sao lưu database cần giữ trước khi nâng cấp V3.
3. Chạy `mvn verify` và kiểm tra báo cáo. Bản đóng gói: `target/sunicoffee-0.3.0.jar`.
4. Khi có dữ liệu thật/mẫu khoa/môi trường triển khai, dùng các phần còn phụ thuộc đầu vào ở trên làm checklist tiếp tục.
