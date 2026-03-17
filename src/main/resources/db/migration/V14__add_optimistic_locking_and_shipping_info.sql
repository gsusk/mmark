ALTER TABLE minimarket.cart ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE minimarket.orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE minimarket.orders ADD COLUMN shipping_full_name VARCHAR(255);
ALTER TABLE minimarket.orders ADD COLUMN shipping_address_line VARCHAR(255);
ALTER TABLE minimarket.orders ADD COLUMN shipping_city VARCHAR(100);
ALTER TABLE minimarket.orders ADD COLUMN shipping_zip_code VARCHAR(20);
ALTER TABLE minimarket.orders ADD COLUMN shipping_country VARCHAR(100);
