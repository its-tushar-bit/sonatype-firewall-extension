-- Since 1.67.0
CREATE TABLE migration_tracker (
    id varchar(100) NOT NULL,
    version int NULL,
    configuration varchar(100) NULL,
    CONSTRAINT id PRIMARY KEY (id)
);
