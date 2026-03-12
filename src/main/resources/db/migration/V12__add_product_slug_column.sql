ALTER TABLE product ADD COLUMN slug VARCHAR(300) NULL;

UPDATE product SET slug = LOWER(REPLACE(REPLACE(REPLACE(name, ' ', '-'), '.', ''), ',', ''));

ALTER TABLE product MODIFY COLUMN slug VARCHAR(300) NOT NULL;

CREATE UNIQUE INDEX idx_product_slug ON product(slug);
