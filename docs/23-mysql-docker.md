# SuniCoffee sử dụng MySQL Docker — 28/10/2026

**Database chính đã chuyển từ H2 sang MySQL 8.4.13 chạy bằng Docker.** Profile mặc định của ứng dụng là `mysql`; `mvn spring-boot:run`, chạy main trong IDE và `bash scripts/run-local.sh` đều dùng MySQL. H2 được giữ làm bản dự phòng và cho kiểm thử, không tự được chọn khi MySQL chưa chạy.

## Chạy từ Terminal hoặc IDE

Mở Docker Desktop. Trong thư mục project:

```bash
bash scripts/run-local.sh
```

Script tự khởi động MySQL, đợi database sẵn sàng rồi chạy ứng dụng. Mở <http://localhost:8080>. Dừng ứng dụng bằng **Ctrl+C**; MySQL và dữ liệu vẫn được giữ.

Có thể dùng `bash scripts/run-mysql.sh` tương đương. Khi chạy class `SuniCoffeeApplication` trong IDE hoặc dùng `mvn spring-boot:run`, khởi động database trước:

```bash
docker compose up -d --wait mysql
mvn spring-boot:run
```

Working directory của IDE phải là thư mục gốc SuniCoffee để đọc các file `.runtime/`. Xóa cấu hình ép `SPRING_PROFILES_ACTIVE=local` cũ nếu có. Tài khoản website và liên kết Google được giữ nguyên; đăng nhập lại sau khi đổi phiên ứng dụng.

## Chạy cả ứng dụng và MySQL trong Docker

Dừng ứng dụng đang chiếm cổng 8080 trước khi dùng cách này:

```bash
mvn -B verify
docker compose --profile app up -d --build
```

Ứng dụng Docker dùng Java 21, kết nối nội bộ tới `mysql:3306`, nhận cấu hình Google/Gemini/SePay/email qua các file mount chỉ đọc và mật khẩu database qua Docker secret. Image không chứa file cấu hình bí mật hoặc H2. UID/GID của ứng dụng lấy từ `.env` do setup tạo để đọc được các file chỉ dành cho chủ sở hữu.

Dừng riêng ứng dụng Docker để quay về chạy Terminal:

```bash
docker compose stop app
bash scripts/run-local.sh
```

## Xem dữ liệu bằng MySQL Workbench

Tạo kết nối **Standard (TCP/IP)**:

| Trường | Giá trị |
|---|---|
| Connection Name | SuniCoffee Docker |
| Hostname | `127.0.0.1` |
| Port | **`3307`** |
| Username | `sunicoffee` |
| Password | Xem giá trị `spring.datasource.password` trong `.runtime/mysql.properties` trên máy của bạn |
| Default Schema | `sunicoffee` |

Không gửi mật khẩu vào chat hoặc đưa `.runtime/` lên Git. Mật khẩu MySQL khác mật khẩu đăng nhập website. Port 3307 được chọn vì 3306 trên máy đã bị chiếm; bên trong container MySQL vẫn dùng 3306.

Nhấn **Test Connection** → **OK**, mở kết nối rồi vào **SCHEMAS → sunicoffee → Tables**. Nhấp phải bảng → **Select Rows - Limit 1000**, hoặc chạy:

```sql
USE sunicoffee;
SELECT COUNT(*) AS tong_san_pham FROM products;
SELECT id, sku, name, price, stock FROM products ORDER BY id;
SELECT id, recipient_name, total, status, created_at
FROM customer_orders ORDER BY id DESC;
```

## Dữ liệu và cấu hình được lưu ở đâu?

- Database MySQL: Docker volume **`sunicoffee_mysql_data`**, tồn tại độc lập với container.
- Cấu hình JDBC cho Terminal/IDE: `.runtime/mysql.properties`.
- Mật khẩu ứng dụng/root và client root cho công cụ backup: `.runtime/docker/`, quyền file 600; không chứa trong Compose hoặc Git.
- Các cấu hình Google, chatbot, SePay và email hiện có tiếp tục được đọc từ `.runtime/`.
- Nguồn H2 gốc: `.runtime/sunicoffee.mv.db`, được giữ nguyên khi chuyển.
- Snapshot H2 dùng để chuyển chính thức: `.backups/mysql-migration-20261028-9f6f662d/source.mv.db`.
- Bản sao lưu MySQL sau chuyển: `.backups/mysql-after-migration-20261028.sql`.

Dừng database khi không cần dùng: `docker compose stop mysql`. Khởi động lại: `docker compose up -d --wait mysql`. Không dùng `docker compose down -v` nếu cần giữ dữ liệu, vì tùy chọn `-v` xóa volume.

## Sao lưu và thử khôi phục

```bash
bash scripts/backup-mysql-docker.sh
# Hoặc chọn đường dẫn file mới:
bash scripts/backup-mysql-docker.sh .backups/ban-sao-moi.sql
```

Script không ghi đè bản sao cũ, dùng transaction nhất quán và giữ nguyên ảnh BLOB dưới dạng hex. File SQL chứa dữ liệu riêng nên được tạo với quyền 600.

Thử khôi phục vào **container kiểm thử riêng, cổng 13306**:

```bash
bash scripts/restore-mysql-docker-check.sh .backups/ban-sao-moi.sql
```

Script chỉ chấp nhận tên database mới có hậu tố `_restore_check`; không phục hồi đè database chính. Sau khi kiểm tra, dừng container thử:

```bash
docker compose --profile test stop mysql-test
```

Container kiểm thử dùng tmpfs; dữ liệu thử không được giữ khi dừng. MySQL chính dùng volume riêng. Bản backup sau chuyển đã được khôi phục thử và so sánh toàn bộ dữ liệu, ảnh và bộ đếm ID với nguồn.

## Công cụ chuyển dữ liệu dùng lại cho máy khác

Lần chuyển hiện tại **đã hoàn tất**, không cần chạy lại. Với máy/database khác, mở Docker Desktop, tạo cấu hình một lần, build và khởi động MySQL:

```bash
python3 scripts/setup-mysql.py
mvn -B verify dependency:build-classpath -Dmdep.outputFile=.runtime/migration-classpath.txt
docker compose up -d --wait mysql
# Dừng ứng dụng đang mở H2 trước:
python3 scripts/migrate-h2-to-mysql.py
```

Setup không thay mật khẩu đã có và từ chối tạo lại bí mật khi phát hiện volume cũ thiếu cấu hình. Máy mới cần nhận bản backup và cấu hình bí mật qua kênh riêng; GitHub không chứa dữ liệu database hoặc mật khẩu.

Công cụ sao lưu H2 trước, tạo schema đích bằng Flyway, chỉ nhập vào database rỗng, giữ khóa ngoại hoạt động và nạp theo thứ tự phụ thuộc. Toàn bộ hàng được so sánh trước khi commit; sau đó phục hồi và xác minh bộ đếm ID, kể cả bảng hiện đang trống. Không nhập lịch sử Flyway H2 vào MySQL; MySQL có lịch sử migration gốc riêng.

Migration **V12** tăng các cột thời gian nghiệp vụ MySQL lên `TIMESTAMP(6)` để giữ nguyên microsecond từ H2. Giá/số tiền, Unicode, NULL, dữ liệu nhị phân, mật khẩu đã băm, liên kết Google, phiên bản tài khoản và ID được đối chiếu đầy đủ.

Nếu một lần nhập bị từ chối do database đích có dữ liệu, công cụ không xóa để nhập lại. Chọn database mới hoặc kiểm tra tình trạng trước khi xử lý tiếp.

## Kiểm chứng đợt chuyển

- **175/175 kiểm thử H2 đạt** và **175/175 kiểm thử MySQL 8.4.13 đạt**, trên database kiểm thử riêng.
- Chuyển thử, chuyển chính thức, kiểm tra lại và khôi phục backup đều khớp **19 bảng nghiệp vụ / 11 bộ đếm ID**.
- Giữ nguyên **50 sản phẩm, 28 tài khoản, 75 đơn, 60 đánh giá, 120 yêu thích**, cùng ảnh tải lên và toàn bộ lịch sử liên quan.
- Toàn bộ 50 trang chi tiết và ảnh truy cập được trên ứng dụng Docker; danh mục giống hoàn toàn khi chạy từ JVM host với profile mặc định MySQL.
- Nguồn H2 gốc được kiểm tra SHA-256 và không bị thay đổi. Khởi động lại container MySQL vẫn giữ nguyên toàn bộ dữ liệu và bộ đếm ID.
- Không gọi thanh toán, Google OAuth, gửi email hoặc Gemini thật trong quá trình kiểm tra chuyển database.

Tổng hợp: [kết quả chuyển MySQL](verification/mysql-migration-20261028/results.json). Bằng chứng trong [thư mục nghiệm thu](verification/mysql-migration-20261028/), đặc biệt [đối chiếu nhập chính thức](verification/mysql-migration-20261028/production-import.json), [khôi phục backup](verification/mysql-migration-20261028/backup-restored.json) và [HTTP](verification/mysql-migration-20261028/http.json).

Flyway 11.7.2 hiện có cảnh báo MySQL 8.4 mới hơn phiên bản đã được thư viện này xác minh; migration và cả bộ kiểm thử thực tế trên 8.4.13 đều đạt. Cảnh báo được giữ nguyên, không tắt xác minh migration. Theo dõi [vấn đề tương thích upstream](https://github.com/flyway/flyway/issues/4168) khi nâng dependency.

Chạy H2 dự phòng chỉ khi chủ động cần quay lại dữ liệu trước chuyển: `bash scripts/run-h2.sh`. Sau khi MySQL nhận giao dịch mới, H2 là bản cũ; không đổi ngược trực tiếp nếu chưa xử lý dữ liệu phát sinh.

Tham khảo: [Docker Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/), [MySQL Docker](https://dev.mysql.com/doc/refman/8.4/en/docker-mysql-getting-started.html), [Workbench TCP/IP](https://dev.mysql.com/doc/workbench/en/wb-mysql-connections-methods-standard.html).
