ALTER TABLE category
    ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE category c
SET status = EXISTS (
    SELECT 1
    FROM item i
    WHERE i.category_id = c.id
);
