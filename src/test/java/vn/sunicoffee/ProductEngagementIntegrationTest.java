package vn.sunicoffee;
import java.math.BigDecimal;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.CheckoutForm;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProductEngagementIntegrationTest {
    @Autowired ProductEngagementService engagement; @Autowired ProductImageService images;
    @Autowired ProductRepository products; @Autowired CategoryRepository categories;
    @Autowired UserService users; @Autowired CartService cart; @Autowired OrderService orders;
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    String email,other; Product product,second;
    @BeforeEach void setup() {
        String key=UUID.randomUUID().toString();email=key+"@example.test";other="other-"+email;
        users.create("Khách mua",email,"Password!2026",Role.CUSTOMER);users.create("Khách khác",other,"Password!2026",Role.CUSTOMER);
        var c=new Category();c.setCode(key.substring(0,8));c.setName("Cà phê "+key);c.setDescription("");c=categories.save(c);
        product=create(c,key);second=create(c,key+"-2");
    }
    Product create(Category c,String sku) {var p=new Product();p.setSku(sku);p.setName("Sản phẩm");p.setDescription("Mô tả");p.setCategory(c);p.setPrice(new BigDecimal("100000"));p.setStock(20);p.setSalesUnit("túi");p.setActive(true);return products.saveAndFlush(p);}
    void purchased() {
        cart.add(email,product.getId(),1);var f=new CheckoutForm();f.setCheckoutToken(UUID.randomUUID().toString());f.setRecipientName("Khách");f.setPhone("0900000000");f.setAddress("Địa chỉ kiểm thử");
        var o=orders.checkout(email,f);
        jdbc.update("UPDATE customer_orders SET status='COMPLETED' WHERE id=?",o.getId());
    }
    MockMultipartFile image(int color) throws Exception {
        var b=new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB);b.setRGB(0,0,color);var out=new ByteArrayOutputStream();ImageIO.write(b,"png",out);
        return new MockMultipartFile("image","photo.png","image/png",out.toByteArray());
    }
    @Test void favoritesArePrivateIdempotentAndRendered() throws Exception {
        engagement.favorite(email,product.getId(),true);engagement.favorite(email,product.getId(),true);
        assertThat(engagement.favorites(email,0).getTotalElements()).isEqualTo(1);
        assertThat(engagement.favorites(other,0)).isEmpty();
        mvc.perform(get("/favorites").with(user(email))).andExpect(status().isOk());
        mvc.perform(post("/products/"+product.getId()+"/favorite").with(user(email)).param("selected","false")).andExpect(status().isForbidden());
        mvc.perform(post("/favorites/"+product.getId()+"/remove").with(user(other)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(engagement.favorite(email,product.getId())).isTrue();
        product.setActive(false);products.flush();assertThat(engagement.favorites(email,0)).isEmpty();
        engagement.favorite(email,product.getId(),false);assertThat(engagement.favorite(email,product.getId())).isFalse();
    }
    @Test void reviewsRequireCompletedPurchaseValidateEscapeAndRemainOwned() throws Exception {
        assertThatThrownBy(()->engagement.review(email,product.getId(),5,"Tốt")).isInstanceOf(BusinessException.class);
        purchased();
        assertThatThrownBy(()->engagement.review(email,product.getId(),6,"Tốt")).isInstanceOf(BusinessException.class);
        engagement.review(email,product.getId(),5,"<script>alert(1)</script>");
        engagement.review(email,product.getId(),4,"<script>alert(1)</script>");
        assertThat(engagement.summary(product.getId()).count()).isEqualTo(1);
        assertThat(engagement.summary(product.getId()).average()).isEqualTo(4);
        mvc.perform(get("/products/"+product.getId()).with(user(email))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
        engagement.deleteReview(other,product.getId());assertThat(engagement.summary(product.getId()).count()).isEqualTo(1);
        engagement.deleteReview(email,product.getId());assertThat(engagement.summary(product.getId()).count()).isZero();
    }
    @Test void galleryPreservesCoverBoundsOwnershipAndPromotesOnDelete() throws Exception {
        images.add(product.getId(),image(0));images.add(product.getId(),image(255));
        var extra=images.gallery(product.getId()).getFirst();var old=images.get(product.getId(),true).content();var next=images.getGallery(product.getId(),extra.id(),true).content();
        images.cover(product.getId(),extra.id());assertThat(images.get(product.getId(),false).content()).isEqualTo(next);
        assertThat(images.getGallery(product.getId(),extra.id(),true).content()).isEqualTo(old);
        mvc.perform(get("/product-images/"+second.getId()+"/gallery/"+extra.id())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/products/"+product.getId()+"/gallery/"+extra.id()+"/remove").with(user(email)).with(csrf())).andExpect(status().isForbidden());
        for(int i=0;i<4;i++)images.add(product.getId(),image(i));
        assertThatThrownBy(()->images.add(product.getId(),image(10))).isInstanceOf(BusinessException.class);
        images.remove(product.getId());assertThat(images.gallery(product.getId())).hasSize(4);assertThat(product.isHasImage()).isTrue();
        product.setActive(false);products.flush();
        mvc.perform(get("/product-images/"+product.getId()+"/gallery/"+images.gallery(product.getId()).getFirst().id())).andExpect(status().isNotFound());
    }
    @Test void adminGalleryAndPublicProductRender() throws Exception {
        images.add(product.getId(),image(0));images.add(product.getId(),image(200));
        mvc.perform(get("/admin/products/"+product.getId()+"/edit").with(user(email).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/products/"+product.getId())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-gallery-image")));
        assertThatThrownBy(()->images.add(product.getId(),new MockMultipartFile("image","bad.svg","image/svg+xml","<svg/>".getBytes()))).isInstanceOf(BusinessException.class);
    }
}
