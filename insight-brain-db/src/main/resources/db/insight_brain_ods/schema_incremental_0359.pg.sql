-- Since 1.185
-- SaaS Compatible
-- H2 does not support functional indexes. This change is only required for the largest databases and the recommendation
-- is that those customers move to postgres
CREATE INDEX IF NOT EXISTS member_name_lower_idx ON membership_mapping(LOWER(member_name));
CREATE INDEX IF NOT EXISTS member_name_upper_idx ON membership_mapping(UPPER(member_name));
