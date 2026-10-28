# Mở rộng danh mục SuniCoffee lên 50 sản phẩm — 28/10/2026

Theo yêu cầu của chủ dự án, bổ sung **14 sản phẩm**, đưa danh mục từ 36 lên **50 SKU / 4 nhóm**: 14 cà phê, 8 máy pha, 17 dụng cụ và 11 phụ kiện. Tên, mô tả, giá, tồn đầu và đường dẫn của 36 sản phẩm gốc trong JSON được giữ nguyên.

## Sản phẩm mới

Giá và tồn đầu dưới đây là dữ liệu bổ sung cho danh mục dự án theo phạm vi đã được giao.

| SKU | Sản phẩm | Giá | Tồn đầu |
|---|---|---:|---:|
| DEMO-CF-011 | Cà phê Arabica rang đậm 500 g | 235.000 ₫ | 18 |
| DEMO-CF-012 | Cà phê phối trộn pha espresso 1 kg | 325.000 ₫ | 16 |
| DEMO-CF-013 | Cà phê xay pha cold brew 500 g | 175.000 ₫ | 22 |
| DEMO-CF-014 | Cà phê túi lọc treo 10 g × 20 túi | 205.000 ₫ | 24 |
| DEMO-MC-007 | Máy pha espresso di động Suni G | 1.250.000 ₫ | 7 |
| DEMO-MC-008 | Máy pha nhỏ giọt mini Suni H | 990.000 ₫ | 9 |
| DEMO-BR-013 | Bình pha pour over thủy tinh 600 ml | 285.000 ₫ | 15 |
| DEMO-BR-014 | Phễu lọc cà phê inox 2 lớp | 195.000 ₫ | 20 |
| DEMO-BR-015 | Ấm pha moka 3 tách | 340.000 ₫ | 12 |
| DEMO-BR-016 | Tay nén cà phê espresso 51 mm | 180.000 ₫ | 18 |
| DEMO-BR-017 | Bộ chia hạt cà phê 6 ống | 245.000 ₫ | 14 |
| DEMO-OD-009 | Túi cuộn dụng cụ pha cà phê | 290.000 ₫ | 17 |
| DEMO-OD-010 | Bình rót dã ngoại 350 ml | 220.000 ₫ | 20 |
| DEMO-OD-011 | Hộp đựng giấy lọc di động | 110.000 ₫ | 25 |

## Ảnh sản phẩm

Đã tạo 14 ảnh riêng bằng **built-in image_gen**, không dùng lại ảnh của sản phẩm khác. Ảnh JPEG vuông 1200 × 1200 px, tông kem–xanh rừng, ánh sáng studio mềm, tổng 2.445.316 byte. Đã quan sát từng ảnh sau khi tạo và kiểm tra định dạng, kích thước, SHA-256.

- Ảnh sử dụng trên website: `src/main/resources/static/images/products/demo-cf-011.jpg` … `demo-cf-014.jpg`, `demo-mc-007.jpg` … `demo-mc-008.jpg`, `demo-br-013.jpg` … `demo-br-017.jpg`, `demo-od-009.jpg` … `demo-od-011.jpg`.
- [Manifest đầy đủ: đường dẫn, từng prompt, mã băm, dung lượng và kích thước](../output/product-images/catalog-50/manifest.json).
- [Dữ liệu sản phẩm](../data/products-demo.json) và [quy tắc nạp](../data/README-products.md).

## Cập nhật database đang dùng

Thêm cờ một lần `--app.catalog.add-missing=true`. Chế độ này chỉ thêm SKU chưa có và nhật ký tồn đầu; bỏ qua hoàn toàn bản ghi đã tồn tại. Không nạp lại lịch sử hoạt động mẫu. Chạy lặp không cộng tồn hoặc tạo trùng sản phẩm.

```bash
mvn -B verify
# Dừng ứng dụng và sao lưu database trước khi cập nhật.
bash scripts/run-local.sh --app.catalog.add-missing=true
# Sau khi nạp và kiểm tra, dừng rồi chạy lại bình thường:
bash scripts/run-local.sh
```

`--app.catalog.refresh-sample=true` vẫn là thao tác chủ động riêng để cập nhật mô tả theo JSON; không dùng cho lần bổ sung dữ liệu đã chốt này. Không có thay đổi schema.

## Kiểm chứng

**173/173 kiểm thử H2 đạt**, không lỗi hoặc bỏ qua; baseline chatbot, 6 kiểm thử cấu hình và hồi quy JavaScript đạt. HTTP xác nhận **50/50 trang và 50/50 ảnh** truy cập được. Đối chiếu SHA-256 từng bản ghi của 20 bảng trên bản sao H2 trước/sau: mọi bản ghi cũ giữ nguyên; chỉ thêm 14 sản phẩm và 14 nhật ký tồn đầu. Bản local đã khởi động lại bình thường không kèm cờ nhập, readiness UP và vẫn đủ 50 sản phẩm.

Bộ kiểm thử kiểm tra danh mục mới đủ ảnh, dữ liệu và nhật ký tồn đầu; có ca bổ sung dựng database 36 SKU, thêm đúng 14 SKU rồi nạp lặp, xác nhận tên/mô tả/giá/tồn/ảnh/trạng thái cũ giữ nguyên. Kết quả build, ảnh, nạp local và đối chiếu dữ liệu được lưu tại [bằng chứng mở rộng danh mục](verification/catalog-50-20261028/results.json).

Các nâng cấp giao diện từ đợt rà soát trước được đưa vào cùng lần bàn giao mã nguồn này. [Báo cáo giao diện và giới hạn tích hợp](21-danh-gia-cuoi-cung-20261028.md) giữ kết quả kiểm tra tại thời điểm danh mục còn 36 sản phẩm.
