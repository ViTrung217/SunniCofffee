package vn.sunicoffee.web;
import java.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import vn.sunicoffee.service.*;

@Controller
public class RevenueController {
    private final RevenueService revenue;
    public RevenueController(RevenueService r) {revenue=r;}
    @GetMapping("/admin/revenue") String report(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,Model m) {
        var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));if(from==null) from=today.withDayOfMonth(1);if(to==null) to=today;
        m.addAttribute("from",from);m.addAttribute("to",to);
        try {m.addAttribute("report",revenue.report(from,to));} catch(BusinessException e) {m.addAttribute("error",e.getMessage());}
        return "admin/revenue";
    }
}
