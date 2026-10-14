# Phát hành SuniCoffee 1.0.0 và bàn giao đồ án

Cập nhật hướng dẫn: 14/10/2026. Mục tiêu được xác nhận: bản hoàn chỉnh để bàn giao và bảo vệ đồ án, chưa mở cửa hàng cho khách mua thật. Số phiên bản chỉ phiên bản phần mềm; không chứng nhận giá, tồn kho hay chính sách đã được cửa hàng xác minh.

**Phạm vi cuối: chỉ chạy local, không deploy, không yêu cầu tên miền, HTTPS, Nginx hay máy chủ.** Các mẫu deploy và profile prod thử trong lúc rà soát đã được loại khỏi bản bàn giao.

## Các chế độ chạy

| Profile | Mục đích | Cơ sở dữ liệu | Nạp mẫu / tài khoản công khai |
| --- | --- | --- | --- |
| `local` (mặc định) | Chạy bản bàn giao trên máy | H2 tại `.runtime/sunicoffee` | Không tự nạp; giữ dữ liệu cũ |
| `mysql` | Nghiệm thu đồ án với MySQL trên máy | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Mặc định không nạp; chỉ bật `SEED_DEMO` khi chủ động dựng dữ liệu minh họa |
| `demo` | Dựng lại dữ liệu minh họa trên máy riêng | Cùng tệp H2 cục bộ | Tự nạp khi bảng trống; không dùng trên máy công khai |
| `test` | Kiểm thử tự động | H2 bộ nhớ, hoặc MySQL kiểm thử riêng | Không dùng dữ liệu cần giữ |

`local` giữ tệp H2, đơn cũ và khóa Gemini hiện có tại `.runtime/chatbot.properties`. Chuyển profile không tự chuyển dữ liệu H2 sang MySQL. Không chạy hai phiên cùng trỏ tệp H2. Dừng phiên 8080 cũ trước khi chạy JAR mới. Nếu Terminal đang đặt `SPRING_PROFILES_ACTIVE=demo`, bỏ biến đó hoặc chỉ rõ `--spring.profiles.active=local`.

## Chạy bản bàn giao

```bash
python3 scripts/verify-chatbot-baseline.py
mvn -B verify
bash scripts/run-local.sh
```

Truy cập `http://localhost:8080`. Máy hiện tại đã có tài khoản và dữ liệu, không cần nạp lại. Trên máy mới, tạo quản trị bằng `ADMIN_EMAIL` và `ADMIN_PASSWORD` trong môi trường trước khi chạy; mật khẩu tối thiểu 8 ký tự, tối đa 72 byte UTF-8. Không điền mật khẩu vào lệnh có thể lưu trong shell history. Có thể dùng một phiên Bash để nhập kín:

```bash
bash
read -r -p 'Email quản trị: ' ADMIN_EMAIL
read -r -s -p 'Mật khẩu quản trị: ' ADMIN_PASSWORD
export ADMIN_EMAIL ADMIN_PASSWORD
bash scripts/run-local.sh
```

Khởi tạo quản trị không đặt lại mật khẩu của tài khoản đã tồn tại. Sau lần tạo đầu, bỏ hai biến bootstrap. Tài khoản công khai từ phiên demo cũ vẫn tồn tại nếu dùng dữ liệu cũ: tạo quản trị riêng, đăng nhập tài khoản mới rồi khóa tài khoản demo trong Quản trị → Tài khoản. Không tự xóa tài khoản để tránh mất lịch sử.

Máy mới không có danh mục: nhập danh mục/sản phẩm ở Quản trị; mẫu và công cụ kiểm tra ở [data/catalog](../data/catalog/README.md). Nếu dùng dữ liệu mẫu khi bảo vệ, phải công bố đó là dữ liệu mô phỏng. Nhãn dữ liệu hiện dựa trên profile demo hoặc SKU `DEMO-`; không phải cơ chế chứng nhận mọi SKU khác đều là hàng thật.

## Kiểm tra ứng dụng đang chạy

```bash
curl --fail http://127.0.0.1:8080/actuator/health
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

Health chỉ trả trạng thái, không lộ kết nối DB hoặc khóa. Readiness có kiểm tra DB; liveness tách khỏi DB. Health UP không có nghĩa Gemini còn hạn mức hay đã đạt điểm đánh giá chất lượng. Khi Gemini lỗi, các chức năng bán hàng/tra cứu đơn vẫn độc lập với AI.

## Nghiệm thu trước bảo vệ

1. Build và kiểm thử H2, MySQL riêng; kiểm tra khóa bộ 30 câu. Lưu kết quả ở `docs/verification/release-1.0.0/`.
2. Đăng ký/đăng nhập, tìm sản phẩm, thêm giỏ, đặt COD; xác nhận → giao → hoàn thành → đối soát; thử hủy và kiểm tra tồn hoàn một lần.
3. Quản trị sửa sản phẩm/ảnh, nhập xuất kho, lọc đơn, xem doanh thu và khóa tài khoản. Không dùng tiền/địa chỉ thật trong buổi diễn tập.
4. Thử cửa sổ chat ở góc phải: mở/thu nhỏ, gửi nhiều lượt, xóa cuộc trò chuyện, người khác không xem được đơn. Chạy bộ đánh giá AI theo hướng dẫn dưới, phân biệt kiểm thử chức năng và điểm chất lượng.
5. Sao lưu rồi khôi phục vào cơ sở dữ liệu thử riêng; ghi thời điểm và kết quả. Không coi một tệp backup chưa thử khôi phục là đủ bằng chứng.
6. Chốt báo cáo theo mẫu khoa, ảnh màn hình, sơ đồ và kịch bản [thuyết trình](09-dan-y-thuyet-trinh.md). Không tự điền khảo sát, doanh thu thật hay chữ ký nghiệm thu.

## Sử dụng và trình diễn

Khách tìm sản phẩm → thêm giỏ → nhập thông tin nhận hàng → đặt COD → xem **Đơn của tôi**. Giỏ chưa giữ tồn; chỉ hủy đơn còn chờ xác nhận. Giá và thông tin nhận hàng được chụp vào đơn.

Admin vào **Đơn hàng**, xác nhận đơn rồi nhập hãng/mã vận đơn để bàn giao. Giao thành công và **Đã nhận đủ tiền COD** là hai thao tác riêng. Nếu giao thất bại, nhập lý do rồi chọn giao lại hoặc chuyển hoàn; chỉ hoàn kho sau khi nhận kiện và xác nhận số còn bán được của từng dòng.

Nhập/xuất ở **Kho**, không sửa trực tiếp tồn trên biểu mẫu sản phẩm. Phiếu sai được bù bằng phiếu ngược chiều có lý do. Báo cáo doanh thu dùng ngày hoàn thành, loại phí giao và chưa trừ giá vốn; không đồng nghĩa tiền COD đã thu.

Cấu hình tùy chọn: `SHIPPING_FEE=0`, `PENDING_REMINDER_HOURS=48`; khởi động lại sau khi đổi. Phí mới không thay phí trên đơn đã đặt. Xem [chat với khách của admin](17-chat-khach-hang.md), [chatbot Gemini](12-chatbot-gemini.md) và [cách nạp 36 sản phẩm mẫu](../data/README-products.md).

Khi diễn tập, dùng tài khoản và đơn giả lập: đặt → xác nhận → giao thành công → đối soát; thêm một đơn giao thất bại → nhận hoàn để kiểm tra tồn. Mở hai phiên khách/admin riêng để thử gửi/nhận chat. Kiểm tra desktop và chiều rộng 390 px, thao tác bàn phím, thông báo lỗi biểu mẫu và nút mở/thu nhỏ cửa sổ chat. Đây là checklist, không phải tuyên bố các bước đã được nghiệm thu trực quan.

## Đánh giá chatbot có thể chạy lại

```bash
mvn -B -Dtest=ChatbotEvaluationIT -Dchatbot.eval.live=true \
  -Dchatbot.eval.output=evaluation/results/ten-dot-moi test
```

Lệnh này gọi Gemini thật, có thể tính phí/hạn mức; chỉ chạy khi chủ động đánh giá. Khóa đọc từ `GEMINI_API_KEY` hoặc tệp cục bộ đã được bỏ qua Git. Không đưa khóa vào tham số dòng lệnh. Mỗi câu là hội thoại mới, 30 câu × 3 lần; backend fixture tách biệt, không đọc/ghi đơn thật. Hệ thống chỉ gửi câu hỏi và tri thức truy xuất đến Gemini, không gửi đáp án/fixture. Mô hình và hash prompt/mã nguồn được ghi vào metadata.

`ChatbotEvaluationIT` không tự chạy trong `mvn verify`. Thành công của bộ thu thập không đồng nghĩa đạt chất lượng. Kết quả nguyên văn, nguồn và dấu vết tra cứu nằm ngoài `evaluation/chatbot/v1`; chấm theo [quy trình đã khóa](../evaluation/chatbot/v1/PROTOCOL.md). Bộ thu thập kiểm tra tầng dịch vụ với danh tính mô phỏng; các kiểm thử tích hợp riêng kiểm tra HTTP, phiên và quyền trên DB thật. Phải nói rõ giới hạn này trong báo cáo.

## MySQL trên máy cá nhân (tùy chọn)

H2 mặc định đủ để chạy bản bàn giao mà không cài MySQL. Khi cần trình bày hệ quản trị cơ sở dữ liệu MySQL, tạo DB riêng và cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, rồi chạy:

```bash
java -jar target/sunicoffee-1.0.0.jar --spring.profiles.active=mysql
```

Không bật `SEED_DEMO` khi dùng danh mục đã xác nhận. Cả H2 và MySQL local đọc cùng tệp khóa Gemini cục bộ. Không tự chuyển dữ liệu giữa hai hệ quản trị.

## Sao lưu bản local

H2: dừng ứng dụng bằng Ctrl+C, chờ dừng hoàn tất, sao chép `.runtime/sunicoffee.mv.db` ra vị trí sao lưu riêng rồi khởi động lại. Không sao chép tệp đang được ghi và không đưa tệp DB/khóa vào Git. Khi thử khôi phục, chép backup sang thư mục mới và đặt `spring.datasource.url` trỏ bản sao; không ghi đè bản đang dùng.

MySQL local: dùng `scripts/backup-mysql.sh` và `scripts/restore-mysql-check.sh` với tệp kết nối riêng. Lệnh restore chỉ chấp nhận DB mới có hậu tố `_restore_check`. Đối chiếu đơn, tổng tiền, COD, tồn và nhật ký sau khôi phục.

Tệp kết nối MySQL đặt ngoài repository, phân quyền `chmod 600`. Chạy với đường dẫn riêng của máy:

```bash
scripts/backup-mysql.sh /absolute/private/client.cnf sunicoffee /absolute/backups/sunicoffee.sql
scripts/restore-mysql-check.sh /absolute/private/client.cnf sunicoffee_restore_check /absolute/backups/sunicoffee.sql
```

Backup không ghi đè tệp có sẵn; nếu lệnh lỗi, không dùng tệp dở. Không thay schema trong khi sao lưu. Restore dừng nếu DB đích đã tồn tại; chỉ dùng dump đáng tin cậy. Kiểm tra thêm ảnh, đăng nhập và lịch sử chat trên bản khôi phục. Giữ backup ngoài Git.

## Giới hạn đã công bố

Bản này chạy trên một máy và một tiến trình. Session, giới hạn đăng nhập/AI và lịch sử chatbot AI ở bộ nhớ; restart kết thúc phiên và ngữ cảnh AI. Tin nhắn khách–admin và trạng thái hội thoại lưu trong DB, vẫn còn sau khi đăng nhập lại; giới hạn gửi tin nhân viên được tính từ tin đã lưu. Gemini cần Internet và còn hạn mức; nếu không có AI, cửa hàng và tra cứu giá/tồn/đơn vẫn hoạt động. Không cần bật cổng ra Internet để bảo vệ đồ án.

Trạng thái Gemini chỉ cho biết đã cấu hình, không bảo đảm dịch vụ luôn sẵn sàng. Xem phản hồi nguyên văn và kết quả chấm sơ bộ ở [biên bản nghiệm thu](14-nghiem-thu-1.0.0.md). Khi báo cáo điểm AI, nêu rõ Codex hỗ trợ chấm, không phải đánh giá mù bởi người độc lập.

Tham chiếu health/readiness: [Spring Boot Actuator endpoints](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html).
