package vn.sunicoffee.web;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.sunicoffee.domain.StockMovementType;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.StockForm;

@Controller
@RequestMapping("/admin/inventory")
public class InventoryController {
    private final InventoryService inventory;
    private final ProductRepository products;
    private final StockMovementRepository movements;
    public InventoryController(InventoryService i,ProductRepository p,StockMovementRepository m) {inventory=i;products=p;movements=m;}
    @GetMapping String view(@RequestParam(required=false) Long productId,@RequestParam(defaultValue="0") int page,HttpSession session,Model m) {
        String token=UUID.randomUUID().toString();session.setAttribute("stockToken",token);
        var f=new StockForm();f.setRequestToken(token);f.setProductId(productId);f.setType(StockMovementType.RECEIPT);f.setQuantity(1);m.addAttribute("form",f);
        populate(m,productId,page);return "admin/inventory";
    }
    @PostMapping String submit(@Valid @ModelAttribute("form") StockForm f,BindingResult result,Principal p,HttpSession session,Model m,RedirectAttributes flash) {
        if(!f.getRequestToken().equals(session.getAttribute("stockToken"))) result.reject("token","Phiên lập phiếu đã thay đổi. Hãy tải lại trang kho.");
        if(!result.hasErrors()) {
            try {inventory.manual(p.getName(),f);flash.addFlashAttribute("success","Đã ghi phiếu và cập nhật tồn kho.");return "redirect:/admin/inventory?productId="+f.getProductId();}
            catch(BusinessException e) {result.reject("stock",e.getMessage());}
            catch(DataIntegrityViolationException e) {result.reject("stock","Phiếu đã được xử lý. Hãy tải lại lịch sử kho.");}
        }
        populate(m,f.getProductId(),0);return "admin/inventory";
    }
    private void populate(Model m,Long productId,int page) {
        m.addAttribute("products",products.findAll(Sort.by("sku")));
        m.addAttribute("movements",movements.history(productId,PageRequest.of(Math.max(0,page),20,Sort.by(Sort.Direction.DESC,"id"))));
        m.addAttribute("selectedProduct",productId);
    }
}
