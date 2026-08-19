-- Since 1.13.0
alter table license alter column license_id varchar(1000) not null;
alter table license alter column shortDisplayName varchar(1000) not null;
alter table license alter column longDisplayName varchar(1000) default null;

alter table multi_license alter column multi_license_id varchar(1000) not null;
alter table multi_license alter column shortDisplayName varchar(1000) not null;
alter table multi_license alter column longDisplayName varchar(1000) default null;

alter table multi_license_license alter column multi_license_id varchar(1000) not null;
alter table multi_license_license alter column license_id varchar(1000) not null;
