-- MySQL 8 (also used by H2 in MySQL mode)

CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(64)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    total_amount    DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- Supports the background job (WHERE status = 'PENDING') and filtered listing
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
CREATE INDEX idx_orders_customer ON orders (customer_id);

CREATE TABLE order_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT         NOT NULL,
    product_id      VARCHAR(64)    NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    quantity        INT            NOT NULL,
    unit_price      DECIMAL(15, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_qty   CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
