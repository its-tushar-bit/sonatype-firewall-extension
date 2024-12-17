-- Since 1.186
-- SaaS Compatible
CREATE INDEX membership_mapping_role_id_member_type_member_name_idx ON membership_mapping (role_id, member_type, member_name);
