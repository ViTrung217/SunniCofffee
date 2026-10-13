# Kiểm tra cửa sổ chat — 13/10/2026

Ứng dụng: `http://localhost:8080/`, người dùng chưa đăng nhập. Thao tác qua giao diện Safari trên macOS.

| Thao tác | Kết quả quan sát |
| --- | --- |
| Tải lại trang chủ | Có nút “Chat với Suni” ở góc dưới bên phải. |
| Mở cửa sổ chat | Có tiêu đề, nút thu gọn, link mở trang đầy đủ; iframe `/chat?widget=true` tải lời chào, biểu mẫu và trạng thái AI đã được bật. |
| Gửi “AeroPress Original dùng giấy lọc cỡ nào?” | Hiện trạng thái đang tìm câu trả lời; vô hiệu nút gửi trong khi chờ. |
| Nhận phản hồi | “AeroPress Original sử dụng giấy lọc cỡ Standard.” và link `https://aeropress.com/products/aeropress-coffee-maker`; nút gửi được bật lại. |
| Bấm thu gọn | Khung chat ẩn, nút mở chat trở về trạng thái collapsed. |
| Mở lại | Câu hỏi, câu trả lời và link nguồn còn nguyên. |

Đã xác nhận nội dung và thao tác qua cây trợ năng. Công cụ chụp màn hình có lúc trả ảnh trắng/ảnh trễ nên chưa xác nhận đầy đủ bố cục panel bằng ảnh. Chưa kiểm tra trực tiếp trên điện thoại, Escape, nhiều lượt đồng thời hoặc bộ 30 câu đã khóa. Chrome có hiện nút mở chat, nhưng phiên kiểm tra Chrome bị gián đoạn bởi lỗi công cụ ghi màn hình; không tính là đã kiểm tra end-to-end trên Chrome.
