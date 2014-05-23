-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation ADD COLUMN (
  action_type_id varchar(20),
  notifications CLOB
);
