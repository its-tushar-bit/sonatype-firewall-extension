-- Since 1.12
SET SCHEMA insight_brain_ods;

CREATE TABLE last_policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  CONSTRAINT last_policy_evaluation_PK PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT last_policy_evaluation_uk UNIQUE KEY (application_id, stage_type_id),
  CONSTRAINT last_policy_evaluation_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT last_policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);


--initial load
INSERT INTO last_policy_evaluation(policy_evaluation_id, application_id, stage_type_id)
  SELECT e.policy_evaluation_id, e.application_id, e.stage_type_id
  FROM (
    SELECT application_id, stage_type_id, max(time) as maxtime
    FROM policy_evaluation
      WHERE for_obsolete_scan = false
      GROUP BY application_id, stage_type_id
  ) AS x
  INNER JOIN policy_evaluation AS e
    ON e.application_id = x.application_id AND e.stage_type_id = x.stage_type_id AND e.time = maxtime
    WHERE e.for_obsolete_scan = false;
