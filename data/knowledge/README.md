# Kho tri thức SuniCoffee v1

Ngày đối chiếu: 12/10/2026. Có **55 mục**: 15 sản phẩm, 15 pha chế, 25 nghiệp vụ. Mỗi mục có nội dung, ID nguồn, vị trí trong nguồn, phạm vi áp dụng và ngày đối chiếu.

- [Bản đọc 55 mục](../../docs/ai/kho-tri-thuc-v1.md).
- [Dữ liệu JSON](knowledge.v1.json): nội dung dùng cho truy xuất sau này.
- [Danh mục nguồn](sources.v1.json): URL chính thức hoặc đường dẫn trong repo; 6 nguồn bên ngoài, 7 tài liệu/mã nguồn dự án, 1 tài liệu yêu cầu thiết kế.
- [Phạm vi chatbot](../../docs/ai/pham-vi-chatbot-v1.md).
- [30 câu hỏi và đáp án](../../evaluation/chatbot/v1/QUESTIONS.md), [quy trình đánh giá](../../evaluation/chatbot/v1/PROTOCOL.md).

Thông số sản phẩm chỉ là tham khảo hãng, chưa chứng minh hàng đang bán tại SuniCoffee. Không thay dữ liệu demo bằng hàng hãng hoặc tự gán thông số cho SKU giả lập. Nội dung được diễn giải ngắn bằng tiếng Việt, không sao chép toàn bộ tài liệu ngoài. Không chứa dữ liệu cá nhân hay đơn hàng thật. Ngày đối chiếu là ngày đọc nguồn, không phải ngày hãng xuất bản.

Các mục SRC14 là yêu cầu thiết kế được đặt ra cho bước chatbot; các mục nguồn mã và tài liệu 0.3 mô tả nghiệp vụ đã có. Khi tích hợp phải kiểm tra quyền riêng cho dịch vụ đơn hàng; kho tri thức công khai không phải nơi chứa kết quả truy vấn tài khoản. Những quy tắc quản trị như đối soát, doanh thu chỉ giải thích nghiệp vụ, không cấp quyền thao tác cho khách.

Kiểm tra số lượng, ID, tham chiếu, fixture và khóa SHA-256:

```bash
python3 scripts/verify-chatbot-baseline.py
```

Giữ bản v1 làm mốc tái lập. Cập nhật tri thức hoặc nguồn bằng phiên bản mới khi nội dung thay đổi; không coi ngày đối chiếu này là bảo đảm thông tin còn đúng mãi. Không đưa thư mục `evaluation/` vào chỉ mục truy xuất hoặc dữ liệu huấn luyện.
