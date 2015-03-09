-- Since 1.13.0
SET SCHEMA insight_brain_ods;

CREATE TABLE application_component_new (
  application_component_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  time datetime NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates stored in json format
  match_state_id varchar(20) NOT NULL,
  identification_source_id varchar(20) NOT NULL,
  proprietary bool DEFAULT false NOT NULL,
  pathnames CLOB -- the paths to the component that caused the policy violation, paths are new line delimited
);

INSERT INTO application_component_new
  SELECT application_component_id, application_id, stage_type_id, time, hash, 'maven' AS component_id_format, ('{"artifactId":"' || STRINGENCODE(artifact_id) || '","groupId":"' || STRINGENCODE(group_id) || '","version":"' || STRINGENCODE(version) || '"}') AS component_id_coordinates_json, match_state_id, identification_source_id, proprietary, pathnames FROM application_component  WHERE group_id IS NOT NULL;

INSERT INTO application_component_new
  SELECT application_component_id, application_id, stage_type_id, time, hash, NULL AS component_id_format, NULL AS component_id_coordinates_json, match_state_id, identification_source_id, proprietary, pathnames FROM application_component  WHERE group_id IS NULL;

DROP TABLE application_component;

ALTER TABLE application_component_new RENAME TO application_component;

ALTER TABLE application_component ADD CONSTRAINT application_component_pk PRIMARY KEY (application_component_id);
ALTER TABLE application_component ADD CONSTRAINT application_component_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id);
ALTER TABLE application_component ADD CONSTRAINT application_component_uk UNIQUE KEY (application_id, stage_type_id, hash);
CREATE INDEX application_component_hash_idx ON application_component(hash);
CREATE INDEX application_component_time_idx ON application_component(time);
