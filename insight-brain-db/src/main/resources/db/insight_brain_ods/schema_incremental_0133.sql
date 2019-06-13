-- Since 1.67.0
CREATE TABLE migration_tracker (
    id varchar(100) NOT NULL,
    version int NULL,
    configuration varchar(1000) NULL,
    CONSTRAINT migration_tracker_pk PRIMARY KEY (id)
);
