SET SCHEMA TESTMIGRATE_H2SPECIFICSCRIPT;
CREATE TABLE schema_version_h2
(
    schema_version integer               NOT NULL,
    data_store_id  character varying(32) NOT NULL
)

