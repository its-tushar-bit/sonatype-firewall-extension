-- Since 1.15.0
SET SCHEMA insight_brain_ods;

UPDATE role SET description='Can manage any element of the CLM Server. Can evaluate Applications and individual components.' WHERE role_id='1b92fae3e55a411793a091fb821c422d';
UPDATE role SET description='Can manage any element within the scope of their assigned Organization or Application. Can evaluate Applications and individual components.' WHERE role_id='1cddabf7fdaa47d6833454af10e0a3ef';
UPDATE role SET description='Can view any element within the scope of their assigned Organization or Application. Can evaluate individual components.' WHERE role_id='1da70fae1fd54d6cb7999871ebdb9a36';
UPDATE role SET description='Can submit Applications for evaluation and retrieve corresponding summary-level results within the scope of their assigned Organization or Application.' WHERE role_id='2cb71b3468d649789163ea2e212b541e';
UPDATE role SET description='Can submit individual components for evaluation and retrieve corresponding summary-level results within the scope of their assigned Organization or Application.' WHERE role_id='90c7c98683b4471cb77a916744540bcc';
