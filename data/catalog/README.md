# Chuẩn bị danh mục được xác nhận

Sao chép `catalog.template.json` ra `.runtime/catalog-reviewed.json`, điền dữ liệu rồi chạy:

```bash
python3 scripts/validate-catalog.py .runtime/catalog-reviewed.json
```

Mẫu cố ý không có giá, tồn kho hoặc thông số bịa. Mẫu chưa điền sẽ không vượt qua kiểm tra. Script chỉ đọc tệp, không thay đổi cơ sở dữ liệu và không chứng minh tính đúng đắn của nguồn.

| Trường | Cách thu thập / nhập |
| --- | --- |
| categories | Mã, tên, mô tả; nhập ở Quản trị → Danh mục trước |
| sku / name / category_code | Mã nội bộ duy nhất và mẫu cụ thể; không đổi SKU DEMO thành SKU thật |
| description | Thông số đúng mẫu từ hãng/tài liệu có nguồn; không gán thông số từ sản phẩm tương tự |
| price_vnd | Giá bán VND nguyên, được người phụ trách xác nhận |
| opening_stock / stock_counted_on | Tồn đếm thực tế và ngày kiểm kê; chỉ nhập tồn đầu khi tạo sản phẩm mới |
| sales_unit | túi 250 g, chiếc… theo đơn vị bán thật |
| active | Chỉ mở bán sau khi kiểm tra đầy đủ |
| source / verified_by / verified_on | URL hoặc tên tài liệu và trang, người xác nhận, ngày xác nhận; lưu tệp đã duyệt trong hồ sơ bàn giao riêng |
| image_file / image_rights | Tệp ảnh và căn cứ quyền sử dụng; tải ảnh qua trang sửa sản phẩm |

Sau khi kiểm tra, nhập thủ công qua biểu mẫu quản trị đã có; đây **không phải tính năng nhập JSON hàng loạt**. Thay đổi tồn sau khi tạo phải dùng Nhập/xuất kho để giữ nhật ký. Ngừng bán sản phẩm mẫu, giữ nguyên đơn cũ và lịch sử tồn; không chạy DELETE để chuyển dữ liệu.

Thông tin nghiệp vụ cần chủ cửa hàng bổ sung: liên hệ, phí/khu vực/thời gian giao, thời hạn và điều kiện đổi trả, bảo hành theo từng mẫu, quy trình xác nhận/đối soát COD, người phụ trách sao lưu. Không tự công bố chính sách chưa được duyệt.

Kho tri thức `v1` đã khóa không sửa theo dữ liệu mới. Khi có tài liệu được xác nhận, tạo phiên bản tri thức/đánh giá tiếp theo và ghi nguồn. Giá/tồn hiện tại của sản phẩm vẫn do chatbot đọc từ cơ sở dữ liệu.
