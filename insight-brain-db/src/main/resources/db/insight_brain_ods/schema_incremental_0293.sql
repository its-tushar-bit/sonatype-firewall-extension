-- Since 1.161
ALTER TABLE repository ADD COLUMN last_manual_configure_time timestamp DEFAULT NULL;
