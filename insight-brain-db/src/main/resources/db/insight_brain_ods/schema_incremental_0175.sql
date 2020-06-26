-- Since 1.95
ALTER TABLE ldap_connection
  ADD COLUMN referral_ignored boolean NOT NULL DEFAULT false;
