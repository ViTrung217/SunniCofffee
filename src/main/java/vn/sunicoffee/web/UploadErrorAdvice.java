package vn.sunicoffee.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;

@ControllerAdvice
public class UploadErrorAdvice {
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class) @ResponseStatus(HttpStatus.CONFLICT)
    String busy(Model m) {
        m.addAttribute("status",409);m.addAttribute("uploadError","Dữ liệu đang được xử lý bởi yêu cầu khác. Hãy tải lại trang và thử lại.");return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String tooLarge(Model m) {m.addAttribute("status",413);m.addAttribute("uploadError","Ảnh không được vượt quá 5 MB. Hãy giảm kích thước rồi tải lại.");return "error";}
}
