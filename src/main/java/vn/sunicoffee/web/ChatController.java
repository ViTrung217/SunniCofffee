package vn.sunicoffee.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.chat.*;

@Controller
public class ChatController {
    private final ChatService chat;
    private final KnowledgeBase knowledge;
    private final ChatRateLimit limit;
    public ChatController(ChatService chat, KnowledgeBase knowledge, ChatRateLimit limit) {
        this.chat = chat; this.knowledge = knowledge; this.limit = limit;
    }
    public record Message(@NotBlank @Size(max = 1500) String message,
            @Pattern(regexp="[a-zA-Z0-9-]{1,64}") String conversationId) {}
    private static final class Conversations {
        final String identity;
        final java.util.LinkedHashMap<String, ChatConversation> chats = new java.util.LinkedHashMap<>();
        Conversations(String identity) { this.identity = identity; }
    }
    @GetMapping("/chat") public String page(@RequestParam(defaultValue="false") boolean widget, Model model, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("aiReady", chat.ready());
        model.addAttribute("embedded", widget);
        model.addAttribute("chatFullPage", true);
        return "chat/index";
    }
    @PostMapping("/chat/messages") @ResponseBody
    public ResponseEntity<ChatReply> message(@Valid @RequestBody Message request, Principal principal, HttpServletRequest servlet) {
        if (!limit.allow(servlet.getRemoteAddr())) return ResponseEntity.status(429).header("Retry-After", "60")
            .cacheControl(CacheControl.noStore()).body(ChatReply.of("RATE_LIMITED", "Bạn gửi tin nhắn hơi nhanh. Vui lòng chờ khoảng một phút rồi thử lại."));
        String identity = principal == null ? null : principal.getName();
        var session = servlet.getSession();
        ChatConversation conversation;
        synchronized (session) {
            var state = (Conversations) session.getAttribute("chat.conversations");
            if (state == null || !java.util.Objects.equals(state.identity, identity)) {
                state = new Conversations(identity);
                session.setAttribute("chat.conversations", state);
            }
            String id = request.conversationId() == null ? "default" : request.conversationId();
            conversation = state.chats.computeIfAbsent(id, ignored -> new ChatConversation());
            while (state.chats.size() > 4) state.chats.remove(state.chats.keySet().iterator().next());
        }
        ChatReply result;
        synchronized (conversation) {
            result = chat.answer(request.message().strip(), identity, conversation);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @GetMapping("/chat/sources/{id}") public String source(@PathVariable String id, Model model) {
        var source = knowledge.source(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("source", source);
        model.addAttribute("entries", knowledge.entriesForSource(id));
        return "chat/source";
    }
}
