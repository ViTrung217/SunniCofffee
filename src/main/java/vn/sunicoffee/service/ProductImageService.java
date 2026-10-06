package vn.sunicoffee.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.repository.ProductRepository;

@Service
public class ProductImageService {
    public static final int MAX_BYTES=5*1024*1024;
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    public ProductImageService(JdbcTemplate j,ProductRepository p) {jdbc=j;products=p;}
    public record ImageData(byte[] content,String mediaType) {}
    @Transactional public void upload(Long id,MultipartFile file) {
        if(file==null || file.isEmpty()) throw new BusinessException("Vui lòng chọn một ảnh JPG hoặc PNG.");
        if(file.getSize()>MAX_BYTES) throw new BusinessException("Ảnh không được vượt quá 5 MB.");
        ImageData data=decode(file);
        var product=products.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        jdbc.update("DELETE FROM product_images WHERE product_id=?",id);
        jdbc.update("INSERT INTO product_images(product_id,media_type,content) VALUES (?,?,?)",id,data.mediaType(),data.content());
        product.setHasImage(true);
    }
    private ImageData decode(MultipartFile file) {
        try(var raw=file.getInputStream();var input=ImageIO.createImageInputStream(raw)) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new BusinessException("Tệp không phải ảnh JPG/PNG hợp lệ.");
            var reader=readers.next();
            try {
                reader.setInput(input,true,true);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!format.equals("jpeg") && !format.equals("png")) throw new BusinessException("Chỉ chấp nhận ảnh JPG hoặc PNG; không nhận SVG/GIF.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width>4096 || height>4096 || (long)width*height>16000000)
                    throw new BusinessException("Ảnh tối đa 4096 px mỗi chiều và 16 triệu điểm ảnh.");
                BufferedImage image=reader.read(0);
                var output=new ByteArrayOutputStream();
                if(!ImageIO.write(image,format,output)) throw new IOException("Cannot encode image");
                if(output.size()>MAX_BYTES) throw new BusinessException("Ảnh sau xử lý vượt 5 MB. Hãy giảm kích thước ảnh.");
                return new ImageData(output.toByteArray(),"image/"+format);
            } finally {reader.dispose();}
        } catch(IOException | IllegalArgumentException e) {throw new BusinessException("Không đọc được ảnh. Hãy chọn ảnh JPG/PNG khác.");}
    }
    @Transactional public void remove(Long id) {
        var product=products.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        jdbc.update("DELETE FROM product_images WHERE product_id=?",id);product.setHasImage(false);
    }
    @Transactional(readOnly=true) public ImageData get(Long id,boolean admin) {
        var product=products.findById(id).filter(p -> admin || p.isActive()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return jdbc.query("SELECT content,media_type FROM product_images WHERE product_id=?",(rs,n) -> new ImageData(rs.getBytes(1),rs.getString(2)),product.getId())
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
