# Dữ liệu mẫu SuniCoffee

`products-demo.json` chứa 4 danh mục và 12 mặt hàng giả lập để học thiết kế dữ liệu và phát triển chức năng. Tên, giá và tồn kho đều là dữ liệu minh họa, không phải danh mục hoặc giá bán thật của SuniCoffee. Tệp này chưa phải tập lệnh nhập MySQL.

- `sku`: mã duy nhất của mặt hàng bán và quản lý tồn kho.
- `category_code`: mã danh mục, phải tồn tại trong danh sách `categories`.
- `price_vnd`: giá bán mẫu tính bằng đồng Việt Nam.
- `opening_stock`: tồn kho ban đầu giả lập, chưa có nghiệp vụ nhập hoặc bán phát sinh.
- `sales_unit`: đơn vị bán.
- `description`: mô tả minh họa, không phải thông số được kiểm chứng.
- `image_url` và `source_url`: để trống vì chưa có ảnh và nguồn sản phẩm thực tế.

Hai túi cà phê cùng dòng nhưng khác khối lượng có mã riêng để minh họa nhu cầu quản lý biến thể. Cách tách bảng sản phẩm và biến thể sẽ quyết định trong bước thiết kế cơ sở dữ liệu.

Chưa dùng những mô tả này làm kho tri thức có nguồn cho chatbot. Khi có dữ liệu thật, cần xác minh thông tin, nguồn và quyền sử dụng ảnh trước khi thay thế.
