CREATE TABLE user
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100)              NOT NULL UNIQUE,
    password_hash VARCHAR(100)              NOT NULL,
    full_name     VARCHAR(100)              NOT NULL,
    role          ENUM ('CUSTOMER','ADMIN') NOT NULL,
    deleted_at    DATETIME                  NULL,
    created_at    DATETIME                  NOT NULL
);

CREATE TABLE category
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE product
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_type   VARCHAR(20)    NOT NULL,
    name           VARCHAR(150)   NOT NULL,
    description    TEXT           NULL,
    price          DECIMAL(10, 2) NOT NULL,
    stock_quantity INT            NOT NULL,
    image_url      VARCHAR(255)   NULL,
    weight_kg      DECIMAL(6, 2)  NULL,
    download_url   VARCHAR(255)   NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    deleted_at     DATETIME       NULL,
    created_at     DATETIME       NOT NULL
);

CREATE TABLE product_category
(
    category_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    PRIMARY KEY (category_id, product_id),
    INDEX idx_product_category_product (product_id),
    CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
);

CREATE TABLE cart_item
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
);

CREATE TABLE orders
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT                                                    NOT NULL,
    status           ENUM ('PENDING','PAID','SHIPPED','COMPLETED','CANCELLED') NOT NULL,
    total_amount     DECIMAL(10, 2)                                            NOT NULL,
    receiver_name    VARCHAR(100)                                              NOT NULL,
    receiver_phone   VARCHAR(20)                                               NOT NULL,
    shipping_address VARCHAR(255)                                              NOT NULL,
    created_at       DATETIME                                                  NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT
);

CREATE TABLE order_item
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    UNIQUE (order_id, product_id),
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT
);

CREATE TABLE payment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT                              NOT NULL UNIQUE,
    status     ENUM ('PENDING','SUCCESS','FAILED') NOT NULL,
    method     VARCHAR(20)                         NOT NULL,
    finish_at  DATETIME                            NULL,
    created_at DATETIME                            NOT NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);