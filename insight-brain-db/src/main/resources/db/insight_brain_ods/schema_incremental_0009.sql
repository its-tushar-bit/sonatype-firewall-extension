SET SCHEMA insight_brain_ods;

INSERT INTO license_threat_group_license (license_threat_group_license_id, application_id, license_threat_group_id, license_id)
   SELECT CONCAT('10253775601246dea', ROWNUM()), ltg.application_id, ltg.license_threat_group_id, l.license_id
      FROM license_threat_group ltg, 
           (SELECT c1 AS license_id FROM (VALUES 'Adobe','Adobe-AFM','Adobe-EULA','ATT','Beerware','Boost','DOCBOOK','Dyade','HP-DEC','IETF','IETF-style','ImageMagick','InfoSeek','IPTC','ISO-8879','Java-Multi-Corp','Java-WSDL-Policy','Java-WSDL-Schema','JPEG','MS-IP','OSD','RedHat','RSA-Security','Sun','Sun-BCLA','Sun-EULA','Sun-IP','Sun-Non-commercial','Sun-Restricted','Sun-TM','Unicode','Xerox')) l
      WHERE ltg.name='Non Standard';

