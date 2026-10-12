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
    public record Message(@NotBlank @Size(max = 1500) String message) {}
    @GetMapping("/chat") public String page(Model model, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("aiReady", chat.ready());
        return "chat/index";
    }
    @PostMapping("/chat/messages") @ResponseBody
    public ResponseEntity<ChatReply> message(@Valid @RequestBody Message request, Principal principal, HttpServletRequest servlet) {
        if (!limit.allow(servlet.getRemoteAddr())) return ResponseEntity.status(429).header("Retry-After", "60")
            .cacheControl(CacheControl.noStore()).body(ChatReply.of("RATE_LIMITED", "Bạn gửi tin nhắn hơi nhanh. Vui lòng chờ khoảng một phút rồi thử lại."));
        var result = chat.answer(request.message().strip(), principal == null ? null : principal.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @GetMapping("/chat/sources/{id}") public String source(@PathVariable String id, Model model) {
        var source = knowledge.source(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("source", source);
        model.addAttribute("entries", knowledge.entriesForSource(id));
        return "chat/source";
    }
}
