package vn.sunicoffee.chat;

import java.util.regex.Pattern;
import java.util.*;
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
        return answer(question, authenticatedEmail, new ChatConversation());
    }
    public ChatReply answer(String question, String authenticatedEmail, ChatConversation conversation) {
        String normalized = KnowledgeBase.normalize(question);
        // Security and order routing run before any model call; no order reply becomes model history.
        if (Pattern.compile("\\b(mat khau|password|api key|toan bo don|tat ca don|khach khac|nguoi khac|xam nhap)\\b").matcher(normalized).find()) {
            conversation.clear();
            return ChatReply.of("REFUSED", "Mình không cung cấp dữ liệu riêng hoặc thông tin đăng nhập của khách hàng. Bạn chỉ có thể tra cứu đơn của chính mình sau đăng nhập.");
        }
        var code = ORDER_CODE.matcher(question);
        var number = ORDER_NUMBER.matcher(normalized);
        String id = code.find() ? code.group(1) : number.find() ? number.group(1) : null;
        boolean personalOrder = id != null || Pattern.compile("\\b(don cua|don hang cua|ma don|ma van don|tra don|tra cuu don|theo doi don|order status)\\b").matcher(normalized).find();
        if (personalOrder) {
            conversation.clear();
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
            conversation.clear();
            return ChatReply.of("REFUSED", "Chatbot chỉ tư vấn và tra cứu, chưa thực hiện đặt/hủy đơn hoặc chuyển tiền. Bạn có thể thao tác trên trang cửa hàng; hiện hỗ trợ COD.");
        }
        // Store policy is not established in the frozen v1 sources. A manufacturer policy
        // or a requested promise cannot become a SuniCoffee commitment, even if AI is down.
        boolean storePolicy=Pattern.compile("\\b(sunicoffee|cua hang)\\b").matcher(normalized).find()
            && Pattern.compile("\\b(bao hanh|doi tra|doi moi|hoan tien)\\b").matcher(normalized).find();
        if (storePolicy) {
            conversation.clear();
            return new ChatReply("INSUFFICIENT",
                "Chưa có chính sách bảo hành, đổi trả hoặc hoàn tiền riêng của SuniCoffee được xác minh để cam kết điều này. Bạn cần xác nhận trực tiếp với cửa hàng trước khi mua; chính sách của hãng không tự động là chính sách của cửa hàng.",
                knowledge.citations(List.of("NV20", "NV21")));
        }
        String previousQuestions = conversation.history().stream().map(ChatConversation.Turn::question).collect(java.util.stream.Collectors.joining("\n"));
        var needs = AdviceNeeds.from(conversation.history(), question);
        boolean advice = !AdviceNeeds.knowledgeQuestion(question) && (Pattern.compile("\\b(tu van|nen mua|goi y|nguoi moi|bat dau|ngan sach|chon mua)\\b")
            .matcher(KnowledgeBase.normalize(previousQuestions + " " + question)).find()
            || (!needs.brewing().isBlank() && needs.budgetVnd() != null));
        if (!advice && Pattern.compile("\\b(gia|ton kho|con hang|het hang|bao nhieu tien)\\b").matcher(normalized.replace("danh gia", "nhan xet").replace("gia tri", "value")).find()) {
            try { return data.catalog(question); }
            catch (org.springframework.dao.DataAccessException ex) {
                return ChatReply.of("UNAVAILABLE", "Hiện chưa tra cứu được giá và tồn kho. Vui lòng thử lại sau.");
            }
        }
        if (!gemini.ready()) return ChatReply.of("NOT_CONFIGURED", "Trợ lý AI chưa được cấu hình. Bạn vẫn có thể hỏi giá/tồn theo mã SKU hoặc tra cứu đơn SUNI sau đăng nhập.");
        // Minimize common personal identifiers voluntarily entered into a general knowledge question.
        String sanitized = ChatPrivacy.redact(question);
        try {
            var context = new ArrayList<>(knowledge.search(AdviceNeeds.knowledgeQuestion(question) ? sanitized : previousQuestions + "\n" + sanitized));
            Map<String, ChatReply.Citation> storeCitations = Map.of();
            boolean demoAdvice = false;
            if (advice) {
                try {
                    var catalog = data.adviceContext(AdviceNeeds.from(conversation.history(), sanitized));
                    var newNeeds = AdviceNeeds.from(List.of(), sanitized);
                    boolean preferenceReply = !newNeeds.brewing().isBlank() || !newNeeds.people().isBlank()
                        || newNeeds.budgetVnd() != null || !newNeeds.beans().isBlank();
                    if (catalog.demoMachineReply() != null && preferenceReply && AdviceNeeds.preferenceStatement(sanitized)) {
                        conversation.remember(sanitized, catalog.demoMachineReply().answer());
                        return catalog.demoMachineReply();
                    }
                    context.addAll(catalog.entries()); storeCitations = catalog.citations(); demoAdvice = catalog.demonstration();
                } catch (org.springframework.dao.DataAccessException ex) {
                    return ChatReply.of("UNAVAILABLE", "Hiện chưa đọc được danh mục để tư vấn chính xác. Vui lòng thử lại sau.");
                }
            }
            var history = conversation.history();
            var result = history.isEmpty() ? gemini.answer(sanitized, context) : gemini.answer(sanitized, context, history);
            if (result.status().equals("RESEARCH")) {
                if (!ChatPrivacy.publicResearch(sanitized) || result.researchQuery() == null
                        || !ChatPrivacy.publicResearch(result.researchQuery())) {
                    return ChatReply.of("INSUFFICIENT", "Chưa có dữ liệu đã xác minh cho nội dung này. Giá, tồn kho, đơn hàng và chính sách SuniCoffee cần được kiểm tra trong cửa hàng.");
                }
                try {
                    var researched = gemini.research(result.researchQuery());
                    if (researched.status().equals("ANSWER")) conversation.remember(sanitized, researched.answer());
                    return researched;
                } catch (GeminiClient.Unavailable ex) {
                    String reason = ex.failure() == GeminiClient.Failure.QUOTA_EXCEEDED
                        ? "Google đang giới hạn lượt tra cứu web."
                        : "Dịch vụ tra cứu web đang gián đoạn.";
                    return ChatReply.of("UNAVAILABLE", reason + " Mình chưa kiểm chứng được thông tin để trả lời câu hỏi này. Bạn có thể thử lại sau.");
                }
            }
            if (result.status().equals("OUT_OF_SCOPE")) {
                conversation.clear();
                return ChatReply.of("OUT_OF_SCOPE", "Mình hỗ trợ sản phẩm cà phê, dụng cụ pha chế và nghiệp vụ cửa hàng. Bạn có thể hỏi trong các chủ đề này.");
            }
            var citations = new ArrayList<>(knowledge.citations(result.knowledgeIds()));
            for (String ref : new LinkedHashSet<>(result.knowledgeIds())) if (storeCitations.containsKey(ref)) citations.add(storeCitations.get(ref));
            // A clarification does not need an irrelevant policy link below it.
            if (result.status().equals("CLARIFY")) citations.clear();
            String answer = result.answer();
            if (demoAdvice && result.knowledgeIds().stream().anyMatch(storeCitations::containsKey) && !result.status().equals("CLARIFY")) answer = "Danh mục tư vấn là dữ liệu minh họa, không phải sản phẩm/giá/tồn thực tế.\n\n" + answer;
            conversation.remember(sanitized, answer);
            return new ChatReply(result.status(), answer, List.copyOf(citations));
        } catch (GeminiClient.Unavailable ex) {
            String message = switch (ex.failure()) {
                case QUOTA_EXCEEDED -> "Dịch vụ AI đang hết hạn mức hoặc nhận quá nhiều yêu cầu. Vui lòng thử lại sau; bạn vẫn có thể tra cứu đơn.";
                case KEY_REJECTED, MODEL_UNAVAILABLE, INVALID_REQUEST -> "Dịch vụ AI đang gặp lỗi cấu hình. Cửa hàng cần kiểm tra lại kết nối Gemini; bạn vẫn có thể tra cứu đơn.";
                case BUSY -> "Trợ lý AI đang bận. Vui lòng chờ một chút rồi gửi lại câu hỏi.";
                case NETWORK -> "Kết nối đến dịch vụ AI đang gián đoạn. Vui lòng thử lại sau.";
                default -> "Trợ lý AI hiện chưa trả lời được. Vui lòng thử lại sau; bạn vẫn có thể xem cửa hàng và tra cứu đơn.";
            };
            return ChatReply.of("UNAVAILABLE", message);
        }
    }
}
