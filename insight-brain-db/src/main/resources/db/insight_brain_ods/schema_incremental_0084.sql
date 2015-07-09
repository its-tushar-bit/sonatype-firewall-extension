-- Since 1.16.0
SET SCHEMA insight_brain_ods;

UPDATE ldap_connection SET retry_delay=30 WHERE retry_delay=300;
