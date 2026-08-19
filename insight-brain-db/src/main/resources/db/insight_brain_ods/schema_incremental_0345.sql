-- Since 1.181
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS policy_waiver_reason (
  waiver_reason_id varchar(50) NOT NULL,
  type varchar(50) NOT NULL DEFAULT 'system',
  reason_text varchar(60) NOT NULL,
  CONSTRAINT policy_waiver_reason_pk PRIMARY KEY (waiver_reason_id),
  CONSTRAINT policy_waiver_reason_uk UNIQUE (reason_text)
);

-- The foreign key constraint is not SaaS compatible and has to be added in a later deployment
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS waiver_reason_id varchar(50) NULL;

INSERT INTO policy_waiver_reason VALUES ('9b704ef5bc064fc29d7fe08a251ee9a6', 'system', 'Acknowledged violation');
INSERT INTO policy_waiver_reason VALUES ('42069f58114f4df8b435a40a415d2835', 'system', 'Mitigated externally');
INSERT INTO policy_waiver_reason VALUES ('39984de3d6e64f508df82b4cbfd72f70', 'system', 'No upgrade path');
INSERT INTO policy_waiver_reason VALUES ('f6990a32cd8d4ea78853ca829d948927', 'system', 'Not exploitable');
INSERT INTO policy_waiver_reason VALUES ('3446e70e60e04676a90131f3dea9bdb5', 'system', 'Researching');
INSERT INTO policy_waiver_reason VALUES ('c991ef95866d4903ad0c6c217ac47c07', 'system', 'Other');
