-- Since 1.98
ALTER TABLE policy_evaluation
  ADD COLUMN initiator varchar(50) DEFAULT '' NOT NULL;
