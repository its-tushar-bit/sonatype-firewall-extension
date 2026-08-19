-- Since 1.67.0
CREATE TABLE migration_tracker (
    migration_tracker_id varchar(100) NOT NULL,
    version int NULL,
    configuration varchar(1000) NULL,
    CONSTRAINT migration_tracker_pk PRIMARY KEY (migration_tracker_id)
);
