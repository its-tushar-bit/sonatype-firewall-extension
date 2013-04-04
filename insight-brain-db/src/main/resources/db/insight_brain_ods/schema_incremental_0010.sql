SET SCHEMA insight_brain_ods;

CREATE TABLE application_profile_policy (
  application_profile_policy_id varchar(50) NOT NULL,
  application_profile_id varchar(50) NOT NULL,
  policy_id varchar(50) NOT NULL,
  CONSTRAINT application_profile_policy_pk PRIMARY KEY (application_profile_policy_id),
  CONSTRAINT application_profile_policy_uk UNIQUE KEY (application_profile_id, policy_id),
  CONSTRAINT application_profile_policy_profile_fk FOREIGN KEY (application_profile_id) REFERENCES application_profile(application_profile_id)
);
