package vn.sunicoffee;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.UserService;
import static org.assertj.core.api.Assertions.*;

/** Exercises real servlet multipart parsing and CSRF, not only MockMvc. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UploadHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired UserService accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;

    @Test void multipartUploadAndSizeErrorWorkThroughRealServer() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8).toUpperCase();
        String email="http-"+suffix+"@example.test";
        accounts.create("HTTP admin",email,"HttpTest!2026",Role.ADMIN);
        var c=new Category();c.setCode("HTTP-"+suffix);c.setName("HTTP "+suffix);c.setDescription("Test");c=categories.save(c);
        var p=new Product();p.setSku("HTTP-"+suffix);p.setName("HTTP image");p.setDescription("Test");p.setPrice(java.math.BigDecimal.TEN);p.setStock(0);p.setSalesUnit("unit");p.setActive(true);p.setCategory(c);p=products.save(p);
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
        String root="http://127.0.0.1:"+port;
        var login=client.send(HttpRequest.newBuilder(URI.create(root+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
        String token=csrf(login.body());
        var auth=client.send(HttpRequest.newBuilder(URI.create(root+"/login")).header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password=HttpTest%212026&_csrf="+URLEncoder.encode(token,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(auth.statusCode()).isEqualTo(302);
        var edit=client.send(HttpRequest.newBuilder(URI.create(root+"/admin/products/"+p.getId()+"/edit")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(edit.statusCode()).isEqualTo(200);token=csrf(edit.body());
        var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",image);
        var uploaded=upload(client,root,p.getId(),token,image.toByteArray());assertThat(uploaded.statusCode()).isEqualTo(302);
        var content=client.send(HttpRequest.newBuilder(URI.create(root+"/product-images/"+p.getId())).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(content.statusCode()).isEqualTo(200);assertThat(content.headers().firstValue("Content-Type").orElse("")).isEqualTo("image/png");
        var oversized=upload(client,root,p.getId(),token,new byte[5*1024*1024+1]);
        assertThat(oversized.statusCode()).isEqualTo(413);assertThat(oversized.body()).contains("5 MB");
    }
    private static String csrf(String html) {
        var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        if(!matcher.find()) throw new AssertionError("CSRF token missing from form");return matcher.group(1);
    }
    private HttpResponse<String> upload(HttpClient client,String root,Long id,String token,byte[] image) throws Exception {
        String boundary="SuniBoundary";var body=new ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(root+"/admin/products/"+id+"/image?_csrf="+URLEncoder.encode(token,StandardCharsets.UTF_8)))
            .timeout(Duration.ofSeconds(15)).header("Content-Type","multipart/form-data; boundary="+boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
}
