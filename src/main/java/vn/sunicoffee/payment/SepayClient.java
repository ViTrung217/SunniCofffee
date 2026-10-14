package vn.sunicoffee.payment;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class SepayClient {
    public record Transfer(String id,String bankAccountId,String accountNumber,String va,String type,
            BigDecimal amountIn,BigDecimal amountOut,String content,LocalDateTime at,String reference) {}
    public record Query(String code,String bankAccountId,boolean sandbox) {}
    public static class Unavailable extends RuntimeException {
        public Unavailable() { super("Chưa thể kiểm tra SePay. Vui lòng thử lại sau."); }
    }
    private final SepaySettings settings;
    private final RestClient client;
    private long nextRequestNanos;
    @org.springframework.beans.factory.annotation.Autowired
    public SepayClient(SepaySettings settings) { this(settings,builder()); }
    // Separate constructor permits an HTTP mock without real bank requests.
    SepayClient(SepaySettings settings,RestClient.Builder builder) { this.settings=settings;client=builder.build(); }
    private static RestClient.Builder builder() {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(8000);
        return RestClient.builder().requestFactory(factory);
    }
    private synchronized void reserve() {
        long now=System.nanoTime();
        if(now<nextRequestNanos) throw new Unavailable();
        nextRequestNanos=now+1_000_000_000L;
    }
    public List<Transfer> transfers(Query query) {
        if(!settings.ready() || query.sandbox()!=settings.sandbox()) throw new Unavailable();
        reserve();
        try {
            String base=query.sandbox()?"https://userapi-sandbox.sepay.vn/v2":"https://userapi.sepay.vn/v2";
            JsonNode root=client.get().uri(org.springframework.web.util.UriComponentsBuilder.fromUriString(base+"/transactions")
                .queryParam("q",query.code()).queryParam("bank_account_id",query.bankAccountId()).queryParam("transfer_type","in")
                .queryParam("per_page",100).queryParam("page",1).queryParam("timestamp_format","iso8601").build().encode().toUri())
                .header("Authorization","Bearer "+settings.apiToken()).retrieve().body(JsonNode.class);
            if(root==null || !root.path("status").asText().equals("success") || !root.path("data").isArray()
                || !root.path("meta").path("pagination").path("has_more").isBoolean()
                || root.path("meta").path("pagination").path("has_more").asBoolean() || root.path("data").size()>100) throw new Unavailable();
            List<Transfer> rows=new ArrayList<>();
            for(var row:root.path("data")) {
                rows.add(new Transfer(UUID.fromString(required(row,"id")).toString(),required(row,"bank_account_id"),required(row,"account_number"),row.path("va").asText(""),
                    required(row,"transfer_type"),new BigDecimal(required(row,"amount_in")).setScale(0),new BigDecimal(required(row,"amount_out")).setScale(0),
                    required(row,"transaction_content"),OffsetDateTime.parse(required(row,"transaction_date")).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime(),
                    required(row,"reference_number")));
            }
            return List.copyOf(rows);
        } catch(Exception ex) {
            // Never log provider bodies, API tokens, or unrelated bank transactions.
            throw new Unavailable();
        }
    }
    private static String required(JsonNode node,String key) {
        if(node.path(key).isMissingNode() || node.path(key).isNull()) throw new Unavailable();
        return node.path(key).asText();
    }
}
