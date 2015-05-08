-- Since 1.15.0
SET SCHEMA insight_brain_ods;

UPDATE role SET name='System Administrator', name_lowercase_no_whitespace='systemadministrator', description='Can manage LDAP, product license, users and other global aspects.' WHERE role_id='1b92fae3e55a411793a091fb821c422d';
