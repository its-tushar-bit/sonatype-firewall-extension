-- Since 1.107
CREATE TABLE auto_unquarantine_policy_condition_type (
  condition_type_id varchar(100) NOT NULL, -- stores the id from ConditionType
  CONSTRAINT auto_unquarantine_policy_condition_type_pk PRIMARY KEY (condition_type_id)
);