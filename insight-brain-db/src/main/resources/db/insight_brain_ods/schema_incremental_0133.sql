-- Since 1.67.0
CREATE TABLE migration_tracker (
    applied_migration varchar(100) NOT NULL,
    CONSTRAINT applied_migration PRIMARY KEY (applied_migration)
);
