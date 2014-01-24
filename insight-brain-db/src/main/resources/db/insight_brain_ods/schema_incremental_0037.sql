SET SCHEMA insight_brain_ods;

CREATE TABLE policy_tag (
  policy_tag_id varchar(50) NOT NULL,
  policy_id varchar(50) NOT NULL,
  tag_id varchar(50) NOT NULL,
  CONSTRAINT policy_tag_pk PRIMARY KEY (policy_tag_id),
  CONSTRAINT policy_tag_uk UNIQUE KEY (policy_id, tag_id),
  CONSTRAINT policy_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
);