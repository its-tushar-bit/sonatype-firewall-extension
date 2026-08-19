-- since 1.162.0
-- SDEV-228 This table records the last timestamp of a user using IDE to initiate a policy evaluation
CREATE TABLE user_ide_policy_evaluation (
  user_ide_policy_evaluation_id varchar(50) NOT NULL,
  username varchar(200) NOT NULL,
  last_evaluation_time timestamp NOT NULL,
  CONSTRAINT user_ide_policy_evaluation_pk PRIMARY KEY (user_ide_policy_evaluation_id),
  CONSTRAINT username_uk UNIQUE (username)
);
