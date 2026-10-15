package vn.sunicoffee.chat;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.OrderStatus;
import vn.sunicoffee.repository.*;

@Service
@Transactional(readOnly = true)
public class ChatDataService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final ProductRepository products;
    private final Environment environment;
    public ChatDataService(OrderRepository orders, UserRepository users, ProductRepository products, Environment environment) {
        this.orders = orders; this.users = users; this.products = products; this.environment = environment;
    }
    private static String money(java.math.BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + " VND";
    }
    public record AdviceContext(List<KnowledgeBase.Entry> entries, Map<String, ChatReply.Citation> citations, boolean demonstration, ChatReply demoMachineReply) {}
    public AdviceContext adviceContext(AdviceNeeds needs) {
        var budget = needs.budgetVnd();
        var entries = new ArrayList<KnowledgeBase.Entry>();
        var citations = new LinkedHashMap<String, ChatReply.Citation>();
        var page = products.search("", null, false, PageRequest.of(0, 40));
        for (var p : page.getContent()) {
            String id = "STORE" + p.getId();
            String content = "Dữ liệu cửa hàng hiện tại. "
                + "SKU: " + p.getSku() + ". Danh mục: " + p.getCategory().getName()
                + ". Giá: " + money(p.getPrice()) + "/" + p.getSalesUnit() + ". Tồn: " + p.getStock()
                + ". Mô tả: " + p.getDescription()
                + ". Chưa có thông số ngoài mô tả. Không suy đoán tương thích Arabica, công suất hoặc chất lượng espresso từ tên sản phẩm."
                + (budget != null && p.getPrice().compareTo(budget) > 0 ? " VƯỢT NGÂN SÁCH khách đã nêu " + money(p.getPrice().subtract(budget)) + "; không được giới thiệu như lựa chọn trong ngân sách." : "")
                + (p.getStock() <= 0 ? " HẾT HÀNG; không đề xuất khách mua ngay." : "")
                + (page.hasNext() ? " Đây chỉ là một phần danh mục." : "");
            entries.add(new KnowledgeBase.Entry(id, p.getName(), content, List.of(id), "Danh mục công khai tại thời điểm hỏi"));
            citations.put(id, new ChatReply.Citation(id, p.getName(), "/products/" + p.getId()));
        }
        boolean demonstration = environment.matchesProfiles("demo") || page.getContent().stream().anyMatch(p -> p.getSku().startsWith("DEMO-"));
        ChatReply demoReply = null;
        // Demo items have no validated brewing specifications. Do not ask a model to invent suitability.
        if (demonstration && Set.of("pha may", "espresso").contains(needs.brewing()) && !needs.people().isBlank() && budget != null) {
            var machines = page.getContent().stream().filter(p -> KnowledgeBase.normalize(p.getCategory().getName()).contains("may pha")
                || KnowledgeBase.normalize(p.getName()).contains("may pha")).toList();
            var answer = new StringBuilder("Bạn cần máy pha cho " + needs.people() + " người, ngân sách " + money(budget)
                + (needs.beans().isBlank() ? "" : ", dùng hạt " + needs.beans()) + ".\nCác lựa chọn trong danh mục hiện tại:\n");
            var refs = new ArrayList<ChatReply.Citation>();
            var affordable = machines.stream().filter(p -> p.getStock() > 0 && p.getPrice().compareTo(budget) <= 0)
                .sorted(Comparator.comparing(vn.sunicoffee.domain.Product::getPrice)).limit(2).toList();
            for (var p : affordable) {
                answer.append("• ").append(p.getName()).append(": ").append(money(p.getPrice())).append(", còn hàng.\n");
                refs.add(citations.get("STORE" + p.getId()));
            }
            if (affordable.isEmpty()) answer.append("Chưa có máy còn hàng trong ngân sách ở phần danh mục đã tra.\n");
            machines.stream().filter(p -> p.getStock() > 0 && p.getPrice().compareTo(budget) > 0)
                .min(Comparator.comparing(vn.sunicoffee.domain.Product::getPrice)).ifPresent(p -> {
                    answer.append("• ").append(p.getName()).append(": ").append(money(p.getPrice()))
                        .append(", vượt ngân sách ").append(money(p.getPrice().subtract(budget))).append(".\n");
                    refs.add(citations.get("STORE" + p.getId()));
                });
            answer.append("Chưa có thông số xác nhận máy đáp ứng nhu cầu pha cho ").append(needs.people()).append(" người")
                .append(needs.beans().isBlank() ? "" : " với hạt " + needs.beans())
                .append(". Máy cầm tay không mặc nhiên là máy điện để bàn; các mẫu trên chỉ để tham khảo, chưa đủ căn cứ chốt mua.");
            demoReply = new ChatReply("INSUFFICIENT", answer.toString(), List.copyOf(refs));
        }
        return new AdviceContext(List.copyOf(entries), Map.copyOf(citations), demonstration, demoReply);
    }
    public ChatReply order(long id, String email) {
        if (email == null || users.findByEmail(email).filter(u -> u.isEnabled()).isEmpty()) {
            return ChatReply.of("AUTH_REQUIRED", "Vui lòng đăng nhập để xem đơn của chính bạn.");
        }
        // Scope the SQL query to the session identity, including for administrator accounts.
        var found = orders.findForChat(id, email);
        if (found.isEmpty()) return ChatReply.of("NOT_FOUND", "Không tìm thấy đơn trong tài khoản của bạn.");
        var o = found.get();
        var answer = new StringBuilder("Đơn SUNI-" + o.getId() + ": " + o.getStatus().getLabel() + ".\n");
        o.getItems().forEach(i -> answer.append(i.getQuantity()).append(" × ").append(i.getProductName())
            .append(" — ").append(money(i.getUnitPrice())).append("/sản phẩm\n"));
        answer.append("Tiền hàng: ").append(money(o.getMerchandiseTotal())).append(". Phí giao: ")
            .append(money(o.getShippingFee())).append(". Tổng thanh toán: ").append(money(o.getTotal())).append(".\n");
        if (!o.getTrackingCode().isBlank()) answer.append("Đơn vị vận chuyển: ").append(o.getCarrier())
            .append(". Mã vận đơn: ").append(o.getTrackingCode()).append(". Chưa có dữ liệu vị trí thực tế hoặc ngày giao dự kiến.\n");
        if (o.getPaymentMethod() == vn.sunicoffee.domain.PaymentMethod.COD && o.getStatus() == OrderStatus.COMPLETED) answer.append(o.getCodSettledAt() == null
            ? "Chưa có ghi nhận đối soát COD; trạng thái hoàn thành không chứng minh cửa hàng đã nhận tiền.\n"
            : "Đã ghi nhận nhận đủ tiền COD.\n");
        if(o.getPaymentMethod()==vn.sunicoffee.domain.PaymentMethod.SEPAY) answer.append("Thanh toán QR SePay: ").append(o.getPaymentStatus().getLabel()).append(o.isPaymentSandbox()?" (Sandbox)":"").append(".\n");
        if (o.getStatus() == OrderStatus.PENDING && o.getReceivedAmount().signum()==0) answer.append("Đơn còn đủ điều kiện tự hủy trong Đơn của tôi. Chatbot chưa thực hiện hủy đơn.");
        else answer.append("Khách chỉ tự hủy khi đơn còn chờ xác nhận và chưa ghi nhận tiền chuyển khoản. Nếu đã chuyển tiền, hãy liên hệ cửa hàng. Chatbot chỉ tra cứu, không sửa đơn.");
        return new ChatReply("ORDER", answer.toString(), List.of(new ChatReply.Citation("ORDER", "Chi tiết đơn SUNI-" + o.getId(), "/orders/" + o.getId())));
    }
    public ChatReply catalog(String question) {
        String normalized = KnowledgeBase.normalize(question);
        // Manufacturer reference products are deliberately not mapped to synthetic SKUs.
        if (Pattern.compile("\\b(picopresso|wacaco|hario|v60|aeropress)\\b").matcher(normalized).find()) {
            return ChatReply.of("INSUFFICIENT", "Chưa có ánh xạ sản phẩm hãng này với danh mục cửa hàng để xác nhận giá hoặc tồn kho. Bạn có thể kiểm tra trong Cửa hàng hoặc cung cấp mã SKU.");
        }
        var skuMatcher = Pattern.compile("(?i)\\b[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+\\b").matcher(question);
        if (skuMatcher.find()) {
            return products.findBySku(skuMatcher.group().toUpperCase(Locale.ROOT)).filter(p -> p.isActive())
                .map(p -> productReply(List.of(p))).orElseGet(() -> ChatReply.of("INSUFFICIENT", "Không tìm thấy mã SKU đang bán trong danh mục hiện tại."));
        }
        var ignored = Set.of("gia", "ban", "con", "hang", "ton", "kho", "bao", "nhieu", "tien", "toi", "muon", "mua", "cho", "hoi", "cua", "hom", "nay", "khong", "co", "sunicoffee", "la");
        var terms = Arrays.stream(normalized.split(" +")).filter(t -> !ignored.contains(t)).toList();
        if (terms.isEmpty()) return ChatReply.of("CLARIFY", "Bạn muốn xem giá và tồn kho của sản phẩm nào? Hãy gửi tên hoặc mã SKU trong Cửa hàng.");
        // Bounded catalogue scan is sufficient for this small store; no private fields are exposed.
        var matches = products.search("", null, false, PageRequest.of(0, 200)).getContent().stream()
            .filter(p -> terms.stream().allMatch(t -> (" " + KnowledgeBase.normalize(p.getName()) + " ").contains(" " + t + " "))).limit(4).toList();
        if (matches.isEmpty()) return ChatReply.of("INSUFFICIENT", "Chưa tìm được sản phẩm phù hợp để xác nhận giá/tồn kho. Hãy gửi mã SKU hoặc tên đúng trong Cửa hàng.");
        return productReply(matches);
    }
    private ChatReply productReply(List<vn.sunicoffee.domain.Product> list) {
        var text = new StringBuilder("Thông tin sản phẩm tại thời điểm tra cứu:\n");
        var refs = new ArrayList<ChatReply.Citation>();
        list.forEach(p -> {
            text.append(p.getName()).append(" (").append(p.getSku()).append(") — ").append(money(p.getPrice()))
                .append("/").append(p.getSalesUnit()).append("; tồn ").append(p.getStock()).append(".\n");
            refs.add(new ChatReply.Citation(p.getSku(), p.getName(), "/products/" + p.getId()));
        });
        text.append("Giá và tồn kho sẽ được kiểm tra lại khi đặt hàng.");
        return new ChatReply("CATALOG", text.toString(), List.copyOf(refs));
    }
}
