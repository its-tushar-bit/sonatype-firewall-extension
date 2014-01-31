SET SCHEMA insight_brain_ods;

ALTER TABLE policy_waiver
  ADD CONSTRAINT policy_waiver_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id);

ALTER TABLE policy_tag
  ADD CONSTRAINT policy_tag_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id);
  