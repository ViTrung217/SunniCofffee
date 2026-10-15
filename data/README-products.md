# Danh mục sản phẩm phục vụ đồ án

Bản dữ liệu: **14/10/2026**, tệp [products-demo.json](products-demo.json). Gồm **36 sản phẩm / 4 nhóm**.

| Nhóm | Số lượng | Nội dung bổ sung |
| --- | ---: | --- |
| Cà phê | 10 | Hạt, xay sẵn, các mức rang, Arabica/Robusta/phối trộn, túi lọc treo, bộ quy cách nhỏ |
| Máy pha | 6 | Cầm tay, để bàn, nhỏ giọt, viên nén, bán tự động |
| Dụng cụ pha | 12 | Phin, phễu, cối xay, French press, cold brew, ấm rót, cân, giấy lọc, ca sữa, chổi, hũ |
| Phụ kiện cà phê phượt | 8 | Cốc, túi, bộ phụ kiện, bình mang đồ uống, cốc gấp, hộp chia hạt, thảm, khăn |

<a id="nguon-va-pham-vi"></a>

## Nguồn và phạm vi

Đây là **dữ liệu do dự án biên soạn để mô phỏng danh mục**, không phải kết quả khảo sát cửa hàng hoặc catalogue của nhà sản xuất. `source_ref` trỏ về tài liệu này; `source_url` để `null` vì chưa có nguồn sản phẩm thực tế. Không coi đường dẫn này là chứng cứ xác minh thông số hãng.

Tên, giá, tồn ban đầu và thuộc tính của các SKU `DEMO-*` là giả lập. Quy cách mới (khối lượng, dung tích, số món) được ghi rõ là giả lập; các thông số chưa biết như công suất, áp suất, khả năng giữ nhiệt, tính tương thích, xuất xứ và bảo hành được ghi là chưa xác minh. Không gán các SKU này cho Wacaco, HARIO hoặc thương hiệu thật.

Mỗi mô tả có phần giới thiệu, quy cách/thông tin, gợi ý sử dụng, bảo quản/lưu ý và nguồn dữ liệu. Gợi ý chung không thay thế tài liệu đúng model. Bốn hình SVG mới là minh họa nhóm, có nhãn ngay trên ảnh; 12 ảnh minh họa ban đầu được giữ. Mô tả luôn được xuất bằng `th:text`, không chạy HTML nhập vào.

Kho tri thức và bộ đánh giá chatbot v1 vẫn giữ nguyên. Chatbot nhận mô tả/giá/tồn qua danh mục công khai; thông tin giả lập không biến thành thông số đã được hãng xác nhận. Khi có hàng thật, dùng [cấu trúc thu thập dữ liệu xác minh](catalog/README.md).

## Nạp hoặc cập nhật database local

Mặc định khởi động **không tự nạp** dữ liệu. Với DB trống, profile `demo` có thể tạo toàn bộ danh mục theo cơ chế seed hiện có.

Với DB đang dùng, dừng phiên ứng dụng hiện tại bằng Ctrl+C, sao lưu DB khi đã dừng, rồi chạy một lần:

```bash
mvn -B verify
bash scripts/run-local.sh --app.catalog.refresh-sample=true
```

Sau khi kiểm tra xong, Ctrl+C và chạy lại bình thường:

```bash
mvn spring-boot:run
```

Không lưu cờ `app.catalog.refresh-sample=true` vào cấu hình thường trực. Lần chủ động nhập lại sẽ cập nhật mô tả SKU mẫu theo tệp dữ liệu, nên có thể thay phần mô tả mẫu đã chỉnh tay.

Quy tắc nhập:

- SKU mẫu đã có: chỉ cập nhật mô tả; giữ ID, tên, giá, tồn, danh mục, ảnh và trạng thái bán. Liên kết đơn hàng giữ nguyên.
- SKU mẫu chưa có: thêm sản phẩm và ghi nhật ký tồn đầu tương ứng; chạy lại không cộng tồn hoặc tạo thêm SKU trùng.
- Sản phẩm ngoài danh mục mẫu: giữ nguyên dữ liệu.
- Không xóa tài khoản, giỏ hàng, đơn hàng hoặc nhật ký cũ; toàn bộ lần nhập nằm trong một giao dịch.

Nếu thay giá/tồn trong JSON rồi nhập lại, giá/tồn của SKU đã có **không** đổi. Dùng quản trị sản phẩm và nghiệp vụ nhập/xuất kho để thay đổi số liệu hiện hành.

## Xác nhận sử dụng ngày 15/10/2026

Chủ dự án đã xác nhận và đồng ý sử dụng toàn bộ danh mục mặc định cho đồ án. Đây là phê duyệt dữ liệu dự án, không thay đổi nguồn gốc biên soạn thành khảo sát hàng thực tế. Đã bổ sung 24 ảnh AI riêng, đủ ảnh cho 36 SKU. Xem [manifest ảnh mới](../output/product-images/cream-v2/manifest.json).
