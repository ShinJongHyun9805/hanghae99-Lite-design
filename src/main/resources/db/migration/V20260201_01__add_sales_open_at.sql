ALTER TABLE concert_schedule
    ADD COLUMN sales_open_at DATETIME NULL;

UPDATE concert_schedule
   SET sales_open_at = concert_date
 WHERE sales_open_at IS NULL;

ALTER TABLE concert_schedule
    MODIFY COLUMN sales_open_at DATETIME NOT NULL;
