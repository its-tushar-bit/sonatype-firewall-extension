-- Since 1.183
-- SaaS Compatible
BEGIN;
    CREATE TABLE policy_violation_constraint_facts (
           policy_violation_constraint_facts_id VARCHAR(20) NOT NULL,
           constraint_facts_json text NOT NULL,
           CONSTRAINT policy_violation_constraint_facts_pk PRIMARY KEY (policy_violation_constraint_facts_id)
    );

    ALTER TABLE policy_violation ADD COLUMN constraint_facts_id VARCHAR(20);
    ALTER TABLE policy_violation
        ADD CONSTRAINT policy_violation_constraint_facts_id_fk
            FOREIGN KEY (constraint_facts_id) REFERENCES policy_violation_constraint_facts (policy_violation_constraint_facts_id);
    ALTER TABLE policy_violation ALTER COLUMN constraint_facts_json DROP NOT NULL;

    ALTER TABLE repository_policy_violation ADD COLUMN constraint_facts_id VARCHAR(20);
    ALTER TABLE repository_policy_violation
        ADD CONSTRAINT repository_policy_violation_constraint_facts_id_fk
            FOREIGN KEY (constraint_facts_id) REFERENCES policy_violation_constraint_facts (policy_violation_constraint_facts_id);
    ALTER TABLE repository_policy_violation ALTER COLUMN constraint_facts_json DROP NOT NULL;
COMMIT;
