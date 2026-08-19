-- Since 1.81.0
ALTER TABLE user_viewed_product_notification ADD COLUMN realm_id varchar(50) NULL;

ALTER TABLE user_viewed_product_notification ADD COLUMN username_lowercase varchar(60) NULL;
UPDATE user_viewed_product_notification SET username_lowercase=LOWER(username);
ALTER TABLE user_viewed_product_notification ALTER COLUMN username_lowercase SET NOT NULL;

ALTER TABLE user_viewed_product_notification DROP CONSTRAINT notification_viewed_uk;
ALTER TABLE user_viewed_product_notification ADD CONSTRAINT notification_viewed_uk UNIQUE (notification_id, username_lowercase, realm_id);
