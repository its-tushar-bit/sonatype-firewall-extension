DELETE FROM multi_license_license where multi_license_id ='CECILL-1.1English';
DELETE FROM multi_license where multi_license_id ='CECILL-1.1English';
DELETE FROM license where license_id ='CECILL-1.1English';

INSERT INTO license (license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('CECILL-1.1','CECILL-1.1','CeCILL Free Software License Agreement v1.1',NULL,'http://www.spdx.org/licenses/CECILL-1.1');
INSERT INTO license (license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('NPL-1.0','NPL-1.0','Netscape Public License v1.0',NULL,'http://www.spdx.org/licenses/NPL-1.0');
INSERT INTO license (license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('NPL-1.1','NPL-1.1','Netscape Public License v1.1',NULL,'http://www.spdx.org/licenses/NPL-1.1');
INSERT INTO license (license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('OSL-2.1','OSL-2.1','Open Software License 2.1',NULL,'http://www.spdx.org/licenses/OSL-2.1');

INSERT INTO multi_license (multi_license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('CECILL-1.1','CECILL-1.1','CeCILL Free Software License Agreement v1.1',NULL,'http://www.spdx.org/licenses/CECILL-1.1');
INSERT INTO multi_license (multi_license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('NPL-1.0','NPL-1.0','Netscape Public License v1.0',NULL,'http://www.spdx.org/licenses/NPL-1.0');
INSERT INTO multi_license (multi_license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('NPL-1.1','NPL-1.1','Netscape Public License v1.1',NULL,'http://www.spdx.org/licenses/NPL-1.1');
INSERT INTO multi_license (multi_license_id, shortDisplayName, longDisplayName, description, licenseUrl) VALUES ('OSL-2.1','OSL-2.1','Open Software License 2.1',NULL,'http://www.spdx.org/licenses/OSL-2.1');

INSERT INTO multi_license_license (multi_license_id, license_id) VALUES ('CECILL-1.1','CECILL-1.1');
INSERT INTO multi_license_license (multi_license_id, license_id) VALUES ('NPL-1.0','NPL-1.0');
INSERT INTO multi_license_license (multi_license_id, license_id) VALUES ('NPL-1.1','NPL-1.1');
INSERT INTO multi_license_license (multi_license_id, license_id) VALUES ('OSL-2.1','OSL-2.1');

UPDATE license SET license_category_id = 'WEAKCOPYLEFT' where license_id = 'CECILL-1.1';
UPDATE license SET license_category_id = 'COPYLEFT' where license_id = 'OSL-2.1';
