-- Since 1.165
-- SaaS Compatible

UPDATE system_configuration_property
SET name='TRANSITIVE_SOLVER_ENABLED', value = 'false'
WHERE name = 'TRANSITIVE_SOLVER_DISABLED' AND value = 'true';

UPDATE system_configuration_property
SET name='TRANSITIVE_SOLVER_ENABLED', value = 'true'
WHERE name = 'TRANSITIVE_SOLVER_DISABLED' AND value = 'false';
