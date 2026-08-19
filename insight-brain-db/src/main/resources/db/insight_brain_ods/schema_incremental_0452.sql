-- SaaS Compatible
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS last_renewal_old_expiry_date timestamp NULL;
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS last_renewed_by varchar(210) NULL;
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS last_renewed_at timestamp NULL;
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS last_renewal_comment varchar(1000) NULL;
ALTER TABLE policy_waiver ADD COLUMN IF NOT EXISTS last_renewal_reason_id varchar(50) NULL;
