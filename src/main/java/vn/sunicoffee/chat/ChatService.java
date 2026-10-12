package vn.sunicoffee.chat;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Pattern ORDER_CODE = Pattern.compile("(?i)\\bSUNI[\\s-]*(\\d+)\\b");
    private static final Pattern ORDER_NUMBER = Pattern.compile("\\b(?:don|order)\\s+(?:so\\s+|ma\\s+|#\\s*)?(\\d+)\\b");
    private final KnowledgeBase knowledge;
    private final GeminiClient gemini;
    private final ChatDataService data;
    public ChatService(KnowledgeBase knowledge, GeminiClient gemini, ChatDataService data) {
        this.knowledge = knowledge; this.gemini = gemini; this.data = data;
    }
    public boolean ready() { return gemini.ready(); }
    public ChatReply answer(String question, String authenticatedEmail) {
        String normalized = KnowledgeBase.normalize(question);
        // Security and order routing run before any model call; no order reply becomes model history.
        if (Pattern.compile("\\b(mat khau|password|api key|toan bo don|tat ca don|khach khac|nguoi khac|xam nhap)\\b").matcher(normalized).find()) {
            return ChatReply.of("REFUSED", "Mình không cung cấp dữ liệu riêng hoặc thông tin đăng nhập của khách hàng. Bạn chỉ có thể tra cứu đơn của chính mình sau đăng nhập.");
        }
        var code = ORDER_CODE.matcher(question);
        var number = ORDER_NUMBER.matcher(normalized);
        String id = code.find() ? code.group(1) : number.find() ? number.group(1) : null;
        boolean personalOrder = id != null || Pattern.compile("\\b(don cua|don hang cua|ma don|ma van don|tra don|tra cuu don|theo doi don|order status)\\b").matcher(normalized).find();
        if (personalOrder) {
            if (authenticatedEmail == null) return ChatReply.of("AUTH_REQUIRED", "Vui lòng đăng nhập để xem đơn của chính bạn.");
            if (id == null) return ChatReply.of("CLARIFY", "Bạn muốn tra cứu đơn nào? Hãy gửi mã SUNI ở trang Đơn của tôi.");
            try {
                return data.order(Long.parseLong(id), authenticatedEmail);
            } catch (NumberFormatException ex) {
                return ChatReply.of("NOT_FOUND", "Không tìm thấy đơn trong tài khoản của bạn.");
            } catch (org.springframework.dao.DataAccessException ex) {
                return ChatReply.of("UNAVAILABLE", "Hiện chưa tra cứu được dữ liệu đơn hàng. Vui lòng thử lại sau.");
            }
        }
        if (Pattern.compile("\\b(chuyen tien|thanh toan giup|huy giup|dat giup)\\b").matcher(normalized).find()) {
            return ChatReply.of("REFUSED", "Chatbot chỉ tư vấn và tra cứu, chưa thực hiện đặt/hủy đơn hoặc chuyển tiền. Bạn có thể thao tác trên trang cửa hàng; hiện hỗ trợ COD.");
        }
        if (Pattern.compile("\\b(gia|ton kho|con hang|het hang|bao nhieu tien)\\b").matcher(normalized).find()) {
            try { return data.catalog(question); }
            catch (org.springframework.dao.DataAccessException ex) {
                return ChatReply.of("UNAVAILABLE", "Hiện chưa tra cứu được giá và tồn kho. Vui lòng thử lại sau.");
            }
        }
        if (!gemini.ready()) return ChatReply.of("NOT_CONFIGURED", "Trợ lý AI chưa được cấu hình. Bạn vẫn có thể hỏi giá/tồn theo mã SKU hoặc tra cứu đơn SUNI sau đăng nhập.");
        // Minimize common personal identifiers voluntarily entered into a general knowledge question.
        String sanitized = question.replaceAll("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "[email đã ẩn]")
            .replaceAll("(?<!\\d)\\+?\\d[\\d .-]{7,}\\d(?!\\d)", "[số liên hệ đã ẩn]");
        try {
            var result = gemini.answer(sanitized, knowledge.search(sanitized));
            if (result.status().equals("OUT_OF_SCOPE")) return ChatReply.of("OUT_OF_SCOPE", "Mình hỗ trợ sản phẩm cà phê, dụng cụ pha chế và nghiệp vụ cửa hàng. Bạn có thể hỏi trong các chủ đề này.");
            return new ChatReply(result.status(), result.answer(), knowledge.citations(result.knowledgeIds()));
        } catch (GeminiClient.Unavailable ex) {
            return ChatReply.of("UNAVAILABLE", "Trợ lý AI hiện chưa trả lời được. Vui lòng thử lại sau; bạn vẫn có thể xem cửa hàng và tra cứu đơn.");
        }
    }
}
