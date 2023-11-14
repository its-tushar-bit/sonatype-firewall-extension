-- since 1.170
-- SaaS Compatible

ALTER TABLE coordinate_security ALTER COLUMN ref_id TYPE varchar(255);
ALTER TABLE third_party_vulnerability ALTER COLUMN ref_id TYPE varchar(255);
ALTER TABLE vulnerability_exploitability ALTER COLUMN ref_id TYPE varchar(255);
