# Làm mới giao diện cửa hàng — 07/10/2026

Trang chủ dùng tông xanh rừng, kem và cam đất, với phần giới thiệu lớn, minh họa cà phê dã ngoại, liên kết danh mục, danh sách sản phẩm và gợi ý chuẩn bị hành trình. Trang `/products` tập trung vào tìm kiếm/lọc sản phẩm. Header và footer dùng chung được đồng bộ.

CSS bổ sung nằm ở `src/main/resources/static/css/storefront.css`; JavaScript thuần nằm ở `src/main/resources/static/js/storefront.js`. Không thêm thư viện hoặc tải font từ bên ngoài. Giá, tồn kho, danh mục và ảnh vẫn lấy từ dữ liệu hiện có; ảnh sản phẩm mẫu vẫn là ảnh minh họa.

Hiệu ứng gồm xuất hiện khi cuộn, chuyển động mở đầu của minh họa, nâng thẻ và phóng nhẹ ảnh khi rê chuột. Nội dung luôn đọc được khi JavaScript không chạy. Có nút tắt chuyển động lưu trên trình duyệt, hỗ trợ `prefers-reduced-motion`, liên kết bỏ qua điều hướng và bố cục responsive.

Kiểm chứng: `mvn -q -Ddebug=false test` đạt 61 ca, không thất bại hoặc lỗi trên cấu hình kiểm thử H2. Ca `publicPagesAndApiRender` bổ sung kiểm tra phân biệt trang chủ/trang tìm kiếm và truy cập CSS/JS khi chưa đăng nhập. `git diff --check` đạt. Môi trường không có lệnh Node nên chưa chạy kiểm tra cú pháp JavaScript bằng Node. Chưa chạy lại MySQL vì không thay đổi nghiệp vụ dữ liệu.

Đã quan sát phần đầu trang chủ trên Safari bằng bản demo H2 trong bộ nhớ riêng. Công cụ điều khiển cửa sổ mất kết nối khi cuộn tiếp; chưa hoàn tất quan sát cuối trang, tương tác toàn bộ hiệu ứng hoặc kiểm tra trên điện thoại thật. Cần kiểm tra thêm ở chiều rộng 360/390/768 px, tìm kiếm có/không có kết quả, tab bàn phím và bật/tắt chuyển động.

## Điều chỉnh theo trang tham khảo Highlands

Tham khảo cấu trúc nội dung tại https://www.highlandscoffee.com.vn/: điều hướng theo nhóm sản phẩm và các khối khám phá với lời kêu gọi hành động rõ ràng. Áp dụng cho SuniCoffee bằng tiêu đề banner đậm, ô danh mục lớn và khối giới thiệu thương hiệu dẫn tới danh sách sản phẩm. Giữ nhận diện SuniCoffee; không đưa các dịch vụ chưa có vào giao diện. Không sao chép ảnh hoặc logo Highlands.

Sau điều chỉnh, chạy riêng `ShopIntegrationTest`: 21 ca đạt, không lỗi; `git diff --check` đạt. Chưa quan sát trực quan lại phiên bản điều chỉnh này.
