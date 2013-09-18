SET SCHEMA insight_brain_ods;

ALTER TABLE user ALTER COLUMN password_hash RENAME TO password;
