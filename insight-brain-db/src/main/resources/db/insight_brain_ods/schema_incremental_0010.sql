SET SCHEMA insight_brain_ods;

CREATE TABLE hash_gav (
  hash_gav_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  group_id varchar(100) NOT NULL,
  artifact_id varchar(100) NOT NULL,
  version varchar(100) NOT NULL,
  extension varchar(50),
  classifier varchar(50),
  CONSTRAINT hash_gav_id PRIMARY KEY (hash_gav_id),
  CONSTRAINT hash_gav_hash_uk UNIQUE KEY (hash),
  CONSTRAINT hash_gav_gavec_uk UNIQUE KEY (group_id, artifact_id, version, extension, classifier)
);
