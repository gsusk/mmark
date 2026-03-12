UPDATE product
SET slug = REGEXP_REPLACE(
    REGEXP_REPLACE(
        REPLACE(REPLACE(REPLACE(REPLACE(slug, '/', '-'), '.', '-'), ',', '-'), ':', '-'),
        '[^a-z0-9-]', '-'
    ),
    '-{2,}', '-'
);

UPDATE product
SET slug = TRIM(BOTH '-' FROM slug);
