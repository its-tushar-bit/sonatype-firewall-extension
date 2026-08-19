-- Since 1.183
-- SaaS Compatible
ALTER TABLE policy_waiver_reason ADD COLUMN IF NOT EXISTS sort_order INTEGER NULL;

DELETE FROM policy_waiver_reason WHERE type = 'system';
INSERT INTO policy_waiver_reason VALUES ('9b704ef5bc064fc29d7fe08a251ee9a6', 'system', 'Acknowledged violation', 0);
INSERT INTO policy_waiver_reason VALUES ('42069f58114f4df8b435a40a415d2835', 'system', 'Mitigated externally', 1);
INSERT INTO policy_waiver_reason VALUES ('39984de3d6e64f508df82b4cbfd72f70', 'system', 'No upgrade path', 2);
INSERT INTO policy_waiver_reason VALUES ('f6990a32cd8d4ea78853ca829d948927', 'system', 'Not exploitable', 3);
INSERT INTO policy_waiver_reason VALUES ('19bbf1a7d591497698ab3172461d971a', 'system', 'Not reachable', 4);
INSERT INTO policy_waiver_reason VALUES ('3446e70e60e04676a90131f3dea9bdb5', 'system', 'Researching', 5);
INSERT INTO policy_waiver_reason VALUES ('c991ef95866d4903ad0c6c217ac47c07', 'system', 'Other', 6);
