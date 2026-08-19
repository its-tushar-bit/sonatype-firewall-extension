-- Since 1.120
ALTER TABLE policy_violation ADD COLUMN grandfather_applied bool;
UPDATE policy_violation SET grandfather_applied = true;
ALTER TABLE policy_violation ALTER COLUMN grandfather_applied SET NOT NULL;
ALTER TABLE policy_violation ALTER COLUMN grandfather_applied SET DEFAULT false;
