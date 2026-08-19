-- SaaS Compatible

UPDATE policy_waiver_reason SET sort_order = (sort_order + 1) * 10;

INSERT INTO policy_waiver_reason VALUES ('ab704ef5bc064fc29d7fe08a251ee9aa', 'system', 'Evaluating component', 15);
