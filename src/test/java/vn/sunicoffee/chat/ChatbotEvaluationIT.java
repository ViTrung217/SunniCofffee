package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Opt-in, paid provider evaluation. No Spring context, live database or real orders. */
@EnabledIfSystemProperty(named="chatbot.eval.live", matches="true")
class ChatbotEvaluationIT {
    static final String OWNER="eval-a@example.test";
    final ObjectMapper mapper=new ObjectMapper();

    @Test void collectFrozenThirtyQuestionsThreeTimes() throws Exception {
        var baseline=new ProcessBuilder("python3", "scripts/verify-chatbot-baseline.py").inheritIO().start();
        assertThat(baseline.waitFor()).as("Frozen baseline must remain intact").isZero();
        var config=new Properties();
        Path local=Path.of(".runtime/chatbot.properties");
        if (Files.exists(local)) try (var reader=Files.newBufferedReader(local,StandardCharsets.UTF_8)) { config.load(reader); }
        String key=System.getenv().getOrDefault("GEMINI_API_KEY", config.getProperty("app.chat.gemini-api-key", ""));
        assertThat(!key.isBlank()).as("Configure GEMINI_API_KEY locally; never pass it on the command line").isTrue();
        String model=System.getenv().getOrDefault("GEMINI_MODEL", config.getProperty("app.chat.model", "gemini-3.1-flash-lite"));
        var knowledge=new KnowledgeBase(mapper);
        var gemini=new RecordingGemini(mapper,key,model);
        var cases=mapper.readTree(Path.of("evaluation/chatbot/v1/questions.json").toFile()).path("cases");
        var fixture=mapper.readTree(Path.of("evaluation/chatbot/v1/fixtures.json").toFile());
        Path output=Path.of(System.getProperty("chatbot.eval.output", "evaluation/results/"+Instant.now().toString().replace(':','-')));
        assertThat(Files.exists(output)).as("Use a new output directory; never overwrite a run").isFalse();
        Files.createDirectories(output);
        var metadata=new LinkedHashMap<String,Object>();
        metadata.put("started_at", Instant.now().toString()); metadata.put("model",model);
        metadata.put("application_version","1.0.0"); metadata.put("evaluation_version","v1");
        metadata.put("backend","isolated repository fixtures; real ChatService, ChatDataService, KnowledgeBase and GeminiClient");
        metadata.put("session_simulation","server principal supplied from frozen scenario; expired/public -> null; HTTP authorization tested separately");
        metadata.put("sampling", Map.of("temperature","provider default (not explicitly configured)","seed","not supplied", "maxOutputTokens",2048,"retries","at most one quick HTTP 503; wait 250 ms; first attempt must finish within 5 s"));
        var hashes=new LinkedHashMap<String,String>();
        for (String file : List.of("evaluation/chatbot/v1/LOCK.json", "src/main/resources/chat/system-prompt.txt",
                "src/main/java/vn/sunicoffee/chat/ChatService.java", "src/main/java/vn/sunicoffee/chat/ChatDataService.java",
                "src/main/java/vn/sunicoffee/chat/KnowledgeBase.java", "src/main/java/vn/sunicoffee/chat/GeminiClient.java")) {
            hashes.put(file, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(file)))));
        }
        metadata.put("sha256",hashes);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("metadata.json").toFile(),metadata);
        var results=new ArrayList<Map<String,Object>>();
        for (int repeat=1; repeat<=3; repeat++) for (JsonNode c : cases) {
            String scenario=c.path("scenario").asText();
            var state=fixture.path("scenarios").path(scenario);
            String principal=state.path("authenticated").asBoolean() && state.path("session_valid").asBoolean() ? OWNER : null;
            var users=mock(UserRepository.class); var orders=mock(OrderRepository.class); var products=mock(ProductRepository.class);
            var user=new AppUser(); user.setEmail(OWNER); user.setEnabled(true);
            when(users.findByEmail(OWNER)).thenReturn(Optional.of(user));
            when(products.search(anyString(),nullable(Long.class),eq(false),any())).thenReturn(Page.empty());
            var trace=new ArrayList<Map<String,Object>>();
            when(orders.findForChat(anyLong(),anyString())).thenAnswer(call -> {
                long id=call.getArgument(0); String identity=call.getArgument(1);
                var event=new LinkedHashMap<String,Object>(); event.put("tool","findForChat");
                event.put("id","SUNI-"+id); event.put("principal",identity.equals(OWNER)?"EVAL-A":"UNEXPECTED");
                trace.add(event);
                if (state.path("order_service").asText().equals("unavailable")) {
                    event.put("result","UNAVAILABLE"); throw new DataAccessResourceFailureException("synthetic fixture outage");
                }
                for (JsonNode order : fixture.path("orders")) {
                    if (order.path("id").asText().equals("SUNI-"+id) && identity.equals(OWNER) && order.path("owner").asText().equals("EVAL-A")) {
                        event.put("result","FOUND"); return Optional.of(order(order));
                    }
                }
                event.put("result","NOT_FOUND"); return Optional.empty();
            });
            var service=new ChatService(knowledge,gemini,new ChatDataService(orders,users,products,new MockEnvironment()));
            gemini.reset(); long start=System.nanoTime();
            var reply=service.answer(c.path("question").asText(),principal,new ChatConversation());
            long elapsed=(System.nanoTime()-start)/1_000_000;
            var row=new LinkedHashMap<String,Object>();
            row.put("id",c.path("id").asText()); row.put("repeat",repeat); row.put("group",c.path("group").asText());
            row.put("scenario",scenario); row.put("question",c.path("question").asText()); row.put("reply",reply);
            row.put("elapsed_ms",elapsed); row.put("order_tool_trace",trace); row.put("gemini_calls",gemini.calls);
            row.put("gemini_http_attempts",gemini.httpAttempts);
            row.put("retrieved_ids",List.copyOf(gemini.retrieved)); row.put("cited_knowledge_ids",List.copyOf(gemini.cited));
            row.put("repository_methods",mockingDetails(orders).getInvocations().stream().map(i -> i.getMethod().getName()).toList());
            row.put("score",null); row.put("grading_reason",null); row.put("grader",null);
            results.add(row);
            mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("responses.json").toFile(),results);
            System.out.printf("EVAL %s repeat=%d status=%s ms=%d%n",c.path("id").asText(),repeat,reply.status(),elapsed);
            // Security checks inspect calls as well as the answer; quality is graded separately.
            if (principal==null) assertThat(trace).isEmpty();
            assertThat(mockingDetails(orders).getInvocations()).allMatch(i -> i.getMethod().getName().equals("findForChat"));
            if (c.path("group").asText().equals("du_lieu_don_hang")) assertThat(gemini.calls).isZero();
            assertThat(reply.answer()).doesNotContain("SECRET-B-TRACK","Sản phẩm riêng B","777000","777.000");
        }
        metadata.put("finished_at",Instant.now().toString()); metadata.put("responses",results.size());
        metadata.put("grading_status","PENDING — collection success is not a quality pass");
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("metadata.json").toFile(),metadata);
        assertThat(results).hasSize(90);
    }

    private CustomerOrder order(JsonNode node) {
        var order=new CustomerOrder(); ReflectionTestUtils.setField(order,"id",Long.parseLong(node.path("id").asText().substring(5)));
        order.setStatus(OrderStatus.valueOf(node.path("status").asText())); order.setTotal(node.path("total_vnd").decimalValue());
        order.setShippingFee(node.path("shipping_fee_vnd").decimalValue()); order.setCarrier(node.path("carrier").asText(""));
        order.setTrackingCode(node.path("tracking_code").asText(""));
        for (JsonNode item : node.path("items")) {
            var i=new OrderItem(); i.setProductName(item.path("name").asText()); i.setSku(item.path("sku").asText());
            i.setQuantity(item.path("quantity").asInt()); i.setUnitPrice(item.path("unit_price_vnd").decimalValue()); order.getItems().add(i);
        }
        return order;
    }

    static class RecordingGemini extends GeminiClient {
        int calls=0, httpAttempts=0; List<String> retrieved=List.of(), cited=List.of();
        RecordingGemini(ObjectMapper mapper,String key,String model) throws Exception { super(mapper,true,key,model); }
        void reset() { calls=0; httpAttempts=0; retrieved=List.of(); cited=List.of(); }
        @Override protected void onRequestAttempt() { httpAttempts++; }
        @Override public Answer answer(String question,List<KnowledgeBase.Entry> context,List<ChatConversation.Turn> history) {
            calls++; retrieved=context.stream().map(KnowledgeBase.Entry::id).toList();
            var response=super.answer(question,context,history); cited=response.knowledgeIds(); return response;
        }
    }
}
