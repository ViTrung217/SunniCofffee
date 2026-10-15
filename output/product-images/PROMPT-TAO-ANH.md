# Prompt tạo ảnh sản phẩm SuniCoffee

Bộ hiện tại đã có ảnh cho đủ 36 SKU trong `data/products-demo.json`. Phong cách chọn: nền kem, ánh sáng mềm, sản phẩm ở giữa. Khi tạo ảnh mới, đính kèm `../../src/main/resources/static/images/products/demo-cf-001.png` để tham chiếu nền và ánh sáng; nếu là sản phẩm thật, đính kèm thêm ảnh thật và yêu cầu giữ đúng hình dáng.

## Prompt dùng lại

Thay các phần trong ngoặc vuông, xóa những dòng không áp dụng:

```text
Tạo 1 ảnh sản phẩm riêng cho cửa hàng SuniCoffee, phong cách chụp sản phẩm studio chân thực, đồng bộ với ảnh tham chiếu đính kèm.

SẢN PHẨM
- Tên: [tên sản phẩm].
- Loại: [cà phê đóng gói / máy pha / dụng cụ / phụ kiện].
- Hình dáng và bộ phận: [mô tả chính xác cấu tạo nhìn thấy].
- Vật liệu: [giấy nhám / inox xước / gốm tráng men / vải canvas...].
- Màu: [màu sản phẩm; với thiết kế mô phỏng ưu tiên xanh rừng, kem và điểm nhấn cam đất].
- Số lượng và thành phần trong ảnh: [một sản phẩm hoặc liệt kê chính xác từng món của bộ].
- Chữ được phép xuất hiện, ghi đúng nguyên văn: [nội dung nhãn; hoặc “không có chữ”].

BỐ CỤC VÀ ÁNH SÁNG
Ảnh vuông 1:1, độ phân giải tối thiểu 1024 × 1024 px. Nền và mặt bàn liền mạch màu kem ấm nhạt. Sản phẩm ở giữa, nhìn trọn vẹn, chiếm khoảng 65–75% khung hình theo chiều lớn nhất, có khoảng trống đều quanh các cạnh. Góc nhìn ba phần tư phía trước, phối cảnh tự nhiên, thấy rõ đặc điểm chính. Ánh sáng lớn và mềm từ phía trên bên trái, bóng đổ nhẹ sang phải, tiếp xúc mặt bàn tự nhiên. Vật liệu sắc nét, màu trung thực, không bóng nhựa giả, không cháy sáng.

THAM CHIẾU
Ảnh mẫu SuniCoffee chỉ dùng để khớp màu nền, ánh sáng, khoảng trống và phong cách. Không sao chép túi cà phê vào ảnh của sản phẩm khác. Nếu có ảnh sản phẩm thật: giữ nguyên hình dáng, tỷ lệ, màu sắc, logo và các bộ phận của sản phẩm trong ảnh thật; chỉ thay bối cảnh và ánh sáng theo phong cách studio trên.

RÀNG BUỘC
Không thêm đồ trang trí hoặc phụ kiện ngoài danh sách. Không cắt mất tay cầm, vòi, dây hoặc bộ phận sản phẩm. Cấu tạo phải hợp lý, không thêm lỗ hoặc nút không có trong mô tả. Không thêm giá, thông số kỹ thuật, chứng nhận, khẩu hiệu, watermark, khung viền hoặc chữ ngoài nhãn được yêu cầu. Không tạo ảnh ghép nhiều ô. Với thiết kế mô phỏng: không dùng logo hãng khác.
```

## Ví dụ thay phần sản phẩm

- Cà phê: “Một túi cà phê hạt 250 g, túi đứng xanh rừng nhám, khóa zip, nhãn kem hình chữ nhật. Chữ: SUNI COFFEE / WHOLE BEAN / 250 g. Nếu cần thống nhất bao bì, giữ nguyên logo và bố cục nhãn từ ảnh túi tham chiếu, chỉ thay thông tin được chỉ định.”
- Phin: “Một phin cà phê inox gồm thân, nắp và đế. Thành bên kín, không có lỗ; lỗ lọc chỉ ở đáy. Nắp đặt trên thân. Không có cốc hoặc chữ.”
- Cốc: “Một cốc men màu kem, viền và tay cầm xanh rừng, lòng cốc trống. Không có hơi nước, thìa hoặc chữ.”

## Khi dùng ảnh

Kiểm tra chữ, cấu tạo, số món và phần bị cắt trước khi dùng. Ảnh AI của dữ liệu mô phỏng không chứng minh thông số sản phẩm thật. Lưu tên theo SKU, ví dụ `new-cf-004.png`.

Để thêm ảnh cho sản phẩm mới trên ứng dụng hiện tại, dùng chức năng tải ảnh trong trang quản trị sản phẩm. Chỉ chép ảnh vào thư mục hoặc khai báo `image_url` trong JSON không tự gắn ảnh cho SKU mới: ánh xạ ảnh demo hiện chỉ gồm 12 SKU đã có. Ảnh tải lên từ quản trị được ưu tiên hiển thị.
