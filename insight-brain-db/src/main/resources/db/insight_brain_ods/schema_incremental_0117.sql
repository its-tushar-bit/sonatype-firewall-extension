-- Since 1.46.0
SET SCHEMA insight_brain_ods;

CREATE INDEX membership_mapping_member_name_idx ON membership_mapping(member_name);
