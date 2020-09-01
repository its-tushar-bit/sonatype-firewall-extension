UPDATE policy_evaluation
  SET initiator = 'unknown' WHERE initiator = '';

ALTER TABLE policy_evaluation
  ALTER COLUMN initiator DROP DEFAULT;
