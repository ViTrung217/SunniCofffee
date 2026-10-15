CREATE TABLE product_favorites (
 user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL,
 PRIMARY KEY(user_id,product_id),
 FOREIGN KEY(user_id) REFERENCES app_users(id) ON DELETE CASCADE,
 FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE TABLE product_reviews (
 user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, rating INT NOT NULL,
 comment VARCHAR(1500) NOT NULL, updated_at TIMESTAMP NOT NULL,
 PRIMARY KEY(user_id,product_id),
 CHECK(rating BETWEEN 1 AND 5),
 FOREIGN KEY(user_id) REFERENCES app_users(id) ON DELETE CASCADE,
 FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE TABLE product_gallery (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT NOT NULL,
 media_type VARCHAR(30) NOT NULL, content LONGBLOB NOT NULL,
 FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE INDEX idx_gallery_product ON product_gallery(product_id,id);
CREATE INDEX idx_reviews_product ON product_reviews(product_id,updated_at);
