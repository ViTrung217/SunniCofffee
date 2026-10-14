package vn.sunicoffee.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.support.SupportService;

@Controller
public class SupportController {
    private final SupportService support;
    public SupportController(SupportService support) { this.support=support; }
    public record Send(@NotBlank @Size(max=1500) String content,
        @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}") String requestToken) {}
    public record Read(@Positive long throughId) {}
    public record State(@NotNull Boolean closed) {}
    @ModelAttribute void privatePage(HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    @GetMapping("/support") String customer(@RequestParam(defaultValue="false") boolean widget,Model model) {
        model.addAttribute("embedded",widget);model.addAttribute("supportAdmin",false);model.addAttribute("chatFullPage",true);
        model.addAttribute("endpoint","/support");return "support/conversation";
    }
    @GetMapping("/admin/support") String inbox(Principal p,@RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="all") String state,@RequestParam(defaultValue="0") int page,Model model) {
        validInbox(q,state,page);model.addAttribute("inbox",support.inbox(p.getName(),q,state,page));
        model.addAttribute("q",q);model.addAttribute("state",state);model.addAttribute("page",page);return "support/inbox";
    }
    @GetMapping("/admin/support/threads") @ResponseBody SupportService.Inbox threads(Principal p,@RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="all") String state,@RequestParam(defaultValue="0") int page) {
        validInbox(q,state,page);return support.inbox(p.getName(),q,state,page);
    }
    @GetMapping("/admin/support/{id}") String admin(Principal p,@PathVariable long id,Model model) {
        var data=support.messages(p.getName(),id,true,0,0);
        model.addAttribute("conversation",data.conversation());model.addAttribute("embedded",false);model.addAttribute("supportAdmin",true);
        model.addAttribute("chatFullPage",true);model.addAttribute("endpoint","/admin/support/"+id);return "support/conversation";
    }
    @GetMapping("/support/messages") @ResponseBody SupportService.Messages customerMessages(Principal p,@RequestParam(defaultValue="0") long after,@RequestParam(defaultValue="0") long before) {
        validRange(after,before);return support.messages(p.getName(),null,false,after,before);
    }
    @GetMapping("/admin/support/{id}/messages") @ResponseBody SupportService.Messages adminMessages(Principal p,@PathVariable long id,@RequestParam(defaultValue="0") long after,@RequestParam(defaultValue="0") long before) {
        validRange(after,before);return support.messages(p.getName(),id,true,after,before);
    }
    @PostMapping("/support/messages") @ResponseBody SupportService.Message customerSend(Principal p,@Valid @RequestBody Send form) {
        return support.send(p.getName(),null,false,form.content(),form.requestToken());
    }
    @PostMapping("/admin/support/{id}/messages") @ResponseBody SupportService.Message adminSend(Principal p,@PathVariable long id,@Valid @RequestBody Send form) {
        return support.send(p.getName(),id,true,form.content(),form.requestToken());
    }
    @PostMapping("/support/read") @ResponseBody void customerRead(Principal p,@Valid @RequestBody Read form) { support.read(p.getName(),null,false,form.throughId()); }
    @PostMapping("/admin/support/{id}/read") @ResponseBody void adminRead(Principal p,@PathVariable long id,@Valid @RequestBody Read form) { support.read(p.getName(),id,true,form.throughId()); }
    @PostMapping("/admin/support/{id}/state") @ResponseBody void state(Principal p,@PathVariable long id,@Valid @RequestBody State form) { support.close(p.getName(),id,form.closed()); }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    @ResponseBody org.springframework.http.ResponseEntity<java.util.Map<String,String>> invalidMessage() {
        // Avoid the default resolver logging rejected private message text.
        return org.springframework.http.ResponseEntity.badRequest().cacheControl(org.springframework.http.CacheControl.noStore())
            .body(java.util.Map.of("error","Tin nhắn hoặc tham số không hợp lệ."));
    }
    private void validRange(long after,long before) {
        if(after<0 || before<0 || (after>0 && before>0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mốc tin nhắn không hợp lệ");
    }
    private void validInbox(String q,String state,int page) {
        if(q.length()>100 || page<0 || page>10000 || !java.util.Set.of("all","open","closed","unread").contains(state)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bộ lọc không hợp lệ");
    }
}
