-- Since 1.157
ALTER TABLE vulnerability_custom_detail
DROP CONSTRAINT vulnerability_custom_detail_fk;

ALTER TABLE vulnerability_custom_detail RENAME COLUMN application_tag_id to tag_id;

ALTER TABLE vulnerability_custom_detail ADD CONSTRAINT vulnerability_custom_detail_fk FOREIGN KEY (tag_id) REFERENCES tag(tag_id);
