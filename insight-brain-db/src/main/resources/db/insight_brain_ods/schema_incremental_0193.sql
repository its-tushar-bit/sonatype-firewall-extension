-- Since 1.98
CREATE TABLE repository_migration (
  repository_migration_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  state varchar(50) NOT NULL,
  CONSTRAINT repository_migration_pk PRIMARY KEY (repository_migration_id),
  CONSTRAINT repository_migration_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_id_uk UNIQUE (repository_id)
);
