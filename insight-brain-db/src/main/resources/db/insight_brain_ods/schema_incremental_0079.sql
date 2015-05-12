-- Since 1.15.0
SET SCHEMA insight_brain_ods;

-- Set final descriptions on roles
UPDATE role SET description='Manages CLM Server configuration and users.' WHERE role_id='1b92fae3e55a411793a091fb821c422d';
UPDATE role SET description='Manages all organizations, applications, policies, and policy violations.' WHERE role_id='b9646757e98e486da7d730025f5245f8';
UPDATE role SET description='Manages assigned organizations, applications, policies, and policy violations.' WHERE role_id='1cddabf7fdaa47d6833454af10e0a3ef';
UPDATE role SET description='Views all information for their assigned organization or application.' WHERE role_id='1da70fae1fd54d6cb7999871ebdb9a36';
UPDATE role SET description='Evaluates applications and views policy violation summary results.' WHERE role_id='2cb71b3468d649789163ea2e212b541e';
UPDATE role SET description='Evaluates individual components and views policy violation results for a specified application.' WHERE role_id='90c7c98683b4471cb77a916744540bcc';
