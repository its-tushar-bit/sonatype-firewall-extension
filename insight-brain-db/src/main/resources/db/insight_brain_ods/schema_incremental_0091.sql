-- Since 1.17.0
ALTER TABLE repository ADD COLUMN quarantine_enabled bool DEFAULT false NOT NULL;
