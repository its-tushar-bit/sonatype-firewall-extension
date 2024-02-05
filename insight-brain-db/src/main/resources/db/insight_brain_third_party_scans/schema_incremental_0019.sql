-- since 1.172
-- SaaS Compatible

ALTER TABLE coordinate_security ALTER COLUMN attack_vector TYPE varchar(255);

ALTER TABLE third_party_vulnerability ALTER COLUMN attack_vector TYPE varchar(255);
