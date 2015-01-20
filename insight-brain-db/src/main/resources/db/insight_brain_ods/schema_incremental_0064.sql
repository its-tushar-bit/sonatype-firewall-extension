-- Since 1.13.0
SET SCHEMA insight_brain_ods;

alter table license_threat_group_license alter column license_id varchar(1000) not null;
alter table license_override alter column license_id varchar(1000) not null;
