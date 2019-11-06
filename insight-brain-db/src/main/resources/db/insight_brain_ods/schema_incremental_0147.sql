-- since 1.77.0
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('0df19389f12947fabfa3028afb28eb26', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_AUTOMATIC_SCM_CONFIGURATION');
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('f488d35a40d24ba589ba14280c40fe04', 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED', 'false');

-- set it to true for customers that engaged in source control activities
update system_configuration_property
set value = case when (select count(1) from source_control) > 0 then 'true' else 'false' end
where name = 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED';
